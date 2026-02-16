package com.pekari.e2e.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LandingPage {

    private WebDriver driver;
    private WebDriverWait wait;

    @FindBy(css = "a[href='/login']")
    private WebElement loginLink;

    @FindBy(css = ".ride-title")
    private WebElement rideTitle;

    public LandingPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void navigateTo(String baseUrl) {
        driver.get(baseUrl);
        wait.until(ExpectedConditions.visibilityOf(rideTitle));
    }

    public void clickLoginLink() {
        wait.until(ExpectedConditions.elementToBeClickable(loginLink));
        loginLink.click();
    }
}
