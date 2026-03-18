package com.dimenna.truck.ui;

import com.dimenna.truck.App;
import com.dimenna.truck.core.Database;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OpenCloseView extends BorderPane implements Refreshable {

    private final Label status = new Label("—");
    private final Button btnOpen = new Button("ABRIR DÍA");
    private final Button btnClose = new Button("CERRAR DÍA");
    private final VBox reportBox = new VBox(8);
    private final Label reportTotal = new Label("$0");
    private final Button btnPrintPartial = new Button("IMPRIMIR REPORTE");
    private final java.text.DecimalFormat money = new java.text.DecimalFormat("#,##0");
    private final Label printHint = new Label();
    private final Label ticketsInfo = new Label("Tickets vendidos: 0");


    public OpenCloseView() {
        Database.init();

        setPadding(new Insets(24));
        setStyle("-fx-background-color: -background;");

        Button back = new Button("⬅ Volver");
        back.getStyleClass().add("btn-secondary");
        back.setFocusTraversable(false);
        back.setOnAction(e -> App.get().showMainMenu());

        HBox header = new HBox(back);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        setTop(header);

        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24));

        Label title = new Label("Apertura / Cierre");
        title.setStyle("-fx-font-size: 1.6em; -fx-font-weight: 900; -fx-text-fill: -text-main;");

        status.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 1.05em;");

        btnOpen.getStyleClass().add("btn-primary");
        btnOpen.setPrefWidth(220);
        btnOpen.setPrefHeight(48);
        btnOpen.setOnAction(e -> openDay());

        btnClose.getStyleClass().add("btn-secondary");
        btnClose.setPrefWidth(220);
        btnClose.setPrefHeight(48);
        btnClose.setOnAction(e -> closeDay());

        HBox actions = new HBox(12, btnOpen, btnClose);
        actions.setAlignment(Pos.CENTER_LEFT);

        Region leftSpacer = new Region();
        VBox.setVgrow(leftSpacer, Priority.ALWAYS);

        card.getChildren().addAll(title, status, new Separator(), actions);

        VBox center = new VBox(card);
        center.setAlignment(Pos.TOP_CENTER);
        setCenter(center);

        Label rptTitle = new Label("Ventas de hoy (parcial)");
        rptTitle.setStyle("-fx-font-size: 1.45em; -fx-font-weight: 900; -fx-padding: 0 0 4 0; -fx-text-fill: -text-main;");

        reportBox.setFillWidth(true);

        VBox reportContainer = new VBox(reportBox);
        reportContainer.setStyle(
            "-fx-background-color: #23272f; " +
            "-fx-background-radius: 4; " +
            "-fx-padding: 8;"
        );

        ScrollPane rptScroll = new ScrollPane(reportContainer);
        rptScroll.setFitToWidth(true);
        rptScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rptScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        rptScroll.setPrefWidth(420);
        rptScroll.setStyle(
            "-fx-background: transparent; " +
            "-fx-background-color: transparent; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4;"
        );
        rptScroll.getStyleClass().add("scroll-catalog");

        Label totalTxt = new Label("TOTAL:");
        totalTxt.setStyle("-fx-font-size: 1.8em; -fx-font-weight: 900; -fx-text-fill: -text-main;");

        reportTotal.setStyle("-fx-font-size: 2em; -fx-font-weight: 900; -fx-text-fill: #ff9800");

        ticketsInfo.setStyle("-fx-font-size: 1.15em; -fx-font-weight: 800; -fx-text-fill: -text-main;");

        HBox ticketsRow = new HBox(ticketsInfo);
        ticketsRow.setAlignment(Pos.CENTER_LEFT);

        VBox.setMargin(ticketsRow, new Insets(6, 0, 0, 0));

        HBox totalRow = new HBox(10,     totalTxt, reportTotal);
        totalRow.setAlignment(Pos.CENTER_LEFT);

        btnPrintPartial.getStyleClass().add("btn-primary");
        btnPrintPartial.setPrefHeight(42);
        btnPrintPartial.setOnAction(e -> printPartial());

        Region pushDown = new Region();
        VBox.setVgrow(pushDown, Priority.ALWAYS);

        // HINT
        printHint.setWrapText(true);
        printHint.setStyle("-fx-text-fill: -text-danger; -fx-font-size: 0.95em;");
        printHint.setVisible(false);
        printHint.setManaged(false);

        VBox rightCard = new VBox(12, rptTitle, rptScroll, pushDown, new Separator(), ticketsRow, totalRow, btnPrintPartial, printHint);
        rightCard.getStyleClass().add("card");
        rightCard.setPadding(new Insets(24));
        rightCard.setPrefWidth(480);
        rightCard.setAlignment(Pos.TOP_CENTER);

        HBox content = new HBox(24, card, rightCard);
        content.setAlignment(Pos.TOP_CENTER);

        content.setFillHeight(true); 
        card.setMaxHeight(Double.MAX_VALUE); 
        rightCard.setMaxHeight(Double.MAX_VALUE);

        HBox.setHgrow(card, Priority.NEVER);
        HBox.setHgrow(rightCard, Priority.NEVER);
        setCenter(content);

        refreshState();
    }

    @Override
    public void onShow() {
        refreshState();
    }

    private void refreshState() {
        var s = getActiveSession();
        if (s == null) {
            status.setText("No hay un día abierto.");
            btnOpen.setDisable(false);
            btnClose.setDisable(true);
        } else {
            status.setText("Sesión #" + s.id + " abierta desde " + fmt(s.apertura));
            btnOpen.setDisable(true);
            btnClose.setDisable(false);
        }
        refreshReport();
    }

    private void openDay() {
        System.out.println("DEBUG: Se ha hecho clic en ABRIR DÍA.");
        if (getActiveSession() != null) {
            System.out.println("DEBUG: Ya hay una sesión activa, cancelando.");
            Alerts.info(getScene().getWindow(), "Ya hay un día abierto.");
            refreshState();
            return;
        }
        try (Connection c = Database.getConnection();
             var ps = c.prepareStatement(
                     "INSERT INTO sesiones(fecha_apertura,total) VALUES(datetime('now'),0)",
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.executeUpdate();
            long sid = -1;
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) sid = gk.getLong(1);
            }
            if (sid <= 0) {
                try (var rs = c.createStatement().executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) sid = rs.getLong(1);
                }
            }
            System.out.println("DEBUG: Sesión abierta con ID: " + sid);
            Alerts.info(getScene().getWindow(), "Día abierto. Sesión #" + sid);
            refreshState();
        } catch (SQLException ex) {
            System.err.println("DEBUG: Error de SQL al abrir el día.");
            System.err.println("DEBUG: Mensaje de la excepción: " + ex.getMessage()); // Nuevo
            Alerts.error(getScene().getWindow(), "No se pudo abrir el día: " + ex.getMessage());
        }
    }

