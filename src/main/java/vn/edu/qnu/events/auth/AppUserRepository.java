package vn.edu.qnu.events.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
}

