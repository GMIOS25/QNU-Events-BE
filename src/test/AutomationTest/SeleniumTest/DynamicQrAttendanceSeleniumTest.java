package SeleniumTest;

import SeleniumTest.config.SeleniumConfig;
import SeleniumTest.config.SeleniumTestDataFactory;
import SeleniumTest.config.BaseSeleniumTest;
import SeleniumTest.pages.EventDetailPage;
import SeleniumTest.pages.EventQrPage;
import SeleniumTest.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Module 4.5: Selenium WebDriver UI/E2E tests for Dynamic QR Projection Screen (/events/:id/qr)
 * and Student AttendanceSlot (Fallback Token, Camera Toggle, Attempt Counter 0/3 -> 3/3 Lockout & Responsive).
 * Traceability: docs/test/testcase/4.5-diem-danh-dynamic-qr.md
 */
@DisplayName("4.5 Selenium UI - Màn hình Chiếu Dynamic QR & Điểm danh Sinh viên")
class DynamicQrAttendanceSeleniumTest extends BaseSeleniumTest {

    @Test
    @DisplayName("TC-QR-01 & TC-QR-02: Màn hình chiếu QR hiển thị SVG, Epoch, đếm ngược, nút Chép mã & tự xoay vòng 10s")
    void tcQr01_02_qrProjectionScreenCopyAndRotation() throws Exception {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String eventId = apiCreateAndPublishEvent(ownerToken, SeleniumTestDataFactory.activeNowEventPayload(
                SeleniumTestDataFactory.uniqueTitle("UI QR Rotate"), 20, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));

        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_01, SeleniumConfig.demoPassword());
        waitForPath("/events");

        EventDetailPage detail = new EventDetailPage(driver, wait).open(eventId);
        detail.clickOpenQrScreen();
        waitForPath("/events/" + eventId + "/qr");

        EventQrPage qrPage = new EventQrPage(driver, wait);
        qrPage.waitForQrRendered();
        assertTrue(qrPage.isQrSvgVisible(), "Phải hiển thị mã QR dạng SVG");
        assertTrue(qrPage.getEpochBadgeText().contains("Chu kỳ 10s"));
        assertTrue(qrPage.getCountdownText().contains("Đổi mã sau:"));

        String firstToken = qrPage.getDisplayedToken();
        String[] parts = firstToken.split("\\.");
        assertEquals(4, parts.length, "Token hiển thị phải có đúng 4 phần qr1.<id>.<epoch>.<sig>");
        assertEquals("qr1", parts[0]);
        assertEquals(eventId, parts[1]);

        // TC-QR-02: Copy button toggles to "Đã chép"
        qrPage.clickCopyToken();
        wait.until(d -> qrPage.getCopyButtonText().contains("Đã chép"));