private void closeDay() {
    var s = getActiveSession();
    if (s == null) {
        Alerts.error(getScene().getWindow(), "No hay sesión abierta.");
        return;
    }

    var a = Alerts.confirm(getScene().getWindow(), "¿Cerrar el día?");
    a.showAndWait().ifPresent(bt -> {
        boolean acepto = (bt == ButtonType.OK || bt == ButtonType.YES ||
                          bt.getButtonData() == ButtonBar.ButtonData.OK_DONE ||
                          bt.getButtonData() == ButtonBar.ButtonData.YES);
        if (!acepto) return;

        try (Connection c = Database.getConnection();
             var ps = c.prepareStatement("UPDATE sesiones SET fecha_cierre=datetime('now') WHERE id=?")) {
            ps.setLong(1, s.id);
            ps.executeUpdate();

            Alerts.info(getScene().getWindow(), "Día cerrado. Sesión #" + s.id);
            refreshState(); 

            maybeAskPrintFinal(s.id);

        } catch (SQLException ex) {
            Alerts.error(getScene().getWindow(), "No se pudo cerrar el día: " + ex.getMessage());
        }
    });
}

private void maybeAskPrintFinal(long sessionId) {
    if (!hasPrinterConfigured()) return;
    if (!hasSales(sessionId)) return;

    var q = Alerts.confirm(getScene().getWindow(),
            "¿Imprimir ahora el REPORTE FINAL (ticket) de la sesión #" + sessionId + "?");
    q.showAndWait().ifPresent(resp -> {
        boolean acepto = (resp == ButtonType.OK || resp == ButtonType.YES ||
                          resp.getButtonData() == ButtonBar.ButtonData.OK_DONE ||
                          resp.getButtonData() == ButtonBar.ButtonData.YES);
        if (!acepto) return;

        String printer = com.dimenna.truck.ui.PrinterService.get().getSelectedPrinter();
        try {
            boolean ok = com.dimenna.truck.ui.PrinterService.get().printFinalReport(printer, sessionId);
            if (ok) {
                Alerts.info(getScene().getWindow(), "Se imprimió el REPORTE FINAL.");
            } else {
                Alerts.error(getScene().getWindow(),
                        "No se pudo imprimir el REPORTE FINAL (verificá que existan ventas).");
            }
        } catch (Exception ex) {
            Alerts.error(getScene().getWindow(), "Error al imprimir: " + ex.getMessage());
        }
    });
}


