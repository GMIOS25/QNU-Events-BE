package SeleniumTest.pages;

import SeleniumTest.config.SeleniumConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * Page Object for QNU-Events-FE LoginPage (/login).
 */
public class LoginPage {

    private static final By LOGIN_ID_INPUT = By.cssSelector("input[name='loginId']");
    private static final By PASSWORD_INPUT = By.cssSelector("input[name='password']");
    private static final By REMEMBER_ME_CHECKBOX = By.cssSelector("input[name='rememberMe']");
    private static final By SUBMIT_BUTTON = By.cssSelector("button[type='submit']");
    private static final By QUICK_FILL_OWNER_BTN = By.xpath("//button[contains(normalize-space(.), 'Chủ sự kiện')]");
    private static final By QUICK_FILL_STUDENT_BTN = By.xpath("//button[contains(normalize-space(.), 'Sinh viên (K49)')]");
    private static final By ERROR_BANNER = By.cssSelector("div[role='alert']");
    private static final By DISMISS_ERROR_BTN = By.cssSelector("button[aria-label='Đóng thông báo lỗi']");
    private static final By LOGIN_ID_ERROR = By.id("loginId-error");
    private static final By PASSWORD_ERROR = By.id("password-error");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public LoginPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public LoginPage open() {
        driver.get(SeleniumConfig.feBaseUrl() + "/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(LOGIN_ID_INPUT));
        return this;
    }

    public LoginPage enterLoginId(String loginId) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(LOGIN_ID_INPUT));
        clearInput(input);
        if (loginId != null && !loginId.isEmpty()) {
            input.sendKeys(loginId);
        }
        return this;
    }

    public LoginPage enterPassword(String password) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_INPUT));
        clearInput(input);
        if (password != null && !password.isEmpty()) {
            input.sendKeys(password);
        }
        return this;
    }

    public LoginPage setRememberMe(boolean remember) {
        WebElement checkbox = wait.until(ExpectedConditions.elementToBeClickable(REMEMBER_ME_CHECKBOX));
        if (checkbox.isSelected() != remember) {
            checkbox.click();
        }
        return this;
    }

    public void clickSubmit() {
        wait.until(ExpectedConditions.elementToBeClickable(SUBMIT_BUTTON)).click();
    }

    public void loginAs(String loginId, String password) {
        open();
        enterLoginId(loginId);
        enterPassword(password);
        clickSubmit();
    }

    public void submitCredentials(String loginId, String password) {
        enterLoginId(loginId);
        enterPassword(password);
        clickSubmit();
    }

    public LoginPage clickQuickFillOwner() {
        wait.until(ExpectedConditions.elementToBeClickable(QUICK_FILL_OWNER_BTN)).click();
        return this;
    }

    public LoginPage clickQuickFillStudent() {
        wait.until(ExpectedConditions.elementToBeClickable(QUICK_FILL_STUDENT_BTN)).click();
        return this;
    }

    public String getLoginIdValue() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(LOGIN_ID_INPUT)).getAttribute("value");
    }

    public String getPasswordValue() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_INPUT)).getAttribute("value");
    }

    public String getErrorBannerText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR_BANNER)).getText();
    }

    public boolean isErrorBannerDisplayed() {
        List<WebElement> banners = driver.findElements(ERROR_BANNER);
        return !banners.isEmpty() && banners.get(0).isDisplayed();
    }

    public void dismissErrorBanner() {
        wait.until(ExpectedConditions.elementToBeClickable(DISMISS_ERROR_BTN)).click();
    }

    public String getLoginIdFieldError() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(LOGIN_ID_ERROR)).getText();
    }

    public String getPasswordFieldError() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_ERROR)).getText();
    }

    private void clearInput(WebElement element) {
        element.click();
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        element.sendKeys(Keys.BACK_SPACE);
    }
}
