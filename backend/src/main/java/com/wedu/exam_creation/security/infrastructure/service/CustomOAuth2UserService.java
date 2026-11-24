package com.wedu.exam_creation.security.infrastructure.service;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/** Loads verified Google identity only. Account creation happens after email OTP. */
@Service
public class CustomOAuth2UserService extends OidcUserService {
    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser user = super.loadUser(request);
        if (!Boolean.TRUE.equals(user.getEmailVerified())
                || user.getEmail() == null
                || user.getSubject() == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("unverified_email"));
        }
        return user;
    }
}
