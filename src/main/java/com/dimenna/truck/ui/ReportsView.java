package com.dimenna.truck.ui;

import com.dimenna.truck.App;
import com.dimenna.truck.core.Database;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import org.apache.poi.util.Units;
import java.io.InputStream;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class ReportsView extends BorderPane implements Refreshable  {

    private final TableView<SesionRow>       tblSesiones      = new TableView<>();
    private final TableView<TicketRow>       tblTickets       = new TableView<>();
    private final TableView<ProductTotalRow> tblProductTotals = new TableView<>();

    private final Label lblTitle   = new Label("Reportes");
    private final Label lblInfo    = new Label("Seleccioná un día para ver el detalle.");
    private final Label lblTickets = new Label("-");
    private final Label lblTotal   = new Label("-");
    private final VBox  topList    = new VBox(6);

    private final Label printFinalHint = new Label();

    private final Button btnPrintFinal = new Button("IMPRIMIR REPORTE FINAL");
    private final Button btnExportFinal = new Button("EXPORTAR A EXCEL");

    private final DecimalFormat money = new DecimalFormat("#,##0");

    public ReportsView() {
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

        
        styleTable(tblSesiones);
        tblSesiones.setPlaceholder(new Label("-"));

        TableColumn<SesionRow, Number> cId = new TableColumn<>("ID");
        cId.setCellValueFactory(d -> d.getValue().idProperty());
        cId.setStyle("-fx-alignment: CENTER;");

        TableColumn<SesionRow, String> cApertura = new TableColumn<>("Apertura");
        cApertura.setCellValueFactory(d -> d.getValue().aperturaProperty());
        cApertura.setStyle("-fx-alignment: CENTER;");

        TableColumn<SesionRow, String> cCierre = new TableColumn<>("Cierre");
        cCierre.setCellValueFactory(d -> d.getValue().cierreProperty());
        cCierre.setStyle("-fx-alignment: CENTER;");

        TableColumn<SesionRow, Number> cTickets = new TableColumn<>("Tickets");
        cTickets.setCellValueFactory(d -> d.getValue().ticketsProperty());
        cTickets.setStyle("-fx-alignment: CENTER;");

        TableColumn<SesionRow, Number> cTotal = new TableColumn<>("Total");
        cTotal.setCellValueFactory(d -> d.getValue().totalProperty());
        cTotal.setStyle("-fx-alignment: CENTER-RIGHT;");
        cTotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? null : "$ " + money.format(Math.round(n.doubleValue())));
            }
        });
        cTotal.setComparator(java.util.Comparator.comparingDouble(Number::doubleValue));

        TableColumn<SesionRow, Void> cPadSes = spacerCol();

        tblSesiones.getColumns().addAll(cId, cApertura, cCierre, cTickets, cTotal, cPadSes);

        tblSesiones.setRowFactory(tv -> {
            TableRow<SesionRow> r = new TableRow<>();
            r.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !r.isEmpty()) showDetailAsync(r.getItem().getId());
            });
            return r;
        });
        tblSesiones.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) showDetailAsync(n.getId());
            recomputeFinalActionsState();
        });

        
        styleTable(tblTickets);
        tblTickets.setPlaceholder(new Label("—"));

        TableColumn<TicketRow, Number> tcNro   = new TableColumn<>("Nro");
        tcNro.setCellValueFactory(d -> d.getValue().nroProperty());
        tcNro.setStyle("-fx-alignment: CENTER;");

        TableColumn<TicketRow, String> tcFecha = new TableColumn<>("Fecha");
        tcFecha.setCellValueFactory(d -> d.getValue().fechaProperty());
        tcFecha.setStyle("-fx-alignment: CENTER;");

        TableColumn<TicketRow, String> tcProd  = new TableColumn<>("Producto");
        tcProd.setCellValueFactory(d -> d.getValue().productoProperty());
        tcProd.setStyle("-fx-alignment: CENTER;");

        TableColumn<TicketRow, Number> tcPrecio = new TableColumn<>("Precio");
        tcPrecio.setCellValueFactory(d -> d.getValue().precioProperty());
        tcPrecio.setStyle("-fx-alignment: CENTER-RIGHT;");
        tcPrecio.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? null : "$ " + money.format(Math.round(n.doubleValue())));
            }
        });
        tcPrecio.setComparator(java.util.Comparator.comparingDouble(Number::doubleValue));

        TableColumn<TicketRow, Void> cPadTck = spacerCol();

        tblTickets.getColumns().addAll(tcNro, tcFecha, tcProd, tcPrecio, cPadTck);
        makeNonSelectable(tblTickets);

        Label leftTitle1 = new Label("Días cerrados / abiertos");
        Label leftTitle2 = new Label("Tickets del día");
        leftTitle1.setStyle("-fx-font-weight: 800; -fx-text-fill: -primary;");
        leftTitle2.setStyle("-fx-font-weight: 800; -fx-text-fill: -primary;");

        tblSesiones.setPrefHeight(340);
        tblTickets.setPrefHeight(300);

        VBox leftCard = new VBox(12,
                leftTitle1, tblSesiones,
                new Separator(),
                leftTitle2, tblTickets
        );
        leftCard.getStyleClass().add("card");
        leftCard.setPadding(new Insets(16));
        VBox.setVgrow(tblSesiones, Priority.ALWAYS);
        VBox.setVgrow(tblTickets, Priority.ALWAYS);
        leftCard.setMaxWidth(Double.MAX_VALUE);

        
        lblTitle.setStyle("-fx-font-size: 1.8em; -fx-font-weight: 900; -fx-text-fill: -primary;");
        lblInfo.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 1.05em;");
        lblTickets.setStyle("-fx-text-fill: -primary; -fx-font-weight: 900;");
        lblTotal.setStyle("-fx-text-fill: -primary; -fx-font-weight: 900;");

        GridPane stats = new GridPane();
        stats.setHgap(16);
        stats.setVgap(8);
        Label t1 = new Label("Tickets emitidos");
        Label t2 = new Label("Total del día");
        Label t3 = new Label("Top productos");
        t1.setStyle("-fx-text-fill: -text-muted; -fx-font-weight: 700;");
        t2.setStyle("-fx-text-fill: -text-muted; -fx-font-weight: 700;");
        t3.setStyle("-fx-text-fill: -text-muted; -fx-font-weight: 700;");
        stats.addRow(0, t1, lblTickets);
        stats.addRow(1, t2, lblTotal);
        stats.addRow(2, t3, topList);

        
        styleTable(tblProductTotals);
        tblProductTotals.setPlaceholder(new Label("—"));

        TableColumn<ProductTotalRow, String> pProd = new TableColumn<>("Producto");
        pProd.setCellValueFactory(d -> d.getValue().productoProperty());
        pProd.setStyle("-fx-alignment: CENTER;");

        TableColumn<ProductTotalRow, Number> pCant = new TableColumn<>("Cant.");
        pCant.setCellValueFactory(d -> d.getValue().cantidadProperty());
        pCant.setStyle("-fx-alignment: CENTER;");

        TableColumn<ProductTotalRow, Number> pSubt = new TableColumn<>("Subtotal");
        pSubt.setCellValueFactory(d -> d.getValue().subtotalProperty());
        pSubt.setStyle("-fx-alignment: CENTER;");
        pSubt.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "$ " + money.format(Math.round(item.doubleValue())));
            }
        });

        TableColumn<ProductTotalRow, Void> cPadTot = spacerCol();

        tblProductTotals.getColumns().addAll(pProd, pCant, pSubt, cPadTot);
        tblProductTotals.setPrefHeight(320); 
        makeNonSelectable(tblProductTotals);

        Label rightTitleTotals = new Label("Ventas por producto");
        rightTitleTotals.setStyle("-fx-font-weight: 800; -fx-text-fill: -primary;");

        
        btnPrintFinal.getStyleClass().add("btn-primary");
        btnPrintFinal.setPrefHeight(42);
        btnPrintFinal.setMaxWidth(Double.MAX_VALUE);
        btnPrintFinal.setOnAction(e -> onPrintFinal());

        btnExportFinal.getStyleClass().add("btn-secondary");
        btnExportFinal.setPrefHeight(42);
        btnExportFinal.setMaxWidth(Double.MAX_VALUE);
        btnExportFinal.setOnAction(e -> onExportFinal());

        
        printFinalHint.setWrapText(true);
        printFinalHint.setStyle("-fx-text-fill: -text-danger; -fx-font-size: 0.95em; -fx-font-weight: 700;");
        printFinalHint.setVisible(false);
        printFinalHint.setManaged(false);

        Region pushDown = new Region();
        VBox.setVgrow(pushDown, Priority.ALWAYS);

        VBox rightCard = new VBox(12,
                lblTitle, lblInfo,
                new Separator(),
                stats,
                new Separator(),
                rightTitleTotals,
                tblProductTotals,
                pushDown,
                btnPrintFinal,
                btnExportFinal,
                printFinalHint  
        );
        rightCard.getStyleClass().add("card");
        rightCard.setPadding(new Insets(16));
        rightCard.setMaxWidth(Double.MAX_VALUE);

        GridPane content = new GridPane();
        content.setHgap(16);

        ColumnConstraints leftCol  = new ColumnConstraints();
        ColumnConstraints rightCol = new ColumnConstraints();
        leftCol.setPercentWidth(60);
        rightCol.setPercentWidth(40);
        content.getColumnConstraints().addAll(leftCol, rightCol);

        content.add(leftCard, 0, 0);
        content.add(rightCard, 1, 0);

        GridPane.setHgrow(leftCard, Priority.ALWAYS);
        GridPane.setHgrow(rightCard, Priority.ALWAYS);

        setCenter(content);

        btnPrintFinal.setDisable(true);
        btnExportFinal.setDisable(true);
        printFinalHint.setVisible(false);
        printFinalHint.setManaged(false);

        recomputeFinalActionsState();

        applyColumnWidths();
    }

    private static void styleTable(TableView<?> t) {
        t.getStyleClass().addAll("dt-table", "scroll-catalog");
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        t.setFixedCellSize(44);
        t.setStyle("-fx-font-size: 0.95em; -fx-font-family: 'Roboto','Inter','Segoe UI',Arial,sans-serif;");
    }

    private static <R> TableColumn<R, Void> spacerCol() {
        TableColumn<R, Void> sp = new TableColumn<>("");
        sp.setSortable(false);
        sp.setReorderable(false);
        sp.setResizable(false);
        sp.setMinWidth(22);
        sp.setPrefWidth(22);
        sp.setMaxWidth(22);
        sp.setStyle("-fx-padding: 0; -fx-background-color: transparent;");
        return sp;
    }

   private void applyColumnWidths() {
    tblSesiones.widthProperty().addListener((obs, ow, nw) -> {
        double w = nw.doubleValue();
        setWidthBounded(tblSesiones.getColumns().get(0), w * 0.07, 52,  84);   
        setWidthBounded(tblSesiones.getColumns().get(1), w * 0.30, 170, 300);  
        setWidthBounded(tblSesiones.getColumns().get(2), w * 0.27, 160, 280);  
        setWidthBounded(tblSesiones.getColumns().get(3), w * 0.10,  60, 100);  
        setWidthBounded(tblSesiones.getColumns().get(4), w * 0.26, 160, 280);  
    });

    
    tblTickets.widthProperty().addListener((obs, ow, nw) -> {
        double w = nw.doubleValue();
        setWidthBounded(tblTickets.getColumns().get(0), w * 0.12,  84, 120);   
        setWidthBounded(tblTickets.getColumns().get(1), w * 0.30, 180, 300);   
        setWidthBounded(tblTickets.getColumns().get(2), w * 0.38, 180, 520);   
        setWidthBounded(tblTickets.getColumns().get(3), w * 0.20, 140, 220);   
    });

    
    tblProductTotals.widthProperty().addListener((obs, ow, nw) -> {
        double w = nw.doubleValue();
        setWidthBounded(tblProductTotals.getColumns().get(0), w * 0.50, 190, 520); 
        setWidthBounded(tblProductTotals.getColumns().get(1), w * 0.12,  64, 110); 
        setWidthBounded(tblProductTotals.getColumns().get(2), w * 0.38, 160, 300); 
    });
}

    private static void setWidthBounded(TableColumn<?, ?> col, double pref, double min, double max) {
        if (pref < min) pref = min;
        if (pref > max) pref = max;
        col.setMinWidth(min);
        col.setPrefWidth(pref);
        col.setMaxWidth(max);
    }

    @Override public void onShow() { loadSessionsAsync(); }

    

    private void loadSessionsAsync() {
        tblSesiones.setDisable(true);
        lblTitle.setText("Reportes");
        lblInfo.setText("Seleccioná un día para ver el detalle.");
        topList.getChildren().setAll(new Label("—"));
        tblTickets.setItems(FXCollections.observableArrayList());
        tblProductTotals.setItems(FXCollections.observableArrayList());

        new Thread(() -> {
            ObservableList<SesionRow> rows = FXCollections.observableArrayList();
            try (Connection c = Database.getConnection();
                 var ps = c.prepareStatement(
                         """
                         SELECT s.id,
                                s.fecha_apertura,
                                s.fecha_cierre,
                                COALESCE(COUNT(t.id),0)     AS tickets,
                                COALESCE(s.total, 0)        AS total_guardado
                         FROM sesiones s
                         LEFT JOIN tickets t ON t.sesion_id = s.id
                         GROUP BY s.id
                         ORDER BY s.id DESC
                         """
                 );
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long   id = rs.getLong("id");
                    String ap = fmt(rs.getString("fecha_apertura"));
                    String ci = fmtNullable(rs.getString("fecha_cierre"));
                    int    tk = rs.getInt("tickets");
                    double tot= rs.getDouble("total_guardado");
                    rows.add(new SesionRow(id, ap, ci, tk, tot));
                }
            } catch (SQLException e) {
                Platform.runLater(() ->
                        Alerts.error(getScene().getWindow(), "Error cargando sesiones: " + e.getMessage())
                );
            }

            Platform.runLater(() -> {
                tblSesiones.setItems(rows);
                recomputeFinalActionsState();
                tblSesiones.setDisable(false);
                if (!rows.isEmpty()) {
                    tblSesiones.getSelectionModel().selectFirst();
                    showDetailAsync(rows.get(0).getId());
                } else {
                    lblTitle.setText("Reportes");
                    lblInfo.setText("No hay sesiones registradas.");
                    lblTickets.setText("-");
                    lblTotal.setText("-");
                    topList.getChildren().setAll(new Label("—"));
                    tblTickets.setItems(FXCollections.observableArrayList());
                    tblProductTotals.setItems(FXCollections.observableArrayList());
                }
                recomputeFinalActionsState();
            });
        }, "reports-load-sessions").start();
    }

    private void showDetailAsync(long sesionId) {
        lblTitle.setText("Sesión #" + sesionId);
        lblInfo.setText("Cargando detalle...");
        tblTickets.setDisable(true);
        tblProductTotals.setDisable(true);
        topList.getChildren().setAll(new Label("Cargando..."));

        new Thread(() -> {
            int tickets = 0;
            double total = 0;
            List<Label> top = new ArrayList<>();
            ObservableList<TicketRow>       ticketsRows = FXCollections.observableArrayList();
            ObservableList<ProductTotalRow> totalsRows  = FXCollections.observableArrayList();

            try (Connection c = Database.getConnection()) {
                try (var ps = c.prepareStatement(
                        "SELECT COUNT(*), COALESCE(SUM(precio),0) FROM tickets WHERE sesion_id=?")) {
                    ps.setLong(1, sesionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            tickets = rs.getInt(1);
                            total   = rs.getDouble(2);
                        }
                    }
                }
                
                try (var ps = c.prepareStatement(
                        """
                        SELECT producto, COUNT(*) as cant, COALESCE(SUM(precio),0) as tot
                          FROM tickets
                         WHERE sesion_id=?
                      GROUP BY producto
                      ORDER BY cant DESC, tot DESC
                      LIMIT 3
                        """)) {
                    ps.setLong(1, sesionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        boolean any = false;
                        while (rs.next()) {
                            any = true;
                            String prod = rs.getString(1);
                            int    cant = rs.getInt(2);
                            double tot  = rs.getDouble(3);
                            Label l = new Label(prod + "  x" + cant + "  ($ " + money.format(Math.round(tot)) + ")");
                            l.setStyle("-fx-text-fill: -text-main; -fx-font-size: 1.05em;");
                            top.add(l);
                        }
                        if (!any) top.add(new Label("—"));
                    }
                }
                
                try (var ps = c.prepareStatement(
                        "SELECT nro_ticket, fecha, producto, precio FROM tickets WHERE sesion_id=? ORDER BY id ASC")) {
                    ps.setLong(1, sesionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            ticketsRows.add(new TicketRow(
                                    rs.getLong(1),
                                    fmtLocalNoShift(rs.getString(2)),
                                    rs.getString(3),
                                    rs.getDouble(4)
                            ));
                        }
                    }
                }
                
                try (var ps = c.prepareStatement(
                        """
                        SELECT producto, COUNT(*) as cant, COALESCE(SUM(precio),0) as tot
                          FROM tickets
                         WHERE sesion_id=?
                      GROUP BY producto
                      ORDER BY cant DESC, tot DESC
                        """)) {
                    ps.setLong(1, sesionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            totalsRows.add(new ProductTotalRow(
                                    rs.getString("producto"),
                                    rs.getInt("cant"),
                                    rs.getDouble("tot")
                            ));
                        }
                    }
                }

            } catch (SQLException e) {
                Platform.runLater(() ->
                        Alerts.error(getScene().getWindow(), "Error cargando detalle: " + e.getMessage())
                );
            }

            final int fTickets = tickets;
            final double fTotal = total;
            Platform.runLater(() -> {
                lblInfo.setText(detailInfo(sesionId));
                lblTickets.setText(String.valueOf(fTickets));
                lblTotal.setText("$ " + money.format(Math.round(fTotal)));
                topList.getChildren().setAll(top);

                tblTickets.setItems(ticketsRows);
                tblTickets.setDisable(false);

                tblProductTotals.setItems(totalsRows);
                tblProductTotals.setDisable(false);
                recomputeFinalActionsState();
            });
        }, "reports-load-detail").start();
    }

    private String detailInfo(long sesionId) {
        try (Connection c = Database.getConnection();
             var ps = c.prepareStatement("SELECT fecha_apertura, fecha_cierre FROM sesiones WHERE id=?")) {
            ps.setLong(1, sesionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String ap = fmt(rs.getString(1));
                    String ci = fmtNullable(rs.getString(2));
                    return "Apertura: " + ap + (ci != null ? "   |   Cierre: " + ci : "   |   (abierta)");
                }
            }
        } catch (SQLException ignore) {}
        return "";
    }

    

    private static String fmt(String iso) {
    try {
        var dt = parseDbDateTime(iso); 
        if (dt == null) return iso;
        var z = dt.atOffset(java.time.ZoneOffset.UTC)
                  .atZoneSameInstant(java.time.ZoneId.systemDefault());
        return z.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    } catch (Exception e) {
        return iso;
    }
}
private static String fmtNullable(String iso) {
    if (iso == null) return null;
    return fmt(iso);
}

    

    public static class SesionRow {
        private final SimpleLongProperty id = new SimpleLongProperty();
        private final SimpleStringProperty apertura = new SimpleStringProperty();
        private final SimpleStringProperty cierre = new SimpleStringProperty();
        private final SimpleIntegerProperty tickets = new SimpleIntegerProperty();
        private final SimpleDoubleProperty total = new SimpleDoubleProperty();
        public SesionRow(long id, String ap, String ci, int tk, double tot) {
            this.id.set(id); this.apertura.set(ap); this.cierre.set(ci);
            this.tickets.set(tk); this.total.set(tot);
        }
        public long getId() { return id.get(); }
        public SimpleLongProperty idProperty() { return id; }
        public SimpleStringProperty aperturaProperty() { return apertura; }
        public SimpleStringProperty cierreProperty() { return cierre; }
        public SimpleIntegerProperty ticketsProperty() { return tickets; }
        public SimpleDoubleProperty totalProperty() { return total; }
    }

    public static class TicketRow {
        private final SimpleLongProperty nro = new SimpleLongProperty();
        private final SimpleStringProperty fecha = new SimpleStringProperty();
        private final SimpleStringProperty producto = new SimpleStringProperty();
        private final SimpleDoubleProperty precio = new SimpleDoubleProperty();
        public TicketRow(long n, String f, String p, double pr) {
            this.nro.set(n); this.fecha.set(f); this.producto.set(p); this.precio.set(pr);
        }
        public SimpleLongProperty nroProperty() { return nro; }
        public SimpleStringProperty fechaProperty() { return fecha; }
        public SimpleStringProperty productoProperty() { return producto; }
        public SimpleDoubleProperty precioProperty() { return precio; }
    }

    public static class ProductTotalRow {
        private final SimpleStringProperty producto = new SimpleStringProperty();
        private final SimpleIntegerProperty cantidad = new SimpleIntegerProperty();
        private final SimpleDoubleProperty subtotal = new SimpleDoubleProperty();
        public ProductTotalRow(String p, int c, double s) {
            this.producto.set(p); this.cantidad.set(c); this.subtotal.set(s);
        }
        public SimpleStringProperty productoProperty() { return producto; }
        public SimpleIntegerProperty cantidadProperty() { return cantidad; }
        public SimpleDoubleProperty subtotalProperty() { return subtotal; }
    }
    
    private static <T> void makeNonSelectable(TableView<T> t) {
    t.setFocusTraversable(false); 
    t.getSelectionModel().clearSelection();

    t.setRowFactory(tv -> {
        TableRow<T> r = new TableRow<>();
        r.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED,  e -> e.consume());
        r.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, e -> e.consume());
        r.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED,  e -> e.consume());
        return r;
    });

}

    private void onPrintFinal() {

    printFinalHint.setText("");
    printFinalHint.setVisible(false);
    printFinalHint.setManaged(false);


    SesionRow sel = tblSesiones.getSelectionModel().getSelectedItem();
    if (sel == null) {
        showFinalHint("Seleccioná un día de la lista de la izquierda para imprimir su reporte final.");
        return;
    }

    long sesionId = sel.getId();


    String cierre = null;
    List<com.dimenna.truck.ui.RawPrinterJps.ItemTotal> items = new ArrayList<>();

    try (Connection c = Database.getConnection()) {

        try (var ps = c.prepareStatement("SELECT fecha_apertura, fecha_cierre FROM sesiones WHERE id=?")) {
            ps.setLong(1, sesionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cierre   = rs.getString(2);
                }
            }
        }
        if (cierre == null) {
            showFinalHint("Este día sigue abierto. Andá a “Apertura / Cierre” para imprimir el reporte PARCIAL.");
            return;
        }


        try (var ps = c.prepareStatement("""
                SELECT producto, COUNT(*) AS cant, COALESCE(SUM(precio),0) AS subtotal
                  FROM tickets
                 WHERE sesion_id=?
              GROUP BY producto
              ORDER BY cant DESC, producto ASC
            """)) {
            ps.setLong(1, sesionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String prod = rs.getString("producto");
                    int    cant = rs.getInt("cant");
                    long   sub  = Math.round(rs.getDouble("subtotal"));
                    items.add(new com.dimenna.truck.ui.RawPrinterJps.ItemTotal(prod, cant, sub));
                }
            }
        }
    } catch (SQLException ex) {
        Alerts.error(getScene().getWindow(), "Error leyendo datos: " + ex.getMessage());
        return;
    }


    if (items.isEmpty()) {
        showFinalHint("No hay productos vendidos en esta sesión.");
        return;
    }

    String printer = com.dimenna.truck.ui.PrinterService.get().getSelectedPrinter();
    if (printer == null || printer.isBlank()) {
        showFinalHint("No hay impresora configurada. Abrí Configuración y elegí una.");
        return;
    }

    try {
        boolean ok = com.dimenna.truck.ui.PrinterService.get()
                        .printFinalReport(printer, sesionId);
        if (ok) {
            Alerts.info(getScene().getWindow(), "Se imprimió el REPORTE FINAL de la sesión #" + sesionId + ".");
        } else {
            showFinalHint("No se pudo imprimir el REPORTE FINAL (verificá que existan ventas).");
        }
    } catch (Exception ex) {
        showFinalHint("Error al imprimir: " + ex.getMessage());
    }
}

