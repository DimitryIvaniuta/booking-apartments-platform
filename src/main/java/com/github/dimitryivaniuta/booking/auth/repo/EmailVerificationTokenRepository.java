package com.github.dimitryivaniuta.booking.auth.repo;

import com.github.dimitryivaniuta.booking.auth.domain.EmailVerificationToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for email verification tokens. */
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

  Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
}
