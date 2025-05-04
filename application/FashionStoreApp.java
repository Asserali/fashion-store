package com.fashionstore.application;

import com.fashionstore.models.User;
import com.fashionstore.storage.DataManager;
import com.fashionstore.utils.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.Timer;
import java.util.TimerTask;

public class FashionStoreApp extends Application {

    private static DataManager dataManager;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialize the data manager and load data
        dataManager = new DataManager();
        dataManager.loadAllData();

        // Print all registered users for debugging
        System.out.println("========= REGISTERED USERS =========");
        for (User user : dataManager.getAllUsers()) {
            System.out.println("User: " + user.getUsername() + ", ID: " + user.getUserId() +
                    ", Password hash: " + user.getPasswordHash());
        }
        System.out.println("===================================");

        // Setup auto-save timer
        setupAutoSave();

        // Configure the primary stage
        primaryStage.setTitle("Fashion Store");

        // Set minimum window size
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);

        // Set initial window size
        primaryStage.setWidth(1024);
        primaryStage.setHeight(768);

        // Center the window on screen
        primaryStage.centerOnScreen();

        // Set the title bar color to black (this will only work on some platforms)
        String darkTitleBar = "-fx-background-color: #000000;";
        try {
            // Apply dark style to the window decoration
            primaryStage.getScene().getRoot().setStyle(darkTitleBar);
        } catch (Exception e) {
            // The scene may not be available yet
            System.out.println("Will apply title bar color after scene is set");
        }

        SceneManager.setPrimaryStage(primaryStage);

        // Load the initial scene
        SceneManager.loadScene("LoginView.fxml");

        // Add a shutdown hook to save data on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Saving data before shutdown...");
            dataManager.saveAllData();
        }));
    }

    public static DataManager getDataManager() {
        return dataManager;
    }

    private void setupAutoSave() {
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Auto-saving data...");
                dataManager.saveAllData();
            }
        }, 5 * 60 * 1000, 5 * 60 * 1000); // Every 5 minutes
    }
}