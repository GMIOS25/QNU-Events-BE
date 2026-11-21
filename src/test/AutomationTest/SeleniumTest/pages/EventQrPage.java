package SeleniumTest.pages;

import SeleniumTest.config.SeleniumConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * Page Object for EventQrPage (/events/:id/qr).
 */
public class EventQrPage {

    private static final By PAGE_TITLE = By.tagName("h1");
    private static final By QR_SVG = By.cssSelector("div.bg-white svg");
    private static final By EPOCH_BADGE = By.xpath("//span[contains(normalize-space(.), 'Epoch #')]");
    private static final By COUNTDOWN_LABEL = By.xpath("//span[contains(normalize-space(.), 'Đổi mã sau:')]");
    private static final By REFRESH_NOW_BTN = By.xpath("//button[contains(normalize-space(.), 'Làm mới ngay')]");
    private static final By COPY_TOKEN_BTN = By.xpath("//button[contains(normalize-space(.), 'Chép mã') or contains(normalize-space(.), 'Đã chép')]");
    private static final By FALLBACK_TOKEN_TEXT = By.xpath("//div[contains(@class,'font-mono') and starts-with(normalize-space(.), 'qr1.')]");
    private static final By ERROR_BANNER = By.cssSelector("div[role='alert']");
    private static final By BACK_TO_DETAIL_BTN = By.xpath("//button[contains(normalize-space(.), 'Quay lại chi tiết')]");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public EventQrPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public EventQrPage open(String eventId) {
        driver.get(SeleniumConfig.feBaseUrl() + "/events/" + eventId + "/qr");
        wait.until(ExpectedConditions.visibilityOfElementLocated(PAGE_TITLE));
        return this;
    }

    public void waitForQrRendered() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(QR_SVG));
        wait.until(ExpectedConditions.visibilityOfElementLocated(FALLBACK_TOKEN_TEXT));
    }

    public boolean isQrSvgVisible() {
        List<WebElement> svgs = driver.findElements(QR_SVG);
        return !svgs.isEmpty() && svgs.get(0).isDisplayed();
    }

    public String getEpochBadgeText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(EPOCH_BADGE)).getText();
    }

    public String getCountdownText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(COUNTDOWN_LABEL)).getText();
    }

    public String getDisplayedToken() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(FALLBACK_TOKEN_TEXT)).getText().trim();
    }

    public void clickRefreshNow() {
        wait.until(ExpectedConditions.elementToBeClickable(REFRESH_NOW_BTN)).click();
    }

    public void clickCopyToken() {
        wait.until(ExpectedConditions.elementToBeClickable(COPY_TOKEN_BTN)).click();
    }

    public String getCopyButtonText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(COPY_TOKEN_BTN)).getText().trim();
    }

    public String getErrorBannerText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR_BANNER)).getText();
    }

    public String getBodyText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body"))).getText();
    }

    public void clickBackToDetail() {
        wait.until(ExpectedConditions.elementToBeClickable(BACK_TO_DETAIL_BTN)).click();
    }
}
