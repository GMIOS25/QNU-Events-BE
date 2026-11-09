package vn.edu.qnu.events.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qnu.events.student.OrgClass;
import vn.edu.qnu.events.student.OrgClassRepository;

import java.util.Set;
import java.util.UUID;

@Component
@Profile({"dev", "demo"})
@Order(1)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final OrgClassRepository orgClassRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.demo-password:Demo@12345}")
    private String demoPassword;

    public DemoDataSeeder(
            OrgClassRepository orgClassRepository,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.orgClassRepository = orgClassRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Running DemoDataSeeder for dev/demo profile...");

        OrgClass class1 = orgClassRepository.findByCode("K49CNTT1")
                .orElseGet(() -> orgClassRepository.save(new OrgClass(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "K49CNTT1",
                        "K49 Công nghệ thông tin 1",
                        "Khoa Công nghệ Thông tin",
                        (short) 2024
                )));

        OrgClass class2 = orgClassRepository.findByCode("K49CNTT2")
                .orElseGet(() -> orgClassRepository.save(new OrgClass(
                        UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        "K49CNTT2",
                        "K49 Công nghệ thông tin 2",
                        "Khoa Công nghệ Thông tin",
                        (short) 2024
                )));

        String hash = passwordEncoder.encode(demoPassword);

        seedUser("owner01", hash, "Nguyễn Văn Chủ Sự Kiện", null, null, Set.of("EVENT_OWNER"));
        seedUser("owner02", hash, "Lê Văn Chủ Sự Kiện 2", null, null, Set.of("EVENT_OWNER"));
        seedUser("sv001", hash, "Trần Sinh Viên 1", "4901040001", class1.getId(), Set.of("STUDENT"));
        seedUser("sv002", hash, "Lê Sinh Viên 2", "4901040002", class1.getId(), Set.of("STUDENT"));
        seedUser("sv003", hash, "Phạm Sinh Viên 3", "4901040003", class2.getId(), Set.of("STUDENT"));
        seedUser("admin01", hash, "Quản Trị Viên Hệ Thống", null, null, Set.of("SYSTEM_ADMIN"));

        log.info("DemoDataSeeder completed successfully.");
    }

    private void seedUser(String loginId, String passwordHash, String fullName, String studentCode, UUID classId, Set<String> roles) {
        appUserRepository.findByLoginId(loginId).ifPresentOrElse(user -> {
            boolean dirty = false;
            if (roles != null && !roles.isEmpty() && (user.getRoles() == null || !user.getRoles().containsAll(roles))) {
                user.getRoles().clear();
                user.getRoles().addAll(roles);
                dirty = true;
            }
            if (classId != null && !classId.equals(user.getClassId())) {
                user.setClassId(classId);
                dirty = true;
            }
            if (dirty) {
                appUserRepository.save(user);
            }
        }, () -> {
            AppUser user = new AppUser(UUID.randomUUID(), loginId, passwordHash, fullName, studentCode, classId, roles);
            appUserRepository.save(user);
        });
    }
}

