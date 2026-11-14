package com.wedu.exam_creation.user.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.dto.user.mapper.UserCommonDTOMapper;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.notification.telegram.TelegramNotificationService;
import com.wedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.wedu.exam_creation.storage.service.S3Service;
import com.wedu.exam_creation.user.domain.entity.UserEntity;
import com.wedu.exam_creation.user.dto.mapper.UserDTOMapper;
import com.wedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;

class UserUsecaseGoogleTest {
    private final UserRepositoryImpl repo = mock(UserRepositoryImpl.class);
    private final UserUsecase usecase =
            new UserUsecase(
                    repo,
                    Mappers.getMapper(UserDTOMapper.class),
                    Mappers.getMapper(UserCommonDTOMapper.class),
                    mock(RefreshTokenService.class),
                    mock(S3Service.class),
                    mock(TelegramNotificationService.class));

    private UserEntity user(String method) {
        var user = new UserEntity();
        user.setId("teacher-id");
        user.setEmail("teacher@example.com");
        user.setRole("ROLE_TEACHER");
        user.setIsActive(true);
        user.setLoginMethod(method);
        user.setHashedPassword("GOOGLE".equals(method) ? "" : "bcrypt-hash");
        user.setAccountType("FREE");
        return user;
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOCAL", "MIXED"})
    void localCredentialAndConfirmedGoogleLinkBecomeMixed(String method) {
        var current = user(method);
        var linked = user("MIXED");
        linked.setGoogleSubject("sub");
        when(repo.findById("teacher-id")).thenReturn(current);
        when(repo.linkGoogle("teacher-id", "sub", "MIXED")).thenReturn(linked);
        var result = usecase.linkGoogle("teacher-id", "sub");
        assertThat(result.getLoginMethod()).isEqualTo("MIXED");
        assertThat(result.getHashedPassword()).isEqualTo("bcrypt-hash");
        verify(repo).linkGoogle("teacher-id", "sub", "MIXED");
    }

    @Test
    void googleOnlyWithoutLocalCredentialStaysGoogle() {
        var user = user("GOOGLE");
        when(repo.findById("teacher-id")).thenReturn(user);
        when(repo.linkGoogle("teacher-id", "sub", "GOOGLE")).thenReturn(user);
        assertThat(usecase.linkGoogle("teacher-id", "sub").getLoginMethod()).isEqualTo("GOOGLE");
        verify(repo, never()).linkGoogle(any(), any(), eq("MIXED"));
    }

    @Test
    void missingLocalPasswordCannotProduceMixed() {
        var user = user("LOCAL");
        user.setHashedPassword(null);
        when(repo.findById("teacher-id")).thenReturn(user);
        assertThatThrownBy(() -> usecase.linkGoogle("teacher-id", "sub"))
                .isInstanceOf(BadRequestException.class);
        verify(repo, never()).linkGoogle(any(), any(), any());
    }

    @Test
    void googleLinkIsCheckedOnlyAfterEmailLookup() {
        var user = user("MIXED");
        user.setGoogleSubject("sub");
        when(repo.findByEmail("teacher@example.com")).thenReturn(Optional.of(user));
        assertThat(usecase.hasGoogleLink("teacher@example.com", "sub")).isTrue();
        assertThat(usecase.hasGoogleLink("teacher@example.com", "another-sub")).isFalse();
        verify(repo, times(2)).findByEmail("teacher@example.com");
        verifyNoMoreInteractions(repo);
    }
}
