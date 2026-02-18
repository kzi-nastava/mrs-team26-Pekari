package com.pekari.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class PassengerHomePage {

    private WebDriver driver;
    private WebDriverWait wait;

    @FindBy(css = ".ride-title")
    private WebElement rideTitle;

    @FindBy(css = "a[href='/passenger-history']")
    private WebElement historyLink;

    @FindBy(xpath = "//button[contains(@class,'secondary') and contains(text(),'Choose Favorite Route')]")
    private WebElement chooseFavoriteRouteButton;

    @FindBy(css = "input[placeholder='Pickup location']")
    private WebElement pickupInput;

    @FindBy(css = "input[placeholder='Final destination']")
    private WebElement dropoffInput;

    @FindBy(css = ".form-panel .error")
    private WebElement errorElement;

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

    public void clickChooseFavoriteRoute() {
        wait.until(ExpectedConditions.elementToBeClickable(chooseFavoriteRouteButton));
        chooseFavoriteRouteButton.click();
    }

    public boolean isFavoriteRoutesModalVisible() {
        List<WebElement> overlays = driver.findElements(By.cssSelector(".modal-overlay"));
        if (overlays.isEmpty()) return false;
        WebElement overlay = overlays.get(0);
        if (!overlay.isDisplayed()) return false;
        List<WebElement> headings = driver.findElements(By.xpath("//h3[text()='Choose Favorite Route']"));
        return !headings.isEmpty() && headings.get(0).isDisplayed();
    }

    public List<WebElement> getFavoriteRouteItems() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".favorite-routes-list .favorite-route-item")));
        return driver.findElements(By.cssSelector(".favorite-route-item"));
    }

    public void clickFavoriteRouteByIndex(int index) {
        List<WebElement> items = getFavoriteRouteItems();
        if (index < 0 || index >= items.size()) {
            throw new IndexOutOfBoundsException("Favorite route index: " + index + ", size: " + items.size());
        }
        wait.until(ExpectedConditions.elementToBeClickable(items.get(index)));
        items.get(index).click();
    }

    public void closeFavoriteRoutesModal() {
        WebElement closeBtn = wait.until(ExpectedConditions.elementToBeClickable(
            By.cssSelector(".modal-content .modal-header .icon-btn")));
        closeBtn.click();
    }

    public String getPickupAddressValue() {
        wait.until(ExpectedConditions.visibilityOf(pickupInput));
        return pickupInput.getAttribute("value") != null ? pickupInput.getAttribute("value") : "";
    }

    public String getDropoffAddressValue() {
        wait.until(ExpectedConditions.visibilityOf(dropoffInput));
        return dropoffInput.getAttribute("value") != null ? dropoffInput.getAttribute("value") : "";
    }

    public String getErrorMessage() {
        List<WebElement> errors = driver.findElements(By.cssSelector(".form-panel .error"));
        if (errors.isEmpty()) return "";
        WebElement el = errors.get(0);
        return el.isDisplayed() ? el.getText() : "";
    }
}
