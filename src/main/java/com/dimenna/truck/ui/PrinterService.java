package com.dimenna.truck.ui;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.util.ArrayList;
import java.util.List;

public final class PrinterService {
    private static final PrinterService I = new PrinterService();
    public static PrinterService get() { return I; }

    private PrinterService(){}

    public List<String> listPrinters() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        List<String> names = new ArrayList<>();
        for (var s : services) names.add(s.getName());
        return names;
    }

    public void setSelectedPrinter(String name) { AppSettings.setSelectedPrinter(name); }
    public String getSelectedPrinter() { return AppSettings.getSelectedPrinter(); }


    public boolean printTest(String printerName) {
        try {
            RawPrinterJps.printTest(printerName);
            return true;
        } catch (Exception ex) {
            Alerts.error(null, humanize(ex));
            return false;
        }
    }


    public boolean printItem(String printerName, String productName, int priceCents) {
        try {
            RawPrinterJps.printItem(printerName, productName, priceCents);
            return true;
        } catch (Exception ex) {
            Alerts.error(null, humanize(ex));
            return false;
        }
    }

    private String humanize(Exception ex) {
        String m = (ex.getMessage() == null ? "" : ex.getMessage()).toLowerCase();
        if (m.contains("paper") || m.contains("sin papel")) return "La impresora no tiene papel. Cargá papel y tocá Reintentar.";
        if (m.contains("cover") || m.contains("tapa")) return "Cerrá la tapa de la impresora y tocá Reintentar.";
        if (m.contains("cutter") || m.contains("cuchilla")) return "Cuchilla atascada. Liberala según el manual y tocá Reintentar.";
        if (m.contains("offline") || m.contains("desconect")) return "No se detecta la impresora. Revisá USB/energía.";
        if (m.contains("timeout")) return "La impresora no respondió (cola). Revisá conexión/encendido y reintentá.";
        if (m.contains("spooler") || m.contains("trabajo falló")) return "Falló en la cola de impresión. Revisá el driver o reiniciá la impresora.";
        return "Error de impresión: " + ex.getMessage();
    }

     public boolean printPartialReport(String printerName, long sesionId) {
    try (var c = com.dimenna.truck.core.Database.getConnection();
         var ps = c.prepareStatement("""
                SELECT producto,
                       COUNT(*)  AS cant,
                       SUM(precio) AS subtotal
                  FROM tickets
                 WHERE sesion_id = ?
              GROUP BY producto
              ORDER BY cant DESC, producto ASC
            """)) {
        ps.setLong(1, sesionId);

        java.util.List<RawPrinterJps.ItemTotal> items = new java.util.ArrayList<>();
        long total = 0;

        try (var rs = ps.executeQuery()) {
            while (rs.next()) {
                String prod = rs.getString("producto");
                int    cant = rs.getInt("cant");
                long   sub  = Math.round(rs.getDouble("subtotal")); // pesos
                total += sub;
                items.add(new RawPrinterJps.ItemTotal(prod, cant, sub));
            }
        }

        if (items.isEmpty()) {
            Alerts.info(null, "No hay ventas en esta sesión.").showAndWait();
            return false;
        }

        RawPrinterJps.printPartial(printerName, sesionId, items, total);
        return true;
    } catch (Exception ex) {
        Alerts.error(null, humanize(ex));
        return false;
    }
}

public boolean printFinalReport(String printerName, long sesionId) throws Exception {
    java.util.List<com.dimenna.truck.ui.RawPrinterJps.ItemTotal> items = new java.util.ArrayList<>();
    long totalPesos = 0;
    int tickets = 0;
    String ap = null, ci = null;

    try (java.sql.Connection c = com.dimenna.truck.core.Database.getConnection()) {
        try (var ps = c.prepareStatement("SELECT fecha_apertura, fecha_cierre FROM sesiones WHERE id=?")) {
            ps.setLong(1, sesionId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { ap = rs.getString(1); ci = rs.getString(2); }
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
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String prod = rs.getString("producto");
                    int    cant = rs.getInt("cant");
                    long   sub  = Math.round(rs.getDouble("subtotal"));
                    totalPesos += sub;
                    tickets    += cant;
                    items.add(new com.dimenna.truck.ui.RawPrinterJps.ItemTotal(prod, cant, sub));
                }
            }
        }
    }

    if (items.isEmpty()) return false;

    com.dimenna.truck.ui.RawPrinterJps.printFinal(
        printerName, sesionId, ap, ci, items, totalPesos, tickets
    );
    return true;
}

}
