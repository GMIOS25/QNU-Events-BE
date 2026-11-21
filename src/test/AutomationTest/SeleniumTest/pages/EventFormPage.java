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
 * Page Object for EventFormPage (/events/new and /events/:id/edit).
 * Uses label-anchored XPaths because Field.tsx generates dynamic IDs when name/id is omitted.
 */
public class EventFormPage {

    private static final By HEADING = By.cssSelector("main h1");
    private static final By ERROR_BANNER = By.cssSelector("main div[role='alert']");
    private static final By DESCRIPTION_TEXTAREA = By.xpath("//label[contains(normalize-space(.), 'Mô tả sự kiện')]/following-sibling::textarea");
    private static final By SELECT_ALL_CLASSES_BTN = By.xpath("//button[contains(normalize-space(.), 'Chọn tất cả các lớp')]");
    private static final By SUBMIT_BTN = By.cssSelector("main form button[type='submit']");
    private static final By CLASS_CHECKBOXES = By.cssSelector("main form input[type='checkbox']");
    private static final By CLASS_SECTION_HEADING = By.xpath("//h2[contains(normalize-space(.), 'Lớp đối tượng được mời tham gia')]");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public EventFormPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public EventFormPage openCreate() {
        driver.get(SeleniumConfig.feBaseUrl() + "/events/new");
        wait.until(ExpectedConditions.visibilityOfElementLocated(HEADING));
        wait.until(ExpectedConditions.presenceOfElementLocated(CLASS_CHECKBOXES));
        return this;
    }

    public EventFormPage openEdit(String eventId) {
        driver.get(SeleniumConfig.feBaseUrl() + "/events/" + eventId + "/edit");
        wait.until(ExpectedConditions.visibilityOfElementLocated(HEADING));
        wait.until(d -> !getInputByLabel("Tiêu đề sự kiện").getAttribute("value").isBlank());
        return this;
    }

    public String getHeading() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(HEADING)).getText();
    }

    public EventFormPage setTitle(String title) {
        setTextInput(getInputByLabel("Tiêu đề sự kiện"), title);
        return this;
    }

    public EventFormPage setDescription(String description) {
        WebElement ta = wait.until(ExpectedConditions.visibilityOfElementLocated(DESCRIPTION_TEXTAREA));
        clearWithKeys(ta);
        if (description != null && !description.isEmpty()) {
            ta.sendKeys(description);
        }
        return this;
    }

    public EventFormPage setLocation(String location) {
        setTextInput(getInputByLabel("Địa điểm tổ chức"), location);
        return this;
    }

    public EventFormPage setQuotaTotal(int quota) {
        setControlledInputValue(getInputByLabel("Chỉ tiêu số lượng tham gia"), String.valueOf(quota));
        return this;
    }

    public EventFormPage setStartAt(String localDateTimeIso) {
        setControlledInputValue(getInputByLabel("Thời gian bắt đầu sự kiện"), localDateTimeIso);
        return this;
    }

    public EventFormPage setEndAt(String localDateTimeIso) {
        setControlledInputValue(getInputByLabel("Thời gian kết thúc sự kiện"), localDateTimeIso);
        return this;
    }

    public EventFormPage setRegistrationDeadline(String localDateTimeIso) {
        setControlledInputValue(getInputByLabel("Hạn chót sinh viên đăng ký giữ chỗ"), localDateTimeIso);
        return this;
    }

    public EventFormPage setAttendanceOpenAt(String localDateTimeIso) {
        setControlledInputValue(getInputByLabel("Mở cửa sổ điểm danh QR"), localDateTimeIso);
        return this;
    }

    public EventFormPage setAttendanceCloseAt(String localDateTimeIso) {
        setControlledInputValue(getInputByLabel("Đóng cửa sổ điểm danh QR"), localDateTimeIso);
        return this;
    }

    public EventFormPage setActivityType(String activityType) {
        setTextInput(getInputByLabel("Loại hoạt động rèn luyện"), activityType);
        return this;
    }

    public EventFormPage setActivityPoints(int points) {
        setControlledInputValue(getInputByLabel("Điểm rèn luyện cộng thêm"), String.valueOf(points));
        return this;
    }

    public EventFormPage toggleClassByCode(String classCode) {
        WebElement checkbox = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//label[contains(normalize-space(.), '" + classCode + "')]//input[@type='checkbox']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", checkbox);
        checkbox.click();
        return this;
    }

    public EventFormPage clickSelectAllClasses() {
        wait.until(ExpectedConditions.elementToBeClickable(SELECT_ALL_CLASSES_BTN)).click();
        return this;
    }

    public String getClassSectionHeadingText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(CLASS_SECTION_HEADING)).getText();
    }

    public List<WebElement> getAllClassCheckboxes() {
        return driver.findElements(CLASS_CHECKBOXES);
    }

    public long getSelectedClassCount() {
        return getAllClassCheckboxes().stream().filter(WebElement::isSelected).count();
    }

    public String getStartAtValue() {
        return getInputByLabel("Thời gian bắt đầu sự kiện").getAttribute("value");
    }

    /**
     * Disables native HTML5 form constraint validation briefly when testing custom React validation errors
     * (e.g. blank spaces or short titles that might otherwise trigger browser tooltips).
     */
    public void disableHtml5Validation() {
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelector('main form')?.setAttribute('novalidate', 'true');"
        );
    }

    public void clickSubmit() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(SUBMIT_BTN));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        btn.click();
    }

    public String getErrorBannerText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR_BANNER)).getText();
    }

    private WebElement getInputByLabel(String labelPart) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//label[contains(normalize-space(.), '" + labelPart + "')]/following-sibling::input")));
    }

    private void setTextInput(WebElement input, String value) {
        clearWithKeys(input);
        if (value != null && !value.isEmpty()) {
            input.sendKeys(value);
        }
    }

    private void clearWithKeys(WebElement element) {
        element.click();
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        element.sendKeys(Keys.BACK_SPACE);
    }

    /**
     * React-safe native input value setter so datetime-local (YYYY-MM-DDTHH:mm) and number inputs
     * behave deterministically across OS locales in Chrome/Edge.
     */
    private void setControlledInputValue(WebElement input, String value) {
        ((JavascriptExecutor) driver).executeScript("""
                const el = arguments[0];
                const val = arguments[1];
                const proto = window.HTMLInputElement.prototype;
                const setter = Object.getOwnPropertyDescriptor(proto, 'value').set;
                setter.call(el, val);
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
                """, input, value);
    }
}
