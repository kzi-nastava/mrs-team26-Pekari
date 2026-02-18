package com.pekari.e2e;

import com.pekari.e2e.pages.LandingPage;
import com.pekari.e2e.pages.LoginPage;
import com.pekari.e2e.pages.PassengerHomePage;
import com.pekari.e2e.pages.PassengerHistoryPage;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.*;

import static org.testng.Assert.*;

/**
 * E2E tests for feature 2.4.3: Poručivanje vožnje iz omiljenih ruta (Ordering a ride from favorite routes).
 *
 * <p>Preconditions:
 * <ul>
 *   <li>Application running at {@link #BASE_URL} (e.g. http://localhost:4200)</li>
 *   <li>Test passenger ({@link #TEST_EMAIL}) has at least one completed ride in history with
 *       location coordinates, so that "add to favorites" is enabled on history page</li>
 *   <li>For "choose favorite when none" test, the test itself removes all favorites first</li>
 * </ul>
 */
public class FavoriteRoutesE2ETest {

    private WebDriver driver;
    private LandingPage landingPage;
    private LoginPage loginPage;
    private PassengerHomePage passengerHomePage;
    private PassengerHistoryPage passengerHistoryPage;

    private static final String BASE_URL = "http://localhost:4200";
    private static final String TEST_EMAIL = "igorami44@gmail.com";
    private static final String TEST_PASSWORD = "Password123";

    private static final boolean DEBUG_MODE = true;
    private static final int DEBUG_DELAY_MS = 1000;
    /** Pause after each test so the final state is visible in the browser before it closes. */
    private static final int POST_TEST_PAUSE_MS = 3000;

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
            try {
                Thread.sleep(POST_TEST_PAUSE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            driver.quit();
        }
    }

    private void loginAndWaitForPassengerHome() {
        landingPage.navigateTo(BASE_URL);
        debugPause();
        landingPage.clickLoginLink();
        loginPage.waitForPageToLoad();
        debugPause();
        loginPage.login(TEST_EMAIL, TEST_PASSWORD);
        passengerHomePage.waitForPageToLoad();
        debugPause();
    }

    private void navigateToHistoryPage() {
        loginAndWaitForPassengerHome();
        passengerHomePage.clickHistoryLink();
        passengerHistoryPage.waitForPageToLoad();
        debugPause();
    }

    @Test(description = "Add route to favorites from ride history")
    public void testAddRouteToFavoritesOnHistory() {
        navigateToHistoryPage();

        int rideIndex = passengerHistoryPage.getFirstRideIndexWithEnabledFavoriteButton();
        assertTrue(rideIndex >= 0, "Precondition: at least one ride must have coordinates (favorite button enabled)");

        boolean wasFavorited = passengerHistoryPage.isFavoriteButtonFavorited(rideIndex);
        passengerHistoryPage.clickFavoriteOnRide(rideIndex);
        debugPause();

        boolean nowFavorited = passengerHistoryPage.isFavoriteButtonFavorited(rideIndex);
        assertTrue(nowFavorited != wasFavorited, "Favorite state should change after click");
        assertTrue(nowFavorited, "After adding, button should show favorited (★)");
    }

    @Test(description = "Order ride from favorite route: select favorite and verify form is filled")
    public void testOrderRideFromFavoriteRoute() {
        loginAndWaitForPassengerHome();

        passengerHomePage.clickHistoryLink();
        passengerHistoryPage.waitForPageToLoad();
        debugPause();

        int rideIndex = passengerHistoryPage.getFirstRideIndexWithEnabledFavoriteButton();
        assertTrue(rideIndex >= 0, "Precondition: at least one ride must have coordinates");
        if (!passengerHistoryPage.isFavoriteButtonFavorited(rideIndex)) {
            passengerHistoryPage.clickFavoriteOnRide(rideIndex);
            debugPause();
        }

        passengerHistoryPage.clickHomeLink();
        passengerHomePage.waitForPageToLoad();
        debugPause();

        passengerHomePage.clickChooseFavoriteRoute();
        assertTrue(passengerHomePage.isFavoriteRoutesModalVisible(), "Favorite routes modal should be visible");
        debugPause();

        passengerHomePage.clickFavoriteRouteByIndex(0);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertFalse(passengerHomePage.isFavoriteRoutesModalVisible(), "Modal should close after selection");
        String pickup = passengerHomePage.getPickupAddressValue();
        String dropoff = passengerHomePage.getDropoffAddressValue();
        assertFalse(pickup.isEmpty(), "Pickup should be filled after selecting favorite route");
        assertFalse(dropoff.isEmpty(), "Dropoff should be filled after selecting favorite route");
    }

    @Test(description = "Remove route from favorites on ride history")
    public void testRemoveRouteFromFavoritesOnHistory() {
        navigateToHistoryPage();

        int rideIndex = passengerHistoryPage.getFirstRideIndexWithEnabledFavoriteButton();
        assertTrue(rideIndex >= 0, "Precondition: at least one ride with coordinates");
        if (!passengerHistoryPage.isFavoriteButtonFavorited(rideIndex)) {
            passengerHistoryPage.clickFavoriteOnRide(rideIndex);
            debugPause();
        }

        passengerHistoryPage.clickFavoriteOnRide(rideIndex);
        debugPause();

        assertFalse(passengerHistoryPage.isFavoriteButtonFavorited(rideIndex),
            "After remove, button should not be favorited (☆)");
    }

    @Test(description = "Choose favorite when user has no favorites shows error and no modal")
    public void testChooseFavoriteRouteWhenNoneShowsError() {
        loginAndWaitForPassengerHome();

        passengerHomePage.clickHistoryLink();
        passengerHistoryPage.waitForPageToLoad();
        debugPause();

        for (int i = 0; i < passengerHistoryPage.getRideCount(); i++) {
            if (passengerHistoryPage.isFavoriteButtonEnabled(i) && passengerHistoryPage.isFavoriteButtonFavorited(i)) {
                passengerHistoryPage.clickFavoriteOnRide(i);
                debugPause();
            }
        }

        passengerHistoryPage.clickHomeLink();
        passengerHomePage.waitForPageToLoad();
        debugPause();

        passengerHomePage.clickChooseFavoriteRoute();
        debugPause();

        String error = passengerHomePage.getErrorMessage();
        assertTrue(error.contains("favorite") || error.contains("omiljen"),
            "Error message should mention favorites: " + error);
        assertFalse(passengerHomePage.isFavoriteRoutesModalVisible(),
            "Modal should not be visible when user has no favorites");
    }

    @Test(description = "Favorite routes modal opens and closes via close button")
    public void testFavoriteRoutesModalOpensAndCloses() {
        loginAndWaitForPassengerHome();

        passengerHomePage.clickHistoryLink();
        passengerHistoryPage.waitForPageToLoad();
        debugPause();

        int rideIndex = passengerHistoryPage.getFirstRideIndexWithEnabledFavoriteButton();
        if (rideIndex >= 0 && !passengerHistoryPage.isFavoriteButtonFavorited(rideIndex)) {
            passengerHistoryPage.clickFavoriteOnRide(rideIndex);
            debugPause();
        }
        passengerHistoryPage.clickHomeLink();
        passengerHomePage.waitForPageToLoad();
        debugPause();

        passengerHomePage.clickChooseFavoriteRoute();
        assertTrue(passengerHomePage.isFavoriteRoutesModalVisible(), "Modal should be visible after click");
        debugPause();

        passengerHomePage.closeFavoriteRoutesModal();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertFalse(passengerHomePage.isFavoriteRoutesModalVisible(), "Modal should be closed after clicking ×");
    }
}
