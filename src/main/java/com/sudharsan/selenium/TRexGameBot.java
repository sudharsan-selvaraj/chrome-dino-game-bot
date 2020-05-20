package com.sudharsan.selenium;

import com.google.common.collect.ImmutableMap;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TRexGameBot {

    private ChromeDriver driver;
    private WebElement canvas;
    private Boolean isFirstJump;

    private static final int DEFAULT_DISTANCE = 195;
    private static final int FLYING_OBSTACLE_WIDTH = 46;
    private static final int TREX_HEIGHT = 51;

    public TRexGameBot() {
        initializeDriver();
    }

    public void startGame() {
        initializeGamePage();
        start();
    }

    private void start() {
        isFirstJump = true;
        hitSpaceBar();

        while (!isGameEnded()) {
            if(isObstaclePresent()){
                hitSpaceBar();
            }
        }

        System.out.println("Your score is:" + getScore());
    }

    private void initializeGamePage() {
        disableInternet();
        driver.get("http://www.google.com");
        canvas = driver.findElement(By.cssSelector(".runner-canvas"));
        new WebDriverWait(driver, 1000).until(ExpectedConditions.visibilityOf(canvas));

        /* Added a delay to avoid race condition before starting the game */
        sleep(1000);
    }

    private void initializeDriver() {
        driver = new ChromeDriver();
    }

    private void disableInternet() {
        Map<String, Object> networkDetailsMap = new HashMap<String, Object>();
        networkDetailsMap.put("offline", true);
        networkDetailsMap.put("latency", 5);
        networkDetailsMap.put("download_throughput", 5000);
        networkDetailsMap.put("upload_throughput", 5000);

        try {
            driver.getCommandExecutor().execute(new Command(
                    driver.getSessionId(),
                    "setNetworkConditions",
                    ImmutableMap.of("network_conditions", ImmutableMap.copyOf(networkDetailsMap))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Boolean isObstaclePresent() {
        Map<String, Long> obstacle = (Map<String, Long>) executeScript("return Runner.instance_.horizon.obstacles.filter(o => (o.xPos > 25))[0] || {}");
        Long tRexPos = (Long) executeScript("return Runner.instance_.tRex.xPos");
        Double currentSpeed = (Double) executeScript("return Runner.instance_.currentSpeed");

        Long distanceToStartJump = isFirstJump ? new Long(DEFAULT_DISTANCE+ 30) : new Long(DEFAULT_DISTANCE);

        /* Dynamically calculate the distance difference to initiate the space bar press event based on current game sped */
        if(currentSpeed >= 10) {
            distanceToStartJump = Math.round(distanceToStartJump + (20 * (currentSpeed % 10))) + 35;
        }

        /*  If the game speed is > 13, space bar needs to be pressed in advance to match with the speed of obstacle movement */
        if(currentSpeed > 13) {
            distanceToStartJump += 50;
        }

        /* Check if there is an obstacle present in the current view */
        if(obstacle!=null && obstacle.containsKey("xPos")) {

            /* If the obstacle is flying, we need to jump only if the height of Trex >= vertical position of the obstacle */
            if(obstacle.get("width") == FLYING_OBSTACLE_WIDTH && obstacle.get("yPos") < TREX_HEIGHT) {
                return false;
            }

            Long currentDistance = obstacle.get("xPos") - tRexPos;

            if(obstacle.get("xPos") > tRexPos && currentDistance <= distanceToStartJump) {
                if(isFirstJump) {
                    isFirstJump = false;
                }
                System.out.println("Identified Obstacle at "+ currentDistance);
                return true;
            }
        }

        return false;
    }

    private void hitSpaceBar() {
        new Actions(driver).sendKeys(canvas, Keys.SPACE).build().perform();
    }

    private Boolean isGameEnded() {
        return (Boolean) executeScript("return Runner.instance_.playCount > 1");
    }

    private String getScore() {
        return (String) executeScript("return Runner.instance_.distanceMeter.highScore.join(\"\").substring(4)");
    }

    private Object executeScript(String command) {
        return ((JavascriptExecutor)driver).executeScript(command);
    }

    private void sleep(Integer time) {
        try {
            Thread.sleep(time);
        } catch (Exception e) {}
    }
}
