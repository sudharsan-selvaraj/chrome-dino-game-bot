package com.sudharsan.selenium;

public class GameEntry {

    public static void main(String[] args) {

        /* Set the system property with the path to chromedriver executable file */
        System.setProperty("webdriver.chrome.driver", "/Users/sudharsan/Documents/Applications/chromedriver");

        new TRexGameBot()
                .startGame();
    }

}
