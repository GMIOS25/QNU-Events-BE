package vn.edu.qnu.events.student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.qnu.events.common.ApiException;
import vn.edu.qnu.events.common.CurrentUser;
import vn.edu.qnu.events.common.CurrentUserProvider;
import vn.edu.qnu.events.common.ErrorCode;

@RestController
@RequestMapping("/api/v1/me")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/history")
    public Page<HistoryItem> getHistory(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) String attendanceStatus,
            @RequestParam(required = false) Boolean upcoming,
            @RequestParam(required = false) String activityType,
            @PageableDefault(size = 20, sort = "startAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        CurrentUser currentUser = CurrentUserProvider.require();
        if (!currentUser.hasRole("STUDENT")) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        HistoryFilter filter = new HistoryFilter(academicYear, semester, attendanceStatus, upcoming, activityType);
        return historyService.getStudentHistory(currentUser.userId(), filter, pageable);
    }
}
