package com.pekari.e2e;

import com.pekari.e2e.pages.LandingPage;
import com.pekari.e2e.pages.LoginPage;
import com.pekari.e2e.pages.PassengerHomePage;
import com.pekari.e2e.pages.PassengerHistoryPage;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.time.Duration;
import java.util.List;

import static org.testng.Assert.*;

public class PassengerRatingRideDriverTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private LandingPage landingPage;
    private LoginPage loginPage;
    private PassengerHomePage passengerHomePage;
    private PassengerHistoryPage passengerHistoryPage;

    private static final String BASE_URL = "http://localhost:4200";
    private static final String TEST_EMAIL = "zoran.repic@jetbrains.com";
    private static final String TEST_PASSWORD = "Zoranrepic10";

    // Enable to slow steps for visual debugging if needed
    private static final boolean DEBUG_MODE = false;
    private static final int DEBUG_DELAY_MS = 800;

    private void debugPause() {
        if (DEBUG_MODE) {
            try { Thread.sleep(DEBUG_DELAY_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    @BeforeSuite
    public void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeMethod
    public void setupTest() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        landingPage = new LandingPage(driver);
        loginPage = new LoginPage(driver);
        passengerHomePage = new PassengerHomePage(driver);
        passengerHistoryPage = new PassengerHistoryPage(driver);
    }

    @AfterMethod
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void navigateToHistoryPage() {
        driver.get(BASE_URL);
        landingPage.clickLoginLink();
        loginPage.waitForPageToLoad();
        loginPage.login(TEST_EMAIL, TEST_PASSWORD);
        passengerHomePage.waitForPageToLoad();
        debugPause();
        passengerHomePage.clickHistoryLink();
        
        // Ensure we are on the history page and title is visible
        passengerHistoryPage.waitForPageToLoad();

        // Wait for rides to load/render on the page
        waitForRidesToLoad();
        debugPause();
    }

    private void waitForRidesToLoad() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(12))
                    .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".ride-card")));
        } catch (TimeoutException te) {
            // Proceed; some environments may be slow but we will attempt to continue
        }
    }

    private WebElement findFirstRatableCompletedRideCardOrSkip() {
        // Ensure ride cards are present first
        waitForRidesToLoad();

        // Wait up to 20s for a Rate button to appear (rendered based on isRatable flag)
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".ride-card .rate-btn")));
        } catch (TimeoutException ignored) {
            // continue to manual scan below
        }

        List<WebElement> cards = driver.findElements(By.cssSelector(".ride-card"));
        for (WebElement card : cards) {
            List<WebElement> rateBtns = card.findElements(By.cssSelector(".rate-btn"));
            if (!rateBtns.isEmpty()) {
                // Optionally bring into view to ensure it's interactable
                try {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", card);
                } catch (Exception ignored) {}
                return card;
            }
        }
        throw new SkipException("No ratable rides available (no .rate-btn found) for user " + TEST_EMAIL + ". Verify the test user has at least one unrated COMPLETED ride within 3 days and that the UI shows the Rate button.");
    }

    private void openRatingModalFromCard(WebElement card) {
        WebElement rateBtn = card.findElement(By.cssSelector(".rate-btn"));
        rateBtn.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".modal-overlay")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".rating-modal")));
        debugPause();
    }

    private void selectVehicleAndDriverStars(int vehicle, int driverRating) {
        // Vehicle stars: first rating-group
        List<WebElement> vehicleStars = driver.findElements(By.cssSelector(".rating-group:first-of-type .star-btn"));
        assertTrue(vehicle >= 1 && vehicle <= 5, "Vehicle rating out of range");
        vehicleStars.get(vehicle - 1).click();

        // Driver stars: second rating-group
        List<WebElement> driverStars = driver.findElements(By.cssSelector(".rating-group:nth-of-type(2) .star-btn"));
        assertTrue(driverRating >= 1 && driverRating <= 5, "Driver rating out of range");
        driverStars.get(driverRating - 1).click();
        debugPause();
    }

    private void typeComment(String text) {
        WebElement textarea = driver.findElement(By.cssSelector("textarea.rating-textarea"));
        textarea.clear();
        textarea.sendKeys(text);
        debugPause();
    }

    private void clickSubmit() {
        WebElement submit = driver.findElement(By.cssSelector(".modal-actions .primary-btn"));
        submit.click();
    }

    private void clickSkip() {
        WebElement skip = driver.findElement(By.cssSelector(".modal-actions .secondary-btn"));
        skip.click();
    }

    private String getErrorMessageIfAny() {
        List<WebElement> errs = driver.findElements(By.cssSelector(".error-message"));
        return errs.isEmpty() ? null : errs.get(0).getText();
    }

    private String getSuccessMessageIfAny() {
        List<WebElement> ok = driver.findElements(By.cssSelector(".success-message"));
        return ok.isEmpty() ? null : ok.get(0).getText();
    }

    private boolean isRatingModalOpen() {
        return !driver.findElements(By.cssSelector(".modal-overlay")).isEmpty();
    }

    private boolean isRideDetailModalOpen() {
        return !driver.findElements(By.cssSelector(".modal-backdrop")).isEmpty();
    }


    private int countStars(String starString) {
        int count = 0;
        for (char c : starString.toCharArray()) {
            if (c == '\u2605') { // ★
                count++;
            }
        }
        return count;
    }


    // =====================
    // Happy Path
    // =====================
    @Test(description = "Passenger can rate a completed ride )
    public void testRateRideHappyPath() {
        navigateToHistoryPage();

        WebElement card = findFirstRatableCompletedRideCardOrSkip();
        openRatingModalFromCard(card);

        // 1. Rate the ride
        int vehicleRating = 4;
        int driverRating = 5;
        String comment = "Great ride! Very smooth.";
        selectVehicleAndDriverStars(vehicleRating, driverRating);
        typeComment(comment);

        clickSubmit();


        try {
            WebElement overlay = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".modal-overlay")));
            overlay.click();
        } catch (Exception ignored) {

        }

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".modal-overlay")));
    }


    @Test(description = "Submit disabled/validation error when no stars selected")
    public void testSubmitWithoutRatingsShowsError() {
        navigateToHistoryPage();

        WebElement card = findFirstRatableCompletedRideCardOrSkip();
        openRatingModalFromCard(card);

        // Submit should be disabled initially (checked via UI property)
        WebElement submit = driver.findElement(By.cssSelector(".modal-actions .primary-btn"));
        assertFalse(submit.isEnabled(), "Submit should be disabled before choosing stars");

        // Close with Skip to clean state
        clickSkip();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".modal-overlay")));
        assertFalse(isRatingModalOpen(), "Modal should be closed after clicking Skip");
    }

    @Test(description = "Modal resets state between openings")
    public void testModalStateResetsOnOpen() {
        navigateToHistoryPage();

        WebElement card = findFirstRatableCompletedRideCardOrSkip();
        openRatingModalFromCard(card);

        selectVehicleAndDriverStars(3, 3);
        typeComment("Temp comment");
        clickSkip();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".modal-overlay")));

        // Reopen
        openRatingModalFromCard(card);

        // Submit should be disabled initially (no stars)
        WebElement submit = driver.findElement(By.cssSelector(".modal-actions .primary-btn"));
        assertFalse(submit.isEnabled(), "Submit should be disabled before choosing stars");

        // No success message initially
        assertNull(getSuccessMessageIfAny());

        // Close cleanly
        clickSkip();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".modal-overlay")));
        assertFalse(isRatingModalOpen());
    }
}
