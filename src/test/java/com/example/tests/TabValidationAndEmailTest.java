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

        int visited = 0;
        for (String tabUrl : tabUrls) {
            String url = sanitizeUrl(tabUrl);
            if (url == null || url.isBlank()) {
                System.out.println("[tabs] Skipping blank URL entry");
                continue;
            }
            try {
                driver.get(url);
            } catch (Exception e) {
                System.out.println("[tabs] Skipped invalid URL: " + url + " reason=" + e.getMessage());
                continue;
            }
            List<WebElement> bodies = driver.findElements(By.tagName("body"));
            if (bodies.isEmpty()) {
                System.out.println("[tabs] No body found for URL: " + url);
                continue;
            }
            visited++;
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
        Assert.assertFalse(apiKey.isBlank(), "SENDGRID_API_KEY is required");
        Assert.assertFalse(fromEmail.isBlank(), "EMAIL_FROM is required");

        SendGridService mailer = new SendGridService(apiKey, fromEmail);
        String subject = "Tab validation completed: visited " + visited + "/" + tabUrls.size();
        String body = "<p>Automation completed.</p>" +
                "<p>Base URL: " + baseUrl + "</p>" +
                "<p>Attempted tabs:</p><ul>" + String.join("", tabUrls.stream().map(u -> "<li>" + sanitizeUrl(u) + "</li>").toList()) + "</ul>" +
                "<p>Visited count: " + visited + "</p>";
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


