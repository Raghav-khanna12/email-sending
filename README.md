## Java Selenium + SendGrid: Email recipients from GitHub Workflow YAML

This TestNG project validates multiple website tabs using Selenium. If all validations pass, it reads recipient emails from a GitHub Actions workflow YAML and sends a success email via SendGrid.

### Prerequisites
- Java 17+
- Maven 3.8+
- Chrome browser and matching ChromeDriver on PATH (or use WebDriverManager as needed)
- A GitHub workflow file at `.github/workflows/ci.yml` containing recipient emails somewhere in the YAML
- SendGrid API Key

### Configure environment
Set environment variables (or pass as Maven/TestNG system properties):

- `BASE_URL` – your site home page
- `TAB_URLS` – comma-separated list of tab URLs to validate
- `SENDGRID_API_KEY` – SendGrid API key
- `EMAIL_FROM` – From email address verified in SendGrid

Example PowerShell:

```powershell
$env:BASE_URL = "https://example.com"
$env:TAB_URLS = "https://example.com/tab1,https://example.com/tab2"
$env:SENDGRID_API_KEY = "SG.xxxxx"
$env:EMAIL_FROM = "no-reply@example.com"
```

Place/ensure emails appear in `.github/workflows/ci.yml` (they can be anywhere in the YAML; they will be regex-extracted):

```yaml
name: CI
on: [push]
jobs:
  notify:
    runs-on: ubuntu-latest
    steps:
      - name: Recipients
        run: echo "to: [alice@example.com, bob@example.org]"
```

### Run tests

```powershell
mvn -q -Dtest=com.example.tests.TabValidationAndEmailTest test
```

On success, an email is sent to the emails discovered in the workflow YAML.

### Notes
- Default workflow path is `.github/workflows/ci.yml`. Adjust path in `TabValidationAndEmailTest` if needed.
- If you prefer dynamic driver management, integrate WebDriverManager.
- This sample uses headless Chrome by default.


