package com.zufar.icedlatte.sync;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.zufar.icedlatte.security.api.CurrentUserProvider;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sync")
public class SyncEndpoint implements com.zufar.icedlatte.openapi.sync.api.SyncApi {
    private final SyncService sync;
    private final CurrentUserProvider users;

    @Override
    @GetMapping("/{resource}")
    public ResponseEntity<SyncService.Snapshot> readSyncResource(@PathVariable String resource) {
        return ResponseEntity.ok(sync.read(users.getUserId(), resource));
    }

    @Override
    @PostMapping("/{resource}/operations")
    public ResponseEntity<SyncService.Result> executeSyncOperation(
            @PathVariable String resource, @Valid @RequestBody SyncService.Command command) {
        return ResponseEntity.ok(sync.execute(users.getUserId(), resource, command));
    }

    @Override
    @GetMapping("/{resource}/operations/{operationId}")
    public ResponseEntity<SyncService.Result> getSyncOperation(
            @PathVariable String resource, @PathVariable UUID operationId) {
        return ResponseEntity.ok(sync.operation(users.getUserId(), resource, operationId));
    }
}
