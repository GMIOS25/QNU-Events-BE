package vn.edu.qnu.events.student;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
public class ClassController {

    private final OrgClassRepository orgClassRepository;

    public ClassController(OrgClassRepository orgClassRepository) {
        this.orgClassRepository = orgClassRepository;
    }

    @GetMapping
    public List<ClassResponse> getClasses() {
        return orgClassRepository.findAll().stream()
                .map(ClassResponse::from)
                .toList();
    }
}

