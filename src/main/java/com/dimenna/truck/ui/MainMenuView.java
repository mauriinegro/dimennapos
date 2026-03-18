package com.dimenna.truck.ui;

import com.dimenna.truck.App;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MainMenuView extends VBox {

    public MainMenuView() {
        setPadding(new Insets(36));
        setSpacing(18);
        setAlignment(Pos.CENTER);

        ImageView logoView = null;
        var url = App.class.getResource("/logo.png");
        if (url != null) {
            logoView = new ImageView(new Image(url.toExternalForm(), 160, 160, true, true));
            logoView.setPreserveRatio(true);
            logoView.getStyleClass().add("menu-logo");
        }

        Label titleMain = new Label("Dimenna");
        titleMain.getStyleClass().addAll("menu-title", "title");
        Label titlePos = new Label("POS");
        titlePos.getStyleClass().add("title-pos");
        titlePos.setStyle("-fx-font-size: 2.8em; -fx-font-weight: 900; -fx-background-color: -accent; -fx-text-fill: #23272f; -fx-background-radius: 4; -fx-padding: 0 4 0 4;");
        HBox titleBox = new HBox(4, titleMain, titlePos);
        titleBox.setAlignment(Pos.BASELINE_CENTER);

        Button btnVender    = big("Vender", App.get()::showSell);
        Button btnApertura  = big("Apertura / Cierre", App.get()::showOpenClose);
        Button btnReportes  = big("Reportes", App.get()::showReports);
        Button btnProductos = big("Productos (ABM)", App.get()::showProducts);
        Button btnConfig    = big("Configuración", App.get()::showSettings);

        VBox header = new VBox(12);
        if (logoView != null) {
            header.getChildren().addAll(logoView, titleBox);
        } else {
            header.getChildren().add(titleBox);
        }
        header.setAlignment(Pos.CENTER);
        header.getStyleClass().add("menu-logo");

        VBox menuItems = new VBox(12, btnVender, btnApertura, btnReportes, btnProductos, btnConfig);
        menuItems.setAlignment(Pos.CENTER);

        getChildren().addAll(header, menuItems);
        getStyleClass().add("menu-main");
    }

    private Button big(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("big-button");
        b.setPrefWidth(480);
        b.setPrefHeight(70);
        b.setFocusTraversable(false);
        b.setOnAction(e -> action.run());
        return b;
    }
}