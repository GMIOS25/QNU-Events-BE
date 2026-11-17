package vn.edu.qnu.events.student;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OrgClassRepository extends JpaRepository<OrgClass, UUID> {
    Optional<OrgClass> findByCode(String code);
}

