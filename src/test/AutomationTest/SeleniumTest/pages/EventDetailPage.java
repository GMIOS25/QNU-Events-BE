package SeleniumTest.pages;

import SeleniumTest.config.SeleniumConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * Page Object for EventDetailPage (/events/:id), including Preview & Publish Modal,
 * RegistrationSlot and AttendanceSlot.
 */
public class EventDetailPage {

    private static final By EVENT_TITLE = By.cssSelector("main h1");
    private static final By BACK_TO_LIST_BTN = By.xpath("//main//button[contains(normalize-space(.), 'Quay lại danh sách')]");
    private static final By EDIT_BTN = By.xpath("//main//button[contains(normalize-space(.), 'Chỉnh sửa')]");
    private static final By PREVIEW_PUBLISH_BTN = By.xpath("//main//button[contains(normalize-space(.), 'Xem trước & Công bố')]");
    private static final By OPEN_QR_SCREEN_BTN = By.xpath("//main//button[contains(normalize-space(.), 'Màn hình chiếu QR')]");
    private static final By ERROR_BANNER = By.cssSelector("main div[role='alert']");
    private static final By DISMISS_ERROR_BTN = By.cssSelector("main button[aria-label='Đóng thông báo lỗi']");

    // Preview & Publish Modal
    private static final By PREVIEW_MODAL = By.xpath("//div[contains(@class,'fixed')][.//button[contains(normalize-space(.), 'Xác nhận công bố')]]");
    private static final By PREVIEW_CONFIRM_CHECKBOX = By.xpath("//div[contains(@class,'fixed')]//input[@type='checkbox']");
    private static final By PREVIEW_CANCEL_BTN = By.xpath("//div[contains(@class,'fixed')]//button[contains(normalize-space(.), 'Hủy bỏ')]");
    private static final By PREVIEW_CONFIRM_BTN = By.xpath("//div[contains(@class,'fixed')]//button[contains(normalize-space(.), 'Xác nhận công bố')]");

    // RegistrationSlot
    private static final By REGISTER_BTN = By.xpath("//main//button[contains(normalize-space(.), 'Đăng ký giữ chỗ')]");

    // AttendanceSlot
    private static final By ATTENDANCE_HEADING = By.xpath("//main//h3[contains(normalize-space(.), 'Điểm danh sự kiện (QR Động)')]");
    private static final By ATTEMPT_COUNTER_BADGE = By.cssSelector("[data-testid='attempt-counter-badge']");
    private static final By CAMERA_TOGGLE_BTN = By.xpath("//main//button[contains(normalize-space(.), 'Quét mã QR bằng Camera') or contains(normalize-space(.), 'Tắt Camera Quét')]");
    private static final By CLOSE_CAMERA_BTN = By.xpath("//main//button[contains(normalize-space(.), 'Đóng camera')]");
    private static final By QR_READER_CONTAINER = By.id("qr-reader-container");
    private static final By MANUAL_TOKEN_INPUT = By.cssSelector("main input[placeholder*='Dán chuỗi token dự phòng'], main input[placeholder='Chức năng đã bị khóa']");
    private static final By SUBMIT_TOKEN_BTN = By.xpath("//main//button[contains(normalize-space(.), 'Gửi mã')]");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public EventDetailPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public EventDetailPage open(String eventId) {
        driver.get(SeleniumConfig.feBaseUrl() + "/events/" + eventId);
        waitUntilReady();
        return this;
    }

    public EventDetailPage waitUntilReady() {
        wait.until(d -> !d.findElements(EVENT_TITLE).isEmpty() || !d.findElements(ERROR_BANNER).isEmpty());
        return this;
    }

