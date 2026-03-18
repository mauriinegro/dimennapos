package com.dimenna.truck.ui;

import com.dimenna.truck.App;
import com.dimenna.truck.dao.ProductDAO;
import com.dimenna.truck.model.Product;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.text.DecimalFormat;
import java.util.List;

public class ProductsView extends BorderPane {

    private final TableView<Row> table = new TableView<>();
    private final ObservableList<Row> data = FXCollections.observableArrayList();
    private final ProductDAO dao = new ProductDAO();
    private final DecimalFormat money = new DecimalFormat("#,##0");

    public ProductsView() {
        setPadding(new Insets(24));
        setStyle("-fx-background-color: -background;");


        Button back = new Button("⬅ Volver");
        back.getStyleClass().add("btn-secondary");
        back.setFocusTraversable(false);
        back.setOnAction(e -> App.get().showMainMenu());

        Label title = new Label("Productos");
        title.getStyleClass().add("title");
        title.setStyle("-fx-font-size: 2.0em; -fx-font-weight: 900;");
        HBox.setHgrow(title, Priority.ALWAYS);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        Region spacerRight = new Region();
        spacerRight.setMinWidth(60);

        HBox header = new HBox(18, back, title, spacerRight);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 18, 0));
        setTop(header);


        table.getStyleClass().addAll("dt-table", "productos-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(44);
        table.setPrefHeight(600);
        table.setStyle("-fx-font-size: 1.15em; -fx-font-family: 'Roboto','Inter','Segoe UI',Arial,sans-serif;");


        TableColumn<Row, Number> cOrden = new TableColumn<>("Orden");
        cOrden.setCellValueFactory(new PropertyValueFactory<>("orden"));
        cOrden.setSortable(false);
        cOrden.setStyle("-fx-alignment: CENTER;");

        TableColumn<Row, String> cNombre = new TableColumn<>("Nombre");
        cNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        cNombre.setSortable(false);
        cNombre.setStyle("-fx-alignment: CENTER;");
        cNombre.setCellFactory(col -> {
            TableCell<Row, String> cell = new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };
            cell.setAlignment(Pos.CENTER);
            return cell;
        });

        TableColumn<Row, String> cPrecio = new TableColumn<>("Precio");
        cPrecio.setCellValueFactory(cell ->
            new SimpleStringProperty("$" + money.format(Math.round(cell.getValue().getPrecio()))));
        cPrecio.setSortable(false);
        cPrecio.setStyle("-fx-alignment: CENTER;");
        cPrecio.setCellFactory(col -> {
            TableCell<Row, String> cell = new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };
            cell.setAlignment(Pos.CENTER);
            return cell;
        });

        TableColumn<Row, Void> cAcciones = new TableColumn<>("Acciones");
        cAcciones.setSortable(false);
        cAcciones.setCellFactory(col -> new TableCell<>() {
            final Button btnEdit  = small("Editar");
            final Button btnDel   = small("Eliminar");
            final Button btnUp    = tinySquare("▲");
            final Button btnDown  = tinySquare("▼");
            final HBox box = new HBox(8, btnEdit, btnDel, new Separator(), btnUp, btnDown);
            {
                box.setAlignment(Pos.CENTER);
                box.setFillHeight(true);

                btnEdit.setOnAction(e -> {
                    Row r = getTableView().getItems().get(getIndex());
                    onEdit(r);
                });
                btnDel.setOnAction(e -> {
                    Row r = getTableView().getItems().get(getIndex());
                    onDelete(r);
                });
                btnUp.setOnAction(e -> {
                    Row r = getTableView().getItems().get(getIndex());
                    dao.moveUp(r.getId());   
                    reload();
                    App.get().notifyCatalogChanged(); 
                });
                btnDown.setOnAction(e -> {
                    Row r = getTableView().getItems().get(getIndex());
                    dao.moveDown(r.getId()); 
                    reload();
                    App.get().notifyCatalogChanged(); 
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
                setAlignment(Pos.CENTER);
            }
            private Button small(String text) {
                Button b = new Button(text);
                b.getStyleClass().add("btn-secondary");
                b.setMinHeight(28); b.setPrefHeight(28); b.setMaxHeight(28);
                b.setMinWidth(72);  b.setPrefWidth(72);  b.setMaxWidth(90);
                b.setFocusTraversable(false);
                b.setStyle("-fx-padding: 2 8; -fx-font-size: 0.95em;");
                return b;
            }
            private Button tinySquare(String text) {
                Button b = new Button(text);
                b.getStyleClass().add("btn-secondary");
                b.setFocusTraversable(false);
                b.setMinSize(24, 24); b.setPrefSize(24, 24); b.setMaxSize(24, 24);
                b.setStyle("-fx-padding: 0; -fx-font-size: 0.9em; -fx-background-radius: 4; -fx-border-radius: 4;");
                return b;
            }
        });

        table.getColumns().addAll(cOrden, cNombre, cPrecio, cAcciones);
        table.setItems(data);

        table.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue();
            setWidthBounded(cOrden,   w * 0.12,  70, 110);
            setWidthBounded(cNombre,  w * 0.46, 250, 800);
            setWidthBounded(cPrecio,  w * 0.18, 110, 170);
            setWidthBounded(cAcciones,w * 0.24, 200, 320);
        });

        VBox tableCard = new VBox(table);
        tableCard.getStyleClass().add("card");
        tableCard.setPadding(new Insets(12));
        VBox.setVgrow(table, Priority.ALWAYS);

        ScrollPane sp = new ScrollPane(tableCard);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.getStyleClass().add("scroll-catalog");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        setCenter(sp);


        Button btnNew = new Button("Nuevo producto");
        btnNew.getStyleClass().add("btn-primary");
        btnNew.setFocusTraversable(false);
        btnNew.setOnAction(e -> onNew());
        btnNew.setStyle(
            "-fx-pref-width: 240; -fx-pref-height: 56;" +
            "-fx-font-size: 1.5em; -fx-font-weight: 900; -fx-padding: 0 0;"
        );

        HBox bottom = new HBox(btnNew);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(20, 0, 0, 0));
        setBottom(bottom);
        reload();
    }

    private static void setWidthBounded(TableColumn<?, ?> col, double pref, double min, double max) {
        if (pref < min) pref = min;
        if (pref > max) pref = max;
        col.setMinWidth(min);
        col.setPrefWidth(pref);
        col.setMaxWidth(max);
    }

    private void reload() {
        List<Product> list = dao.getAll();
        data.setAll(list.stream().map(Row::from).toList());
    }

    private void onNew() {
        ProductFormDialog dlg = new ProductFormDialog(null, null);
        dlg.showAndWait().ifPresent(res -> {
            try {
                dao.insert(res.nombre(), res.precio());
                reload();
                App.get().notifyCatalogChanged();
            } catch (Exception ex) {
                showError("No se pudo crear el producto:\n" + ex.getMessage());
            }
        });
    }

    private void onEdit(Row row) {
        ProductFormDialog dlg = new ProductFormDialog(row.getNombre(), row.getPrecio());
        dlg.showAndWait().ifPresent(res -> {
            try {
                dao.update(row.getId(), res.nombre(), res.precio());
                reload();
                App.get().notifyCatalogChanged();
            } catch (Exception ex) {
                showError("No se pudo actualizar:\n" + ex.getMessage());
            }
        });
    }

    private void onDelete(Row row) {
        Alert confirm = Alerts.confirm(getScene().getWindow(),
                "¿Eliminar \"" + row.getNombre() + "\"?");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    dao.deleteAndCompact(row.getId());
                    reload();
                    App.get().notifyCatalogChanged();
                } catch (Exception ex) {
                    Alerts.error(getScene().getWindow(), "No se pudo eliminar:\n" + ex.getMessage())
                          .showAndWait();
                }
            }
        });
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static class Row {
        private final LongProperty id = new SimpleLongProperty();
        private final IntegerProperty orden = new SimpleIntegerProperty();
        private final StringProperty nombre = new SimpleStringProperty();
        private final DoubleProperty precio = new SimpleDoubleProperty();

        public static Row from(Product p) {
            Row r = new Row();
            r.setId(p.id());
            r.setOrden(p.orden());
            r.setNombre(p.nombre());
            r.setPrecio(p.precio());
            return r;
        }

        public long getId() { return id.get(); }
        public void setId(long v) { id.set(v); }

        public int getOrden() { return orden.get(); }
        public void setOrden(int v) { orden.set(v); }

        public String getNombre() { return nombre.get(); }
        public void setNombre(String v) { nombre.set(v); }

        public double getPrecio() { return precio.get(); }
        public void setPrecio(double v) { precio.set(v); }
    }
}
