package com.fashionstore.controllers;

import com.fashionstore.application.FashionStoreApp;
import com.fashionstore.models.User;
import com.fashionstore.storage.DataManager;
import com.fashionstore.utils.SceneManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;

import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML
    private Label userLabel;
    @FXML
    private TabPane tabPane;

    private DataManager dataManager;
    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dataManager = FashionStoreApp.getDataManager();
        currentUser = dataManager.getCurrentUser();

        if (currentUser == null) {
            // Not logged in, redirect to login
            SceneManager.loadScene("LoginView.fxml");
            return;
        }

        // Set welcome message
        String firstName = currentUser.getFirstName();
        if (firstName != null && !firstName.isEmpty()) {
            userLabel.setText("Welcome, " + firstName);
        } else {
            userLabel.setText("Welcome, " + currentUser.getUsername());
        }
    }

    @FXML
    private void handleLogout() {
        dataManager.setCurrentUser(null);
        SceneManager.loadScene("LoginView.fxml");
    }
}