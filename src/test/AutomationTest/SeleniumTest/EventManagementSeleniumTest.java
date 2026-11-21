package SeleniumTest;

import SeleniumTest.config.SeleniumConfig;
import SeleniumTest.config.SeleniumTestDataFactory;
import SeleniumTest.config.BaseSeleniumTest;
import SeleniumTest.pages.EventDetailPage;
import SeleniumTest.pages.EventFormPage;
import SeleniumTest.pages.EventsPage;
import SeleniumTest.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WindowType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Module 4.2: Selenium WebDriver UI/E2E tests for Event Creation, Validation, Editing,
 * Optimistic Locking, Preview-then-Publish Workflow & Responsive Layout.
 * Traceability: docs/test/testcase/4.2-quan-ly-xem-truoc-cong-bo-su-kien.md
 */
@DisplayName("4.2 Selenium UI - Quản lý, Chỉnh sửa, Xem trước & Công bố Sự kiện")
class EventManagementSeleniumTest extends BaseSeleniumTest {

    private static final DateTimeFormatter LOCAL_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Test
    @DisplayName("TC-EVT-001 & TC-EVT-002: Mở /events/new nạp danh sách lớp, chọn từng lớp và 'Chọn tất cả các lớp'")
    void tcEvt001_002_loadClassesAndToggleSelection() {
        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_01, SeleniumConfig.demoPassword());
        EventsPage eventsPage = new EventsPage(driver, wait).waitUntilLoaded();
        assertEquals("Quản lý sự kiện đơn vị", eventsPage.getPageHeading());
        assertTrue(eventsPage.isCreateEventButtonVisible());

        eventsPage.clickCreateEventButton();
        waitForPath("/events/new");

        EventFormPage formPage = new EventFormPage(driver, wait);
        assertEquals("Tạo sự kiện mới", formPage.getHeading());
        int totalClasses = formPage.getAllClassCheckboxes().size();
        assertTrue(totalClasses >= 2, "Phải nạp đủ tối thiểu 2 lớp mẫu (K49CNTT1, K49CNTT2)");

        // Select individual class K49CNTT1 -> (1 lớp đã chọn)
        formPage.toggleClassByCode("K49CNTT1");
        assertTrue(formPage.getClassSectionHeadingText().contains("(1 lớp đã chọn)"));

        // Deselect K49CNTT1 -> (0 lớp đã chọn)
        formPage.toggleClassByCode("K49CNTT1");
        assertTrue(formPage.getClassSectionHeadingText().contains("(0 lớp đã chọn)"));