        // TC-QR-01: Wait ~11s across epoch boundary & verify token rotates automatically
        Thread.sleep(11_000L);
        qrPage.clickRefreshNow();
        wait.until(d -> !firstToken.equals(qrPage.getDisplayedToken()));
        assertNotEquals(firstToken, qrPage.getDisplayedToken(), "Token QR phải tự động xoay vòng sang epoch mới");
    }

    @Test
    @DisplayName("TC-QR-03 & TC-QR-04: Màn hình /qr ngoài cửa sổ điểm danh hiện cảnh báo & ErrorBanner; Student bị chặn route /qr")
    void tcQr03_04_closedWindowQrWarningAndStudentRouteBlocked() {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String closedEventId = apiCreateAndPublishEvent(ownerToken, SeleniumTestDataFactory.validFutureEventPayload(
                SeleniumTestDataFactory.uniqueTitle("UI QR Closed"), 20, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));

        // TC-QR-03: Owner opens /qr outside attendance window
        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_01, SeleniumConfig.demoPassword());
        waitForPath("/events");
        EventQrPage qrPage = new EventQrPage(driver, wait).open(closedEventId);
        assertTrue(qrPage.getBodyText().contains("Cửa sổ điểm danh:"));
        assertTrue(qrPage.getErrorBannerText().contains("Không thể tạo mã QR điểm danh."));

        // TC-QR-04: Student trying to open /events/:id/qr is redirected away to /events
        clearBrowserStorage();
        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        waitForPath("/events");
        openPath("/events/" + closedEventId + "/qr");
        waitForPath("/events");
    }

    @Test
    @DisplayName("TC-QR-05 & TC-QR-07: Đóng/mở Camera dự phòng và điểm danh thành công bằng ô nhập token QR trên UI")
    void tcQr05_07_cameraToggleFallbackAndCheckInSuccess() {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String sv1Token = apiLogin(SeleniumConfig.STUDENT_01);
        String eventId = apiCreateAndPublishEvent(ownerToken, SeleniumTestDataFactory.activeNowEventPayload(
                SeleniumTestDataFactory.uniqueTitle("UI CheckIn Fallback"), 20, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));
        apiRegisterStudent(sv1Token, eventId);

        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        waitForPath("/events");
        EventDetailPage detail = new EventDetailPage(driver, wait).open(eventId);
        assertTrue(detail.isAttendanceSlotVisible());
        assertTrue(detail.getAttemptCounterText().contains("0/3"));

        // TC-QR-07: Open camera container (camera permission blocked in ChromeOptions) -> close cleanly without crash
        detail.clickToggleCamera();
        assertTrue(detail.isQrReaderContainerVisible(), "Khung #qr-reader-container phải mở khi bấm Quét mã QR bằng Camera");
        detail.clickCloseCamera();
        assertFalse(detail.isQrReaderContainerVisible(), "Khung camera phải ẩn sạch sau khi bấm Đóng camera");

        // TC-QR-05: Fetch fresh valid token & submit via fallback input
        String validToken = apiFetchQrToken(ownerToken, eventId);
        detail.submitManualQrToken(validToken);
        detail.waitForCheckInSuccess();

        String text = detail.getMainText();
        assertTrue(text.contains("Điểm danh thành công!"));
        assertTrue(text.contains("Đã điểm danh"));
        assertTrue(text.contains("Thời gian xác nhận:"));
    }

    @Test
    @DisplayName("TC-QR-08, TC-QR-09 & TC-QR-10: NOT_REGISTERED banner, ngoài cửa sổ disable controls & chặn input khoảng trắng")
    void tcQr08_09_10_notRegisteredClosedWindowAndWhitespaceInput() {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String activeId = apiCreateAndPublishEvent(ownerToken, SeleniumTestDataFactory.activeNowEventPayload(
                SeleniumTestDataFactory.uniqueTitle("UI Att Active Unreg"), 20, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));
        String closedId = apiCreateAndPublishEvent(ownerToken, SeleniumTestDataFactory.validFutureEventPayload(
                SeleniumTestDataFactory.uniqueTitle("UI Att Closed Window"), 20, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));

        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        waitForPath("/events");

        // TC-QR-10 (Step 1): Blank / whitespace-only token keeps "Gửi mã" disabled
        EventDetailPage activeDetail = new EventDetailPage(driver, wait).open(activeId);
        assertFalse(activeDetail.isSubmitTokenButtonEnabled(), "Nút Gửi mã phải disabled khi ô token trống");
        activeDetail.enterManualQrToken("     ");
        assertFalse(activeDetail.isSubmitTokenButtonEnabled(), "Nút Gửi mã phải disabled khi chỉ nhập khoảng trắng");

        // TC-QR-08: Unregistered student submits valid token -> NOT_REGISTERED ErrorBanner
        String validQr = apiFetchQrToken(ownerToken, activeId);
        activeDetail.submitManualQrToken(validQr);
        assertTrue(activeDetail.getErrorBannerText().contains("Bạn chưa đăng ký giữ chỗ sự kiện này nên không thể điểm danh."));
        // Counted = false -> attempt counter stays 0/3
        assertTrue(activeDetail.getAttemptCounterText().contains("0/3"));

        // TC-QR-09: Closed attendance window displays amber banner & disables controls
        EventDetailPage closedDetail = new EventDetailPage(driver, wait).open(closedId);
        assertTrue(closedDetail.getMainText().contains("Chưa đến hoặc đã hết giờ mở điểm danh cho sự kiện này."));
        assertFalse(closedDetail.isCameraToggleEnabled());
        assertFalse(closedDetail.isManualTokenInputEnabled());
        assertFalse(closedDetail.isSubmitTokenButtonEnabled());
    }

    @Test
    @DisplayName("TC-QR-17 & TC-QR-21: Đếm ngược 3 lần thử sai (0/3 -> 3/3 khóa UI), ẩn AttendanceSlot với Owner & Responsive 320px")
    void tcQr17_21_threeWrongAttemptsLockoutAndResponsive320() {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String sv2Token = apiLogin(SeleniumConfig.STUDENT_02);
        String eventId = apiCreateAndPublishEvent(ownerToken, SeleniumTestDataFactory.activeNowEventPayload(
                SeleniumTestDataFactory.uniqueTitle("UI Att Lockout 3x"), 20, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));
        apiRegisterStudent(sv2Token, eventId);

        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_02, SeleniumConfig.demoPassword());
        waitForPath("/events");

        setViewport(320, 568, true);
        EventDetailPage detail = new EventDetailPage(driver, wait).open(eventId);
        assertFalse(hasHorizontalOverflow(), "AttendanceSlot ở 320px không được tràn ngang");

        // TC-QR-21: Offline error banner & dismiss
        setNetworkOffline(true);
        detail.submitManualQrToken("qr1.offline.test.sig");
        assertTrue(detail.getErrorBannerText().contains("Điểm danh không thành công."));
        detail.dismissErrorBanner();
        assertFalse(detail.isErrorBannerVisible());
        setNetworkOffline(false);

        // TC-QR-17: 3 wrong tokens increment attempt counter 1/3 -> 2/3 -> 3/3 and lock UI
        for (int attempt = 1; attempt <= 3; attempt++) {
            detail.submitManualQrToken("bad-token-" + attempt);
            final int expected = attempt;
            wait.until(d -> detail.getAttemptCounterText().contains(expected + "/3"));
        }

        wait.until(d -> detail.getMainText().contains("Đã khóa chức năng điểm danh tự động"));
        assertTrue(detail.getAttemptCounterText().contains("3/3"));
        assertFalse(detail.isCameraToggleEnabled(), "Nút Camera phải bị khóa sau 3 lần sai");
        assertFalse(detail.isManualTokenInputEnabled(), "Ô nhập token phải bị khóa sau 3 lần sai");
        assertFalse(detail.isSubmitTokenButtonEnabled(), "Nút Gửi mã phải bị khóa sau 3 lần sai");

        // TC-QR-21: Owner does NOT see AttendanceSlot on PUBLISHED event
        resetViewport();
        clearBrowserStorage();
        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_01, SeleniumConfig.demoPassword());
        EventDetailPage ownerDetail = new EventDetailPage(driver, wait).open(eventId);
        assertFalse(ownerDetail.isAttendanceSlotVisible(), "AttendanceSlot chỉ hiển thị cho STUDENT");
    }
}
