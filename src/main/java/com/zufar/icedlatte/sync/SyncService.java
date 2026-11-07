package com.zufar.icedlatte.sync;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zufar.icedlatte.common.sync.SyncResource;
import com.zufar.icedlatte.common.sync.SyncRevisionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SyncService {
    public record Command(
            @NotNull UUID operationId,
            @NotNull UUID expectedUserId,
            @NotNull @PositiveOrZero Long expectedRevision,
            @NotBlank String type,
            @NotNull Map<String, Object> payload) {}

    public record Snapshot(UUID userId, long revision, Object data) {}

    public record Result(
            UUID operationId,
            String outcome,
            String code,
            String message,
            List<SyncResource.Conflict> conflicts,
            long appliedRevision,
            Snapshot snapshot) {}

    private record Receipt(String hash, String outcome, String code, String message, String conflicts, long revision) {}

    private final List<SyncResource> resources;
    private final SyncRevisionService revisions;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    @Transactional
    public Snapshot read(UUID userId, String resource) {
        SyncResource handler = handler(resource);
        revisions.lock(userId, resource);
        return snapshot(userId, handler);
    }

    @Transactional
    public Result operation(UUID userId, String resource, UUID operationId) {
        SyncResource handler = handler(resource);
        revisions.lock(userId, resource);
        Receipt receipt = receipts(userId, resource, operationId).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operation not found"));
        return new Result(
                operationId,
                receipt.outcome(),
                receipt.code(),
                receipt.message(),
                decodeConflicts(receipt.conflicts()),
                receipt.revision(),
                snapshot(userId, handler));
    }

    @Transactional
    public Result execute(UUID userId, String resource, Command command) {
        if (!userId.equals(command.expectedUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Account changed. Refresh your session before saving.");
        }
        SyncResource handler = handler(resource);
        long revision = revisions.lock(userId, resource);
        JsonNode payload = mapper.valueToTree(command.payload());
        String hash = hash(command.type() + ":" + command.expectedRevision(), payload);
        var receipts = receipts(userId, resource, command.operationId());
        if (!receipts.isEmpty()) {
            Receipt receipt = receipts.getFirst();
            if (!receipt.hash().equals(hash))
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Operation ID cannot be reused with another payload.");
            return new Result(
                    command.operationId(),
                    receipt.outcome(),
                    receipt.code(),
                    receipt.message(),
                    decodeConflicts(receipt.conflicts()),
                    receipt.revision(),
                    snapshot(userId, handler));
        }

        List<SyncResource.Conflict> conflicts = command.expectedRevision() == revision
                ? handler.validate(userId, command.type(), payload)
                : List.of(new SyncResource.Conflict(
                        null, "REVISION_CONFLICT", "The collection changed. Review the latest data and try again."));
        boolean applied = conflicts.isEmpty();
        if (applied) handler.apply(userId, command.type(), payload);
        String code = applied ? "APPLIED" : conflicts.getFirst().code();
        String message = applied ? "Saved" : conflicts.getFirst().message();
        long finalRevision = revisions.current(userId, resource);
        jdbc.update(
                """
                INSERT INTO sync_operation (user_id, resource, operation_id, payload_hash, outcome, code, message, conflicts, applied_revision)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                userId,
                resource,
                command.operationId(),
                hash,
                applied ? "APPLIED" : "REJECTED",
                code,
                message,
                encode(conflicts),
                finalRevision);
        return new Result(
                command.operationId(),
                applied ? "APPLIED" : "REJECTED",
                code,
                message,
                conflicts,
                finalRevision,
                snapshot(userId, handler));
    }

    private List<Receipt> receipts(UUID userId, String resource, UUID operationId) {
        return jdbc.query(
                """
                SELECT payload_hash, outcome, code, message, conflicts, applied_revision
                FROM sync_operation WHERE user_id = ? AND resource = ? AND operation_id = ?
                """,
                (rs, row) -> new Receipt(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getLong(6)),
                userId,
                resource,
                operationId);
    }

    private SyncResource handler(String name) {
        return resources.stream()
                .filter(resource -> resource.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown collection"));
    }

    private Snapshot snapshot(UUID userId, SyncResource handler) {
        Object data = handler.snapshot(userId);
        return new Snapshot(userId, revisions.current(userId, handler.name()), data);
    }

    private String hash(String type, JsonNode payload) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256")
                            .digest((type + ":" + canonical(payload)).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private JsonNode canonical(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = mapper.createObjectNode();
            var names = new java.util.ArrayList<String>();
            node.fieldNames().forEachRemaining(names::add);
            names.stream().sorted().forEach(name -> sorted.set(name, canonical(node.get(name))));
            return sorted;
        }
        if (node.isArray()) {
            var sorted = mapper.createArrayNode();
            node.forEach(item -> sorted.add(canonical(item)));
            return sorted;
        }
        return node;
    }

    private String encode(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private List<SyncResource.Conflict> decodeConflicts(String json) {
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
