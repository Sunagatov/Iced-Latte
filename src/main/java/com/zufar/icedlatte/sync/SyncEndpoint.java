package com.zufar.icedlatte.sync;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.zufar.icedlatte.security.api.CurrentUserProvider;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sync")
public class SyncEndpoint {
    private final SyncService sync;
    private final CurrentUserProvider users;

    @GetMapping("/{resource}")
    public SyncService.Snapshot read(@PathVariable String resource) {
        return sync.read(users.getUserId(), resource);
    }

    @PostMapping("/{resource}/operations")
    public SyncService.Result execute(@PathVariable String resource, @Valid @RequestBody SyncService.Command command) {
        return sync.execute(users.getUserId(), resource, command);
    }

    @GetMapping("/{resource}/operations/{operationId}")
    public SyncService.Result operation(@PathVariable String resource, @PathVariable UUID operationId) {
        return sync.operation(users.getUserId(), resource, operationId);
    }
}
