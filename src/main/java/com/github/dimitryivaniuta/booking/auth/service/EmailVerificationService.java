package com.github.dimitryivaniuta.booking.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.booking.auth.domain.EmailVerificationToken;
import com.github.dimitryivaniuta.booking.auth.domain.UserAccount;
import com.github.dimitryivaniuta.booking.auth.repo.EmailVerificationTokenRepository;
import com.github.dimitryivaniuta.booking.auth.repo.UserAccountRepository;
import com.github.dimitryivaniuta.booking.outbox.OutboxMessage;
import com.github.dimitryivaniuta.booking.outbox.OutboxRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Email verification flow.
 *
 * <p>In production you would integrate with an email provider; this demo publishes an outbox event
 * containing the verification token.</p>
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private final EmailVerificationTokenRepository tokenRepo;
  private final UserAccountRepository userRepo;
  private final OutboxRepository outboxRepo;
  private final ObjectMapper objectMapper;
  private final AuthProperties props;

  private final SecureRandom random = new SecureRandom();

  /** Issues a verification token for a user and emits outbox event. */
  @Transactional
  public String issueFor(UserAccount user) {
    String tokenRaw = newToken();
    String hash = TokenHashing.sha256Hex(tokenRaw);
    EmailVerificationToken token = EmailVerificationToken.issue(user.getId(), hash, Instant.now().plus(props.verificationTtl()));
    tokenRepo.save(token);

    publishOutbox("EmailVerificationRequested", Map.of(
        "userId", user.getId().toString(),
        "email", user.getEmail(),
        "token", tokenRaw,
        "expiresAt", token.getExpiresAt().toString()
    ));
    return tokenRaw;
  }

  /** Verifies a token and enables the user. */
  @Transactional
  public void verify(String tokenRaw) {
    if (tokenRaw == null || tokenRaw.isBlank()) {
      throw new AuthBadRequestException("token is required");
    }
    String hash = TokenHashing.sha256Hex(tokenRaw);
    EmailVerificationToken token = tokenRepo.findByTokenHash(hash)
        .orElseThrow(() -> new AuthBadRequestException("Invalid token."));
    if (token.isUsed()) {
      throw new AuthBadRequestException("Token already used.");
    }
    if (token.getExpiresAt().isBefore(Instant.now())) {
      throw new AuthBadRequestException("Token expired.");
    }
    UserAccount user = userRepo.findById(token.getUserId())
        .orElseThrow(() -> new AuthNotFoundException("User not found."));

    user.markVerifiedAndEnabled();
    userRepo.save(user);

    token.markUsed();
    tokenRepo.save(token);

    publishOutbox("EmailVerified", Map.of(
        "userId", user.getId().toString(),
        "email", user.getEmail()
    ));
  }

  private void publishOutbox(String eventType, Map<String, Object> payload) {
    try {
      String json = objectMapper.writeValueAsString(payload);
      outboxRepo.save(new OutboxMessage(props.outboxTopic(), "Auth", UUID.randomUUID(), eventType, json));
    } catch (Exception e) {
      throw new AuthException("Failed to serialize auth outbox event", e);
    }
  }

  private String newToken() {
    byte[] buf = new byte[24];
    random.nextBytes(buf);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
  }
}
