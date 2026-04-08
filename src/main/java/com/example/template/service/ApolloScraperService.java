package com.example.template.service;

import com.example.template.entity.SearchConfig;
import com.example.template.enums.TripType;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ApolloScraperService {

    private static final Logger log = LoggerFactory.getLogger(ApolloScraperService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${apollo.scraper.base-url}")
    private String baseUrl;

    @Value("${apollo.scraper.chrome.headless}")
    private boolean headless;

    @Value("${apollo.scraper.chrome.user-agent}")
    private String userAgent;

    @Value("${apollo.scraper.timeout.page-load}")
    private int pageLoadTimeout;

    @Value("${apollo.scraper.timeout.flight-cards}")
    private int flightCardsTimeout;

    @Value("${apollo.scraper.timeout.cookie-dismiss}")
    private int cookieDismissTimeout;

    // Configurable CSS selectors for results extraction
    @Value("${apollo.scraper.selector.cookie-button}")
    private String cookieButtonSelector;

    @Value("${apollo.scraper.selector.flight-card}")
    private String flightCardSelector;

    @Value("${apollo.scraper.selector.price}")
    private String priceSelector;

    @Value("${apollo.scraper.selector.departure-date}")
    private String departureDateSelector;

    @Value("${apollo.scraper.selector.return-date}")
    private String returnDateSelector;

    @Value("${apollo.scraper.selector.departure-time}")
    private String departureTimeSelector;

    @Value("${apollo.scraper.selector.arrival-time}")
    private String arrivalTimeSelector;

    @Value("${apollo.scraper.selector.airline}")
    private String airlineSelector;

    @Value("${apollo.scraper.selector.booking-link}")
    private String bookingLinkSelector;

    @Value("${apollo.scraper.selector.loading-indicator}")
    private String loadingIndicatorSelector;

    @Value("${apollo.scraper.debug.dump-page-source:false}")
    private boolean dumpPageSource;

    // Airport display names used in Apollo's dropdown
    @Value("${apollo.scraper.airports.ARN:Stockholm Arlanda}")
    private String arnDisplayName;

    private final Environment env;

    public ApolloScraperService(Environment env) {
        this.env = env;
    }

    private boolean driverSetup = false;

    private synchronized void setupDriver() {
        if (!driverSetup) {
            WebDriverManager.chromedriver().setup();
            driverSetup = true;
        }
    }

    public List<Map<String, String>> scrape(SearchConfig config) {
        setupDriver();

        List<Map<String, String>> allResults = new ArrayList<>();
        List<TripType> typesToScrape = new ArrayList<>();

        if (config.getTripType() == TripType.BOTH) {
            typesToScrape.add(TripType.RETURN);
            typesToScrape.add(TripType.ONE_WAY);
        } else {
            typesToScrape.add(config.getTripType());
        }

        for (TripType tripType : typesToScrape) {
            List<Map<String, String>> results = scrapeForTripType(config, tripType);
            allResults.addAll(results);
        }

        return allResults;
    }

    private List<Map<String, String>> scrapeForTripType(SearchConfig config, TripType tripType) {
        log.info("Scraping Apollo.se via form: {} -> {} ({}) dates {}-{}",
                config.getOriginAirport(), config.getDestinationAirport(), tripType,
                config.getSearchFromDate(), config.getSearchToDate());

        WebDriver driver = createDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(flightCardsTimeout));

        try {
            // Step 1: Navigate to flight search page
            log.info("Step 1: Loading {}", baseUrl);
            driver.get(baseUrl);
            Thread.sleep(3000);

            // Step 2: Dismiss cookie consent
            log.info("Step 2: Dismissing cookies");
            dismissCookieConsent(driver);
            Thread.sleep(1000);

            // Step 3: Ensure "Flyg" tab is selected (should be pre-selected on /flygbiljetter)
            log.info("Step 3: Ensuring Flyg tab is selected");
            ensureFlightTab(driver, shortWait);
            Thread.sleep(500);

            // Step 4: Select trip type (Tur & retur vs Enkelresa)
            log.info("Step 4: Selecting trip type: {}", tripType);
            selectTripType(driver, tripType, shortWait);
            Thread.sleep(500);

            // Step 5: Select departure airport
            log.info("Step 5: Selecting departure airport: {}", config.getOriginAirport());
            selectDepartureAirport(driver, config.getOriginAirport(), shortWait, js);
            Thread.sleep(500);

            // Step 6: Select destination
            log.info("Step 6: Selecting destination: {}", config.getDestinationAirport());
            selectDestination(driver, config.getDestinationAirport(), shortWait, js);
            Thread.sleep(500);

            // Step 7: Select departure date
            if (config.getSearchFromDate() != null) {
                log.info("Step 7: Selecting departure date: {}", config.getSearchFromDate());
                selectDate(driver, config.getSearchFromDate(), shortWait, js);
                Thread.sleep(500);
            }

            // Step 8: Click search
            log.info("Step 8: Clicking search button");
            clickSearch(driver, shortWait);

            // Step 9: Wait for results page to load
            log.info("Step 9: Waiting for results to load...");
            Thread.sleep(5000);
            waitForResultsPage(driver, longWait);

            if (dumpPageSource) {
                dumpDebugInfo(driver, tripType, "results");
            }

            // Step 10: Extract flight cards
            log.info("Step 10: Extracting flight cards");
            scrollToLoadAll(driver);
            return extractFlightCards(driver, tripType);

        } catch (Exception e) {
            if (dumpPageSource) {
                try { dumpDebugInfo(driver, tripType, "error"); } catch (Exception ignored) {}
            }
            log.error("Scraping failed: {}", e.getMessage(), e);
            throw new RuntimeException("Scraping failed: " + e.getMessage(), e);
        } finally {
            driver.quit();
        }
    }

    // ── Form interaction methods ──────────────────────────────────────

    private void ensureFlightTab(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        try {
            WebElement flightTab = driver.findElement(
                    By.cssSelector("[data-test='integrated-search-box-panel-1']"));
            if (!"true".equals(flightTab.getAttribute("aria-selected"))) {
                flightTab.click();
                Thread.sleep(500);
                log.info("Clicked Flyg tab");
            } else {
                log.info("Flyg tab already selected");
            }
        } catch (NoSuchElementException e) {
            // Fallback: try by tab id
            try {
                WebElement tab = driver.findElement(By.id("1-tab"));
                tab.click();
                log.info("Clicked Flyg tab via #1-tab");
            } catch (NoSuchElementException e2) {
                log.warn("Could not find Flyg tab — proceeding anyway");
            }
        }
    }

    private void selectTripType(WebDriver driver, TripType tripType, WebDriverWait wait) throws InterruptedException {
        // selector-0 = "Tur & retur" (RETURN), selector-1 = "Enkelresa" (ONE_WAY)
        String testId = tripType == TripType.ONE_WAY
                ? "searchbox-flight-only-selector-1"
                : "searchbox-flight-only-selector-0";
        try {
            WebElement radio = driver.findElement(By.cssSelector("[data-test='" + testId + "']"));
            if (!"true".equals(radio.getAttribute("data-checked"))) {
                radio.click();
                Thread.sleep(300);
                log.info("Selected trip type: {}", tripType);
            } else {
                log.info("Trip type {} already selected", tripType);
            }
        } catch (NoSuchElementException e) {
            log.warn("Could not find trip type selector: {}", testId);
        }
    }

    private void selectDepartureAirport(WebDriver driver, String airportCode,
                                         WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        try {
            // Check if the desired airport is already selected
            WebElement dropdown = driver.findElement(
                    By.cssSelector("[data-test='search-box-departure-airports']"));
            String currentValue = dropdown.getText().trim();

            String desiredName = getAirportDisplayName(airportCode);
            if (currentValue.contains(desiredName)) {
                log.info("Departure airport {} already selected", desiredName);
                return;
            }

            // Click the dropdown to open it
            WebElement combobox = dropdown.findElement(By.cssSelector("[role='combobox']"));
            combobox.click();
            Thread.sleep(500);

            // Find and click the matching option in the listbox
            selectOptionFromListbox(driver, desiredName, wait);
            log.info("Selected departure airport: {}", desiredName);
        } catch (Exception e) {
            log.warn("Could not select departure airport {}: {}", airportCode, e.getMessage());
        }
    }

    private void selectDestination(WebDriver driver, String destinationCode,
                                    WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        // Resolve airport code to Apollo path (e.g. IOA → "Grekland>Ioannina")
        String mapping = env.getProperty("apollo.scraper.destinations." + destinationCode, destinationCode);
        String[] path = mapping.split(">");
        log.info("Searching destination: '{}' (path: {})", destinationCode, mapping);

        try {
            WebElement destButton = driver.findElement(
                    By.cssSelector("[data-test='search-box-destinations']"));
            destButton.click();
            Thread.sleep(1000);

            // Navigate each level of the hierarchical dropdown
            for (int level = 0; level < path.length; level++) {
                String target = path[level].trim();
                boolean found = clickItemByText(driver, target);

                if (found) {
                    log.info("Clicked level {}: '{}'", level, target);
                    Thread.sleep(1000);
                } else {
                    // Log visible items for debugging
                    logVisibleItems(driver, target);
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Could not select destination {} ({}): {}", mapping, destinationCode, e.getMessage());
        }
    }

    private boolean clickItemByText(WebDriver driver, String target) {
        String targetLower = target.toLowerCase();

        // Try various selectors for list items, menu items, options
        for (String selector : new String[]{
                "[role='menuitem']",
                "[role='option']",
                "[role='listbox'] li",
                "[role='listbox'] button",
                "li a", "li button", "li span",
                "[class*='list'] li", "[class*='List'] li",
                "[class*='option']", "[class*='Option']",
                "button", "a"
        }) {
            List<WebElement> items = driver.findElements(By.cssSelector(selector));
            for (WebElement item : items) {
                try {
                    String text = item.getText().trim().toLowerCase();
                    if (text.equals(targetLower) || text.startsWith(targetLower)) {
                        item.click();
                        return true;
                    }
                } catch (StaleElementReferenceException ignored) {}
            }
        }
        return false;
    }

    private void logVisibleItems(WebDriver driver, String target) {
        List<WebElement> allVisible = driver.findElements(
                By.cssSelector("li, [role='option'], [role='menuitem'], button"));
        List<String> visibleTexts = allVisible.stream()
                .map(o -> { try { return o.getText().trim(); } catch (Exception e) { return ""; } })
                .filter(t -> !t.isEmpty() && t.length() < 100)
                .distinct()
                .toList();
        log.warn("Could not find '{}'. Visible items ({}): {}",
                target, visibleTexts.size(),
                visibleTexts.size() > 30 ? visibleTexts.subList(0, 30) + "..." : visibleTexts);
    }

    private void selectDate(WebDriver driver, LocalDate targetDate,
                             WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        try {
            // Click the date picker area
            WebElement datePicker = driver.findElement(
                    By.cssSelector(".search-box__content__available-flights-date-picker"));
            datePicker.click();
            Thread.sleep(500);

            // Navigate to the correct month
            String targetMonthYear = targetDate.getMonth()
                    .getDisplayName(TextStyle.FULL, new Locale("sv", "SE")).toLowerCase()
                    + " " + targetDate.getYear();

            for (int i = 0; i < 12; i++) {
                // Check current displayed month
                String currentMonth = "";
                for (String sel : new String[]{
                        "[aria-label*='month']", ".calendar-header", ".month-header",
                        "[class*='month']", "[class*='Month']"
                }) {
                    List<WebElement> headers = driver.findElements(By.cssSelector(sel));
                    for (WebElement h : headers) {
                        String text = h.getText().trim().toLowerCase();
                        if (!text.isEmpty() && text.length() > 3) {
                            currentMonth = text;
                            break;
                        }
                    }
                    if (!currentMonth.isEmpty()) break;
                }

                if (currentMonth.contains(targetMonthYear)) {
                    break;
                }

                // Click next month button
                List<WebElement> nextButtons = driver.findElements(
                        By.cssSelector("[aria-label*='next'], [aria-label*='Next'], button[class*='next'], [class*='Next']"));
                if (!nextButtons.isEmpty()) {
                    nextButtons.get(nextButtons.size() - 1).click();
                    Thread.sleep(300);
                } else {
                    log.warn("No next-month button found");
                    break;
                }
            }

            // Click the target day
            String dayStr = String.valueOf(targetDate.getDayOfMonth());
            boolean clicked = false;

            // Try aria-label with date
            String formattedDate = targetDate.format(DATE_FORMAT);
            List<WebElement> dateCells = driver.findElements(
                    By.cssSelector("[aria-label*='" + formattedDate + "'], " +
                            "[data-date='" + formattedDate + "'], " +
                            "button[data-day='" + dayStr + "']"));

            if (!dateCells.isEmpty()) {
                dateCells.get(0).click();
                clicked = true;
            }

            if (!clicked) {
                // Fallback: find day buttons by text content
                List<WebElement> dayButtons = driver.findElements(
                        By.cssSelector("button[class*='day'], td[class*='day'], [role='gridcell']"));
                for (WebElement btn : dayButtons) {
                    if (btn.getText().trim().equals(dayStr)) {
                        btn.click();
                        clicked = true;
                        break;
                    }
                }
            }

            if (clicked) {
                log.info("Selected date: {}", targetDate);
            } else {
                log.warn("Could not click date: {}", targetDate);
            }

        } catch (Exception e) {
            log.warn("Could not select date {}: {}", targetDate, e.getMessage());
        }
    }

    private void clickSearch(WebDriver driver, WebDriverWait wait) {
        try {
            WebElement searchBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("[data-test='search-box-perform-search-btn']")));
            searchBtn.click();
            log.info("Search button clicked");
        } catch (TimeoutException e) {
            // Fallback selectors
            try {
                WebElement btn = driver.findElement(
                        By.cssSelector(".search-box__content__perform-search-btn"));
                btn.click();
                log.info("Search button clicked (fallback selector)");
            } catch (NoSuchElementException e2) {
                log.error("Could not find search button");
                throw new RuntimeException("Search button not found");
            }
        }
    }

    private void waitForResultsPage(WebDriver driver, WebDriverWait wait) {
        log.info("Waiting for results page... Current URL: {}", driver.getCurrentUrl());

        // Wait for URL to change to booking-guide or for results elements to appear
        try {
            wait.until(d -> {
                String url = d.getCurrentUrl();
                return url.contains("booking-guide") || url.contains("flight/list");
            });
            log.info("Results page URL: {}", driver.getCurrentUrl());
        } catch (TimeoutException e) {
            log.info("URL did not change to booking-guide. Current: {}", driver.getCurrentUrl());
        }

        // Additional wait for content to render
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Wait for any loading spinners to disappear
        for (String selector : loadingIndicatorSelector.split(",")) {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(10)).until(
                        ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(selector.trim())));
            } catch (TimeoutException | NoSuchElementException ignored) {}
        }
    }

    // ── Dropdown helper ──────────────────────────────────────────────

    private void selectOptionFromListbox(WebDriver driver, String optionText, WebDriverWait wait) {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[role='listbox']")));
            List<WebElement> options = driver.findElements(By.cssSelector("[role='option']"));
            for (WebElement option : options) {
                if (option.getText().trim().equalsIgnoreCase(optionText)) {
                    option.click();
                    return;
                }
            }
            // Partial match
            for (WebElement option : options) {
                if (option.getText().trim().toLowerCase().contains(optionText.toLowerCase())) {
                    option.click();
                    return;
                }
            }
            log.warn("Option '{}' not found in listbox", optionText);
        } catch (TimeoutException e) {
            log.warn("Listbox did not appear for option: {}", optionText);
        }
    }

    private String getAirportDisplayName(String code) {
        return switch (code) {
            case "ARN" -> arnDisplayName;
            case "GOT" -> "Göteborg Landvetter";
            case "MMX" -> "Malmö Sturup";
            default -> code;
        };
    }

    // ── Browser setup ────────────────────────────────────────────────

    private WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();

        if (headless) {
            options.addArguments("--headless=new");
        }

        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=" + userAgent);
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        ChromeDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));
        return driver;
    }

    // ── Cookie consent ───────────────────────────────────────────────

    private void dismissCookieConsent(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(cookieDismissTimeout));
            for (String selector : cookieButtonSelector.split(",")) {
                try {
                    WebElement button = wait.until(
                            ExpectedConditions.elementToBeClickable(By.cssSelector(selector.trim())));
                    button.click();
                    log.info("Cookie consent dismissed with selector: {}", selector.trim());
                    Thread.sleep(500);
                    return;
                } catch (TimeoutException | NoSuchElementException ignored) {}
            }

            // Fallback: try Usercentrics shadow DOM accept button
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                Boolean clicked = (Boolean) js.executeScript(
                    "const shadow = document.querySelector('#usercentrics-root')?.shadowRoot;" +
                    "if (!shadow) return false;" +
                    "const btn = shadow.querySelector('button[data-testid=\"uc-accept-all-button\"]');" +
                    "if (btn) { btn.click(); return true; }" +
                    "return false;");
                if (Boolean.TRUE.equals(clicked)) {
                    log.info("Cookie consent dismissed via Usercentrics shadow DOM");
                    return;
                }
            } catch (Exception ignored) {}

            log.info("No cookie consent dialog found — continuing");
        } catch (Exception e) {
            log.warn("Could not dismiss cookie consent: {}", e.getMessage());
        }
    }

    // ── Result extraction ────────────────────────────────────────────

    private void scrollToLoadAll(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            long lastHeight = (long) js.executeScript("return document.body.scrollHeight");

            for (int i = 0; i < 10; i++) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
                Thread.sleep(1500);

                long newHeight = (long) js.executeScript("return document.body.scrollHeight");
                if (newHeight == lastHeight) break;
                lastHeight = newHeight;
            }
        } catch (Exception e) {
            log.warn("Scrolling failed: {}", e.getMessage());
        }
    }

    private List<Map<String, String>> extractFlightCards(WebDriver driver, TripType tripType) {
        List<Map<String, String>> results = new ArrayList<>();

        List<WebElement> cards = driver.findElements(By.cssSelector(".flight-package-card"));
        log.info("Found {} flight-package-card elements", cards.size());

        for (WebElement card : cards) {
            try {
                Map<String, String> flight = new HashMap<>();
                flight.put("tripType", tripType.name());

                // Price: inside .total-price (e.g. "8 996 kr")
                flight.put("price", extractText(card, ".total-price"));

                // Flight content sections: first = outbound, second = return
                List<WebElement> flightSections = card.findElements(By.cssSelector(".flight-content"));

                if (!flightSections.isEmpty()) {
                    WebElement outbound = flightSections.get(0);
                    // Date text: "Utresa, " followed by "fre 6 mars"
                    flight.put("departureDate", outbound.findElement(By.cssSelector("div > span:last-child")).getText().trim());
                    // Times: in Typography__headingS span (e.g. "09:55 - 16:20,")
                    try {
                        String timesText = outbound.findElement(By.cssSelector("[class*='headingS']")).getText().trim();
                        String[] parts = timesText.replace(",", "").split(" - ");
                        if (parts.length >= 2) {
                            flight.put("departureTime", parts[0].trim());
                            flight.put("arrivalTime", parts[1].trim());
                        }
                    } catch (NoSuchElementException ignored) {}
                    // Airline: alt text of carrier icon img
                    try {
                        flight.put("airline", outbound.findElement(By.cssSelector(".flight-content__carrier-icon")).getAttribute("alt"));
                    } catch (NoSuchElementException ignored) {}
                }

                // Return flight (if round trip)
                if (flightSections.size() > 1) {
                    WebElement returnFlight = flightSections.get(1);
                    flight.put("returnDate", returnFlight.findElement(By.cssSelector("div > span:last-child")).getText().trim());
                    try {
                        String timesText = returnFlight.findElement(By.cssSelector("[class*='headingS']")).getText().trim();
                        String[] parts = timesText.replace(",", "").split(" - ");
                        if (parts.length >= 2) {
                            flight.put("returnDepartureTime", parts[0].trim());
                            flight.put("returnArrivalTime", parts[1].trim());
                        }
                    } catch (NoSuchElementException ignored) {}
                }

                // Destination: from the header span
                try {
                    flight.put("destination", card.findElement(By.cssSelector("[class*='headerText']")).getText().trim());
                } catch (NoSuchElementException ignored) {}

                // Booking URL: current page URL serves as the booking context
                flight.put("bookingUrl", driver.getCurrentUrl());

                results.add(flight);
                log.debug("Extracted flight: {} - {} - {}", flight.get("price"), flight.get("departureDate"), flight.get("airline"));

            } catch (StaleElementReferenceException e) {
                log.warn("Flight card became stale — skipping");
            } catch (Exception e) {
                log.warn("Failed to extract flight card: {}", e.getMessage());
            }
        }

        return results;
    }

    private List<WebElement> findElements(WebDriver driver, String selectorList) {
        for (String selector : selectorList.split(",")) {
            List<WebElement> elements = driver.findElements(By.cssSelector(selector.trim()));
            if (!elements.isEmpty()) return elements;
        }
        return List.of();
    }

    private String extractText(WebElement parent, String selectorList) {
        for (String selector : selectorList.split(",")) {
            try {
                WebElement element = parent.findElement(By.cssSelector(selector.trim()));
                String text = element.getText().trim();
                if (!text.isEmpty()) return text;
            } catch (NoSuchElementException ignored) {}
        }
        return null;
    }

    private String extractLink(WebElement parent, String selectorList) {
        for (String selector : selectorList.split(",")) {
            try {
                WebElement element = parent.findElement(By.cssSelector(selector.trim()));
                String href = element.getAttribute("href");
                if (href != null && !href.isEmpty()) return href;
            } catch (NoSuchElementException ignored) {}
        }
        return null;
    }

    // ── Debug dump ───────────────────────────────────────────────────

    private void dumpDebugInfo(WebDriver driver, TripType tripType, String phase) {
        try {
            Path dumpDir = Path.of("data", "debug");
            Files.createDirectories(dumpDir);

            String pageSource = driver.getPageSource();
            String filename = "page-source-" + tripType.name().toLowerCase() + "-" + phase + ".html";
            Files.writeString(dumpDir.resolve(filename), pageSource);
            log.info("Page source dumped to data/debug/{} ({} chars)", filename, pageSource.length());

            log.info("Current URL: {}", driver.getCurrentUrl());
            log.info("Page title: {}", driver.getTitle());

            // Log all visible text elements for debugging
            try {
                @SuppressWarnings("unchecked")
                List<String> iframeSrcs = (List<String>) ((JavascriptExecutor) driver).executeScript(
                        "return Array.from(document.querySelectorAll('iframe')).map(f => f.src || 'no-src')");
                if (!iframeSrcs.isEmpty()) {
                    log.info("Iframes on page: {}", iframeSrcs);
                }
            } catch (Exception ignored) {}

        } catch (IOException e) {
            log.error("Failed to dump debug info: {}", e.getMessage());
        }
    }
}
