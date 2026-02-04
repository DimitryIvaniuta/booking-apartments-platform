package com.github.dimitryivaniuta.booking.auth.repo;

import com.github.dimitryivaniuta.booking.auth.domain.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for roles. */
public interface RoleRepository extends JpaRepository<Role, UUID> {

  Optional<Role> findByName(String name);
}
