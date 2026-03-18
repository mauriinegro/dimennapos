package com.dimenna.truck.ui;

import com.dimenna.truck.App;
import com.dimenna.truck.dao.ProductDAO;
import com.dimenna.truck.model.Product;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SellView extends BorderPane implements Refreshable {

    private final List<Product> queue = new ArrayList<>();
    private final VBox queueBox = new VBox(0);
    private final Label totalLabel = new Label("$0");
    private final DecimalFormat money = new DecimalFormat("#,##0");

    private final Button btnPrint = new Button("IMPRIMIR");
    private final Label sessionHint = new Label();

    private final FlowPane catalog = new FlowPane();
    private static final double BTN_W = 220;
    private static final double BTN_H = 100;
    private static final double HGAP  = 24;
    private static final int    COLS  = 3;

    private boolean dirty = false;

    public SellView() {
        setPadding(new Insets(24));
        setStyle("-fx-background-color: -background;");

        // --- TOP: Volver ---
        Button back = new Button("⬅ Volver");
        back.getStyleClass().add("btn-secondary");
        back.setFocusTraversable(false);
        back.setOnAction(e -> App.get().showMainMenu());

        HBox header = new HBox(back);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        setTop(header);

        // --- LEFT: Catálogo responsive (~3 por fila), botones 220x100 ---
        catalog.setHgap(HGAP);
        catalog.setVgap(24);
        catalog.setAlignment(Pos.TOP_CENTER);
        catalog.setPadding(new Insets(0));
        catalog.setPrefWrapLength(COLS * BTN_W + (COLS - 1) * HGAP);

        reloadCatalog();

        // Scroll SOLO para el catálogo (izquierda)
        ScrollPane productScroll = new ScrollPane(catalog);
        productScroll.setFitToWidth(true);
        productScroll.setFitToHeight(true);
        productScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        productScroll.getStyleClass().add("scroll-catalog");
        VBox.setVgrow(productScroll, Priority.ALWAYS);

        VBox leftCard = new VBox(productScroll);
        leftCard.getStyleClass().add("card");
        leftCard.setPadding(new Insets(24));
        leftCard.setAlignment(Pos.TOP_CENTER);

        // --- RIGHT: Cola + Total + Acciones ---
        queueBox.setPadding(new Insets(0, 0, 0, 0));
        queueBox.setAlignment(Pos.TOP_CENTER);

        VBox queueContainer = new VBox(queueBox);
        queueContainer.setStyle("-fx-background-color: #23272f; -fx-background-radius: 4; -fx-padding: 8;");

        ScrollPane scroll = new ScrollPane(queueContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(480);
        scroll.setPrefHeight(500);
        scroll.setStyle(
            "-fx-background: transparent; " +
            "-fx-background-color: transparent; " +
            "-fx-border-color: -accent; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4;"
        );
        scroll.getStyleClass().add("scroll-queue");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Label qTitle = new Label("Cola de ítems");
        qTitle.getStyleClass().add("queue-title");
        qTitle.setStyle("-fx-font-size: 1.45em; -fx-font-weight: 900; -fx-padding: 0 0 4 0;");

        totalLabel.getStyleClass().add("total-label");
        totalLabel.setStyle("-fx-font-size: 2.2em; -fx-font-weight: 900; -fx-padding: 0; -fx-alignment: baseline-center;");

        Label totalText = new Label("Total:");
        totalText.getStyleClass().add("queue-title");
        totalText.setStyle("-fx-font-size: 2.2em; -fx-font-weight: 900; -fx-padding: 0; -fx-alignment: baseline-center;");

        HBox totalRow = new HBox(10, totalText, totalLabel);
        totalRow.setAlignment(Pos.BASELINE_LEFT);

        btnPrint.getStyleClass().add("btn-primary");
        btnPrint.setPrefHeight(90);
        btnPrint.setPrefWidth(300);
        btnPrint.setStyle("-fx-font-size: 3.2em; -fx-font-weight: 900; -fx-padding: 30 0 30 0; -fx-background-radius: 4;");
        btnPrint.setFocusTraversable(false);

        Button btnClear = new Button("Vaciar");
        btnClear.getStyleClass().add("btn-secondary");
        btnClear.setPrefHeight(32);
        btnClear.setPrefWidth(90);
        btnClear.setStyle("-fx-font-size: 1.08em; -fx-font-weight: 700; -fx-background-radius: 4;");
        btnClear.setFocusTraversable(false);

        // Vaciar cola
        btnClear.setOnAction(e -> {
            if (queue.isEmpty()) return;
            Alert confirm = Alerts.confirm(
                    btnClear.getScene().getWindow(),
                    "¿Vaciar la cola sin imprimir?"
            );
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    queue.clear();
                    refreshQueue();
                }
            });
        });

        // Imprimir
        btnPrint.setOnAction(e -> {
            if (queue.isEmpty()) return;

            Long sesionId = getSesionActivaId();
            if (sesionId == null) { refreshState(); return; }

            String printerName = com.dimenna.truck.ui.PrinterService.get().getSelectedPrinter();
            if (printerName == null || printerName.isBlank()) {
                Alerts.error(getScene().getWindow(), "No hay impresora configurada. Abrí Configuración y elegí una.");
                return;
            }

            try (var c = com.dimenna.truck.core.Database.getConnection()) {
                c.setAutoCommit(false);

                long nextNro = 1;
                try (var psN = c.prepareStatement(
                        "SELECT COALESCE(MAX(nro_ticket),0) FROM tickets WHERE sesion_id=?")) {
                    psN.setLong(1, sesionId);
                    try (var rsN = psN.executeQuery()) {
                        if (rsN.next()) nextNro = rsN.getLong(1) + 1;
                    }
                }

                int printed = 0;
                for (var item : queue) {
                    String producto = item.nombre();
                    double precio = item.precio();
                    int cents = (int) Math.round(precio * 100.0);

                    boolean ok = com.dimenna.truck.ui.PrinterService.get().printItem(printerName, producto, cents);
                    if (!ok) {
                        c.rollback();
                        return;
                    }

                    long nro = nextNro++;

                    try (var ins = c.prepareStatement(
                         "INSERT INTO tickets(nro_ticket,fecha,producto,precio,sesion_id) VALUES(?,?,?,?,?)")) {
                        ins.setLong(1, nro);
                        ins.setString(2, java.time.LocalDateTime.now().toString());
                        ins.setString(3, producto);
                        ins.setDouble(4, precio);
                        ins.setLong(5, sesionId);
                        ins.executeUpdate();
                    }

                    try (var upd = c.prepareStatement("UPDATE sesiones SET total = total + ? WHERE id = ?")) {
                        upd.setDouble(1, precio);
                        upd.setLong(2, sesionId);
                        upd.executeUpdate();
                    }

                    printed++;
                }

                c.commit();
                queue.clear();
                refreshQueue();
                Alerts.info(getScene().getWindow(), "Se imprimieron " + printed + " ticket(s).");
            } catch (Exception ex) {
                Alerts.error(getScene().getWindow(), "Error guardando tickets: " + ex.getMessage());
            }
        });

        sessionHint.setWrapText(true);
        sessionHint.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 0.95em;");
        sessionHint.setMaxWidth(Double.MAX_VALUE);
        sessionHint.setVisible(false);
        sessionHint.setManaged(false);
        VBox.setMargin(sessionHint, new Insets(0));

        HBox actions = new HBox(24, btnPrint, btnClear);
        actions.getStyleClass().add("actions-bar");
        actions.setAlignment(Pos.CENTER);

        VBox rightCard = new VBox(18, qTitle, scroll, totalRow, actions, sessionHint);
        rightCard.getStyleClass().add("card");
        rightCard.setPadding(new Insets(24));
        rightCard.setPrefWidth(520);
        rightCard.setMinWidth(520);
        rightCard.setAlignment(Pos.TOP_CENTER);

        HBox content = new HBox(24, leftCard, rightCard);
        content.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(leftCard, Priority.ALWAYS);
        setCenter(content);

        refreshQueue();
        refreshState();

        sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((oo, ow, nw) -> {
                    if (nw != null) {
                        nw.focusedProperty().addListener((ooo, was, is) -> {
                            if (is) refreshState();
                        });
                    }
                });
            }
        });
    }

    @Override
    public void onCatalogChanged() {
        dirty = true;
    }

    @Override
    public void onShow() {
        if (dirty) {
            reloadCatalog();
            dirty = false;
        }
    }

    private void reloadCatalog() {
        catalog.getChildren().clear();
        var products = new ProductDAO().getAll();
        for (Product p : products) {
            VBox productBox = new VBox(8);
            productBox.setAlignment(Pos.CENTER);

            Label name = new Label(p.nombre());
            name.setStyle("-fx-font-size: 1.45em; -fx-font-weight: 800; -fx-text-fill: -primary;");
            Label price = new Label("$" + money.format(Math.round(p.precio())));
            price.setStyle("-fx-font-size: 1.18em; -fx-font-weight: 700; -fx-text-fill: -accent-dark;");
            productBox.getChildren().addAll(name, price);

            Button btn = new Button();
            btn.getStyleClass().add("product-btn");
            btn.setPrefSize(BTN_W, BTN_H);
            btn.setFocusTraversable(false);
            btn.setGraphic(productBox);
            btn.setOnAction(e -> {
                queue.add(p);
                refreshQueue();
                refreshState();
            });

            catalog.getChildren().add(btn);
        }
    }

    // --- SESIONES ---
    private Long getSesionActivaId() {
        try (var c = com.dimenna.truck.core.Database.getConnection();
             var ps = c.prepareStatement(
                 "SELECT id FROM sesiones WHERE fecha_cierre IS NULL ORDER BY id DESC LIMIT 1");
             var rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void refreshState() {
        Long sid = getSesionActivaId();
        boolean abierta = (sid != null);
        boolean printerOk = hasPrinterConfigured();

        btnPrint.setDisable(!abierta || !printerOk);

        if (!abierta) {
            sessionHint.setText("No hay un día abierto. Abrí el día para habilitar la impresión.");
            sessionHint.setStyle("-fx-text-fill: -danger; -fx-font-size: 1em; -fx-font-weight: 700;");
            sessionHint.setVisible(true);
            sessionHint.setManaged(true);
        } else if (!printerOk) {
            sessionHint.setText("No hay impresora configurada. Abrí Configuración y elegí una.");
            sessionHint.setStyle("-fx-text-fill: -danger; -fx-font-size: 1em; -fx-font-weight: 700;");
            sessionHint.setVisible(true);
            sessionHint.setManaged(true);
        } else {
            sessionHint.setText("");
            sessionHint.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 1em;");
            sessionHint.setVisible(false);
            sessionHint.setManaged(false);
        }
    }

    private void refreshQueue() {
        queueBox.getChildren().clear();
        long total = 0;

        for (int i = 0; i < queue.size(); i++) {
            Product p = queue.get(i);
            long price = Math.round(p.precio());
            total += price;

            int idx = i;

            Label lbl = new Label(p.nombre() + "  $" + money.format(price));
            lbl.setStyle("-fx-font-size: 1.18em; -fx-font-weight: 700;");

            Button del = new Button("✖");
            del.getStyleClass().add("icon");
            del.setStyle("-fx-background-color: #353945; -fx-text-fill: -primary; -fx-background-radius: 4; -fx-font-size: 1.18em; -fx-border-width: 0;");
            del.setOnMouseEntered(e -> del.setStyle("-fx-background-color: #454b57; -fx-text-fill: -primary; -fx-background-radius: 4; -fx-font-size: 1.18em; -fx-border-width: 0;"));
            del.setOnMouseExited(e -> del.setStyle("-fx-background-color: #353945; -fx-text-fill: -primary; -fx-background-radius: 4; -fx-font-size: 1.18em; -fx-border-width: 0;"));
            del.setFocusTraversable(false);
            del.setOnAction(e -> { queue.remove(idx); refreshQueue(); });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(12, lbl, spacer, del);
            row.getStyleClass().add("queue-item");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setFocusTraversable(false);

            queueBox.getChildren().add(row);
        }

        totalLabel.setText("$" + money.format(total));
    }

    private boolean hasPrinterConfigured() {
        String p = com.dimenna.truck.ui.PrinterService.get().getSelectedPrinter();
        return p != null && !p.isBlank();
    }
}
