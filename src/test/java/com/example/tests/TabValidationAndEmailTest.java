package com.example.tests;

import com.example.email.SendGridService;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.InvalidArgumentException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TabValidationAndEmailTest {
    private WebDriver driver;

    @BeforeClass
    public void setup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        String chromeBinary = System.getenv("CHROME_PATH");
        if (chromeBinary != null && !chromeBinary.isBlank()) {
            options.setBinary(chromeBinary);
        }
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @AfterClass(alwaysRun = true)
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void validateTabsAndSendEmail() throws IOException {
        String baseUrl = sanitizeUrl(System.getProperty("base.url", System.getenv().getOrDefault("BASE_URL", "https://careers.ey.com/ey?locale=en_US")));
        String tabUrlsCsv = System.getProperty("tab.urls", System.getenv().getOrDefault("TAB_URLS", "https://www.ey.com/en_gl/careers,https://www.ey.com/en_gl/insights,https://www.ey.com/en_gl/services"));
        List<String> tabUrls = tabUrlsCsv.isBlank() ? List.of(
                "https://www.ey.com/en_gl/careers",
                "https://www.ey.com/en_gl/insights",
                "https://www.ey.com/en_gl/services"
        ) : Arrays.stream(tabUrlsCsv.split(","))
                .map(this::sanitizeUrl)
                .filter(s -> s != null && !s.isBlank())
                .toList();

        driver.get(baseUrl);

        class TabResult { String url; boolean ok; String note; long loadMillis; TabResult(String u, boolean o, String n, long ms){url=u;ok=o;note=n;loadMillis=ms;} }
        int visited = 0; int okCount = 0; int failCount = 0;
        java.time.Instant startSuite = java.time.Instant.now();
        java.util.ArrayList<TabResult> results = new java.util.ArrayList<>();
        for (String tabUrl : tabUrls) {
            String url = sanitizeUrl(tabUrl);
            if (url == null || url.isBlank()) {
                results.add(new TabResult(tabUrl, false, "blank URL", 0));
                System.out.println("[tabs] Skipping blank URL entry");
                failCount++; continue;
            }
            try {
                long t0 = System.nanoTime();
                driver.get(url);
                long t1 = System.nanoTime();
                long elapsedMs = java.time.Duration.ofNanos(t1 - t0).toMillis();
                visited++;
                List<WebElement> bodies = driver.findElements(By.tagName("body"));
                boolean ok = !bodies.isEmpty();
                if (ok) { okCount++; results.add(new TabResult(url, true, "body present", elapsedMs)); }
                else { failCount++; results.add(new TabResult(url, false, "body not found", elapsedMs)); }
            } catch (Exception e) {
                String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
                results.add(new TabResult(url, false, msg, 0));
                System.out.println("[tabs] Failed URL: " + url + " reason=" + msg);
                failCount++;
            }
        }

        Set<String> recipients = new LinkedHashSet<>();
        String toCsv = System.getProperty("email.to", System.getenv().getOrDefault("EMAIL_TO", "raghavk2509@gmail.com"));
        if (!toCsv.isBlank()) {
            for (String addr : toCsv.split(",")) {
                String trimmed = addr.trim();
                if (!trimmed.isEmpty()) {
                    recipients.add(trimmed);
                }
            }
        }
        Assert.assertFalse(recipients.isEmpty(), "No recipient emails provided");

        String apiKey = System.getProperty("sendgrid.api.key", System.getenv().getOrDefault("SENDGRID_API_KEY", ""));
        String fromEmail = System.getProperty("email.from", System.getenv().getOrDefault("EMAIL_FROM", "raghavk15@outlook.com"));
        System.out.println("[Email] Test about to send: keyPresent=" + !apiKey.isBlank() + ", from=" + fromEmail + ", toCount=" + recipients.size());
        Assert.assertFalse(apiKey.isBlank(), "SENDGRID_API_KEY is required");
        Assert.assertFalse(fromEmail.isBlank(), "EMAIL_FROM is required");

        SendGridService mailer = new SendGridService(apiKey, fromEmail);
        boolean allOk = failCount == 0 && visited == tabUrls.size();
        String subject = (allOk ? "SUCCESS" : "FAILURE") + " - Tab validation: " + okCount + " passed / " + failCount + " failed";
        java.time.Instant endSuite = java.time.Instant.now();
        long totalSeconds = java.time.Duration.between(startSuite, endSuite).toSeconds();
        String runNumber = System.getenv().getOrDefault("GITHUB_RUN_NUMBER", "");
        String runAttempt = System.getenv().getOrDefault("GITHUB_RUN_ATTEMPT", "");
        String runId = System.getenv().getOrDefault("GITHUB_RUN_ID", "");

        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("<p>Base URL: ").append(baseUrl).append("</p>");
        bodyBuilder.append("<p>Summary: ").append(okCount).append(" passed, ").append(failCount).append(" failed, visited ").append(visited).append("/ ").append(tabUrls.size()).append("</p>");
        bodyBuilder.append("<p>Total duration: ").append(totalSeconds).append("s</p>");
        if (!runId.isBlank()) {
            bodyBuilder.append("<p>Workflow run: #").append(runNumber).append(" attempt ").append(runAttempt).append(" (ID ").append(runId).append(")</p>");
        }
        bodyBuilder.append("<table border='1' cellpadding='6' cellspacing='0' style='border-collapse:collapse;font-family:Segoe UI,Arial,sans-serif;'>");
        bodyBuilder.append("<tr><th align='left'>URL</th><th>Status</th><th align='left'>Note</th><th>Load (ms)</th></tr>");
        for (TabResult r : results) {
            bodyBuilder.append("<tr>")
                    .append("<td>").append(r.url == null ? "(null)" : r.url).append("</td>")
                    .append("<td style='color:").append(r.ok ? "#27ae60'">PASS" : "#c0392b'">FAIL").append("</td>")
                    .append("<td>").append(r.note == null ? "" : r.note.replace("<","&lt;")).append("</td>")
                    .append("<td align='right'>").append(r.loadMillis).append("</td>")
                    .append("</tr>");
        }
        bodyBuilder.append("</table>");
        String body = bodyBuilder.toString();
        mailer.sendEmail(recipients, subject, body);
    }

    private String sanitizeUrl(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isBlank()) return s;
        s = s.replace(" ", "%20");
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "https://" + s;
        }
        return s;
    }
}


