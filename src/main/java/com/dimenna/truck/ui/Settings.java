package com.dimenna.truck.ui;

import com.dimenna.truck.App;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.List;

public class Settings extends BorderPane {

    private final ComboBox<String> cbPrinters = new ComboBox<>();
    private final Button backBtn   = new Button("⬅ Volver");
    private final Button refreshBtn= new Button("Actualizar lista");
    private final Button saveBtn   = new Button("Guardar");
    private final Button testBtn   = new Button("Ticket de prueba");
    private final Button clearBtn  = new Button("Borrar"); // solo X

    private final SimpleStringProperty savedPrinter = new SimpleStringProperty("");

    public Settings() {
        setPadding(new Insets(24));

        
        var top = new BorderPane();
        top.setPadding(new Insets(0, 0, 16, 0));

        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setFocusTraversable(false);
        backBtn.setOnAction(e -> App.get().showMainMenu());
        top.setLeft(backBtn);

        var title = new Text("Configuración");
        title.getStyleClass().add("title");
        title.setStyle("-fx-fill: #ffffffff;");
        var titleBox = new HBox(title);
        titleBox.setAlignment(Pos.CENTER);
        top.setCenter(titleBox);

        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setFocusTraversable(false);
        refreshBtn.setOnAction(e -> {
    var owner = getScene()==null?null:getScene().getWindow();

    List<String> before = List.copyOf(cbPrinters.getItems());

    loadPrinters();

    List<String> after = List.copyOf(cbPrinters.getItems());
    var agregadas = after.stream().filter(p -> !before.contains(p)).toList();
    var removidas = before.stream().filter(p -> !after.contains(p)).toList();

    String saved = savedPrinter.get();
    String current = cbPrinters.getValue();
    String savedNorm   = saved   == null ? "" : saved.trim();
    String currentNorm = current == null ? "" : current.trim();

    boolean hasSaved = !savedNorm.isBlank();
    boolean savedExists = hasSaved && after.stream().anyMatch(p -> p.trim().equalsIgnoreCase(savedNorm));

    if (hasSaved && !savedExists) {
        savedPrinter.set("");
        cbPrinters.getSelectionModel().clearSelection();
        cbPrinters.setValue(null);
        cbPrinters.setPromptText("Elegí una impresora…");
        Alerts.info(owner, "La impresora guardada ya no está disponible.").showAndWait();
    } else if (hasSaved) {
        boolean same = currentNorm.equalsIgnoreCase(savedNorm);
        if (same) {
            cbPrinters.getSelectionModel().clearSelection();
            cbPrinters.setValue(null);
        }
        cbPrinters.getSelectionModel().select(
            after.stream().filter(p -> p.trim().equalsIgnoreCase(savedNorm)).findFirst().orElse(null)
        );
    } else {
        boolean currentExists = !currentNorm.isBlank() &&
                after.stream().anyMatch(p -> p.trim().equalsIgnoreCase(currentNorm));
        if (!currentExists) {
            cbPrinters.getSelectionModel().clearSelection();
            cbPrinters.setValue(null);
            cbPrinters.setPromptText("Elegí una impresora…");
        }
        if (cbPrinters.getValue() == null) {
            if (!after.isEmpty()) {
                cbPrinters.getSelectionModel().select(0);
            } else {
                Alerts.info(owner, "No se detectaron impresoras.").showAndWait();
            }
        }
    }

    if (!agregadas.isEmpty() || !removidas.isEmpty()) {
        String msg = (agregadas.isEmpty() ? "" : "Agregadas: " + String.join(", ", agregadas))
                   + (( !agregadas.isEmpty() && !removidas.isEmpty()) ? "\n" : "")
                   + (removidas.isEmpty() ? "" : "Removidas: " + String.join(", ", removidas));
        Alerts.info(owner, msg).showAndWait();
    } else {
        Alerts.info(owner, "Lista de impresoras actualizada (sin cambios).").showAndWait();
    }

});




        var rightBox = new HBox(refreshBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        top.setRight(rightBox);

        setTop(top);

        var card = new VBox(12);
        card.setFillWidth(true);
        card.setMaxWidth(520);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(18, 22, 22, 22));
        card.setStyle("""
            -fx-background-color: -surface;
            -fx-background-radius: 8;
            -fx-border-radius: 8;
            -fx-border-color: -border;
            -fx-border-width: 1;
            -fx-text-fill: #ffffffff;
        """);

        var wrapper = new StackPane(card);
        StackPane.setAlignment(card, Pos.CENTER);
        setCenter(wrapper);

        var lbl = new Label("Impresora del sistema");
        lbl.getStyleClass().add("queue-title");
        lbl.setStyle("-fx-text-fill: #ffffffff;");

        cbPrinters.getStyleClass().add("combo-box");
        cbPrinters.setPromptText("Elegí una impresora…");
        cbPrinters.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cbPrinters, Priority.ALWAYS);

cbPrinters.setStyle("""
    -fx-prompt-text-fill: #f5f6fa;
""");