private void showFinalHint(String msg) {
    printFinalHint.setText(msg);
    printFinalHint.setVisible(true);
    printFinalHint.setManaged(true);
}

private void recomputeFinalActionsState() {
    String msg = null;
    boolean ok = true;

      
    if (tblSesiones.getItems() == null || tblSesiones.getItems().isEmpty()) {
        msg = "No hay sesiones registradas.";
        ok = false;

        btnPrintFinal.setDisable(true);
        btnExportFinal.setDisable(true);
        printFinalHint.setText(msg);
        printFinalHint.setVisible(true);
        printFinalHint.setManaged(true);
        return;
    }

    SesionRow sel = tblSesiones.getSelectionModel().getSelectedItem();
    if (sel == null) { 
        msg = "Seleccioná un día de la lista de la izquierda para ver/imprimir su reporte final.";
        ok = false;
    }

    long sesionId = ok ? sel.getId() : -1;

    String cierre = null;
    int tickets = 0;
    boolean anyItems = false;

    if (ok) {
        try (Connection c = Database.getConnection()) {
            
            try (var ps = c.prepareStatement("SELECT fecha_apertura, fecha_cierre FROM sesiones WHERE id=?")) {
                ps.setLong(1, sesionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cierre   = rs.getString(2);
                    }
                }
            }
            if (cierre == null) {
                msg = "Este día sigue abierto. Andá a “Apertura / Cierre” para imprimir el PARCIAL.";
                ok = false;
            }

            
            if (ok) {
                try (var ps = c.prepareStatement("""
                        SELECT COUNT(*) AS cant, COALESCE(SUM(precio),0) AS subtotal
                          FROM tickets
                         WHERE sesion_id=?
                    """)) {
                    ps.setLong(1, sesionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            tickets    = rs.getInt("cant");
                            anyItems   = tickets > 0;
                        }
                    }
                }
                if (!anyItems) {
                    msg = "No hay productos vendidos en esta sesión.";
                    ok = false;
                }
            }

            
            if (ok) {
                String printer = com.dimenna.truck.ui.PrinterService.get().getSelectedPrinter();
                if (printer == null || printer.isBlank()) {
                    msg = "No hay impresora configurada. Abrí Configuración y elegí una.";
                    ok = false;
                }
            }
        } catch (SQLException ex) {
            msg = "Error leyendo datos: " + ex.getMessage();
            ok = false;
        }
    }

   
    btnPrintFinal.setDisable(!ok);
    btnExportFinal.setDisable(!ok);

    if (ok) {
        printFinalHint.setVisible(false);
        printFinalHint.setManaged(false);
        printFinalHint.setText("");
    } else {
        printFinalHint.setText(msg);
        printFinalHint.setVisible(true);
        printFinalHint.setManaged(true);
    }
}

