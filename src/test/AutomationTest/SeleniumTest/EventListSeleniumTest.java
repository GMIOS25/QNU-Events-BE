package SeleniumTest;

import SeleniumTest.config.SeleniumConfig;
import SeleniumTest.config.SeleniumTestDataFactory;
import SeleniumTest.config.BaseSeleniumTest;
import SeleniumTest.pages.EventDetailPage;
import SeleniumTest.pages.EventsPage;
import SeleniumTest.pages.LayoutComponent;
import SeleniumTest.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Module 4.3: Selenium WebDriver UI/E2E tests for Event List, Search, Detail, Anti-IDOR 404 Banners,
 * StatusBadge Accessibility, Keyboard Navigation & Multi-breakpoint Responsive Layout.
 * Traceability: docs/test/testcase/4.3-danh-sach-su-kien.md
 */
@DisplayName("4.3 Selenium UI - Danh sách, Tìm kiếm, Chi tiết Sự kiện & Responsive")
class EventListSeleniumTest extends BaseSeleniumTest {

    @Test
    @DisplayName("TC-4.3-01, 02 & 03: Hiển thị danh sách theo vai trò & phân lập lớp/owner trên FE")
    void tc43_01_02_03_listDisplayAndRoleClassIsolation() {
        String owner1Token = apiLogin(SeleniumConfig.OWNER_01);
        String owner2Token = apiLogin(SeleniumConfig.OWNER_02);

        String pubK49 = SeleniumTestDataFactory.uniqueTitle("UI-PUB-K49");
        String draftK49 = SeleniumTestDataFactory.uniqueTitle("UI-DRAFT-K49");
        String pubOwn2 = SeleniumTestDataFactory.uniqueTitle("UI-PUB-OWN2");

        apiCreateAndPublishEvent(owner1Token, SeleniumTestDataFactory.validFutureEventPayload(
                pubK49, 25, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));
        apiCreateDraftEvent(owner1Token, SeleniumTestDataFactory.validFutureEventPayload(
                draftK49, 25, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));
        apiCreateAndPublishEvent(owner2Token, SeleniumTestDataFactory.validFutureEventPayload(
                pubOwn2, 25, List.of(SeleniumConfig.CLASS_K49CNTT2_ID)));

        // TC-4.3-01: sv001 (K49CNTT1) sees pubK49, not draftK49, not pubOwn2, no Create button
        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        EventsPage eventsPage = new EventsPage(driver, wait).waitUntilLoaded();
        assertEquals("Khám phá sự kiện", eventsPage.getPageHeading());
        assertFalse(eventsPage.isCreateEventButtonVisible());
        eventsPage.search(pubK49);
        eventsPage.waitForEventCardVisible(pubK49);
        String cardText = eventsPage.getEventCardText(pubK49);
        assertTrue(cardText.contains("Đã công bố"));
        assertTrue(cardText.contains("0/25 suất"));

        eventsPage.search(draftK49);
        wait.until(d -> eventsPage.getMainText().contains("Không tìm thấy sự kiện phù hợp"));

        // TC-4.3-02: sv003 (K49CNTT2) cannot see pubK49
        clearBrowserStorage();
        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_03, SeleniumConfig.demoPassword());
        EventsPage sv3Page = new EventsPage(driver, wait).waitUntilLoaded();
        sv3Page.search(pubK49);
        wait.until(d -> sv3Page.getMainText().contains("Không tìm thấy sự kiện phù hợp"));
        sv3Page.search(pubOwn2);
        sv3Page.waitForEventCardVisible(pubOwn2);

