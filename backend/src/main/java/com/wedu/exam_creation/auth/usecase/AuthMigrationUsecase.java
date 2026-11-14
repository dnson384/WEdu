package com.wedu.exam_creation.auth.usecase;

import com.wedu.exam_creation.auth.domain.entity.AuthMigrationChangeEntity;
import com.wedu.exam_creation.auth.domain.entity.AuthMigrationUserEntity;
import com.wedu.exam_creation.auth.domain.repository.IAuthMigrationRepository;
import com.wedu.exam_creation.common.util.EmailNormalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AuthMigrationUsecase {
    private final IAuthMigrationRepository repository;

    public AuthMigrationUsecase(IAuthMigrationRepository repository) {
        this.repository = repository;
    }

    public int migrate() {
        Set<String> emails = new HashSet<>();
        Set<String> subjects = new HashSet<>();
        List<AuthMigrationChangeEntity> changes = new ArrayList<>();
        for (AuthMigrationUserEntity user : repository.findUsers()) {
            if (user.getId() == null || user.getEmail() == null || user.getEmail().isBlank()) {
                throw new IllegalStateException(
                        "User has missing ID/email; resolve before migration (no writes performed)");
            }
            String email = EmailNormalizer.normalize(user.getEmail());
            if (!emails.add(email)) {
                throw new IllegalStateException(
                        "Duplicate normalized emails; resolve before migration (no writes performed)");
            }
            String subject = user.getGoogleSubject();
            if (subject != null && (subject.isBlank() || !subjects.add(subject))) {
                throw new IllegalStateException(
                        "Invalid/duplicate Google subject; resolve before migration (no writes performed)");
            }
            boolean hasGoogle = subject != null;
            boolean hasPassword =
                    user.getHashedPassword() != null && !user.getHashedPassword().isBlank();
            boolean local = "LOCAL".equals(user.getLoginMethod());
            if (("MIXED".equals(user.getLoginMethod()) && (!hasGoogle || !hasPassword))
                    || (local && hasGoogle && !hasPassword)) {
                throw new IllegalStateException(
                        "Inconsistent linked login credentials; resolve before migration (no writes performed)");
            }
            changes.add(
                    new AuthMigrationChangeEntity(
                            user.getId(),
                            email,
                            local && hasGoogle && hasPassword ? "MIXED" : null));
        }
        // Complete every preflight check before the first index or data write.
        repository.dropLegacyIndexes();
        repository.apply(changes);
        return changes.size();
    }
}
