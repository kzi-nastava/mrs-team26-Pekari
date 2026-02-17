package com.pekari.e2e.pages;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class PassengerHistoryPage {

    private WebDriver driver;
    private WebDriverWait wait;

    @FindBy(css = ".page-title")
    private WebElement pageTitle;

    @FindBy(id = "dateFrom")
    private WebElement dateFromInput;

    @FindBy(id = "dateTo")
    private WebElement dateToInput;

    @FindBy(css = ".sort-controls select")
    private WebElement sortBySelect;

    @FindBy(css = ".sort-direction-btn")
    private WebElement sortDirectionButton;

    @FindBy(css = ".filter-btn:not(.reset-btn)")
    private WebElement filterButton;

    @FindBy(css = ".ride-card")
    private List<WebElement> rideCards;

    @FindBy(css = ".ride-date")
    private List<WebElement> rideDates;

    @FindBy(css = ".info-value.price")
    private List<WebElement> ridePrices;

    public PassengerHistoryPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void waitForPageToLoad() {
        wait.until(ExpectedConditions.visibilityOf(pageTitle));
        wait.until(ExpectedConditions.textToBePresentInElement(pageTitle, "Ride history"));
    }

    public void setDateFrom(String date) {
        wait.until(ExpectedConditions.visibilityOf(dateFromInput));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
            dateFromInput, date
        );
    }

    public void setDateTo(String date) {
        wait.until(ExpectedConditions.visibilityOf(dateToInput));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
            dateToInput, date
        );
    }

    public void selectSortBy(String value) {
        wait.until(ExpectedConditions.visibilityOf(sortBySelect));
        Select select = new Select(sortBySelect);
        select.selectByValue(value);
    }

    public void clickSortDirection() {
        wait.until(ExpectedConditions.elementToBeClickable(sortDirectionButton));
        sortDirectionButton.click();
    }

    public void clickFilter() {
        wait.until(ExpectedConditions.elementToBeClickable(filterButton));
        filterButton.click();
    }

    public void clickFilterAndWaitForUpdate(int previousCount) {
        wait.until(ExpectedConditions.elementToBeClickable(filterButton));
        filterButton.click();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(d -> getRideCount() != previousCount);
        } catch (Exception e) {
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getRideCount() {
        return rideCards.size();
    }

    public List<String> getRideDates() {
        return rideDates.stream()
                .map(WebElement::getText)
                .toList();
    }

    public List<String> getRidePrices() {
        return ridePrices.stream()
                .map(WebElement::getText)
                .toList();
    }
}