private void onExportFinal() {
    if (btnExportFinal.isDisabled()) return;

    SesionRow sel = tblSesiones.getSelectionModel().getSelectedItem();
    if (sel == null) return;
    long sesionId = sel.getId();

    String apertura = null, cierre = null;
    int tickets = 0;
    long totalPesos = 0;
    List<com.dimenna.truck.ui.RawPrinterJps.ItemTotal> items = new ArrayList<>();

    try (Connection c = Database.getConnection()) {
        
        try (var ps = c.prepareStatement("SELECT fecha_apertura, fecha_cierre FROM sesiones WHERE id=?")) {
            ps.setLong(1, sesionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    apertura = rs.getString(1);
                    cierre   = rs.getString(2);
                }
            }
        }
        
        try (var ps = c.prepareStatement("""
                SELECT producto, COUNT(*) AS cant, COALESCE(SUM(precio),0) AS subtotal
                  FROM tickets
                 WHERE sesion_id=?
              GROUP BY producto
              ORDER BY cant DESC, producto ASC
        """)) {
            ps.setLong(1, sesionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String prod = rs.getString("producto");
                    int    cant = rs.getInt("cant");
                    long   sub  = Math.round(rs.getDouble("subtotal"));
                    items.add(new com.dimenna.truck.ui.RawPrinterJps.ItemTotal(prod, cant, sub));
                    totalPesos += sub;
                    tickets    += cant;
                }
            }
        }
    } catch (SQLException ex) {
        Alerts.error(getScene().getWindow(), "Error leyendo datos: " + ex.getMessage());
        return;
    }

    
    FileChooser fc = new FileChooser();
