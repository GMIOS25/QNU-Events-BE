package SeleniumTest.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Self-contained configuration for Java Selenium WebDriver automation tests.
 * Priority order: JVM System Properties (-D...) > Environment Variables > .env file > Local Defaults.
 */
public final class SeleniumConfig {

    public static final String OWNER_01 = "owner01";
    public static final String OWNER_02 = "owner02";
    public static final String STUDENT_01 = "sv001";
    public static final String STUDENT_02 = "sv002";
    public static final String STUDENT_03 = "sv003";
    public static final String ADMIN_01 = "admin01";

    public static final String CLASS_K49CNTT1_ID = "00000000-0000-0000-0000-000000000001";
    public static final String CLASS_K49CNTT2_ID = "00000000-0000-0000-0000-000000000002";
    public static final String NON_EXISTENT_UUID = "00000000-0000-0000-0000-000000009999";

    private static final Map<String, String> DOT_ENV = loadDotEnv();

    private SeleniumConfig() {
    }

    public static String feBaseUrl() {
        return resolve("fe.baseUrl", "FE_BASE_URL", "http://localhost:5173");
    }

    public static String apiBaseUrl() {
        return resolve("api.baseUrl", "API_BASE_URL", "http://localhost:8080");
    }

    public static String demoPassword() {
        return resolve("demo.password", "DEMO_PASSWORD", "Demo@12345");
    }

    public static String browser() {
        return resolve("selenium.browser", "SELENIUM_BROWSER", "chrome").toLowerCase();
    }

    public static boolean headless() {
        return Boolean.parseBoolean(resolve("selenium.headless", "SELENIUM_HEADLESS", "true"));
    }

    public static long timeoutSeconds() {
        return Long.parseLong(resolve("selenium.timeoutSeconds", "SELENIUM_TIMEOUT_SECONDS", "15"));
    }

    public static Path screenshotDir() {
        String dir = resolve("selenium.screenshotDir", "SELENIUM_SCREENSHOT_DIR", "target/selenium-screenshots");
        return Path.of(dir);
    }

    public static String dbUrl() {
        return resolve("db.url", "DB_URL", "jdbc:postgresql://localhost:5432/qnu_events");
    }

    public static String dbUsername() {
        return resolve("db.username", "DB_USERNAME", "qnu_events");
    }

    public static String dbPassword() {
        return resolve("db.password", "DB_PASSWORD", "qnu_events_dev");
    }

    public static boolean isDbEnabled() {
        return Boolean.parseBoolean(resolve("db.enabled", "DB_TEST_ENABLED", "true"));
    }

    public static String resolve(String sysProp, String envVar, String defaultValue) {
        String prop = System.getProperty(sysProp);
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }
        String env = System.getenv(envVar);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String dotEnv = DOT_ENV.get(envVar);
        if (dotEnv != null && !dotEnv.isBlank()) {
            return dotEnv.trim();
        }
        return defaultValue;
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> map = new HashMap<>();
        Path[] candidates = new Path[]{
                Path.of(".env"),
                Path.of("QNU-Events-BE", ".env")
        };
        for (Path path : candidates) {
            if (Files.isRegularFile(path)) {
                try {
                    for (String line : Files.readAllLines(path)) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        int idx = trimmed.indexOf('=');
                        if (idx > 0) {
                            String key = trimmed.substring(0, idx).trim();
                            String val = trimmed.substring(idx + 1).trim();
                            if ((val.startsWith("\"") && val.endsWith("\""))
                                    || (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            map.put(key, val);
                        }
                    }
                    break;
                } catch (IOException ignored) {
                }
            }
        }
        return map;
    }
}
