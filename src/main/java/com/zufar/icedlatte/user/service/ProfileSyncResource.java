package com.zufar.icedlatte.user.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.sync.SyncResource;
import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProfileSyncResource implements SyncResource {
    private final UserProfileService profiles;
    private final ObjectMapper mapper;

    @Override
    public String name() {
        return "profile";
    }

    @Override
    public Object snapshot(UUID userId) {
        return profiles.getProfile(userId);
    }

    @Override
    public List<Conflict> validate(UUID userId, String type, JsonNode payload) {
        try {
            if (!type.equals("update")) throw new IllegalArgumentException();
            var request = mapper.convertValue(payload, UpdateUserAccountRequest.class);
            PutUsersRequestValidator.validate(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPhoneNumber(),
                    request.getBirthDate(),
                    request.getAddress());
            return List.of();
        } catch (IllegalArgumentException | BadRequestException ex) {
            return List.of(new Conflict(null, "INVALID_PROFILE", "Review your profile fields before saving."));
        }
    }

    @Override
    public void apply(UUID userId, String type, JsonNode payload) {
        profiles.updateProfile(userId, mapper.convertValue(payload, UpdateUserAccountRequest.class));
    }
}