fc.setTitle("Guardar reporte final");
fc.getExtensionFilters().addAll(
    new FileChooser.ExtensionFilter("Excel (.xlsx)", "*.xlsx"),
    new FileChooser.ExtensionFilter("CSV (Excel) (.csv)", "*.csv")
);
fc.setInitialFileName("reporte-final-sesion-" + sesionId + ".xlsx");
File file = fc.showSaveDialog(getScene().getWindow());
if (file == null) return;

try {
    String name = file.getName().toLowerCase();
    if (name.endsWith(".csv")) {
        exportFinalCsv(file, sesionId, apertura, cierre, items, totalPesos, tickets);
    } else {
        if (!name.endsWith(".xlsx")) {
            file = new File(file.getParentFile(), file.getName() + ".xlsx");
        }
        exportFinalXlsx(file, sesionId, apertura, cierre, items, totalPesos, tickets);
    }
    Alerts.info(getScene().getWindow(), "Exportado a " + file.getAbsolutePath());
} catch (Exception ex) {
    Alerts.error(getScene().getWindow(), "No se pudo exportar: " + ex.getMessage());
}
}


private void exportFinalCsv(File file, long sesionId, String apIso, String ciIso,
                            List<com.dimenna.truck.ui.RawPrinterJps.ItemTotal> items,
                            long totalPesos, int totalTickets) throws Exception {
    try (var os = new FileOutputStream(file);
         var w  = new OutputStreamWriter(os, StandardCharsets.UTF_8);
         var bw = new BufferedWriter(w)) {

        os.write(0xEF); os.write(0xBB); os.write(0xBF);

        java.util.function.Function<String,String> csv = s -> {
            if (s == null) s = "";
            return "\"" + s.replace("\"","\"\"") + "\"";
        };

        String apertura = apIso == null ? "" : formatIso(apIso);
        String cierre   = ciIso == null ? "" : formatIso(ciIso);
        String dur      = (apIso != null && ciIso != null)
                ? sessionDuration(apIso, ciIso) : "";

        
        bw.write(csv.apply("REPORTE FINAL")); bw.newLine();
        bw.write(csv.apply("Sesión") + ";" + csv.apply(Long.toString(sesionId))); bw.newLine();
        bw.write(csv.apply("Apertura") + ";" + csv.apply(apertura)); bw.newLine();
        bw.write(csv.apply("Cierre")   + ";" + csv.apply(cierre));   bw.newLine();
        bw.write(csv.apply("Duración") + ";" + csv.apply(dur));      bw.newLine();
        bw.newLine();

        
        bw.write(String.join(";", "Producto","Cantidad","Precio_unitario","Subtotal")); bw.newLine();

        long totalQty = 0;
        for (var it : items) {
            int  q   = Math.max(1, it.cantidad);
            long uni = Math.round((double) it.subtotalPesos / q);
            totalQty += q;

            bw.write(csv.apply(it.producto) + ";" + q + ";" + uni + ";" + it.subtotalPesos);
            bw.newLine();
        }
        bw.newLine();

        bw.write(csv.apply("Tickets vendidos") + ";" + totalQty); bw.newLine();
        bw.write(csv.apply("Total pesos") + ";" + totalPesos);    bw.newLine();
    }
}


