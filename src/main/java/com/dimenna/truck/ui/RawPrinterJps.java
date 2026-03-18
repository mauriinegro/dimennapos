package com.dimenna.truck.ui;

import javax.imageio.ImageIO;
import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class RawPrinterJps {
    private static final String LOGO_PATH = "/logo-negro.png";

    private static final int PRINTER_DOTS_WIDTH = 576;

    private static final double LOGO_WIDTH_RATIO = 0.40;

    private static final int HR_LEN = 48;

    private RawPrinterJps(){}


    public static void printTest(String printerName) throws Exception {
        printItemStyled(printerName, "PRUEBA", 0);
    }

    public static void printItem(String printerName, String product, int priceCents) throws Exception {
        printItemStyled(printerName, product, priceCents);
    }


    private static void printItemStyled(String printerName, String product, int priceCents) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);

        escInit(out);
        escSelectCodePage(out, 16);        
        escSelectInternational(out, 11);   

        escAlignCenter(out);
        BufferedImage logo = tryLoadLogo();
        if (logo != null) {
            int logoWidth = (int) Math.round(PRINTER_DOTS_WIDTH * LOGO_WIDTH_RATIO);
            writeRasterImage(out, logo, logoWidth);
        }


        writeHr(out);


        escAlignCenter(out);
        escBold(out, true);
        escSize(out, 3, 3);
        writeLine(out, product);      
        escSize(out, 1, 1);
        escBold(out, false);

        escAlignCenter(out);
        escSize(out, 2, 2);
        writeLine(out, formatPrice(priceCents));
        escSize(out, 1, 1);


        writeHr(out);


        escAlignCenter(out);
        writeLine(out, nowLine());


        escCut(out);

        sendToPrinter(printerName, out.toByteArray());
    }


   private static void escSelectCodePage(ByteArrayOutputStream out, int n) {
    out.write(0x1B); out.write('t'); out.write(n); 
}
private static void escSelectInternational(ByteArrayOutputStream out, int n) {
    out.write(0x1B); out.write('R'); out.write(n); 
}

   private static String nowLine() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private static String formatPrice(int cents) {
        int pesos = cents / 100;
        return "$ " + String.format("%,d", pesos);
    }

    private static void escInit(ByteArrayOutputStream out) {
        out.write(0x1B); out.write('@'); 
    }

    private static void escAlignCenter(ByteArrayOutputStream out) {
        out.write(0x1B); out.write('a'); out.write(0x01); 
    }

    private static void escBold(ByteArrayOutputStream out, boolean on) {
        out.write(0x1B); out.write('E'); out.write(on ? 1 : 0); 
    }

    private static void escSize(ByteArrayOutputStream out, int mulW, int mulH) {
        int w = clamp(mulW, 1, 8) - 1; 
        int h = clamp(mulH, 1, 8) - 1; 
        int n = (w << 4) | h;
        out.write(0x1D); out.write('!'); out.write(n);
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    private static void escCut(ByteArrayOutputStream out) {
        
        out.write(0x1D); out.write('V'); out.write(66); out.write(0);
    }

    private static void writeHr(ByteArrayOutputStream out) {
        StringBuilder sb = new StringBuilder(HR_LEN);
        for (int i = 0; i < HR_LEN; i++) sb.append('-');
        writeText(out, sb.append('\n').toString());
    }

    private static void writeLine(ByteArrayOutputStream out, String text) {
        writeText(out, text + "\n");
    }

    private static void writeText(ByteArrayOutputStream out, String s) {
        byte[] data = s.getBytes(java.nio.charset.Charset.forName("windows-1252")); 
        out.writeBytes(data);
    }

    @SuppressWarnings("unused")
    private static void feedLines(ByteArrayOutputStream out, int lines) {
        for (int i = 0; i < lines; i++) out.write('\n');
    }

    private static BufferedImage tryLoadLogo() {
        try (InputStream in = RawPrinterJps.class.getResourceAsStream(LOGO_PATH)) {
            if (in == null) return null;
            return ImageIO.read(in);
        } catch (Exception e) { return null; }
    }


    private static void writeRasterImage(ByteArrayOutputStream out, BufferedImage img, int targetWidthDots) throws Exception {
        BufferedImage bw = toMonochromeScaled(img, targetWidthDots);
        int w = bw.getWidth();
        int h = bw.getHeight();
        int bytesPerRow = (w + 7) / 8;

  
        out.write(0x1D); out.write('v'); out.write(0x30); out.write(0);
        out.write(bytesPerRow & 0xFF);
        out.write((bytesPerRow >> 8) & 0xFF);
        out.write(h & 0xFF);
        out.write((h >> 8) & 0xFF);

        for (int y = 0; y < h; y++) {
            int bit = 0; int cur = 0;
            for (int x = 0; x < w; x++) {
                int rgb = bw.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                boolean black = r < 128; 
                cur <<= 1;
                if (black) cur |= 1;
                bit++;
                if (bit == 8) { out.write(cur); bit = 0; cur = 0; }
            }
            if (bit != 0) { cur <<= (8 - bit); out.write(cur); }
        } 
    }

    private static BufferedImage toMonochromeScaled(BufferedImage src, int targetWidth) {
        int w = targetWidth;
        int h = (int) Math.round(src.getHeight() * (w / (double) src.getWidth()));

        java.awt.image.BufferedImage argb = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g0 = argb.createGraphics();
        g0.setComposite(java.awt.AlphaComposite.SrcOver);
        g0.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g0.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g0.setColor(java.awt.Color.WHITE);
        g0.fillRect(0, 0, w, h);
        g0.drawImage(src, 0, 0, w, h, null);
        g0.dispose();


        java.awt.image.BufferedImage gray = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_BYTE_GRAY);
        var g1 = gray.createGraphics();
        g1.drawImage(argb, 0, 0, null);
        g1.dispose();

        java.awt.image.BufferedImage bw = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_BYTE_BINARY);
        var g2 = bw.createGraphics();
        g2.drawImage(gray, 0, 0, null);
        g2.dispose();

        return bw;
    }

    private static void sendToPrinter(String printerName, byte[] data) throws Exception {
        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        PrintService[] services = PrintServiceLookup.lookupPrintServices(flavor, null);
        PrintService target = null;
        for (var s : services) if (s.getName().equalsIgnoreCase(printerName)) { target = s; break; }
        if (target == null) {
            services = PrintServiceLookup.lookupPrintServices(null, null);
            for (var s : services) if (s.getName().equalsIgnoreCase(printerName)) { target = s; break; }
        }
        if (target == null) throw new IllegalStateException("No se encontró la impresora: " + printerName);

        Doc doc = new SimpleDoc(data, flavor, null);
        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        DocPrintJob job = target.createPrintJob();
        PrintJobWatcher watcher = new PrintJobWatcher(job); 
        job.print(doc, attrs);
        watcher.awaitOrThrow(10000);
    }
