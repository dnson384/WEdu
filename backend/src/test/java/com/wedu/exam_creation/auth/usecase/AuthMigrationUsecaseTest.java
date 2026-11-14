package com.wedu.exam_creation.auth.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.auth.domain.entity.AuthMigrationChangeEntity;
import com.wedu.exam_creation.auth.domain.entity.AuthMigrationUserEntity;
import com.wedu.exam_creation.auth.domain.repository.IAuthMigrationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

class AuthMigrationUsecaseTest {
    private final IAuthMigrationRepository repository = mock(IAuthMigrationRepository.class);
    private final AuthMigrationUsecase migration = new AuthMigrationUsecase(repository);

    private static AuthMigrationUserEntity user(
            String id, String email, String subject, String method, String password) {
        return new AuthMigrationUserEntity(id, email, subject, method, password);
    }

    @SuppressWarnings("unchecked")
    @Test
    void normalizesEmailAndUpgradesConfirmedLocalLinkAfterPreflight() {
        var legacy = user("teacher", " Teacher@Example.COM ", "sub", "LOCAL", "bcrypt-hash");
        when(repository.findUsers()).thenReturn(List.of(legacy));
        assertThat(migration.migrate()).isEqualTo(1);
        var changes = ArgumentCaptor.forClass(List.class);
        var order = inOrder(repository);
        order.verify(repository).findUsers();
        order.verify(repository).dropLegacyIndexes();
        order.verify(repository).apply(changes.capture());
        assertThat((List<AuthMigrationChangeEntity>) changes.getValue())
                .containsExactly(
                        new AuthMigrationChangeEntity("teacher", "teacher@example.com", "MIXED"));
        assertThat(legacy.getHashedPassword()).isEqualTo("bcrypt-hash");
        assertThat(legacy.getEmail()).isEqualTo(" Teacher@Example.COM ");
    }

    @SuppressWarnings("unchecked")
    @Test
    void localWithoutGoogleAndGoogleWithoutPasswordKeepTheirMethods() {
        when(repository.findUsers())
                .thenReturn(
                        List.of(
                                user("local", "LOCAL@example.com", null, "LOCAL", "hash"),
                                user("google", "GOOGLE@example.com", "sub", "GOOGLE", "")));
        migration.migrate();
        var changes = ArgumentCaptor.forClass(List.class);
        verify(repository).apply(changes.capture());
        assertThat((List<AuthMigrationChangeEntity>) changes.getValue())
                .containsExactly(
                        new AuthMigrationChangeEntity("local", "local@example.com", null),
                        new AuthMigrationChangeEntity("google", "google@example.com", null));
    }

    @Test
    void duplicateNormalizedEmailStopsBeforeAnyIndexOrDataWrite() {
        when(repository.findUsers())
                .thenReturn(
                        List.of(
                                user("a", "Teacher@example.com", null, "LOCAL", "hash"),
                                user("b", " teacher@example.com ", null, "LOCAL", "hash")));
        assertThatThrownBy(migration::migrate).hasMessageContaining("Duplicate normalized emails");
        verify(repository, never()).dropLegacyIndexes();
        verify(repository, never()).apply(any());
    }

    @Test
    void duplicateGoogleSubjectStopsBeforeAnyWrite() {
        when(repository.findUsers())
                .thenReturn(
                        List.of(
                                user("a", "a@example.com", "sub", "GOOGLE", ""),
                                user("b", "b@example.com", "sub", "GOOGLE", "")));
        assertThatThrownBy(migration::migrate)
                .hasMessageContaining("Invalid/duplicate Google subject");
        verify(repository, never()).dropLegacyIndexes();
        verify(repository, never()).apply(any());
    }

    @Test
    void blankGoogleSubjectStopsBeforeAnyWrite() {
        when(repository.findUsers())
                .thenReturn(List.of(user("a", "a@example.com", " ", "GOOGLE", "")));
        assertThatThrownBy(migration::migrate)
                .hasMessageContaining("Invalid/duplicate Google subject");
        verify(repository, never()).dropLegacyIndexes();
        verify(repository, never()).apply(any());
    }

    static Stream<AuthMigrationUserEntity> inconsistentCredentials() {
        return Stream.of(
                user("a", "a@example.com", "sub", "MIXED", ""),
                user("a", "a@example.com", null, "MIXED", "hash"),
                user("a", "a@example.com", "sub", "LOCAL", null));
    }

    @ParameterizedTest
    @MethodSource("inconsistentCredentials")
    void inconsistentCredentialsStopBeforeAnyWrite(AuthMigrationUserEntity user) {
        when(repository.findUsers()).thenReturn(List.of(user));
        assertThatThrownBy(migration::migrate)
                .hasMessageContaining("Inconsistent linked login credentials");
        verify(repository, never()).dropLegacyIndexes();
        verify(repository, never()).apply(any());
    }

    static Stream<AuthMigrationUserEntity> missingIdentity() {
        return Stream.of(
                user(null, "a@example.com", null, "LOCAL", "hash"),
                user("a", null, null, "LOCAL", "hash"),
                user("a", "  ", null, "LOCAL", "hash"));
    }

    @ParameterizedTest
    @MethodSource("missingIdentity")
    void missingIdOrEmailStopsBeforeAnyWrite(AuthMigrationUserEntity user) {
        when(repository.findUsers()).thenReturn(List.of(user));
        assertThatThrownBy(migration::migrate).hasMessageContaining("missing ID/email");
        verify(repository, never()).dropLegacyIndexes();
        verify(repository, never()).apply(any());
    }

    @Test
    void emptyDatabaseDoesNotCreateAnyAccount() {
        when(repository.findUsers()).thenReturn(List.of());
        assertThat(migration.migrate()).isZero();
        verify(repository).apply(List.of());
    }

    @Test
    void indexFailureAbortsBeforeUserUpdates() {
        when(repository.findUsers())
                .thenReturn(List.of(user("a", "a@example.com", null, "LOCAL", "hash")));
        doThrow(new IllegalStateException("database unavailable"))
                .when(repository)
                .dropLegacyIndexes();
        assertThatThrownBy(migration::migrate).hasMessage("database unavailable");
        verify(repository, never()).apply(any());
    }

    @Test
    void repeatedMigrationIsIdempotent() {
        List<AuthMigrationUserEntity> state =
                new ArrayList<>(List.of(user("a", " A@Example.COM ", "sub", "LOCAL", "hash")));
        IAuthMigrationRepository memory =
                new IAuthMigrationRepository() {
                    public List<AuthMigrationUserEntity> findUsers() {
                        return state;
                    }

                    public void dropLegacyIndexes() {}

                    public void apply(List<AuthMigrationChangeEntity> changes) {
                        for (var change : changes) {
                            var current =
                                    state.stream()
                                            .filter(user -> user.getId().equals(change.getId()))
                                            .findFirst()
                                            .orElseThrow();
                            current.setEmail(change.getEmail());
                            if (change.getLoginMethod() != null)
                                current.setLoginMethod(change.getLoginMethod());
                        }
                    }
                };
        var migration = new AuthMigrationUsecase(memory);
        assertThat(migration.migrate()).isEqualTo(1);
        assertThat(migration.migrate()).isEqualTo(1);
        assertThat(state.get(0).getEmail()).isEqualTo("a@example.com");
        assertThat(state.get(0).getLoginMethod()).isEqualTo("MIXED");
        assertThat(state.get(0).getHashedPassword()).isEqualTo("hash");
    }
}