@SuppressWarnings("deprecation")
private void exportFinalXlsx(File file, long sesionId, String apIso, String ciIso,
                             List<com.dimenna.truck.ui.RawPrinterJps.ItemTotal> items,
                             long totalPesos, int totalTickets) throws Exception {

    try (XSSFWorkbook wb = new XSSFWorkbook()) {
        XSSFSheet sh = wb.createSheet("Reporte final");
        DataFormat df = wb.createDataFormat();

        
        CellStyle title = wb.createCellStyle();
        Font ftTitle = wb.createFont();
        ftTitle.setBold(true); ftTitle.setFontHeightInPoints((short)16);
        title.setFont(ftTitle);
        title.setAlignment(HorizontalAlignment.CENTER);

        CellStyle metaKey = wb.createCellStyle();
        Font ftKey = wb.createFont(); ftKey.setBold(true);
        metaKey.setFont(ftKey);

        CellStyle metaVal = wb.createCellStyle();

        CellStyle header = wb.createCellStyle();
        Font ftHeader = wb.createFont(); ftHeader.setBold(true); ftHeader.setColor(IndexedColors.WHITE.getIndex());
        header.setFont(ftHeader);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setThinBorders(header);

        CellStyle intStyle = wb.createCellStyle();
        intStyle.setDataFormat(df.getFormat("#,##0"));
        intStyle.setAlignment(HorizontalAlignment.RIGHT);
        setThinBorders(intStyle);

        CellStyle moneyStyle = wb.createCellStyle();
        moneyStyle.setDataFormat(df.getFormat("$ #,##0"));
        moneyStyle.setAlignment(HorizontalAlignment.RIGHT);
        setThinBorders(moneyStyle);


        CellStyle textStyle = wb.createCellStyle();
        setThinBorders(textStyle);

        CellStyle totalLabel = wb.createCellStyle();
        Font ftTot = wb.createFont(); ftTot.setBold(true);
        totalLabel.setFont(ftTot);
        totalLabel.setAlignment(HorizontalAlignment.LEFT);
        setTopBorderBold(totalLabel);

        CellStyle totalNumber = wb.createCellStyle();
        totalNumber.cloneStyleFrom(moneyStyle);
        Font ftTotNum = wb.createFont(); ftTotNum.setBold(true);
        totalNumber.setFont(ftTotNum);
        setTopBorderBold(totalNumber);

        CellStyle totalIntNumber = wb.createCellStyle();
        totalIntNumber.cloneStyleFrom(intStyle);
        Font ftTotInt = wb.createFont(); ftTotInt.setBold(true);
        totalIntNumber.setFont(ftTotInt);
        setTopBorderBold(totalIntNumber);

        CellStyle totalMoney = wb.createCellStyle();
        totalMoney.cloneStyleFrom(moneyStyle);
        Font ftTotMoney = wb.createFont(); ftTotMoney.setBold(true);
        totalMoney.setFont(ftTotMoney);
        setTopBorderBold(totalMoney);

        

sh.setColumnWidth(0, 11 * 256); 
sh.setColumnWidth(1, 28 * 256); 

for (int rr = 0; rr <= 2; rr++) {
    Row r0 = sh.getRow(rr);
    if (r0 == null) r0 = sh.createRow(rr);
    r0.setHeightInPoints(22f);
}

try (InputStream in = getClass().getResourceAsStream("/logo-negro.png")) {
    if (in != null) {
        byte[] bytes = in.readAllBytes();
        int picIdx = wb.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
        Drawing<?> drawing = sh.createDrawingPatriarch();

        XSSFClientAnchor anc = new XSSFClientAnchor();
        anc.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
        anc.setCol1(0); anc.setRow1(0); 
        anc.setCol2(1); anc.setRow2(3); 
        anc.setDx1(Units.toEMU(2)); 
        anc.setDy1(Units.toEMU(2));
        anc.setDx2(Units.toEMU(2));
        anc.setDy2(Units.toEMU(2));

        drawing.createPicture(anc, picIdx);
    }
} catch (Exception ignore) {}

Row rt = sh.getRow(0);
if (rt == null) rt = sh.createRow(0);

CellStyle titleAcross = wb.createCellStyle();
titleAcross.cloneStyleFrom(title);
titleAcross.setAlignment(HorizontalAlignment.CENTER_SELECTION);

for (int c = 1; c <= 3; c++) {
    Cell cc = rt.getCell(c);
    if (cc == null) cc = rt.createCell(c);
    cc.setCellStyle(titleAcross);
}
Cell ct = rt.getCell(1);
ct.setCellValue("REPORTE FINAL");


String apertura = apIso == null ? "" : formatIso(apIso);
String cierre   = ciIso == null ? "" : formatIso(ciIso);
String dur      = (apIso != null && ciIso != null) ? sessionDuration(apIso, ciIso) : "";

Row m1 = sh.createRow(1);
cell(m1, 1, "Sesión",   metaKey); cell(m1, 2, String.valueOf(sesionId), metaVal);
cell(m1, 3, "Apertura", metaKey); cell(m1, 4, apertura,                 metaVal);

Row m2 = sh.createRow(2);
cell(m2, 1, "Duración", metaKey); cell(m2, 2, dur,      metaVal);
cell(m2, 3, "Cierre",   metaKey); cell(m2, 4, cierre,   metaVal);

int headerRow = 3;
Row rh = sh.createRow(headerRow);
String[] cols = {"Producto", "Cantidad", "Precio unitario", "Subtotal"};
for (int c = 0; c < cols.length; c++) {
    Cell h = rh.createCell(c);
    h.setCellValue(cols[c]);
    h.setCellStyle(header);
}

sh.createFreezePane(0, headerRow + 1);

int dataStart = headerRow + 1;
int r = dataStart;
for (var it : items) {
    int  qty  = Math.max(1, it.cantidad);
    long unit = Math.round((double) it.subtotalPesos / qty);

    Row row = sh.createRow(r++);
    cell(row, 0, it.producto, textStyle);
    num (row, 1, qty,         intStyle); 
    num (row, 2, unit,        moneyStyle);
    num (row, 3, it.subtotalPesos, moneyStyle);
}
int dataEnd = r - 1;

if (dataEnd >= dataStart) {
    sh.setAutoFilter(new CellRangeAddress(headerRow, dataEnd, 0, cols.length - 1));
}

addZebra(wb, sh, dataStart, dataEnd, cols.length);


for (int c = 0; c < cols.length; c++) sh.autoSizeColumn(c, true);
bumpColumnWidth(sh, 0, 6);
bumpColumnWidth(sh, 3, 2);


Row rtota = sh.createRow(++dataEnd + 1);
cell(rtota, 0, "TOTAL", totalLabel);

formulaInt(rtota, 1, "SUM(B" + (dataStart+1) + ":B" + (r) + ")", totalIntNumber);

Cell emp = rtota.createCell(2); emp.setCellStyle(totalMoney);

formulaMoney(rtota, 3, "SUM(D" + (dataStart+1) + ":D" + (r) + ")", totalMoney);


addZebra(wb, sh, dataStart, dataEnd, cols.length);

for (int c = 0; c < cols.length; c++) sh.autoSizeColumn(c, true);
bumpColumnWidth(sh, 0, 6);
bumpColumnWidth(sh, 3, 2);



        
        PrintSetup ps = sh.getPrintSetup();
        ps.setLandscape(true);
        sh.setFitToPage(true);
        ps.setFitWidth((short)1); ps.setFitHeight((short)0);
        sh.setMargin(Sheet.LeftMargin, 0.3);
        sh.setMargin(Sheet.RightMargin, 0.3);
        sh.setMargin(Sheet.TopMargin, 0.5);
        sh.setMargin(Sheet.BottomMargin, 0.5);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
    }
}