    public String getTitle() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(EVENT_TITLE)).getText();
    }

    public String getMainText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("main"))).getText();
    }

    public void clickBackToList() {
        wait.until(ExpectedConditions.elementToBeClickable(BACK_TO_LIST_BTN)).click();
    }

    public boolean isEditButtonVisible() {
        return isVisible(EDIT_BTN);
    }

    public void clickEdit() {
        wait.until(ExpectedConditions.elementToBeClickable(EDIT_BTN)).click();
    }

    public boolean isPreviewPublishButtonVisible() {
        return isVisible(PREVIEW_PUBLISH_BTN);
    }

    public void clickPreviewAndPublish() {
        wait.until(ExpectedConditions.elementToBeClickable(PREVIEW_PUBLISH_BTN)).click();
    }

    public boolean isOpenQrScreenButtonVisible() {
        return isVisible(OPEN_QR_SCREEN_BTN);
    }

    public void clickOpenQrScreen() {
        wait.until(ExpectedConditions.elementToBeClickable(OPEN_QR_SCREEN_BTN)).click();
    }

    public String getErrorBannerText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR_BANNER)).getText();
    }

    public boolean isErrorBannerVisible() {
        return isVisible(ERROR_BANNER);
    }

    public void dismissErrorBanner() {
        wait.until(ExpectedConditions.elementToBeClickable(DISMISS_ERROR_BTN)).click();
    }

    // --- Preview & Publish Modal ---

    public String waitForPreviewModalText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(PREVIEW_MODAL)).getText();
    }

    public boolean isPreviewModalVisible() {
        return isVisible(PREVIEW_MODAL);
    }

    public boolean isConfirmPublishButtonEnabled() {
        WebElement btn = wait.until(ExpectedConditions.visibilityOfElementLocated(PREVIEW_CONFIRM_BTN));
        return btn.isEnabled();
    }

    public void toggleConfirmPublishCheckbox() {
        wait.until(ExpectedConditions.elementToBeClickable(PREVIEW_CONFIRM_CHECKBOX)).click();
    }

    public void clickCancelPreviewModal() {
        wait.until(ExpectedConditions.elementToBeClickable(PREVIEW_CANCEL_BTN)).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(PREVIEW_MODAL));
    }

    public void clickConfirmPublish() {
        wait.until(ExpectedConditions.elementToBeClickable(PREVIEW_CONFIRM_BTN)).click();
    }

    // --- RegistrationSlot ---

    public boolean isRegisterButtonVisible() {
        return isVisible(REGISTER_BTN);
    }

    public WebElement getRegisterButton() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(REGISTER_BTN));
    }

    public void clickRegisterButton() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(REGISTER_BTN));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        btn.click();
    }

    public void waitForRegisteredSuccess() {
        wait.until(d -> getMainText().contains("Bạn đã giữ chỗ thành công!"));
    }

    // --- AttendanceSlot ---

    public boolean isAttendanceSlotVisible() {
        return isVisible(ATTENDANCE_HEADING);
    }

    public String getAttemptCounterText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(ATTEMPT_COUNTER_BADGE)).getText();
    }

    public void clickToggleCamera() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(CAMERA_TOGGLE_BTN));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        btn.click();
    }

    public boolean isCameraToggleEnabled() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(CAMERA_TOGGLE_BTN)).isEnabled();
    }

    public boolean isQrReaderContainerVisible() {
        return isVisible(QR_READER_CONTAINER);
    }

    public void clickCloseCamera() {
        wait.until(ExpectedConditions.elementToBeClickable(CLOSE_CAMERA_BTN)).click();
    }

    public void enterManualQrToken(String token) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(MANUAL_TOKEN_INPUT));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", input);
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(Keys.BACK_SPACE);
        if (token != null && !token.isEmpty()) {
            input.sendKeys(token);
        }
    }

    public boolean isManualTokenInputEnabled() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(MANUAL_TOKEN_INPUT)).isEnabled();
    }

    public boolean isSubmitTokenButtonEnabled() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(SUBMIT_TOKEN_BTN)).isEnabled();
    }

    public void clickSubmitManualToken() {
        wait.until(ExpectedConditions.elementToBeClickable(SUBMIT_TOKEN_BTN)).click();
    }

    public void submitManualQrToken(String token) {
        enterManualQrToken(token);
        clickSubmitManualToken();
    }

    public void waitForCheckInSuccess() {
        wait.until(d -> getMainText().contains("Điểm danh thành công!"));
    }

    public List<WebElement> findStatusBadgesWithIcons() {
        return driver.findElements(By.xpath("//main//span[contains(@class,'rounded-full') and .//svg[@aria-hidden='true']]"));
    }

    private boolean isVisible(By locator) {
        List<WebElement> els = driver.findElements(locator);
        return !els.isEmpty() && els.get(0).isDisplayed();
    }
}