public static final class ItemTotal {
    public final String producto; public final int cantidad; public final long subtotalPesos;
    public ItemTotal(String producto, int cantidad, long subtotalPesos) {
        this.producto = producto; this.cantidad = cantidad; this.subtotalPesos = subtotalPesos;
    }
}

private static String formatPesos(long pesos) {
    return "$" + String.format("%,d", pesos);
}

private static final int COL_NAME  = 20;
private static final int COL_QTY   =  6;
private static final int COL_UNIT  = 10;
private static final int COL_TOTAL = 12;

private static String fitLeft(String s, int w) {
    if (s == null) s = "";
    s = s.replace('\n',' ').trim();
    if (s.length() > w) return s.substring(0, w);
    return s + " ".repeat(w - s.length());
}
private static String fitRight(String s, int w) {
    if (s == null) s = "";
    s = s.replace('\n',' ').trim();
    if (s.length() > w) return s.substring(s.length() - w);
    return " ".repeat(w - s.length()) + s;
}

private static void writeTableHeader(ByteArrayOutputStream out) {
    String header =
        fitLeft ("NOMBRE",     COL_NAME) +
        fitRight("CANT",       COL_QTY)  +
        fitRight("UNIDAD",  COL_UNIT) +
        fitRight("SUBTOTAL",      COL_TOTAL);
    writeLine(out, header);
}


private static void writeTableRow(ByteArrayOutputStream out,
                                  String name, int qty, long unitPesos, long totalPesos) {
    String row =
        fitLeft (name,                 COL_NAME) +
        fitRight(Integer.toString(qty),          COL_QTY)  +
        fitRight(formatPesos(unitPesos),         COL_UNIT) +
        fitRight(formatPesos(totalPesos),        COL_TOTAL);
    writeLine(out, row);
}


