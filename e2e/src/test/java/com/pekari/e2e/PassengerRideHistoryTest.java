package com.pekari.e2e;

import com.pekari.e2e.pages.LandingPage;
import com.pekari.e2e.pages.LoginPage;
import com.pekari.e2e.pages.PassengerHomePage;
import com.pekari.e2e.pages.PassengerHistoryPage;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.*;

import java.util.List;

import static org.testng.Assert.*;

public class PassengerRideHistoryTest {

    private WebDriver driver;
    private LandingPage landingPage;
    private LoginPage loginPage;
    private PassengerHomePage passengerHomePage;
    private PassengerHistoryPage passengerHistoryPage;
    private static final String BASE_URL = "http://localhost:4200";

    private static final String TEST_EMAIL = "igorami44@gmail.com";
    private static final String TEST_PASSWORD = "Password123";

    // Set to true to slow down tests for visual debugging
    private static final boolean DEBUG_MODE = true;
    private static final int DEBUG_DELAY_MS = 1000;

    private void debugPause() {
        if (DEBUG_MODE) {
            try {
                Thread.sleep(DEBUG_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @BeforeSuite
    public void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeMethod
    public void setupTest() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
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
        landingPage.navigateTo(BASE_URL);
        debugPause();
        landingPage.clickLoginLink();
        loginPage.waitForPageToLoad();
        debugPause();
        loginPage.login(TEST_EMAIL, TEST_PASSWORD);
        passengerHomePage.waitForPageToLoad();
        debugPause();
        passengerHomePage.clickHistoryLink();
        passengerHistoryPage.waitForPageToLoad();
        debugPause();
    }

    @Test(description = "Test sorting ride history by price ascending")
    public void testSortingRideHistory() {
        navigateToHistoryPage();

        int initialCount = passengerHistoryPage.getRideCount();
        assertTrue(initialCount > 0, "Should have rides to sort");
        debugPause();

        passengerHistoryPage.selectSortBy("price");
        debugPause();

        passengerHistoryPage.clickSortDirection();
        debugPause();

        passengerHistoryPage.clickFilter();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        debugPause();

        List<String> prices = passengerHistoryPage.getRidePrices();
        assertTrue(prices.size() > 0, "Should have rides after sorting");

        double previousPrice = 0;
        for (String priceStr : prices) {
            double price = parsePrice(priceStr);
            assertTrue(price >= previousPrice, "Prices should be in ascending order");
            previousPrice = price;
        }
    }

    @Test(description = "Test filtering ride history by date range")
    public void testFilteringRideHistory() {
        navigateToHistoryPage();

        int initialCount = passengerHistoryPage.getRideCount();
        assertTrue(initialCount == 4, "Should have 4 rides initially");
        debugPause();

        passengerHistoryPage.setDateFrom("2026-02-16");
        debugPause();
        passengerHistoryPage.setDateTo("2026-02-16");
        debugPause();

        passengerHistoryPage.clickFilterAndWaitForUpdate(initialCount);
        debugPause();

        int filteredCount = passengerHistoryPage.getRideCount();
        assertTrue(filteredCount < initialCount, "Filtered count should be less than initial");
        assertTrue(filteredCount == 3, "Should have 3 rides on Feb 16");

        List<String> dates = passengerHistoryPage.getRideDates();
        for (String date : dates) {
            if (!date.equals("Unknown date")) {
                assertTrue(date.contains("16 February 2026"), "All rides should be from Feb 16");
            }
        }
    }

    private double parsePrice(String priceStr) {
        String cleaned = priceStr.replace(",", "").replace(" RSD", "").trim();
        return Double.parseDouble(cleaned);
    }
}
