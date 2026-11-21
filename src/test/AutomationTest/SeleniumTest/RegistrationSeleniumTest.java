package SeleniumTest;

import SeleniumTest.config.SeleniumConfig;
import SeleniumTest.config.SeleniumTestDataFactory;
import SeleniumTest.config.BaseSeleniumTest;
import SeleniumTest.pages.EventDetailPage;
import SeleniumTest.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Module 4.4: Selenium WebDriver UI/E2E tests for Student Event Registration, F5 Persistence,
 * Stale-Tab CAPACITY_FULL & REGISTRATION_CLOSED ErrorBanners, Owner Mode & Responsive 320px.
 * Traceability: docs/test/testcase/4.4-dang-ky-su-kien.md
 */
@DisplayName("4.4 Selenium UI - Đăng ký Giữ suất Sự kiện & Trạng thái RegistrationSlot")
class RegistrationSeleniumTest extends BaseSeleniumTest {

    @Test
    @DisplayName("TC-REG-01 & TC-REG-02: Sinh viên đăng ký giữ chỗ thành công lần đầu qua UI & giữ trạng thái sau khi tải lại trang (F5)")
    void tcReg01_02_studentRegistersAndReloadPersistsState() {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String eventId = apiCreateAndPublishEvent(ownerToken, SeleniumTestDataFactory.validFutureEventPayload(
                SeleniumTestDataFactory.uniqueTitle("UI Reg Happy"), 10, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));

        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        waitForPath("/events");

        EventDetailPage detail = new EventDetailPage(driver, wait).open(eventId);
        assertTrue(detail.getMainText().contains("Đã đăng ký 0/10 chỗ (còn lại 10 chỗ)"));
        assertTrue(detail.isRegisterButtonVisible());

        // TC-REG-01: Click register -> status badge + green confirmation + quota updated to 1/10
        detail.clickRegisterButton();
        detail.waitForRegisteredSuccess();

        String afterText = detail.getMainText();
        assertTrue(afterText.contains("Đã đăng ký"));
        assertTrue(afterText.contains("Bạn đã giữ chỗ thành công!"));
        assertTrue(afterText.contains("Đăng ký lúc:"));
        wait.until(d -> detail.getMainText().contains("Đã đăng ký 1/10 chỗ (còn lại 9 chỗ)"));
        assertFalse(detail.isRegisterButtonVisible());

        // TC-REG-02: F5 reload preserves registered state & quota
        driver.navigate().refresh();
        detail.waitUntilReady();
        detail.waitForRegisteredSuccess();
        assertTrue(detail.getMainText().contains("Đã đăng ký 1/10 chỗ (còn lại 9 chỗ)"));
        assertFalse(detail.isRegisterButtonVisible());
    }

    @Test
    @DisplayName("TC-REG-04: Hiển thị hết suất sau F5 và xử lý lỗi CAPACITY_FULL khi tab cũ bấm đăng ký")
    void tcReg04_capacityFullStaleTabAndReloadedUi() {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String sv2Token = apiLogin(SeleniumConfig.STUDENT_02);

        String eventId = apiCreateAndPublishEvent(ownerToken, SeleniumTestDataFactory.validFutureEventPayload(
                SeleniumTestDataFactory.uniqueTitle("UI Capacity Full"), 1, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));

        // sv001 opens detail page while seat is still available (stale tab with register button visible)
        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        waitForPath("/events");
        EventDetailPage detail = new EventDetailPage(driver, wait).open(eventId);
        assertTrue(detail.isRegisterButtonVisible());

        // Meanwhile, sv002 claims the last seat via API
        apiRegisterStudent(sv2Token, eventId);

        // sv001 clicks the stale register button -> 409 CAPACITY_FULL ErrorBanner
        detail.clickRegisterButton();
        assertTrue(detail.getErrorBannerText().contains("Sự kiện đã hết suất tham gia."));
        assertFalse(detail.getMainText().contains("Bạn đã giữ chỗ thành công!"));

        // After F5 reload -> button hidden, full quota notice displayed
        driver.navigate().refresh();
        detail.waitUntilReady();
        assertFalse(detail.isRegisterButtonVisible());
        assertTrue(detail.getMainText().contains("Sự kiện đã hết suất tham gia."));
    }

