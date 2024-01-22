package com.zufar.icedlatte.security.dto;

public record UserAuthenticationResponse(String token, String refreshToken) {}