package SeleniumTest;

import SeleniumTest.config.SeleniumConfig;
import SeleniumTest.config.SeleniumTestDataFactory;
import SeleniumTest.config.BaseSeleniumTest;
import SeleniumTest.pages.EventDetailPage;
import SeleniumTest.pages.HistoryPage;
import SeleniumTest.pages.LayoutComponent;
import SeleniumTest.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Module 4.6: Selenium WebDriver UI/E2E tests for Participation History (/history), KPI Cards,
 * EmptyState, QR Check-in Cache Invalidation, F5 Persistence, RBAC, Deep-link & Responsive 320px.
 * Traceability: docs/test/testcase/4.6-tra-cuu-lich-su-tham-gia-su-kien.md
 */
@DisplayName("4.6 Selenium UI - Tra cứu Lịch sử Tham gia Sự kiện & Thẻ KPI")
class HistorySeleniumTest extends BaseSeleniumTest {

    @Test
    @DisplayName("TC-4.6-01: Sinh viên chưa đăng ký sự kiện nào mở /history hiển thị KPI 0/0/+0 đ & EmptyState")
    void tc46_01_emptyHistoryShowsZeroKpisAndEmptyState() throws Exception {
        assumeTrue(DB.isAvailable(), "Yêu cầu PostgreSQL để tạo sinh viên mới sạch lịch sử");
        String freshLogin = "uihist0_" + UUID.randomUUID().toString().substring(0, 8);
        UUID userId = DB.createTemporaryUser(
                freshLogin, SeleniumConfig.demoPassword(), "SV Lịch Sử Trống UI",
                null, UUID.fromString(SeleniumConfig.CLASS_K49CNTT1_ID), "STUDENT", "ACTIVE"
        );
        trackUserForCleanup(userId);

        new LoginPage(driver, wait).loginAs(freshLogin, SeleniumConfig.demoPassword());
        waitForPath("/events");

        LayoutComponent layout = new LayoutComponent(driver, wait);
        layout.clickSidebarLink("Lịch sử điểm danh");
        waitForPath("/history");

        HistoryPage historyPage = new HistoryPage(driver, wait).waitUntilLoaded();
        assertEquals("Lịch sử sự kiện & Điểm rèn luyện", historyPage.getHeading());
        assertEquals("0", historyPage.getKpiValue("Đã đăng ký"));
        assertEquals("0", historyPage.getKpiValue("Đã điểm danh"));
        assertEquals("+0 đ", historyPage.getKpiValue("Điểm tích lũy"));
        assertTrue(historyPage.getMainText().contains("Bạn chưa có lịch sử sự kiện nào"));

        historyPage.clickExploreEventsButton();
        waitForPath("/events");
    }