public static void printPartial(String printerName, long sesionId,
                                java.util.List<ItemTotal> items, long totalPesos) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream(4096);

    escInit(out);
    escSelectCodePage(out, 16);        
    escSelectInternational(out, 11);   

 
    escAlignCenter(out);
    escBold(out, true);
    escSize(out, 2, 2);
    writeLine(out, "REPORTE PARCIAL");
    escSize(out, 1, 1);
    escBold(out, false);
    writeLine(out, "Sesión #" + sesionId + "  " + nowLine());
    writeHr(out);


    writeTableHeader(out);
    writeHr(out);

    int totalQty = 0;

    for (var it : items) {
        int  q   = Math.max(1, it.cantidad);
        long uni = Math.round((double) it.subtotalPesos / q);
        writeTableRow(out, it.producto, q, uni, it.subtotalPesos);
        totalQty += q;
    }

    writeHr(out);
    escBold(out, true);

    String totalRow =
        fitLeft("TOTAL", COL_NAME) +
        fitRight(Integer.toString(totalQty), COL_QTY) + 
        fitRight("",             COL_UNIT) +
        fitRight(formatPesos(totalPesos), COL_TOTAL);
    writeLine(out, totalRow);
    escBold(out, false);

    feedLines(out, 1);
    
    escCut(out);
    sendToPrinter(printerName, out.toByteArray());
}

public static void printFinal(String printerName, long sesionId,
                              String aperturaIso, String cierreIso,
                              java.util.List<ItemTotal> items,
                              long totalPesos, int totalTickets) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream(4096);

    escInit(out);
    escSelectCodePage(out, 16);        
    escSelectInternational(out, 11);   


    escAlignCenter(out);
    escBold(out, true);
    escSize(out, 2, 2);
    writeLine(out, "REPORTE FINAL");
    escSize(out, 1, 1);
    escBold(out, false);


    writeLine(out, "Sesión #" + sesionId);
    if (aperturaIso != null) writeLine(out, "Apertura: " + formatIso(aperturaIso));
    if (cierreIso   != null) writeLine(out, "Cierre:   " + formatIso(cierreIso));
    writeHr(out);


    writeTableHeader(out);
    writeHr(out);

    int totalQty = 0;
    for (var it : items) {
        int  q   = Math.max(1, it.cantidad);
        long uni = Math.round((double) it.subtotalPesos / q);
        writeTableRow(out, it.producto, q, uni, it.subtotalPesos);
        totalQty += q;
    }

    writeHr(out);
    escBold(out, true);
    String totalRow =
        fitLeft("TOTAL", COL_NAME) +
        fitRight(Integer.toString(totalQty), COL_QTY) +
        fitRight("", COL_UNIT) +
        fitRight(formatPesos(totalPesos), COL_TOTAL);
    writeLine(out, totalRow);
    escBold(out, false);

    writeHr(out);
    escAlignCenter(out);

    if (aperturaIso != null && cierreIso != null) {
    writeLine(out, "Tiempo de sesión: " + sessionDuration(aperturaIso, cierreIso));
}
    
feedLines(out, 1);    

escCut(out);
    sendToPrinter(printerName, out.toByteArray());
}


private static LocalDateTime parseDb(String s) {
    if (s == null || s.isBlank()) return null;
    try {

        return LocalDateTime.parse(s.replace(' ', 'T'));
    } catch (Exception e) {
        try {

            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e2) {
            return null;
        }
    }
}


private static String formatIso(String iso) {
    var dt = parseDb(iso);
    if (dt == null) return iso;
    var z = dt.atOffset(java.time.ZoneOffset.UTC)
              .atZoneSameInstant(java.time.ZoneId.systemDefault());
    return z.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
}

private static String sessionDuration(String apIso, String ciIso) {
    var ap = parseDb(apIso);
    var ci = parseDb(ciIso);
    if (ap == null || ci == null) return "-";

    var d = java.time.Duration.between(ap, ci);
    long totalMin = Math.max(0, d.toMinutes());
    long days     = totalMin / 1440;
    long hours    = (totalMin % 1440) / 60;
    long minutes  = totalMin % 60;

    String minStr = (hours > 0 || days > 0) ? String.format("%02d", minutes) : Long.toString(minutes);

    if (days > 0)      return String.format("%dd %dh %s min", days, hours, minStr);
    if (hours > 0)     return String.format("%dh %s min", hours, minStr);
                       return String.format("%s min", minStr);
}


}
