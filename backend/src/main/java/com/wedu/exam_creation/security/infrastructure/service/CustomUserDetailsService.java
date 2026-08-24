package com.wedu.exam_creation.security.infrastructure.service;

import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.user.usecase.UserService;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserService userService;

    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        return userService.findByEmail(email)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Tài khoản chưa tồn tại"));
    }
}