    @Test
    @DisplayName("TC-4.6-02, 03, 04 & 05: Luồng NOT_CHECKED_IN (+0 đ) -> Điểm danh QR -> ACCEPTED (+10 đ), đa sự kiện KPI, thứ tự & F5")
    void tc46_02_03_04_05_historyLifecycleKpiOrderingAndReload() throws Exception {
        assumeTrue(DB.isAvailable(), "Yêu cầu PostgreSQL để dùng tài khoản sinh viên mới độc lập cho kiểm định KPI chính xác");
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String freshLogin = "uihistflow_" + UUID.randomUUID().toString().substring(0, 8);
        UUID userId = DB.createTemporaryUser(
                freshLogin, SeleniumConfig.demoPassword(), "SV Lịch Sử E2E",
                null, UUID.fromString(SeleniumConfig.CLASS_K49CNTT1_ID), "STUDENT", "ACTIVE"
        );
        trackUserForCleanup(userId);
        String studentToken = apiLogin(freshLogin);

        // Event 1: currently open (10 pts), registered only -> NOT_CHECKED_IN
        String title1 = SeleniumTestDataFactory.uniqueTitle("HIST-UI-E1");
        String evt1Id = apiCreateAndPublishEvent(ownerToken, SeleniumTestDataFactory.activeNowEventPayload(
                title1, 20, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));
        apiRegisterStudent(studentToken, evt1Id);

        // Login student & verify TC-4.6-02
        new LoginPage(driver, wait).loginAs(freshLogin, SeleniumConfig.demoPassword());
        waitForPath("/events");

        HistoryPage historyPage = new HistoryPage(driver, wait).open();
        assertEquals("1", historyPage.getKpiValue("Đã đăng ký"));
        assertEquals("0", historyPage.getKpiValue("Đã điểm danh"));
        assertEquals("+0 đ", historyPage.getKpiValue("Điểm tích lũy"));
        assertTrue(historyPage.getMainText().contains(title1));
        assertTrue(historyPage.getMainText().contains("Chưa điểm danh"));
        assertFalse(historyPage.getMainText().contains("Điểm danh lúc:"));

        // TC-4.6-03: Check in QR via UI on Event 1 -> navigate via sidebar to /history -> immediate KPI update (+10 đ)
        EventDetailPage detail = new EventDetailPage(driver, wait).open(evt1Id);
        detail.submitManualQrToken(apiFetchQrToken(ownerToken, evt1Id));
        detail.waitForCheckInSuccess();

        new LayoutComponent(driver, wait).clickSidebarLink("Lịch sử điểm danh");
        waitForPath("/history");
        historyPage.waitUntilLoaded();

        assertEquals("1", historyPage.getKpiValue("Đã đăng ký"));
        assertEquals("1", historyPage.getKpiValue("Đã điểm danh"));
        assertEquals("+10 đ", historyPage.getKpiValue("Điểm tích lũy"));
        assertTrue(historyPage.getMainText().contains("Đã điểm danh"));
        assertTrue(historyPage.getMainText().contains("Điểm danh lúc:"));

        // TC-4.6-04: Register a second future event (5 pts, startAt +3 days, NOT_CHECKED_IN)
        String title2 = SeleniumTestDataFactory.uniqueTitle("HIST-UI-E2");
        Map<String, Object> futurePayload = SeleniumTestDataFactory.validFutureEventPayload(
                title2, 20, List.of(SeleniumConfig.CLASS_K49CNTT1_ID));
        Instant laterStart = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        futurePayload.put("startAt", laterStart.toString());
        futurePayload.put("endAt", laterStart.plus(2, ChronoUnit.HOURS).toString());
        futurePayload.put("registrationDeadline", laterStart.minus(1, ChronoUnit.HOURS).toString());
        futurePayload.put("attendanceOpenAt", laterStart.minus(15, ChronoUnit.MINUTES).toString());
        futurePayload.put("attendanceCloseAt", laterStart.plus(2, ChronoUnit.HOURS).toString());
        String evt2Id = apiCreateAndPublishEvent(ownerToken, futurePayload);
        apiRegisterStudent(studentToken, evt2Id);

        driver.navigate().refresh();
        historyPage.waitUntilLoaded();

        // TC-4.6-04 & 05: KPI = 2 registered, 1 attended, +10 đ; Event 2 (later startAt) above Event 1
        assertEquals("2", historyPage.getKpiValue("Đã đăng ký"));
        assertEquals("1", historyPage.getKpiValue("Đã điểm danh"));
        assertEquals("+10 đ", historyPage.getKpiValue("Điểm tích lũy"));

        List<String> titles = historyPage.getEventTitlesInOrder();
        assertTrue(titles.indexOf(title2) < titles.indexOf(title1),
                "Sự kiện có startAt mới hơn phải đứng phía trên");

        // Navigate to detail via "Xem chi tiết"
        historyPage.clickViewDetailForEvent(title2);
        waitForPath("/events/" + evt2Id);
    }

    @Test
    @DisplayName("TC-4.6-12, 13 & 16: Owner bị chặn khỏi /history, Deep-link /history sau login, StatusBadge A11y & Responsive 320px")
    void tc46_12_13_16_rbacDeepLinkAccessibilityAndResponsive320() {
        // TC-4.6-12: Owner does not see History link and direct /history redirects to /events
        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_01, SeleniumConfig.demoPassword());
        waitForPath("/events");
        assertFalse(new LayoutComponent(driver, wait).hasSidebarLink("Lịch sử điểm danh"));
        openPath("/history");
        waitForPath("/events");

        // TC-4.6-16: Deep-link /history while unauthenticated -> login sv001 -> lands on /history
        clearBrowserStorage();
        openPath("/history");
        waitForPath("/login");
        new LoginPage(driver, wait).submitCredentials(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        waitForPath("/history");

        // TC-4.6-13: Responsive 320px & StatusBadge icon + text accessibility
        HistoryPage historyPage = new HistoryPage(driver, wait).waitUntilLoaded();
        setViewport(320, 568, true);
        assertFalse(hasHorizontalOverflow(), "Trang /history ở 320px không được tràn ngang");
        LayoutComponent layout = new LayoutComponent(driver, wait);
        assertTrue(layout.isMobileBottomNavDisplayed());
        assertTrue(layout.getMobileBottomNavText().contains("Lịch sử"));

        List<WebElement> badges = historyPage.findStatusBadgesWithIcons();
        for (WebElement badge : badges) {
            assertFalse(badge.getText().isBlank(), "Mỗi StatusBadge phải có cả icon SVG và nhãn chữ");
        }
        resetViewport();
    }
}
