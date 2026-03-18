package com.dimenna.truck.ui;

import com.dimenna.truck.App;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Window;

public final class Alerts {

    private Alerts() {}

    public static Alert confirm(Window owner, String contentText) {
        return themed(owner, Alert.AlertType.CONFIRMATION, "Confirmación", contentText, true);
    }

    public static Alert info(Window owner, String contentText) {
        return themed(owner, Alert.AlertType.INFORMATION, "Información", contentText, false);
    }

    public static Alert error(Window owner, String contentText) {
        return themed(owner, Alert.AlertType.ERROR, "Error", contentText, false);
    }

    private static Alert themed(Window owner,
                                Alert.AlertType type,
                                String title,
                                String content,
                                boolean withOkCancel) {

        Alert a = new Alert(type);
        if (owner != null) {
            a.initOwner(owner);
            a.initModality(Modality.WINDOW_MODAL);
        }

        a.setTitle(title);
        a.setHeaderText(null);

        var cssUrl = App.class.getResource("/theme.css");
        if (cssUrl != null) {
            a.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        }

        a.getDialogPane().getStyleClass().add("card");
        a.getDialogPane().setStyle("-fx-background-color: -surface;");
        a.getDialogPane().setPadding(new Insets(16));
        a.getDialogPane().setMinWidth(420);
        a.setResizable(false);

        Label body = new Label(content);
        body.setWrapText(true);
        body.setMaxWidth(420);
        body.setStyle("-fx-text-fill: -text-main; -fx-font-size: 1.2em;");
        a.getDialogPane().setContent(body);

        a.getButtonTypes().setAll(withOkCancel
                ? new ButtonType[]{ButtonType.OK, ButtonType.CANCEL}
                : new ButtonType[]{ButtonType.OK});

        a.getDialogPane().applyCss();
        a.getDialogPane().layout();

        Button ok = (Button) a.getDialogPane().lookupButton(ButtonType.OK);
        if (ok != null) {
            styleSmall(ok);
            ok.setDefaultButton(true);
            ok.setStyle(ok.getStyle()
                    + "; -fx-background-color: -accent;"
                    + " -fx-text-fill: #23272f;"
                    + " -fx-border-color: -accent;");
        }
        Button cancel = (Button) a.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancel != null) {
            styleSmall(cancel);
            cancel.setCancelButton(true);
        }

        a.setOnShowing(ev -> {
            javafx.geometry.Rectangle2D vb;
            if (owner != null) {
                var screens = javafx.stage.Screen.getScreensForRectangle(
                        owner.getX(), owner.getY(),
                        Math.max(owner.getWidth(), 1),
                        Math.max(owner.getHeight(), 1));
                vb = screens.isEmpty() ? javafx.stage.Screen.getPrimary().getVisualBounds()
                        : screens.get(0).getVisualBounds();
            } else {
                vb = javafx.stage.Screen.getPrimary().getVisualBounds();
            }

            var dp = a.getDialogPane();
            dp.applyCss();
            dp.layout();

            double prefW = Math.max(420, dp.prefWidth(-1)) + 16;
            double prefH = Math.max(120, dp.prefHeight(-1)) + 16;

            double x = vb.getMinX() + (vb.getWidth()  - prefW) / 2.0;
            double y = vb.getMinY() + (vb.getHeight() - prefH) / 2.0;

            var w = dp.getScene().getWindow();
            w.setX(Math.max(x, 0));
            w.setY(Math.max(y, 0));
        });

        return a;
    }

    private static void styleSmall(Button b) {
        b.getStyleClass().add("btn-secondary");
        b.setMinHeight(36);
        b.setPrefHeight(36);
        b.setMaxHeight(36);
        b.setMinWidth(120);
        b.setPrefWidth(120);
        b.setMaxWidth(160);
        b.setStyle(b.getStyle() + "; -fx-font-weight: 800; -fx-font-size: 1em; -fx-padding: 6 12;");
    }
}
