package com.hospital.integrity.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录用户（含角色与权限）
 */
@Data
public class LoginUser implements UserDetails {

    private Long userId;
    private String username;
    private String password;
    private String realName;
    private Long deptId;
    private String title;
    private boolean enabled = true;

    /** 角色标识集合，如 doctor/dept_admin/auditor */
    private Set<String> roleKeys = new HashSet<>();

    /** 权限标识集合（按钮级 perms） */
    private Set<String> perms = new HashSet<>();

    public Set<String> getRoles() {
        return roleKeys;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.addAll(roleKeys.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                .collect(Collectors.toSet()));
        authorities.addAll(perms.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet()));
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