private static void bumpColumnWidth(Sheet sh, int col, int extraChars) {
    int cur = sh.getColumnWidth(col);
    int max = 255 * 256;
    int neu = Math.min(max, cur + extraChars * 256);
    sh.setColumnWidth(col, neu);
}

private static void setThinBorders(CellStyle st) {
    st.setBorderTop(BorderStyle.THIN);
    st.setBorderBottom(BorderStyle.THIN);
    st.setBorderLeft(BorderStyle.THIN);
    st.setBorderRight(BorderStyle.THIN);
}
private static void setTopBorderBold(CellStyle st) {
    st.setBorderTop(BorderStyle.MEDIUM);
    st.setBorderBottom(BorderStyle.THIN);
    st.setBorderLeft(BorderStyle.THIN);
    st.setBorderRight(BorderStyle.THIN);
}
private static org.apache.poi.ss.usermodel.Cell cell(Row r, int c, String v, CellStyle st) {
    org.apache.poi.ss.usermodel.Cell x = r.createCell(c);
    x.setCellValue(v);
    x.setCellStyle(st);
    return x;
}

private static org.apache.poi.ss.usermodel.Cell num(Row r, int c, double v, CellStyle st) {
    org.apache.poi.ss.usermodel.Cell x = r.createCell(c);
    x.setCellValue(v);
    x.setCellStyle(st);
    return x;
}

