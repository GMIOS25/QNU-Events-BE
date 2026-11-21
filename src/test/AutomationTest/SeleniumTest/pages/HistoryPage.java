package SeleniumTest.pages;

import SeleniumTest.config.SeleniumConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * Page Object for HistoryPage (/history).
 */
public class HistoryPage {

    private static final By PAGE_HEADING = By.cssSelector("main h1");
    private static final By LOADING_INDICATOR = By.xpath("//main//*[contains(text(), 'Đang tải lịch sử tham gia sự kiện')]");
    private static final By EXPLORE_EVENTS_BTN = By.xpath("//main//button[contains(normalize-space(.), 'Khám phá sự kiện')]");
    private static final By ERROR_BOX = By.xpath("//main//*[contains(text(), 'Không thể tải dữ liệu lịch sử sự kiện')]");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public HistoryPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public HistoryPage open() {
        driver.get(SeleniumConfig.feBaseUrl() + "/history");
        waitUntilLoaded();
        return this;
    }

    public HistoryPage waitUntilLoaded() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(PAGE_HEADING));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADING_INDICATOR));
        return this;
    }

    public String getHeading() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(PAGE_HEADING)).getText();
    }

    public String getKpiValue(String kpiLabel) {
        WebElement valueEl = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//main//p[normalize-space(.)='" + kpiLabel + "']/following-sibling::p[1]")));
        return valueEl.getText().trim();
    }

    public String getMainText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("main"))).getText();
    }

    public void clickExploreEventsButton() {
        wait.until(ExpectedConditions.elementToBeClickable(EXPLORE_EVENTS_BTN)).click();
    }

    public boolean isErrorBoxVisible() {
        List<WebElement> els = driver.findElements(ERROR_BOX);
        return !els.isEmpty() && els.get(0).isDisplayed();
    }

    public List<String> getEventTitlesInOrder() {
        return driver.findElements(By.cssSelector("main .divide-y a.text-base"))
                .stream()
                .map(el -> el.getText().trim())
                .toList();
    }

    public void clickViewDetailForEvent(String eventTitleSubstring) {
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//main//div[contains(@class,'p-5')][.//a[contains(normalize-space(.), '"
                        + eventTitleSubstring + "')]]//button[contains(normalize-space(.), 'Xem chi tiết')]"))).click();
    }

    public List<WebElement> findStatusBadgesWithIcons() {
        return driver.findElements(By.xpath("//main//span[contains(@class,'rounded-full') and .//svg[@aria-hidden='true']]"));
    }
}
