package com.appsisben.backend.security;

import com.appsisben.backend.modules.users.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleCode = user.getRole() != null ? user.getRole().getCodigo() : "";
        return List.of(new SimpleGrantedAuthority("ROLE_" + normalizeRole(roleCode)));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return Boolean.TRUE.equals(user.getActivo());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getActivo())
                && user.getRole() != null
                && Boolean.TRUE.equals(user.getRole().getActivo());
    }

    private String normalizeRole(String roleCode) {
        return roleCode == null ? "" : roleCode.trim().toUpperCase(Locale.ROOT);
    }
}
