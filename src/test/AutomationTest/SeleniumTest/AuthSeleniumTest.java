package SeleniumTest;

import SeleniumTest.config.SeleniumConfig;
import SeleniumTest.config.BaseSeleniumTest;
import SeleniumTest.pages.EventsPage;
import SeleniumTest.pages.LayoutComponent;
import SeleniumTest.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Module 4.1: Selenium WebDriver UI/E2E tests for Login, Session Persistence, Logout & Route RBAC.
 * Traceability: docs/test/testcase/4.1-xac-thuc-quan-ly-phien-dang-nhap.md
 */
@DisplayName("4.1 Selenium UI - Đăng nhập, Quản lý phiên & Bảo vệ Route RBAC")
class AuthSeleniumTest extends BaseSeleniumTest {

    @Test
    @DisplayName("TC-AUTH-01: Đăng nhập thành công với vai trò Ban tổ chức (owner01) qua UI & kiểm tra sessionStorage")
    void tcAuth01_ownerLoginSuccess() {
        LoginPage loginPage = new LoginPage(driver, wait).open();
        loginPage.clickQuickFillOwner();
        assertEquals(SeleniumConfig.OWNER_01, loginPage.getLoginIdValue());
        loginPage.clickSubmit();

        waitForPath("/events");
        LayoutComponent layout = new LayoutComponent(driver, wait);
        String header = layout.getHeaderText();
        assertTrue(header.contains("Nguyễn Văn Chủ Sự Kiện"), "Header phải hiển thị tên chủ sự kiện");
        assertTrue(header.contains(SeleniumConfig.OWNER_01), "Header phải hiển thị loginId owner01");
        assertTrue(header.contains("Ban tổ chức"), "Header phải hiển thị vai trò Ban tổ chức");

        assertTrue(layout.hasSidebarLink("Sự kiện của tôi"));
        assertTrue(layout.hasSidebarLink("Tạo sự kiện mới"));
        assertFalse(layout.hasSidebarLink("Lịch sử điểm danh"), "Owner không được thấy menu Lịch sử điểm danh");

        assertNotNull(getSessionStorage("qnu_events_access_token"), "Phải lưu access token trong sessionStorage");
        String userInfoJson = getSessionStorage("qnu_events_user_info");
        assertNotNull(userInfoJson);
        assertTrue(userInfoJson.contains("\"classId\":null"), "Owner phải có classId = null");
    }

    @Test
    @DisplayName("TC-AUTH-02: Đăng nhập thành công với vai trò Sinh viên (sv001) qua UI & kiểm tra menu + classId")
    void tcAuth02_studentLoginSuccess() {
        LoginPage loginPage = new LoginPage(driver, wait).open();
        loginPage.clickQuickFillStudent();
        assertEquals(SeleniumConfig.STUDENT_01, loginPage.getLoginIdValue());
        loginPage.clickSubmit();

        waitForPath("/events");
        LayoutComponent layout = new LayoutComponent(driver, wait);
        String header = layout.getHeaderText();
        assertTrue(header.contains("Trần Sinh Viên 1"));
        assertTrue(header.contains(SeleniumConfig.STUDENT_01));
        assertTrue(header.contains("Sinh viên"));

        assertTrue(layout.hasSidebarLink("Sự kiện"));
        assertTrue(layout.hasSidebarLink("Lịch sử điểm danh"));
        assertFalse(layout.hasSidebarLink("Tạo sự kiện mới"), "Sinh viên không được thấy menu Tạo sự kiện mới");

        String userInfoJson = getSessionStorage("qnu_events_user_info");
        assertNotNull(userInfoJson);
        assertTrue(userInfoJson.contains(SeleniumConfig.CLASS_K49CNTT1_ID), "sv001 phải thuộc lớp K49CNTT1");
    }

