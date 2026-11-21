package SeleniumTest.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared base class for all Java Selenium 4 + JUnit 5 UI/E2E automation tests.
 * Uses Java 21 built-in HttpClient for fast fixture seeding (zero REST Assured dependency)
 * and Selenium 4 Selenium Manager + CDP emulation for responsive 320px/375px & offline testing.
 */
@Tag("selenium")
public abstract class BaseSeleniumTest {

    protected static final SeleniumDbHelper DB = new SeleniumDbHelper();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    protected WebDriver driver;
    protected WebDriverWait wait;
    private final List<UUID> createdEventIds = new ArrayList<>();
    private final List<UUID> createdUserIds = new ArrayList<>();

    @BeforeEach
    void startBrowser() {
        driver = createWebDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(SeleniumConfig.timeoutSeconds()));
    }

    @AfterEach
    void stopBrowserAndCleanup(TestInfo testInfo) {
        try {
            if (driver != null) {
                setNetworkOffline(false);
                driver.quit();
            }
        } catch (Exception ignored) {
        } finally {
            driver = null;
        }

        for (UUID eventId : createdEventIds) {
            DB.cleanupEvent(eventId);
        }
        createdEventIds.clear();

        for (UUID userId : createdUserIds) {
            DB.deleteTemporaryUser(userId);
        }
        createdUserIds.clear();
    }

    protected WebDriver createWebDriver() {
        String browser = SeleniumConfig.browser();
        boolean headless = SeleniumConfig.headless();

        if ("edge".equals(browser)) {
            EdgeOptions options = new EdgeOptions();
            if (headless) {
                options.addArguments("--headless=new");
            }
            options.addArguments(
                    "--window-size=1440,900",
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--lang=vi-VN"
            );
            return new EdgeDriver(options);
        }

        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--window-size=1440,900",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--lang=vi-VN"
        );
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.media_stream_camera", 2);
        options.setExperimentalOption("prefs", prefs);
        return new ChromeDriver(options);
    }

    protected void openPath(String path) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        driver.get(SeleniumConfig.feBaseUrl() + normalized);
    }

    protected void waitForPath(String expectedPath) {
        wait.until(d -> {
            String url = d.getCurrentUrl();
            String pathOnly = url.replace(SeleniumConfig.feBaseUrl(), "");
            int qIdx = pathOnly.indexOf('?');
            if (qIdx >= 0) {
                pathOnly = pathOnly.substring(0, qIdx);
            }
            return expectedPath.equals(pathOnly);
        });
    }

    protected void waitForUrlContains(String fragment) {
        wait.until(d -> d.getCurrentUrl().contains(fragment));
    }

    protected String getSessionStorage(String key) {
        return (String) ((JavascriptExecutor) driver)
                .executeScript("return window.sessionStorage.getItem(arguments[0]);", key);
    }

    protected void clearBrowserStorage() {
        ((JavascriptExecutor) driver).executeScript(
                "window.sessionStorage.clear(); window.localStorage.clear();"
        );
    }

    protected void setViewport(int width, int height, boolean mobile) {
        if (driver instanceof ChromiumDriver chromiumDriver) {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("width", width);
            metrics.put("height", height);
            metrics.put("deviceScaleFactor", mobile ? 2 : 1);
            metrics.put("mobile", mobile);
            chromiumDriver.executeCdpCommand("Emulation.setDeviceMetricsOverride", metrics);
        } else {
            driver.manage().window().setSize(new Dimension(width, height));
        }
    }

    protected void resetViewport() {
        if (driver instanceof ChromiumDriver chromiumDriver) {
            chromiumDriver.executeCdpCommand("Emulation.clearDeviceMetricsOverride", Map.of());
        }
        driver.manage().window().setSize(new Dimension(1440, 900));
    }

    protected boolean hasHorizontalOverflow() {
        Object result = ((JavascriptExecutor) driver).executeScript(
                "return document.documentElement.scrollWidth > (window.innerWidth + 2);"
        );
        return Boolean.TRUE.equals(result);
    }

    protected void setNetworkOffline(boolean offline) {
        if (driver instanceof ChromiumDriver chromiumDriver) {
            try {
                chromiumDriver.executeCdpCommand("Network.enable", Map.of());
                Map<String, Object> params = new HashMap<>();
                params.put("offline", offline);
                params.put("latency", 0);
                params.put("downloadThroughput", offline ? 0 : -1);
                params.put("uploadThroughput", offline ? 0 : -1);
                chromiumDriver.executeCdpCommand("Network.emulateNetworkConditions", params);
            } catch (Exception ignored) {
            }
        }
    }

    protected Path captureScreenshot(String filePrefix) {
        try {
            Path dir = SeleniumConfig.screenshotDir();
            Files.createDirectories(dir);
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Path target = dir.resolve(filePrefix + "_" + System.currentTimeMillis() + ".png");
            Files.write(target, png);
            return target;
        } catch (Exception ignored) {
            return null;
        }
    }

    protected void trackEventForCleanup(String eventId) {
        if (eventId != null && !eventId.isBlank()) {
            createdEventIds.add(UUID.fromString(eventId));
        }
    }

    protected void trackUserForCleanup(UUID userId) {
        if (userId != null) {
            createdUserIds.add(userId);
        }
    }

    // -------------------------------------------------------------------------
    // Built-in JDK HttpClient fixture helpers (Arrange step for Selenium UI)
    // -------------------------------------------------------------------------

    protected String apiLogin(String loginId, String password) {
        JsonNode body = sendJson("POST", "/api/v1/auth/login", null, null,
                Map.of("loginId", loginId, "password", password), 200);
        return body.path("accessToken").asText();
    }

    protected String apiLogin(String loginId) {
        return apiLogin(loginId, SeleniumConfig.demoPassword());
    }

    protected String apiCreateDraftEvent(String ownerToken, Map<String, Object> payload) {
        JsonNode body = sendJson("POST", "/api/v1/events", ownerToken, null, payload, 201);
        String eventId = body.path("id").asText();
        trackEventForCleanup(eventId);
        return eventId;
    }

    protected String apiCreateAndPublishEvent(String ownerToken, Map<String, Object> payload) {
        String eventId = apiCreateDraftEvent(ownerToken, payload);
        JsonNode preview = sendJson("GET", "/api/v1/events/" + eventId + "/preview", ownerToken, null, null, 200);
        int previewVersion = preview.path("previewVersion").asInt();
        sendJson("POST", "/api/v1/events/" + eventId + "/publish", ownerToken, null,
                Map.of("confirm", true, "previewVersion", previewVersion), 200);
        return eventId;
    }

    protected void apiRegisterStudent(String studentToken, String eventId) {
        sendJson("POST", "/api/v1/events/" + eventId + "/registrations", studentToken, null, null, 201);
    }

    protected String apiFetchQrToken(String ownerToken, String eventId) {
        JsonNode body = sendJson("GET", "/api/v1/events/" + eventId + "/qr", ownerToken, null, null, 200);
        return body.path("token").asText();
    }

    protected void apiCheckInStudent(String studentToken, String eventId, String qrToken) {
        sendJson("POST", "/api/v1/events/" + eventId + "/attendance/qr", studentToken,
                UUID.randomUUID().toString(), Map.of("token", qrToken), 200);
    }

    private JsonNode sendJson(
            String method,
            String path,
            String bearerToken,
            String idempotencyKey,
            Object payload,
            int expectedStatus
    ) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(SeleniumConfig.apiBaseUrl() + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json");
            if (bearerToken != null) {
                builder.header("Authorization", "Bearer " + bearerToken);
            }
            if (idempotencyKey != null) {
                builder.header("Idempotency-Key", idempotencyKey);
            }
            if (payload != null) {
                String json = JSON.writeValueAsString(payload);
                builder.header("Content-Type", "application/json; charset=UTF-8");
                builder.method(method, HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            } else if ("POST".equals(method) || "PUT".equals(method)) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.GET();
            }

            HttpResponse<String> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != expectedStatus) {
                throw new IllegalStateException("Fixture API " + method + " " + path
                        + " expected HTTP " + expectedStatus + " but got " + response.statusCode()
                        + ": " + response.body());
            }
            return response.body() == null || response.body().isBlank()
                    ? JSON.createObjectNode()
                    : JSON.readTree(response.body());
        } catch (Exception ex) {
            throw new RuntimeException("Fixture API call failed: " + method + " " + path, ex);
        }
    }
}
