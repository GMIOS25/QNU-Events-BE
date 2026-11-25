package SeleniumTest.pages;

import SeleniumTest.config.SeleniumConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * Page Object for EventsPage (/events).
 */
public class EventsPage {

    private static final By PAGE_HEADING = By.cssSelector("main h1");
    private static final By CREATE_EVENT_BTN = By.xpath("//main//a[@href='/events/new']//button");
    private static final By SEARCH_INPUT = By.cssSelector("input[placeholder='Tìm kiếm sự kiện theo tên tiêu đề...']");
    private static final By ACTIVITY_TYPE_SELECT = By.id("filter-activity-type");
    private static final By RESET_FILTERS_BTN = By.xpath("//main//button[contains(normalize-space(.), 'Đặt lại')]");
    private static final By LOADING_INDICATOR = By.xpath("//main//*[contains(text(), 'Đang tải danh sách sự kiện')]");
    private static final By ERROR_BOX = By.xpath("//main//*[contains(text(), 'Không thể tải danh sách sự kiện')]");
    private static final By EVENT_CARDS = By.cssSelector("main div.grid > div");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public EventsPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public EventsPage open() {
        driver.get(SeleniumConfig.feBaseUrl() + "/events");
        waitUntilLoaded();
        return this;
    }

    public EventsPage waitUntilLoaded() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(PAGE_HEADING));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADING_INDICATOR));
        return this;
    }

    public String getPageHeading() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(PAGE_HEADING)).getText();
    }

    public boolean isCreateEventButtonVisible() {
        List<WebElement> btns = driver.findElements(CREATE_EVENT_BTN);
        return !btns.isEmpty() && btns.get(0).isDisplayed();
    }

    public void clickCreateEventButton() {
        wait.until(ExpectedConditions.elementToBeClickable(CREATE_EVENT_BTN)).click();
    }

    public void search(String keyword) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(SEARCH_INPUT));
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(Keys.BACK_SPACE);
        if (keyword != null && !keyword.isEmpty()) {
            input.sendKeys(keyword);
        }
    }

    public WebElement getSearchInput() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(SEARCH_INPUT));
    }

    public void selectActivityType(String activityType) {
        WebElement selectEl = wait.until(ExpectedConditions.visibilityOfElementLocated(ACTIVITY_TYPE_SELECT));
        new Select(selectEl).selectByValue(activityType);
    }

    public void clickResetFilters() {
        wait.until(ExpectedConditions.elementToBeClickable(RESET_FILTERS_BTN)).click();
    }

    public boolean isEventCardPresent(String titleSubstring) {
        List<WebElement> matches = driver.findElements(
                By.xpath("//main//h3//a[contains(normalize-space(.), " + escapeXpath(titleSubstring) + ")]"));
        return matches.stream().anyMatch(WebElement::isDisplayed);
    }

    public void waitForEventCardVisible(String titleSubstring) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//main//h3//a[contains(normalize-space(.), " + escapeXpath(titleSubstring) + ")]")));
    }

    public String getEventCardText(String titleSubstring) {
        WebElement card = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//main//div[contains(@class,'rounded-2xl')][.//h3//a[contains(normalize-space(.), "
                        + escapeXpath(titleSubstring) + ")]]")));
        return card.getText();
    }

    public void openEventDetailByTitle(String titleSubstring) {
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//main//h3//a[contains(normalize-space(.), " + escapeXpath(titleSubstring) + ")]"))).click();
    }

    public int getVisibleEventCardCount() {
        return driver.findElements(EVENT_CARDS).size();
    }

    public String getMainText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("main"))).getText();
    }

    public boolean isErrorBoxVisible() {
        List<WebElement> els = driver.findElements(ERROR_BOX);
        return !els.isEmpty() && els.get(0).isDisplayed();
    }

    public List<WebElement> findStatusBadgesWithIcons() {
        return driver.findElements(By.xpath("//main//span[contains(@class,'rounded-full') and .//svg[@aria-hidden='true']]"));
    }

    private static String escapeXpath(String text) {
        if (!text.contains("'")) {
            return "'" + text + "'";
        }
        return "concat('" + text.replace("'", "', \"'\", '") + "')";
    }
}
