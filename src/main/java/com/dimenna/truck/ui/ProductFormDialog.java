package com.dimenna.truck.ui;

import com.dimenna.truck.App;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class ProductFormDialog extends Dialog<ProductFormDialog.Result> {

    private final TextField tfNombre = new TextField();
    private final TextField tfPrecio = new TextField();

    public record Result(String nombre, double precio) {}

    public ProductFormDialog(String nombreInicial, Double precioInicial) {
        final boolean isEdit = nombreInicial != null;

        setTitle(isEdit ? "Editar producto" : "Nuevo producto");
        setResizable(false);

        // Botones
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button btnOk = (Button) getDialogPane().lookupButton(ButtonType.OK);
        Button btnCancel = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        btnOk.setDefaultButton(true);
        btnCancel.setCancelButton(true);

        var cssUrl = App.class.getResource("/theme.css");
        if (cssUrl != null) {
            getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        }

        getDialogPane().getStyleClass().add("card");
        getDialogPane().setStyle("-fx-background-color: -surface;");
        getDialogPane().setPadding(new Insets(16));
        getDialogPane().setHeaderText(null);
        getDialogPane().setMinWidth(460);

        // Header
        Label title = new Label(isEdit ? "Editar producto" : "Nuevo producto");
        title.setStyle("-fx-font-size: 1.6em; -fx-font-weight: 900; -fx-text-fill: -primary;");

        Text subtitle = new Text(isEdit
                ? "Actualizá el nombre y el precio."
                : "Cargá el nombre y el precio del nuevo producto.");
        subtitle.setStyle("-fx-fill: -text-muted; -fx-font-size: 1.05em;");

        VBox headerBox = new VBox(4, title, subtitle);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(4, 4, 8, 4));

        // Campos
        tfNombre.getStyleClass().add("text-field");
        tfNombre.setPromptText("Nombre del producto");
        tfNombre.setText(nombreInicial != null ? nombreInicial : "");
        tfNombre.setPrefColumnCount(20);
        tfNombre.setMaxWidth(280);

        tfPrecio.getStyleClass().add("text-field");
        tfPrecio.setPromptText("Precio (ej: 2200)");
        tfPrecio.setPrefColumnCount(12);
        tfPrecio.setMaxWidth(160);
        if (precioInicial != null) {
            long r = Math.round(precioInicial);
            if (Math.abs(precioInicial - r) < 1e-9) {
                tfPrecio.setText(String.valueOf(r));
            } else {
                String s = new java.text.DecimalFormat("#.##").format(precioInicial).replace(',', '.');
                tfPrecio.setText(s);
            }
        }
        tfPrecio.setTextFormatter(numericFormatter());

        GridPane gp = new GridPane();
        gp.setHgap(12);
        gp.setVgap(10);
        gp.setPadding(new Insets(8, 8, 8, 8));

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPrefWidth(120);
        c1.setMinWidth(110);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.NEVER);
        gp.getColumnConstraints().addAll(c1, c2);

        Label lNombre = new Label("Nombre");
        lNombre.setStyle("-fx-text-fill: -primary; -fx-font-weight: 800; -fx-font-size: 1.05em;");
        Label lPrecio = new Label("Precio");
        lPrecio.setStyle("-fx-text-fill: -primary; -fx-font-weight: 800; -fx-font-size: 1.05em;");

        gp.add(lNombre, 0, 0);
        gp.add(tfNombre, 1, 0);
        gp.add(lPrecio, 0, 1);
        gp.add(tfPrecio, 1, 1);

        // Contenido
        VBox content = new VBox(10, headerBox, gp);
        content.setFillWidth(true);
        getDialogPane().setContent(content);

        // Botones
        styleAsSmallButton(btnCancel);
        styleAsSmallButton(btnOk);
        btnOk.setStyle(
            btnOk.getStyle() +
            "; -fx-background-color: -accent; -fx-text-fill: #23272f; -fx-border-color: -accent;"
        );

        // Centrar en pantalla al abrir
        Platform.runLater(() -> {
            var w = getDialogPane().getScene().getWindow();
            if (w instanceof Stage s) s.centerOnScreen();
        });

        // Validación al aceptar
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            String n = tfNombre.getText().trim();
            String p = tfPrecio.getText().trim();
            if (n.isEmpty()) {
                showInlineError("El nombre no puede estar vacío.");
                evt.consume();
                return;
            }
            try {
                double val = parsePrecio(p);
                if (val < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                showInlineError("Precio inválido. Usá números con punto o coma.");
                evt.consume();
            }
        });

        // Resultado
        setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                String n = tfNombre.getText().trim();
                double val = parsePrecio(tfPrecio.getText().trim());
                return new Result(n, val);
            }
            return null;
        });
    }

    // --- Helpers de estilo/validación ---

    private static void styleAsSmallButton(Button b) {
        if (b == null) return;
        b.getStyleClass().add("btn-secondary"); // misma base visual
        b.setMinHeight(36);
        b.setPrefHeight(36);
        b.setMaxHeight(36);
        b.setMinWidth(120);
        b.setPrefWidth(120);
        b.setMaxWidth(160);
        b.setStyle("-fx-font-weight: 800; -fx-font-size: 1em; -fx-padding: 6 12;");
    }

    private static double parsePrecio(String raw) {
    String cleaned = raw.trim();
    if (cleaned.isEmpty()) return 0;
    return Double.parseDouble(cleaned);
}

/** TextFormatter: solo permite dígitos */
private static TextFormatter<String> numericFormatter() {
    java.util.function.UnaryOperator<TextFormatter.Change> filter = change -> {
        String t = change.getControlNewText();
        if (t.isEmpty()) return change;
        if (!t.matches("\\d*")) return null;
        return change;
    };
    return new TextFormatter<>(filter);
}

    private void showInlineError(String msg) {
        Label error = new Label(msg);
        error.setStyle("-fx-text-fill: -danger; -fx-font-weight: 700;");
        Node content = getDialogPane().getContent();
        if (content instanceof VBox box) {
            if (box.getChildren().size() == 2) {
                box.getChildren().add(error);
            } else {
                box.getChildren().set(box.getChildren().size() - 1, error);
            }
        }
    }
}
