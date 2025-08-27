package com.example.tests;

import com.example.email.SendGridService;
import org.testng.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

public class EmailResultListener implements ITestListener, ISuiteListener {
    private int passed = 0;
    private int failed = 0;
    private int skipped = 0;
    private Instant suiteStart;

    @Override
    public void onStart(ISuite suite) {
        suiteStart = Instant.now();
    }

    @Override
    public void onFinish(ISuite suite) {
        Duration duration = suiteStart == null ? Duration.ZERO : Duration.between(suiteStart, Instant.now());

        String toCsv = System.getProperty("email.to", System.getenv().getOrDefault("EMAIL_TO", "raghavk2509@gmail.com"));
        String fromEmail = System.getProperty("email.from", System.getenv().getOrDefault("EMAIL_FROM", "raghavk15@outlook.com"));
        String apiKey = System.getProperty("sendgrid.api.key", System.getenv().getOrDefault("SENDGRID_API_KEY", "SG.JOjB9iJkQYuw08cqGfJDsA.aY-Fs0tjke7oh-5Hxnb_y_AVGwBBI-qice0sHuaABE4Copied!"));

        if (apiKey == null || apiKey.isBlank()) {
            return; // No key configured; skip email silently in local/dev
        }

        Set<String> recipients = new LinkedHashSet<>();
        if (toCsv != null && !toCsv.isBlank()) {
            for (String addr : toCsv.split(",")) {
                String trimmed = addr.trim();
                if (!trimmed.isEmpty()) recipients.add(trimmed);
            }
        }
        if (recipients.isEmpty()) return;

        String subject = String.format("TestNG Results: %d passed, %d failed, %d skipped", passed, failed, skipped);
        String statusColor = failed > 0 ? "#c0392b" : (skipped > 0 ? "#f39c12" : "#27ae60");
        String body = "" +
                "<div style=\"font-family:Segoe UI,Arial,sans-serif;\">" +
                "<h2 style=\"color:" + statusColor + ";margin:0 0 12px;\">" + subject + "</h2>" +
                "<ul style=\"line-height:1.6\">" +
                "<li><strong>Passed</strong>: " + passed + "</li>" +
                "<li><strong>Failed</strong>: " + failed + "</li>" +
                "<li><strong>Skipped</strong>: " + skipped + "</li>" +
                "<li><strong>Duration</strong>: " + duration.toSeconds() + "s</li>" +
                "</ul>" +
                "<p>Sent automatically by Selenium/TestNG on GitHub Actions.</p>" +
                "</div>";

        try {
            new SendGridService(apiKey, fromEmail).sendEmail(recipients, subject, body);
        } catch (IOException e) {
            // Best effort: log to TestNG reporter
            Reporter.log("Failed to send SendGrid email: " + e.getMessage(), true);
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) { passed++; }

    @Override
    public void onTestFailure(ITestResult result) { failed++; }

    @Override
    public void onTestSkipped(ITestResult result) { skipped++; }

    // Unused ITestListener callbacks
    @Override public void onTestStart(ITestResult result) {}
    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) {}
    @Override public void onStart(ITestContext context) {}
    @Override public void onFinish(ITestContext context) {}
}


