package com.github.dimitryivaniuta.booking.auth.service;

import com.github.dimitryivaniuta.booking.auth.domain.UserAccount;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues access JWT tokens signed with server RSA key.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

  private final JwtEncoder encoder;
  private final AuthProperties props;

  /**
   * Creates a signed access token for the given user.
   *
   * @param user user
   * @return jwt
   */
  public Jwt issueAccessToken(UserAccount user) {
    Instant now = Instant.now();
    Instant exp = now.plus(props.accessTtl());
    List<String> roles = user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toList());

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("booking-apartments")
        .issuedAt(now)
        .expiresAt(exp)
        .subject(user.getId().toString())
        .id(UUID.randomUUID().toString())
        .claim("email", user.getEmail())
        .claim("roles", roles)
        .build();

    return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(() -> "RS256").build(), claims));
  }
}
