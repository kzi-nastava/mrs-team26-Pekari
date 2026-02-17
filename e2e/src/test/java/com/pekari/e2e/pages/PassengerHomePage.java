package com.pekari.e2e.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class PassengerHomePage {

    private WebDriver driver;
    private WebDriverWait wait;

    @FindBy(css = ".ride-title")
    private WebElement rideTitle;

    @FindBy(css = "a[href='/passenger-history']")
    private WebElement historyLink;

    public PassengerHomePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void waitForPageToLoad() {
        wait.until(ExpectedConditions.visibilityOf(rideTitle));
        wait.until(ExpectedConditions.textToBePresentInElement(rideTitle, "Request Ride"));
    }

    public void clickHistoryLink() {
        wait.until(ExpectedConditions.elementToBeClickable(historyLink));
        historyLink.click();
    }
}
