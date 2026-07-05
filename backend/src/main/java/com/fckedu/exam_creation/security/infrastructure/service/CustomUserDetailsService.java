package com.fckedu.exam_creation.security.infrastructure.service;

import com.fckedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.fckedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.fckedu.exam_creation.user.usecase.UserService;
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
        CommonUserResponseAllDTO user = userService.findByEmail(email);

        return new CustomUserDetails(user);
    }
}
