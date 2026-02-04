package com.github.dimitryivaniuta.booking.auth.repo;

import com.github.dimitryivaniuta.booking.auth.domain.RefreshTokenFamily;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for refresh token families. */
public interface RefreshTokenFamilyRepository extends JpaRepository<RefreshTokenFamily, UUID> {

  Optional<RefreshTokenFamily> findByUserIdAndDeviceId(UUID userId, String deviceId);

  List<RefreshTokenFamily> findAllByUserId(UUID userId);
}
