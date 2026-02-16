package com.pekari.e2e;

import com.pekari.e2e.pages.RegistrationPage;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.*;

import static org.testng.Assert.*;

public class RegistrationTest {

    private WebDriver driver;
    private RegistrationPage registrationPage;
    private static final String BASE_URL = "http://localhost:4200";

    @BeforeSuite
    public void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeMethod
    public void setupTest() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        registrationPage = new RegistrationPage(driver);
    }

    @AfterMethod
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test(description = "Test successful user registration")
    public void testSuccessfulRegistration() {
        registrationPage.navigateTo(BASE_URL);

        registrationPage.registerUser(
            "John",
            "Doe",
            "test" + System.currentTimeMillis() + "@example.com",
            "123 Main St",
            "+381 60 123 4567",
            "Password123!",
            "Password123!"
        );

        assertTrue(registrationPage.isSuccessMessageDisplayed());
        assertTrue(registrationPage.getSuccessMessageText().contains("Activation email sent"));
    }

    @Test(description = "Test registration with invalid email")
    public void testRegistrationWithInvalidEmail() {
        registrationPage.navigateTo(BASE_URL);

        registrationPage.fillFirstName("John");
        registrationPage.fillLastName("Doe");
        registrationPage.fillEmail("invalid-email");
        registrationPage.fillAddress("123 Main St");
        registrationPage.fillPhoneNumber("+381 60 123 4567");
        registrationPage.fillPassword("Password123!");
        registrationPage.fillConfirmPassword("Password123!");

        assertFalse(registrationPage.isCreateAccountButtonEnabled());
        assertTrue(registrationPage.isFieldErrorDisplayed());
    }

    @Test(description = "Test registration with mismatched passwords")
    public void testRegistrationWithMismatchedPasswords() {
        registrationPage.navigateTo(BASE_URL);

        registrationPage.fillFirstName("John");
        registrationPage.fillLastName("Doe");
        registrationPage.fillEmail("test@example.com");
        registrationPage.fillAddress("123 Main St");
        registrationPage.fillPhoneNumber("+381 60 123 4567");
        registrationPage.fillPassword("Password123!");
        registrationPage.fillConfirmPassword("DifferentPassword123!");
        registrationPage.fillFirstName("John");

        assertFalse(registrationPage.isCreateAccountButtonEnabled());
        assertTrue(registrationPage.isFieldErrorDisplayed());
    }
}
