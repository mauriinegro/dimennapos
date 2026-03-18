package com.dimenna.truck;

import com.dimenna.truck.core.Database;
import com.dimenna.truck.ui.*;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

public class App extends Application {
    private static App instance;
    private Scene scene;

    
    private MainMenuView mainMenuView;
    private ProductsView productsView;
    private SellView sellView;
    private Settings settingsView;
    private OpenCloseView openCloseView;
    private ReportsView reportsView;

    public static App get() { return instance; }

    @Override
    public void start(Stage stage) {
        instance = this;

        Database.init();

        stage.getIcons().setAll(
            new javafx.scene.image.Image(getClass().getResourceAsStream("/icons/icon256.png"))
        );

        mainMenuView = new MainMenuView();

        productsView = new ProductsView();

        scene = new Scene(mainMenuView, 1100, 720);
        var cssUrl = App.class.getResource("/theme.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        stage.setTitle("DimennaPOS");
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(800);
        stage.setMaximized(true);
        stage.show();

        PauseTransition preload = new PauseTransition(Duration.millis(150));
        preload.setOnFinished(e -> Platform.runLater(() -> {
            if (sellView == null) {
                sellView = new SellView();
            }
            if (openCloseView == null) {
                openCloseView = new OpenCloseView();
            }
            if (reportsView == null) {
                reportsView = new ReportsView();
            }
            if (settingsView == null) {
                settingsView = new Settings();
            }
        }));
        preload.play();
    }

    private void setRoot(Parent root) {
        scene.setRoot(root);
        if (root instanceof Refreshable r) r.onShow();
    }

    public void showMainMenu() {
        setRoot(mainMenuView);
    }

    public void showProducts() {
        setRoot(productsView);
    }

    public void showSell() {
        if (sellView == null) {
            sellView = new SellView();
        }
        setRoot(sellView);
    }

    public void showSettings() {
        if (settingsView == null) {
            settingsView = new Settings();
        }
        setRoot(settingsView);
    }

    public void showOpenClose() {
        if (openCloseView == null) {
            openCloseView = new OpenCloseView();
        }
        setRoot(openCloseView);
    }

    public void showReports() {
        if (reportsView == null) {
            reportsView = new ReportsView();
        }
        setRoot(reportsView);
    }

    public void notifyCatalogChanged() {
        if (sellView instanceof Refreshable r) r.onCatalogChanged();
    }
}