        cbPrinters.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTextFill(Color.web("#f5f6fa"));
            }
        });
        cbPrinters.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTextFill(Color.web("#1d1d1dff"));
            }
        });

        var savedChip = new Label();
        savedChip.getStyleClass().add("icon");
        savedChip.setStyle("-fx-text-fill: #f5f6fa;");
        savedChip.textProperty().bind(Bindings.createStringBinding(
                () -> savedPrinter.get().isBlank()
                        ? "Sin impresora guardada"
                        : "Seleccionada: " + savedPrinter.get(),
                savedPrinter
        ));

        var chipRow = new HBox(savedChip);
        chipRow.setAlignment(Pos.CENTER);


clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setOnAction(e -> {
            PrinterService.get().setSelectedPrinter("");
            savedPrinter.set("");
            cbPrinters.getSelectionModel().clearSelection();
            cbPrinters.setValue(null);
            cbPrinters.setPromptText("Elegí una impresora…");
            Alerts.info(getScene()==null?null:getScene().getWindow(), "Se quitó la impresora guardada.").showAndWait();
        });

        saveBtn.getStyleClass().add("btn-secondary");
        saveBtn.setOnAction(e -> {
            var owner = getScene()==null?null:getScene().getWindow();
            String sel = cbPrinters.getValue();
            if (sel == null || sel.isBlank()) {
                Alerts.info(owner, "Elegí una impresora de la lista.").showAndWait();
                return;
            }
            PrinterService.get().setSelectedPrinter(sel);
            savedPrinter.set(sel);
            Alerts.info(owner, "Impresora guardada: " + sel).showAndWait();
        });

        var middleRow = new HBox(12, clearBtn, saveBtn);
        middleRow.setAlignment(Pos.CENTER);

        testBtn.getStyleClass().add("btn-primary");
        testBtn.setDefaultButton(true);
        testBtn.disableProperty().bind(cbPrinters.valueProperty().isNull()
                .or(Bindings.createBooleanBinding(
                        () -> cbPrinters.getValue()!=null && cbPrinters.getValue().isBlank(),
                        cbPrinters.valueProperty())));
        testBtn.setOnAction(e -> {
            var owner = getScene()==null?null:getScene().getWindow();
            String sel = cbPrinters.getValue();
            if (sel == null || sel.isBlank()) {
                Alerts.error(owner, "No hay impresora seleccionada.").showAndWait();
                return;
            }
            boolean ok = PrinterService.get().printTest(sel);
            if (ok) Alerts.info(owner, "Se envió el ticket de prueba.").showAndWait();
        });

        var ticketRow = new HBox(testBtn);
        ticketRow.setAlignment(Pos.CENTER);

        var sep = new Separator();
        sep.setStyle("-fx-background-color: #e5e7eb;");

        var comboRow = new HBox(cbPrinters);
        comboRow.setAlignment(Pos.CENTER);

        card.getChildren().addAll(
                lbl,
                comboRow,
                chipRow,
                middleRow,
                sep,
                ticketRow
        );

        cbPrinters.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER && !testBtn.isDisabled()) testBtn.fire();
        });

        loadPrinters();

        String saved = PrinterService.get().getSelectedPrinter();
        savedPrinter.set(saved == null ? "" : saved);

        if (!savedPrinter.get().isBlank() && cbPrinters.getItems().contains(savedPrinter.get())) {
            cbPrinters.getSelectionModel().select(savedPrinter.get());
        } else {
            if (cbPrinters.getValue() != null && !cbPrinters.getItems().contains(cbPrinters.getValue())) {
                cbPrinters.getSelectionModel().clearSelection();
                cbPrinters.setValue(null);
            }
            if (cbPrinters.getValue() == null && !cbPrinters.getItems().isEmpty()) {
                cbPrinters.getSelectionModel().select(0);
            }
        }
    }

    private void loadPrinters() {
    List<String> list = PrinterService.get().listPrinters();

    String oldValue = cbPrinters.getValue();

    cbPrinters.getItems().setAll(list);

    if (oldValue != null && !list.contains(oldValue)) {
        cbPrinters.getSelectionModel().clearSelection();
        cbPrinters.setValue(null);
    }
}

}