private boolean hasSales(long sessionId) {
    try (Connection c = Database.getConnection();
         var ps = c.prepareStatement("SELECT COUNT(*) FROM tickets WHERE sesion_id=?")) {
        ps.setLong(1, sessionId);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        }
    } catch (SQLException e) {
        Alerts.error(getScene().getWindow(), "Error verificando ventas: " + e.getMessage());
        return false;
    }
}


    private Session getActiveSession() {
        try (Connection c = Database.getConnection();
             var ps = c.prepareStatement("SELECT id, fecha_apertura FROM sesiones WHERE fecha_cierre IS NULL ORDER BY id DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return new Session(rs.getLong(1), rs.getString(2));
        } catch (SQLException ignore) { }
        return null;
    }

    private static String fmt(String iso) {
    try {
        var dt = java.time.LocalDateTime.parse(iso.replace(' ', 'T'));
        var z = dt.atOffset(java.time.ZoneOffset.UTC)
                  .atZoneSameInstant(java.time.ZoneId.systemDefault());
        return z.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    } catch (Exception e) {
        return iso;
    }
}

    private record Session(long id, String apertura) {}

    private void refreshReport() {
        var s = getActiveSession();
        reportBox.getChildren().clear();
        reportTotal.setText("$0");

        boolean any = false;
        long totalGeneral = 0;
        int totalTickets = 0;

        if (s == null) {
            var empty = new Label("Abrí el día para ver ventas en vivo.");
            empty.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 1.05em;");
            reportBox.getChildren().add(empty);

            ticketsInfo.setText("Tickets vendidos: 0");
        } else {
            try (var c = Database.getConnection();
                 var ps = c.prepareStatement("""
                        SELECT producto,
                               COUNT(*) AS cant,
                               SUM(precio) AS subtotal
                          FROM tickets
                         WHERE sesion_id = ?
                      GROUP BY producto
                      ORDER BY cant DESC, producto ASC
                    """)) {
                ps.setLong(1, s.id());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        any = true;
                        String producto = rs.getString("producto");
                        int    cant     = rs.getInt("cant");
                        long   subPesos = Math.round(rs.getDouble("subtotal"));
                        totalGeneral += subPesos;
                        totalTickets += cant;

                        Label left  = new Label(producto + " × " + cant);
                        left.setStyle("-fx-font-size: 1.08em; -fx-font-weight: 700;");
                        Label right = new Label("$" + money.format(subPesos));
                        right.setStyle("-fx-font-size: 1.08em; -fx-font-weight: 700;");

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        HBox row = new HBox(10, left, spacer, right);
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.setStyle("-fx-padding: 4 0 4 0;");
                        reportBox.getChildren().add(row);
                    }
                }
                if (!any) {
                    var empty = new Label("Todavía no hay ventas en esta sesión.");
                    empty.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 1.05em;");
                    reportBox.getChildren().add(empty);
                }
            } catch (Exception ex) {
                reportBox.getChildren().add(new Label("Error cargando reporte: " + ex.getMessage()));
            }
            ticketsInfo.setText("Tickets vendidos: " + totalTickets);
        }

        reportTotal.setText("$" + money.format(totalGeneral));

        boolean hasSession   = (s != null);
        boolean printerOk    = hasPrinterConfigured();
        boolean canPrint     = hasSession && printerOk && any;

        btnPrintPartial.setDisable(!canPrint);

        if (!hasSession) {
            printHint.setText("No hay un día abierto. Abrí el día para habilitar la impresión.");
            printHint.setStyle("-fx-text-fill: -danger; -fx-font-size: 1em; -fx-font-weight: 700;");
            printHint.setVisible(true); printHint.setManaged(true);
        } else if (!printerOk) {
            printHint.setText("No hay impresora configurada. Abrí Configuración y elegí una.");
            printHint.setStyle("-fx-text-fill: -danger; -fx-font-size: 1em; -fx-font-weight: 700;");
            printHint.setVisible(true); printHint.setManaged(true);
        } else {
            printHint.setText("");
            printHint.setVisible(false); printHint.setManaged(false);
        }
    }


    private void printPartial() {
        var s = getActiveSession();
        if (s == null) {
            Alerts.error(getScene().getWindow(), "No hay día abierto.");
            return;
        }
        String printer = com.dimenna.truck.ui.PrinterService.get().getSelectedPrinter();
        if (printer == null || printer.isBlank()) {
            Alerts.error(getScene().getWindow(), "No hay impresora configurada. Abrí Configuración y elegí una.");
            return;
        }
        boolean ok = com.dimenna.truck.ui.PrinterService.get().printPartialReport(printer, s.id());
        if (ok) Alerts.info(getScene().getWindow(), "Se imprimió el reporte parcial.");
    }

    private boolean hasPrinterConfigured() {
        String p = com.dimenna.truck.ui.PrinterService.get().getSelectedPrinter();
        return p != null && !p.isBlank();
    }

}