    @Test
    @DisplayName("TC-REG-05: Hiển thị hết hạn đăng ký trên UI và xử lý lỗi REGISTRATION_CLOSED khi vừa quá hạn")
    void tcReg05_registrationClosedUiAndStaleTabError() throws Exception {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String expiredId = apiCreateAndPublishEvent(ownerToken, SeleniumTestDataFactory.registrationExpiredEventPayload(
                SeleniumTestDataFactory.uniqueTitle("UI Reg Expired"), 10, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));

        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        waitForPath("/events");

        // Step 1: Already expired event hides button and displays closed notice
        EventDetailPage expiredDetail = new EventDetailPage(driver, wait).open(expiredId);
        assertFalse(expiredDetail.isRegisterButtonVisible());
        assertTrue(expiredDetail.getMainText().contains("Đã hết thời hạn đăng ký sự kiện."));

        // Step 2: Open event that expires while tab is still open -> clicking register triggers REGISTRATION_CLOSED banner
        if (DB.isAvailable()) {
            String soonExpiredId = apiCreateAndPublishEvent(ownerToken, SeleniumTestDataFactory.validFutureEventPayload(
                    SeleniumTestDataFactory.uniqueTitle("UI Stale Expired"), 10, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));
            EventDetailPage staleDetail = new EventDetailPage(driver, wait).open(soonExpiredId);
            assertTrue(staleDetail.isRegisterButtonVisible());

            DB.updateEventRegistrationDeadline(UUID.fromString(soonExpiredId), Instant.now().minusSeconds(5));
            staleDetail.clickRegisterButton();
            assertTrue(staleDetail.getErrorBannerText().contains("Đã hết thời hạn đăng ký tham gia sự kiện."));
        }
    }

    @Test
    @DisplayName("TC-REG-06: Hiển thị khối RegistrationSlot ở Chế độ quản lý cho Ban tổ chức (owner01)")
    void tcReg06_ownerSeesManagementModeInRegistrationSlot() {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String eventId = apiCreateAndPublishEvent(ownerToken, SeleniumTestDataFactory.validFutureEventPayload(
                SeleniumTestDataFactory.uniqueTitle("UI Owner Reg Slot"), 10, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));

        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_01, SeleniumConfig.demoPassword());
        waitForPath("/events");

        EventDetailPage detail = new EventDetailPage(driver, wait).open(eventId);
        assertFalse(detail.isRegisterButtonVisible(), "Ban tổ chức không được thấy nút 'Đăng ký giữ chỗ'");
        assertTrue(detail.getMainText().contains("Chế độ quản lý: Sinh viên thuộc lớp được mời có thể đăng ký giữ chỗ tại đây."));
    }

    @Test
    @DisplayName("TC-REG-24: Mất mạng hiển thị ErrorBanner, nút đóng (X) ẩn banner, StatusBadge có icon & Responsive 320px")
    void tcReg24_offlineBannerDismissStatusBadgeAndResponsive320() {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String eventId = apiCreateAndPublishEvent(ownerToken, SeleniumTestDataFactory.validFutureEventPayload(
                SeleniumTestDataFactory.uniqueTitle("UI Reg 320 Offline"), 10, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));

        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        waitForPath("/events");

        setViewport(320, 568, true);
        EventDetailPage detail = new EventDetailPage(driver, wait).open(eventId);
        assertFalse(hasHorizontalOverflow(), "RegistrationSlot ở 320px không được tràn ngang");

        // Offline error banner & dismiss
        setNetworkOffline(true);
        detail.clickRegisterButton();
        assertTrue(detail.getErrorBannerText().contains("Không thể đăng ký tham gia sự kiện."));
        detail.dismissErrorBanner();
        assertFalse(detail.isErrorBannerVisible());

        // Restore network -> register succeeds & StatusBadge shows both icon and label
        setNetworkOffline(false);
        detail.clickRegisterButton();
        detail.waitForRegisteredSuccess();

        List<WebElement> badges = detail.findStatusBadgesWithIcons();
        assertTrue(badges.stream().anyMatch(b -> b.getText().contains("Đã đăng ký")),
                "StatusBadge 'Đã đăng ký' phải hiển thị đồng thời icon SVG và nhãn chữ");
        assertFalse(hasHorizontalOverflow(), "Sau khi đăng ký ở 320px vẫn không được tràn ngang");
        resetViewport();
    }
}