private static org.apache.poi.ss.usermodel.Cell formulaMoney(Row r, int c, String f, CellStyle st) {
    org.apache.poi.ss.usermodel.Cell x = r.createCell(c);
    x.setCellFormula(f);
    x.setCellStyle(st);
    return x;
}

private static org.apache.poi.ss.usermodel.Cell formulaInt(Row r, int c, String f, CellStyle st) {
    org.apache.poi.ss.usermodel.Cell x = r.createCell(c);
    x.setCellFormula(f);
    x.setCellStyle(st);
    return x;
}

private static void addZebra(Workbook wb, Sheet sh, int r1, int r2, int colCount) {
    SheetConditionalFormatting scf = sh.getSheetConditionalFormatting();
    ConditionalFormattingRule rule = scf.createConditionalFormattingRule("MOD(ROW(),2)=0");
    PatternFormatting fill = rule.createPatternFormatting();
    fill.setFillBackgroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    fill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
    CellRangeAddress[] regions = { new CellRangeAddress(r1, r2, 0, colCount - 1) };
    scf.addConditionalFormatting(regions, rule);
}

private static java.time.LocalDateTime parseDbDateTime(String s) {
    if (s == null || s.isBlank()) return null;
    try { return java.time.LocalDateTime.parse(s); } catch (Exception ignore) {}
    try { return java.time.LocalDateTime.parse(s,
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); } catch (Exception ignore) {}
    try { return java.time.LocalDateTime.parse(s,
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")); } catch (Exception ignore) {}
    return null;
}

private static String formatIso(String iso) {
    var dt = parseDbDateTime(iso);
    if (dt == null) return iso;
    var z = dt.atOffset(java.time.ZoneOffset.UTC)
              .atZoneSameInstant(java.time.ZoneId.systemDefault());
    return z.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
}

private static String sessionDuration(String apIso, String ciIso) {
    var ap = parseDbDateTime(apIso);
    var ci = parseDbDateTime(ciIso);
    if (ap == null || ci == null) return "-";
    var d = java.time.Duration.between(ap, ci);
    long totalMin = Math.max(0, d.toMinutes());
    long days     = totalMin / 1440;
    long hours    = (totalMin % 1440) / 60;
    long minutes  = totalMin % 60;
    if (days > 0)  return String.format("%dd %dh %02d min", days, hours, minutes);
    if (hours > 0) return String.format("%dh %02d min", hours, minutes);
    return String.format("%d min", minutes);
}



private static String fmtLocalNoShift(String iso) {
    try {
        var dt = parseDbDateTime(iso);
        if (dt == null) return iso;
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    } catch (Exception e) {
        return iso;
    }
}


}
