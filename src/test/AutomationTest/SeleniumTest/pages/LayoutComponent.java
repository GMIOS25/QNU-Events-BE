package SeleniumTest.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * Page Object for shared Layout header, sidebar navigation, mobile bottom nav & Logout ConfirmDialog.
 */
public class LayoutComponent {

    private static final By HEADER = By.tagName("header");
    private static final By LOGOUT_HEADER_BTN = By.cssSelector("header button[title='Đăng xuất']");
    private static final By CONFIRM_DIALOG = By.cssSelector("div[role='dialog']");
    private static final By CONFIRM_DIALOG_TITLE = By.id("dialog-title");
    private static final By DIALOG_CANCEL_BTN = By.xpath("//div[@role='dialog']//button[normalize-space(.)='Hủy']");
    private static final By DIALOG_CONFIRM_LOGOUT_BTN = By.xpath("//div[@role='dialog']//button[normalize-space(.)='Đăng xuất']");
    private static final By SIDEBAR = By.cssSelector("aside");
    private static final By MOBILE_BOTTOM_NAV = By.cssSelector("nav[aria-label='Điều hướng di động']");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public LayoutComponent(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public String getHeaderText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(HEADER)).getText();
    }

    public String getSidebarText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(SIDEBAR)).getText();
    }

    public boolean hasSidebarLink(String linkText) {
        List<WebElement> links = driver.findElements(
                By.xpath("//aside//a[contains(normalize-space(.), '" + linkText + "')]"));
        return links.stream().anyMatch(WebElement::isDisplayed);
    }

    public void clickSidebarLink(String linkText) {
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//aside//a[contains(normalize-space(.), '" + linkText + "')]"))).click();
    }

    public boolean isMobileBottomNavDisplayed() {
        List<WebElement> navs = driver.findElements(MOBILE_BOTTOM_NAV);
        return !navs.isEmpty() && navs.get(0).isDisplayed();
    }

    public String getMobileBottomNavText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(MOBILE_BOTTOM_NAV)).getText();
    }

    public void clickLogoutButton() {
        wait.until(ExpectedConditions.elementToBeClickable(LOGOUT_HEADER_BTN)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(CONFIRM_DIALOG));
    }

    public String getLogoutDialogTitle() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(CONFIRM_DIALOG_TITLE)).getText();
    }

    public boolean isConfirmDialogVisible() {
        List<WebElement> dialogs = driver.findElements(CONFIRM_DIALOG);
        return !dialogs.isEmpty() && dialogs.get(0).isDisplayed();
    }

    public void cancelLogoutDialog() {
        wait.until(ExpectedConditions.elementToBeClickable(DIALOG_CANCEL_BTN)).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(CONFIRM_DIALOG));
    }

    public void confirmLogoutDialog() {
        wait.until(ExpectedConditions.elementToBeClickable(DIALOG_CONFIRM_LOGOUT_BTN)).click();
    }
}
