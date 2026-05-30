package com.zufar.icedlatte.security.jwt.filter;

record JwtAuthenticationFailure(String typeSlug, String title, String detail, int statusCode, String reasonCode) {}