        // Select all classes
        formPage.clickSelectAllClasses();
        assertEquals(totalClasses, formPage.getSelectedClassCount());
        assertTrue(formPage.getClassSectionHeadingText().contains("(" + totalClasses + " lớp đã chọn)"));
    }

    @Test
    @DisplayName("TC-EVT-003 & TC-EVT-004: Tạo mới sự kiện nháp (DRAFT) đầy đủ và bỏ trống mô tả qua UI")
    void tcEvt003_004_createDraftFullAndWithoutDescription() {
        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_01, SeleniumConfig.demoPassword());
        waitForPath("/events");

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0);
        String fullTitle = SeleniumTestDataFactory.uniqueTitle("Hội thảo AI & Khởi nghiệp UI");

        // TC-EVT-003: Full fields
        EventFormPage form = new EventFormPage(driver, wait).openCreate();
        form.setTitle(fullTitle)
                .setDescription("Nội dung chi tiết hội thảo tự động")
                .setLocation("Hội trường A - Trường ĐH Quy Nhơn")
                .setQuotaTotal(50)
                .setStartAt(tomorrow.format(LOCAL_FMT))
                .setEndAt(tomorrow.plusHours(3).format(LOCAL_FMT))
                .setRegistrationDeadline(tomorrow.minusMinutes(30).format(LOCAL_FMT))
                .setAttendanceOpenAt(tomorrow.minusMinutes(15).format(LOCAL_FMT))
                .setAttendanceCloseAt(tomorrow.plusHours(3).plusMinutes(30).format(LOCAL_FMT))
                .setActivityType("Hội thảo khoa học")
                .setActivityPoints(5)
                .toggleClassByCode("K49CNTT1")
                .clickSubmit();

        waitForUrlContains("/events/");
        wait.until(d -> !d.getCurrentUrl().endsWith("/new"));
        String createdId = extractEventIdFromUrl();
        trackEventForCleanup(createdId);

        EventDetailPage detail = new EventDetailPage(driver, wait).waitUntilReady();
        assertEquals(fullTitle, detail.getTitle());
        assertTrue(detail.getMainText().contains("Bản nháp"));
        assertTrue(detail.isEditButtonVisible());
        assertTrue(detail.isPreviewPublishButtonVisible());

        // TC-EVT-004: Empty description allowed
        String noDescTitle = SeleniumTestDataFactory.uniqueTitle("Sự kiện không mô tả UI");
        EventFormPage form2 = new EventFormPage(driver, wait).openCreate();
        form2.setTitle(noDescTitle)
                .setDescription("")
                .setLocation("Phòng B101")
                .setQuotaTotal(20)
                .setStartAt(tomorrow.format(LOCAL_FMT))
                .setEndAt(tomorrow.plusHours(2).format(LOCAL_FMT))
                .setRegistrationDeadline(tomorrow.minusMinutes(30).format(LOCAL_FMT))
                .setAttendanceOpenAt(tomorrow.minusMinutes(15).format(LOCAL_FMT))
                .setAttendanceCloseAt(tomorrow.plusHours(2).format(LOCAL_FMT))
                .toggleClassByCode("K49CNTT1")
                .clickSubmit();

        waitForUrlContains("/events/");
        wait.until(d -> !d.getCurrentUrl().endsWith("/new"));
        trackEventForCleanup(extractEventIdFromUrl());
        assertEquals(noDescTitle, new EventDetailPage(driver, wait).getTitle());
    }

    @Test
    @DisplayName("TC-EVT-005, 008, 009, 010, 011 & 014: Kiểm định lỗi validation form tạo sự kiện trên UI")
    void tcEvt005_008_009_010_011_014_formValidationErrors() {
        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_01, SeleniumConfig.demoPassword());
        waitForPath("/events");

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        EventFormPage form = new EventFormPage(driver, wait).openCreate();
        form.disableHtml5Validation();

        // Fill base valid values first
        form.setTitle("AB")
                .setLocation("Hội trường A")
                .setQuotaTotal(30)
                .setStartAt(tomorrow.format(LOCAL_FMT))
                .setEndAt(tomorrow.plusHours(2).format(LOCAL_FMT))
                .setRegistrationDeadline(tomorrow.minusMinutes(30).format(LOCAL_FMT))
                .setAttendanceOpenAt(tomorrow.minusMinutes(15).format(LOCAL_FMT))
                .setAttendanceCloseAt(tomorrow.plusHours(2).format(LOCAL_FMT))
                .toggleClassByCode("K49CNTT1")
                .clickSubmit();

        // TC-EVT-005: Title < 3 chars
        assertTrue(form.getErrorBannerText().contains("Tiêu đề sự kiện phải từ 3 ký tự trở lên."));

        // TC-EVT-008: Whitespace location
        form.setTitle("Hội thảo hợp lệ").setLocation("   ").clickSubmit();
        assertTrue(form.getErrorBannerText().contains("Vui lòng nhập địa điểm tổ chức."));

        // TC-EVT-009: endAt <= startAt
        form.setLocation("Hội trường A").setEndAt(tomorrow.minusHours(1).format(LOCAL_FMT)).clickSubmit();
        assertTrue(form.getErrorBannerText().contains("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu."));

        // TC-EVT-010: attendanceCloseAt <= attendanceOpenAt
        form.setEndAt(tomorrow.plusHours(2).format(LOCAL_FMT))
                .setAttendanceCloseAt(tomorrow.minusHours(1).format(LOCAL_FMT))
                .clickSubmit();
        assertTrue(form.getErrorBannerText().contains("Thời gian đóng điểm danh phải sau thời gian mở điểm danh."));

        // TC-EVT-014: Empty audience classes
        form.setAttendanceCloseAt(tomorrow.plusHours(2).format(LOCAL_FMT))
                .toggleClassByCode("K49CNTT1") // uncheck
                .clickSubmit();
        assertTrue(form.getErrorBannerText().contains("Vui lòng chọn ít nhất một lớp đối tượng"));

        // TC-EVT-011: registrationDeadline > attendanceCloseAt (hits BE validator -> ErrorBanner)
        form.toggleClassByCode("K49CNTT1")
                .setRegistrationDeadline(tomorrow.plusHours(5).format(LOCAL_FMT))
                .clickSubmit();
        assertFalse(form.getErrorBannerText().isBlank(), "BE phải từ chối khi registrationDeadline > attendanceCloseAt");
    }

    @Test
    @DisplayName("TC-EVT-016 & TC-EVT-017: Chỉnh sửa sự kiện DRAFT và xung đột phiên bản Optimistic Locking (2 tab)")
    void tcEvt016_017_editDraftAndTwoTabVersionConflict() {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String eventId = apiCreateDraftEvent(ownerToken, SeleniumTestDataFactory.validFutureEventPayload(
                SeleniumTestDataFactory.uniqueTitle("Edit & Conflict Base"), 50, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));

        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_01, SeleniumConfig.demoPassword());
        waitForPath("/events");

        // Tab 1 opens /edit (loads version = 0)
        EventFormPage tab1Form = new EventFormPage(driver, wait).openEdit(eventId);
        String tab1Handle = driver.getWindowHandle();

        // Tab 2 also opens /edit (same session, also loads version = 0)
        driver.switchTo().newWindow(WindowType.TAB);
        EventFormPage tab2Form = new EventFormPage(driver, wait).openEdit(eventId);
        String tab2Handle = driver.getWindowHandle();

        // Tab 1 saves first -> version 0 -> 1
        driver.switchTo().window(tab1Handle);
        String updatedTitle = SeleniumTestDataFactory.uniqueTitle("Updated By Tab 1");
        tab1Form.setTitle(updatedTitle).setQuotaTotal(60).clickSubmit();
        waitForPath("/events/" + eventId);
        EventDetailPage detail = new EventDetailPage(driver, wait).waitUntilReady();
        assertEquals(updatedTitle, detail.getTitle());
        assertTrue(detail.getMainText().contains("60"));

        // Tab 2 tries to save with stale version 0 -> VERSION_CONFLICT ErrorBanner
        driver.switchTo().window(tab2Handle);
        tab2Form.setLocation("Phòng Tab 2 Xung Đột").clickSubmit();
        String conflictBanner = tab2Form.getErrorBannerText();
        assertTrue(conflictBanner.contains("Dữ liệu sự kiện đã bị thay đổi bởi người khác"),
                "Tab 2 phải hiển thị thông báo xung đột phiên bản (VERSION_CONFLICT)");
    }

    @Test
    @DisplayName("TC-EVT-018, 019, 020 & 023: Xem trước modal (Hủy giữ DRAFT), bắt buộc checkbox xác nhận & Công bố thành công")
    void tcEvt018_019_020_023_previewModalCancelCheckboxGatingAndPublish() {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String title = SeleniumTestDataFactory.uniqueTitle("Preview Publish Flow");
        String eventId = apiCreateDraftEvent(ownerToken, SeleniumTestDataFactory.validFutureEventPayload(
                title, 40, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));

        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_01, SeleniumConfig.demoPassword());
        waitForPath("/events");

        EventDetailPage detail = new EventDetailPage(driver, wait).open(eventId);
        assertTrue(detail.getMainText().contains("Bản nháp"));

        // TC-EVT-019: Open preview modal & cancel -> stays DRAFT
        detail.clickPreviewAndPublish();
        String modalText = detail.waitForPreviewModalText();
        assertTrue(modalText.contains("Xem trước sự kiện (Version 0)"));
        assertTrue(modalText.contains(title));
        assertTrue(modalText.contains("40 sinh viên"));

        // TC-EVT-020: Confirm button disabled until checkbox checked
        assertFalse(detail.isConfirmPublishButtonEnabled(), "Nút 'Xác nhận công bố' phải disabled khi chưa tích checkbox");

        detail.clickCancelPreviewModal();
        assertFalse(detail.isPreviewModalVisible());
        assertTrue(detail.getMainText().contains("Bản nháp"));

        // TC-EVT-023: Open preview again, check confirm checkbox & publish
        detail.clickPreviewAndPublish();
        detail.waitForPreviewModalText();
        detail.toggleConfirmPublishCheckbox();
        assertTrue(detail.isConfirmPublishButtonEnabled(), "Nút 'Xác nhận công bố' phải enabled sau khi tích checkbox");
        detail.clickConfirmPublish();

        wait.until(d -> detail.getMainText().contains("Đã công bố"));
        assertTrue(detail.isOpenQrScreenButtonVisible(), "Sau khi công bố phải hiển thị nút 'Màn hình chiếu QR'");

        // TC-EVT-018: Published event hides Edit and Preview buttons
        assertFalse(detail.isEditButtonVisible(), "Sự kiện PUBLISHED không được hiển thị nút Chỉnh sửa");
        assertFalse(detail.isPreviewPublishButtonVisible());
    }

    @Test
    @DisplayName("TC-EVT-025 & TC-EVT-035: Sinh viên không thấy DRAFT (404 banner) & Responsive 375x667 không tràn ngang")
    void tcEvt025_035_studentCannotSeeDraftAndResponsive375() {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String draftTitle = SeleniumTestDataFactory.uniqueTitle("Hidden Draft From Student");
        String draftId = apiCreateDraftEvent(ownerToken, SeleniumTestDataFactory.validFutureEventPayload(
                draftTitle, 30, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));

        // TC-EVT-025: Student cannot find DRAFT in /events or via direct /events/:id
        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        EventsPage eventsPage = new EventsPage(driver, wait).waitUntilLoaded();
        assertFalse(eventsPage.isEventCardPresent(draftTitle));

        EventDetailPage detail = new EventDetailPage(driver, wait).open(draftId);
        assertTrue(detail.getErrorBannerText().contains("Không tìm thấy sự kiện hoặc bạn không có quyền xem sự kiện này."));

        // TC-EVT-035: Owner Responsive 375x667 on /events/new and preview modal + offline preview error
        clearBrowserStorage();
        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_01, SeleniumConfig.demoPassword());
        waitForPath("/events");

        setViewport(375, 667, true);
        new EventFormPage(driver, wait).openCreate();
        assertFalse(hasHorizontalOverflow(), "Trang /events/new ở 375x667 không được tràn ngang");

        EventDetailPage ownerDetail = new EventDetailPage(driver, wait).open(draftId);
        assertFalse(hasHorizontalOverflow(), "Trang chi tiết sự kiện ở 375x667 không được tràn ngang");

        // Offline preview triggers ErrorBanner without crashing
        setNetworkOffline(true);
        ownerDetail.clickPreviewAndPublish();
        assertTrue(ownerDetail.getErrorBannerText().contains("Không thể tải bản xem trước"));
        setNetworkOffline(false);
        resetViewport();
    }

    private String extractEventIdFromUrl() {
        String url = driver.getCurrentUrl();
        int idx = url.lastIndexOf("/events/");
        return url.substring(idx + "/events/".length()).replaceAll("/.*", "");
    }
}