    @Test
    @DisplayName("TC-AUTH-03: Validation client Zod khi bỏ trống Tên đăng nhập hoặc Mật khẩu trên form /login")
    void tcAuth03_emptyLoginFormShowsClientValidationErrors() {
        LoginPage loginPage = new LoginPage(driver, wait).open();

        // Step 1: Both empty
        loginPage.clickSubmit();
        assertTrue(loginPage.getLoginIdFieldError().contains("Tên đăng nhập không được để trống"));
        assertTrue(loginPage.getPasswordFieldError().contains("Mật khẩu không được để trống"));
        assertFalse(loginPage.isErrorBannerDisplayed(), "Không gọi API server khi form rỗng");

        // Step 2: loginId filled, password empty
        loginPage.enterLoginId(SeleniumConfig.STUDENT_01).enterPassword("").clickSubmit();
        assertTrue(loginPage.getPasswordFieldError().contains("Mật khẩu không được để trống"));

        // Step 3: loginId empty, password filled
        loginPage.enterLoginId("").enterPassword(SeleniumConfig.demoPassword()).clickSubmit();
        assertTrue(loginPage.getLoginIdFieldError().contains("Tên đăng nhập không được để trống"));
    }

    @Test
    @DisplayName("TC-AUTH-04 & TC-AUTH-05: Sai mật khẩu và user không tồn tại hiển thị ErrorBanner giống hệt nhau (INVALID_CREDENTIALS)")
    void tcAuth04_05_wrongPasswordAndUnknownUserShowSameBanner() {
        LoginPage loginPage = new LoginPage(driver, wait).open();

        // TC-AUTH-04: Wrong password
        loginPage.submitCredentials(SeleniumConfig.OWNER_01, "WrongPassword@123");
        String wrongPassBanner = loginPage.getErrorBannerText();
        assertTrue(wrongPassBanner.contains("Thông tin đăng nhập không chính xác."));
        assertTrue(wrongPassBanner.contains("INVALID_CREDENTIALS"));
        assertNull(getSessionStorage("qnu_events_access_token"));
        assertTrue(driver.getCurrentUrl().endsWith("/login"));

        // Dismiss banner & verify TC-AUTH-05: Non-existent user
        loginPage.dismissErrorBanner();
        loginPage.submitCredentials("non_existent_user_999", "AnyPassword@123");
        String unknownUserBanner = loginPage.getErrorBannerText();
        assertEquals(wrongPassBanner, unknownUserBanner, "Thông báo lỗi UI phải giống hệt nhau để chống dò tài khoản");
    }

    @Test
    @DisplayName("TC-AUTH-06: Đăng nhập bằng tài khoản bị khóa (LOCKED) hiển thị ErrorBanner FORBIDDEN")
    void tcAuth06_lockedAccountShowsForbiddenBanner() throws Exception {
        assumeTrue(DB.isAvailable(), "Yêu cầu PostgreSQL để tạo tài khoản LOCKED tạm thời");
        String loginId = "uilock_" + UUID.randomUUID().toString().substring(0, 8);
        UUID userId = DB.createTemporaryUser(
                loginId, SeleniumConfig.demoPassword(), "UI Locked User",
                null, UUID.fromString(SeleniumConfig.CLASS_K49CNTT1_ID), "STUDENT", "LOCKED"
        );
        trackUserForCleanup(userId);

        LoginPage loginPage = new LoginPage(driver, wait);
        loginPage.loginAs(loginId, SeleniumConfig.demoPassword());

        String banner = loginPage.getErrorBannerText();
        assertTrue(banner.contains("Tài khoản của bạn đã bị khóa."));
        assertTrue(banner.contains("FORBIDDEN"));
        assertNull(getSessionStorage("qnu_events_access_token"));
    }

    @Test
    @DisplayName("TC-AUTH-07: Duy trì và đồng bộ phiên đăng nhập khi tải lại trang (F5)")
    void tcAuth07_sessionPersistsAfterPageReload() {
        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        waitForPath("/events");

        driver.navigate().refresh();
        waitForPath("/events");

        LayoutComponent layout = new LayoutComponent(driver, wait);
        assertTrue(layout.getHeaderText().contains("Trần Sinh Viên 1"));
        assertNotNull(getSessionStorage("qnu_events_access_token"));
    }