        // TC-4.3-03: owner01 sees own DRAFT and PUBLISHED, not owner02's event
        clearBrowserStorage();
        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_01, SeleniumConfig.demoPassword());
        EventsPage ownerPage = new EventsPage(driver, wait).waitUntilLoaded();
        assertEquals("Quản lý sự kiện đơn vị", ownerPage.getPageHeading());
        assertTrue(ownerPage.isCreateEventButtonVisible());

        ownerPage.search(draftK49);
        ownerPage.waitForEventCardVisible(draftK49);
        assertTrue(ownerPage.getEventCardText(draftK49).contains("Bản nháp"));

        ownerPage.search(pubOwn2);
        wait.until(d -> ownerPage.getMainText().contains("Không tìm thấy sự kiện phù hợp"));
    }

    @Test
    @DisplayName("TC-4.3-05 & TC-4.3-06: Tìm kiếm không phân biệt chữ hoa/thường (debounce 350ms) & EmptyState khi không khớp")
    void tc43_05_06_searchDebounceAndEmptyState() {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String marker = "AIUI_" + UUID.randomUUID().toString().substring(0, 6);
        String title = "Hội thảo Ứng Dụng " + marker;
        apiCreateAndPublishEvent(ownerToken, SeleniumTestDataFactory.validFutureEventPayload(
                title, 20, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));

        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        EventsPage eventsPage = new EventsPage(driver, wait).waitUntilLoaded();

        // Case-insensitive search
        eventsPage.search("ứng dụng " + marker.toLowerCase());
        eventsPage.waitForEventCardVisible(title);

        // Non-matching search -> EmptyState + Reset filters button
        eventsPage.search("khongtontai_xyz_999_" + marker);
        wait.until(d -> eventsPage.getMainText().contains("Không tìm thấy sự kiện phù hợp"));
        eventsPage.clickResetFilters();
        eventsPage.waitUntilLoaded();
    }

    @Test
    @DisplayName("TC-4.3-08, 10, 11 & 12: Xem chi tiết sự kiện (Student mở/hết chỗ/đã đăng ký vs Owner DRAFT/PUBLISHED)")
    void tc43_08_10_11_12_eventDetailStatesForStudentAndOwner() {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        String sv1Token = apiLogin(SeleniumConfig.STUDENT_01);
        String sv2Token = apiLogin(SeleniumConfig.STUDENT_02);

        String titleOpen = SeleniumTestDataFactory.uniqueTitle("Detail Open K49");
        String openId = apiCreateAndPublishEvent(ownerToken, SeleniumTestDataFactory.activeNowEventPayload(
                titleOpen, 1, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));
        String draftId = apiCreateDraftEvent(ownerToken, SeleniumTestDataFactory.validFutureEventPayload(
                SeleniumTestDataFactory.uniqueTitle("Detail Owner Draft"), 10, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));

        // TC-4.3-08: sv001 opens detail -> sees facts + RegistrationSlot + AttendanceSlot
        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        EventsPage eventsPage = new EventsPage(driver, wait).waitUntilLoaded();
        eventsPage.search(titleOpen);
        eventsPage.openEventDetailByTitle(titleOpen);
        waitForPath("/events/" + openId);

        EventDetailPage detail = new EventDetailPage(driver, wait).waitUntilReady();
        String text = detail.getMainText();
        assertTrue(text.contains("Đã công bố"));
        assertTrue(text.contains("Thời gian diễn ra"));
        assertTrue(text.contains("Địa điểm"));
        assertTrue(text.contains("Cửa sổ điểm danh QR"));
        assertTrue(text.contains("Suất tham gia sự kiện"));
        assertTrue(detail.isAttendanceSlotVisible());

        // Back to list works
        detail.clickBackToList();
        waitForPath("/events");

        // sv001 registers via API -> TC-4.3-11: sv001 sees "✓ Bạn đã giữ chỗ" on list & "Đã đăng ký" on detail
        apiRegisterStudent(sv1Token, openId);
        eventsPage.open().search(titleOpen);
        eventsPage.waitForEventCardVisible(titleOpen);
        assertTrue(eventsPage.getEventCardText(titleOpen).contains("Bạn đã giữ chỗ"));

        detail.open(openId);
        assertTrue(detail.getMainText().contains("Bạn đã giữ chỗ thành công!"));

        // TC-4.3-10: sv002 sees full quota (0 remaining) -> "Đã đóng đăng ký" on card & "Sự kiện đã hết suất tham gia." on detail
        clearBrowserStorage();
        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_02, SeleniumConfig.demoPassword());
        EventsPage sv2List = new EventsPage(driver, wait).waitUntilLoaded();
        sv2List.search(titleOpen);
        sv2List.waitForEventCardVisible(titleOpen);
        assertTrue(sv2List.getEventCardText(titleOpen).contains("Đã đóng đăng ký"));

        EventDetailPage sv2Detail = new EventDetailPage(driver, wait).open(openId);
        assertFalse(sv2Detail.isRegisterButtonVisible());
        assertTrue(sv2Detail.getMainText().contains("Sự kiện đã hết suất tham gia."));

        // TC-4.3-12: Owner opens own DRAFT & PUBLISHED detail
        clearBrowserStorage();
        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_01, SeleniumConfig.demoPassword());
        EventDetailPage ownerDraft = new EventDetailPage(driver, wait).open(draftId);
        assertTrue(ownerDraft.getMainText().contains("Bản nháp"));
        assertTrue(ownerDraft.isEditButtonVisible());
        assertTrue(ownerDraft.isPreviewPublishButtonVisible());
        assertTrue(ownerDraft.getMainText().contains("Chế độ quản lý"));
        assertFalse(ownerDraft.isAttendanceSlotVisible());

        EventDetailPage ownerPub = new EventDetailPage(driver, wait).open(openId);
        assertTrue(ownerPub.getMainText().contains("Đã công bố"));
        assertTrue(ownerPub.isOpenQrScreenButtonVisible());
        assertFalse(ownerPub.isAttendanceSlotVisible());
    }

    @Test
    @DisplayName("TC-4.3-13, 14, 15 & 16: Truy cập trực tiếp URL sự kiện ngoài quyền / UUID không tồn tại hiển thị banner lỗi 404")
    void tc43_13_14_15_16_directUrl404ErrorBanners() {
        String owner1Token = apiLogin(SeleniumConfig.OWNER_01);
        String draftId = apiCreateDraftEvent(owner1Token, SeleniumTestDataFactory.validFutureEventPayload(
                SeleniumTestDataFactory.uniqueTitle("404 Draft"), 10, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));
        String pubK49Id = apiCreateAndPublishEvent(owner1Token, SeleniumTestDataFactory.validFutureEventPayload(
                SeleniumTestDataFactory.uniqueTitle("404 K49 Only"), 10, List.of(SeleniumConfig.CLASS_K49CNTT1_ID)));

        String expectedBanner = "Không tìm thấy sự kiện hoặc bạn không có quyền xem sự kiện này.";

        // TC-4.3-13 & 16: sv001 on DRAFT and non-existent UUID
        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        waitForPath("/events");
        assertTrue(new EventDetailPage(driver, wait).open(draftId).getErrorBannerText().contains(expectedBanner));
        assertTrue(new EventDetailPage(driver, wait).open(SeleniumConfig.NON_EXISTENT_UUID).getErrorBannerText().contains(expectedBanner));

        // TC-4.3-14: sv003 (K49CNTT2) on K49CNTT1 event
        clearBrowserStorage();
        new LoginPage(driver, wait).loginAs(SeleniumConfig.STUDENT_03, SeleniumConfig.demoPassword());
        waitForPath("/events");
        assertTrue(new EventDetailPage(driver, wait).open(pubK49Id).getErrorBannerText().contains(expectedBanner));

        // TC-4.3-15: owner02 on owner01's event
        clearBrowserStorage();
        new LoginPage(driver, wait).loginAs(SeleniumConfig.OWNER_02, SeleniumConfig.demoPassword());
        waitForPath("/events");
        assertTrue(new EventDetailPage(driver, wait).open(pubK49Id).getErrorBannerText().contains(expectedBanner));
    }

    @Test
    @DisplayName("TC-4.3-19, 20 & 24: StatusBadge có chữ+icon (A11y), Responsive 320px/640px/768px/1024px, bàn phím & Deep-link /events/:id")
    void tc43_19_20_24_accessibilityResponsiveAndDeepLink() {
        String ownerToken = apiLogin(SeleniumConfig.OWNER_01);
        Map<String, Object> noDescPayload = SeleniumTestDataFactory.validFutureEventPayload(
                SeleniumTestDataFactory.uniqueTitle("DeepLink NoDesc"), 20, List.of(SeleniumConfig.CLASS_K49CNTT1_ID));
        noDescPayload.put("description", null);
        String noDescId = apiCreateAndPublishEvent(ownerToken, noDescPayload);

        // TC-4.3-24: Deep-link /events/:id while logged out -> login -> lands on /events/:id
        openPath("/events/" + noDescId);
        waitForPath("/login");
        new LoginPage(driver, wait).submitCredentials(SeleniumConfig.STUDENT_01, SeleniumConfig.demoPassword());
        waitForPath("/events/" + noDescId);

        EventDetailPage detail = new EventDetailPage(driver, wait).waitUntilReady();
        // TC-4.3-19: Every StatusBadge renders icon SVG (aria-hidden="true") alongside Vietnamese label text
        List<WebElement> badges = detail.findStatusBadgesWithIcons();
        assertFalse(badges.isEmpty(), "Phải có StatusBadge kèm icon SVG aria-hidden='true'");
        assertTrue(badges.stream().anyMatch(b -> b.getText().contains("Đã công bố")));

        // TC-4.3-20 & 24: Responsive checks at 320px, 640px, 768px, 1024px
        EventsPage eventsPage = new EventsPage(driver, wait).open();
        LayoutComponent layout = new LayoutComponent(driver, wait);

        setViewport(320, 568, true);
        assertFalse(hasHorizontalOverflow(), "Trang /events ở 320px không được tràn ngang");
        assertTrue(layout.isMobileBottomNavDisplayed(), "Ở 320px phải hiện thanh điều hướng dưới cùng");
        assertTrue(layout.getMobileBottomNavText().contains("Sự kiện"));

        // Keyboard focus on search input
        WebElement searchInput = eventsPage.getSearchInput();
        searchInput.sendKeys("test");
        searchInput.sendKeys(Keys.TAB);

        for (int width : new int[]{640, 768, 1024}) {
            setViewport(width, 800, false);
            assertFalse(hasHorizontalOverflow(), "Trang /events ở " + width + "px không được tràn ngang");
        }
        resetViewport();
    }
}
