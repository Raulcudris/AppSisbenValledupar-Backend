package com.appsisben.backend.security;

import com.appsisben.backend.shared.api.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        /*
         * Si la solicitud no contiene un token Bearer,
         * se deja continuar.
         *
         * Spring Security determinará después si el endpoint
         * es público o requiere autenticación.
         */
        if (
                authorizationHeader == null
                        || !authorizationHeader.startsWith(BEARER_PREFIX)
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authorizationHeader
                .substring(BEARER_PREFIX.length())
                .trim();

        if (jwt.isEmpty()) {
            writeUnauthorizedResponse(
                    request,
                    response,
                    "El token de autenticación está vacío"
            );
            return;
        }

        try {
            authenticateRequest(jwt, request);
        } catch (
                JwtException
                | IllegalArgumentException
                | AuthenticationException ex
        ) {
            SecurityContextHolder.clearContext();

            writeUnauthorizedResponse(
                    request,
                    response,
                    "El token de autenticación es inválido o está vencido"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateRequest(
            String jwt,
            HttpServletRequest request
    ) {
        String username = jwtService.extractUsername(jwt);

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "El token no contiene un usuario válido"
            );
        }

        /*
         * Evita reemplazar una autenticación que ya haya sido
         * establecida por otro mecanismo.
         */
        if (
                SecurityContextHolder
                        .getContext()
                        .getAuthentication() != null
        ) {
            return;
        }

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(jwt, userDetails)) {
            throw new IllegalArgumentException(
                    "El token no es válido para el usuario"
            );
        }

        if (!isUserAccountValid(userDetails)) {
            throw new IllegalArgumentException(
                    "La cuenta del usuario no está habilitada"
            );
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource()
                        .buildDetails(request)
        );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);
    }

    private boolean isUserAccountValid(UserDetails userDetails) {
        return userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired();
    }

    private void writeUnauthorizedResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            String message
    ) throws IOException {

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        ApiErrorResponse body = ApiErrorResponse.of(
                "INVALID_TOKEN",
                message,
                request.getRequestURI()
        );

        objectMapper.writeValue(
                response.getOutputStream(),
                body
        );
    }
}