    @Test
    @DisplayName("TC-AUTH-23: Biên chuỗi khoảng trắng '   ' (BE trả 400 VALIDATION_FAILED) và chuỗi 1 ký tự (401 INVALID_CREDENTIALS)")
    void tcAuth23_whitespaceAndSingleCharLoginBoundary() {
        LoginPage loginPage = new LoginPage(driver, wait).open();

        // Whitespace passes Zod min(1), hits BE @NotBlank -> 400 VALIDATION_FAILED
        loginPage.submitCredentials("   ", SeleniumConfig.demoPassword());
        String blankBanner = loginPage.getErrorBannerText();
        assertTrue(blankBanner.contains("Dữ liệu không hợp lệ."));
        assertTrue(blankBanner.contains("VALIDATION_FAILED"));

        // 1-char credentials -> 401 INVALID_CREDENTIALS
        loginPage.dismissErrorBanner();
        loginPage.submitCredentials("a", "x");
        String oneCharBanner = loginPage.getErrorBannerText();
        assertTrue(oneCharBanner.contains("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("TC-AUTH-08: Đăng xuất qua hộp thoại ConfirmDialog (Hủy giữ phiên -> Xác nhận xóa sessionStorage & đẩy về /login)")
    void tcAuth08_logoutWithConfirmDialogCancelAndConfirm() {
        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_01, SeleniumConfig.demoPassword());
        waitForPath("/events");

        LayoutComponent layout = new LayoutComponent(driver, wait);
        layout.clickLogoutButton();
        assertTrue(layout.getLogoutDialogTitle().contains("Đăng xuất khỏi hệ thống"));

        // Cancel keeps session active
        layout.cancelLogoutDialog();
        assertFalse(layout.isConfirmDialogVisible());
        assertNotNull(getSessionStorage("qnu_events_access_token"));

        // Confirm clears sessionStorage and redirects to /login
        layout.clickLogoutButton();
        layout.confirmLogoutDialog();
        waitForPath("/login");
        assertNull(getSessionStorage("qnu_events_access_token"));
        assertNull(getSessionStorage("qnu_events_user_info"));

        // Attempting to navigate back to /events redirects to /login
        openPath("/events");
        waitForPath("/login");
    }

    @Test
    @DisplayName("TC-AUTH-17: Chặn truy cập các đường dẫn nội bộ (/events, /events/new, /history) khi chưa đăng nhập")
    void tcAuth17_unauthenticatedRoutesRedirectToLogin() {
        for (String protectedRoute : new String[]{"/events", "/events/new", "/history"}) {
            openPath(protectedRoute);
            waitForPath("/login");
        }
    }

    @Test
    @DisplayName("TC-AUTH-18 & TC-AUTH-19: Bảo vệ route theo vai trò (STUDENT chặn khỏi /new,/edit,/qr; OWNER chặn khỏi /history)")
    void tcAuth18_19_roleBasedRouteProtection() {
        // TC-AUTH-18: Student blocked from owner routes
        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        waitForPath("/events");

        openPath("/events/new");
        waitForPath("/events");

        openPath("/events/" + SeleniumConfig.CLASS_K49CNTT1_ID + "/edit");
        waitForPath("/events");

        openPath("/events/" + SeleniumConfig.CLASS_K49CNTT1_ID + "/qr");
        waitForPath("/events");

        // Switch to Owner -> TC-AUTH-19: Owner blocked from /history
        clearBrowserStorage();
        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_01, SeleniumConfig.demoPassword());
        waitForPath("/events");

        openPath("/history");
        waitForPath("/events");
    }

    @Test
    @DisplayName("TC-AUTH-24: Thông báo lỗi khi mất kết nối mạng & deep-link redirect quay lại đúng trang sau khi đăng nhập")
    void tcAuth24_offlineErrorBannerAndDeepLinkRedirect() {
        // Direct navigation to /events?tab=test or /history while unauthenticated
        openPath("/history");
        waitForPath("/login");

        LoginPage loginPage = new LoginPage(driver, wait);

        // Simulate offline network via Chrome DevTools Protocol
        setNetworkOffline(true);
        loginPage.submitCredentials(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        String offlineError = loginPage.getErrorBannerText();
        assertTrue(offlineError.contains("Không thể kết nối đến máy chủ"),
                "Phải hiển thị thông báo lỗi mạng tiếng Việt khi offline");

        // Restore network and login -> redirected straight to /history (deep-link state.from)
        setNetworkOffline(false);
        loginPage.dismissErrorBanner();
        loginPage.submitCredentials(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        waitForPath("/history");
    }
}
