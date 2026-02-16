package com.pekari.e2e.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class RegistrationPage {

    private WebDriver driver;
    private WebDriverWait wait;

    @FindBy(id = "firstName")
    private WebElement firstNameField;

    @FindBy(id = "lastName")
    private WebElement lastNameField;

    @FindBy(id = "email")
    private WebElement emailField;

    @FindBy(id = "address")
    private WebElement addressField;

    @FindBy(id = "phoneNumber")
    private WebElement phoneNumberField;

    @FindBy(id = "password")
    private WebElement passwordField;

    @FindBy(id = "confirmPassword")
    private WebElement confirmPasswordField;

    @FindBy(css = "button.btn-register")
    private WebElement createAccountButton;

    @FindBy(css = ".register-header h1")
    private WebElement pageHeader;

    @FindBy(css = ".field-error")
    private WebElement fieldError;

    @FindBy(css = ".success-message")
    private WebElement successMessage;

    public RegistrationPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void navigateTo(String baseUrl) {
        driver.get(baseUrl + "/register");
        waitForPageToLoad();
    }

    public void waitForPageToLoad() {
        wait.until(ExpectedConditions.visibilityOf(pageHeader));
    }

    public void fillFirstName(String firstName) {
        wait.until(ExpectedConditions.visibilityOf(firstNameField));
        firstNameField.clear();
        firstNameField.sendKeys(firstName);
    }

    public void fillLastName(String lastName) {
        lastNameField.clear();
        lastNameField.sendKeys(lastName);
    }

    public void fillEmail(String email) {
        emailField.clear();
        emailField.sendKeys(email);
    }

    public void fillAddress(String address) {
        addressField.clear();
        addressField.sendKeys(address);
    }

    public void fillPhoneNumber(String phoneNumber) {
        phoneNumberField.clear();
        phoneNumberField.sendKeys(phoneNumber);
    }

    public void fillPassword(String password) {
        passwordField.clear();
        passwordField.sendKeys(password);
    }

    public void fillConfirmPassword(String confirmPassword) {
        confirmPasswordField.clear();
        confirmPasswordField.sendKeys(confirmPassword);
    }

    public void clickCreateAccount() {
        wait.until(ExpectedConditions.elementToBeClickable(createAccountButton));
        createAccountButton.click();
    }

    public void registerUser(String firstName, String lastName, String email,
                            String address, String phoneNumber, String password, String confirmPassword) {
        fillFirstName(firstName);
        fillLastName(lastName);
        fillEmail(email);
        fillAddress(address);
        fillPhoneNumber(phoneNumber);
        fillPassword(password);
        fillConfirmPassword(confirmPassword);
        clickCreateAccount();
    }

    public boolean isFieldErrorDisplayed() {
        try {
            wait.until(ExpectedConditions.visibilityOf(fieldError));
            return fieldError.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isSuccessMessageDisplayed() {
        try {
            wait.until(ExpectedConditions.visibilityOf(successMessage));
            return successMessage.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getSuccessMessageText() {
        wait.until(ExpectedConditions.visibilityOf(successMessage));
        return successMessage.getText();
    }

    public boolean isCreateAccountButtonEnabled() {
        return createAccountButton.isEnabled();
    }
}
