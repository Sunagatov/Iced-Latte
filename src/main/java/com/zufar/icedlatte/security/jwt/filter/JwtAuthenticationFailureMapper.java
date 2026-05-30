package com.zufar.icedlatte.security.jwt.filter;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.security.jwt.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.jwt.exception.JwtTokenException;
import com.zufar.icedlatte.security.signin.exception.InvalidCredentialsException;

import io.jsonwebtoken.ExpiredJwtException;

@Component
public class JwtAuthenticationFailureMapper {

    JwtAuthenticationFailure map(Exception exception) {
        return switch (exception) {
            case InvalidCredentialsException _ ->
                new JwtAuthenticationFailure(
                        ProblemType.INVALID_CREDENTIALS,
                        "Authentication failed",
                        "Authentication failed.",
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "INVALID_CREDENTIALS");
            case JwtTokenBlacklistedException _ ->
                new JwtAuthenticationFailure(
                        ProblemType.SESSION_EXPIRED,
                        "Session expired",
                        "Session expired. Please sign in again.",
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "TOKEN_REVOKED");
            case ExpiredJwtException _ ->
                new JwtAuthenticationFailure(
                        ProblemType.SESSION_EXPIRED,
                        "Session expired",
                        "Authentication token has expired.",
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "TOKEN_EXPIRED");
            case JwtTokenException _ ->
                new JwtAuthenticationFailure(
                        ProblemType.AUTH_FAILED,
                        "Authentication failed",
                        "Authentication failed.",
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "TOKEN_INVALID_FORMAT");
            case UsernameNotFoundException _ ->
                new JwtAuthenticationFailure(
                        ProblemType.AUTH_FAILED,
                        "Authentication failed",
                        "Authentication failed.",
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "USER_NOT_FOUND");
            default ->
                new JwtAuthenticationFailure(
                        ProblemType.INTERNAL_ERROR,
                        "Authentication error",
                        "An internal server error occurred.",
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "AUTH_INTERNAL_ERROR");
        };
    }
}
