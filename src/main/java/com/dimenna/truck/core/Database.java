package com.dimenna.truck.core;

import java.io.File;
import java.sql.*;

public class Database {
    private static final String DB_DIR = "data";
    private static final String DB_PATH = DB_DIR + File.separator + "dimennatruck.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    
    public static void init() {
        try {
            
            File dir = new File(DB_DIR);
            if (!dir.exists()) dir.mkdirs();

            
            try (Connection c = getConnection(); Statement st = c.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS productos(
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      nombre TEXT NOT NULL,
                      precio REAL NOT NULL,
                      orden INTEGER NOT NULL DEFAULT 0
                    );
                """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS sesiones(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    fecha_apertura TEXT NOT NULL,
                    fecha_cierre   TEXT,
                    total          REAL NOT NULL DEFAULT 0
                    );
                """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS tickets(
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    nro_ticket INTEGER NOT NULL,
                    fecha      TEXT NOT NULL,
                    producto   TEXT NOT NULL,
                    precio     REAL NOT NULL,
                    sesion_id  INTEGER NOT NULL,
                    FOREIGN KEY(sesion_id) REFERENCES sesiones(id)
                    );
                """);
                st.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_tickets_sesion_nro
                    ON tickets(sesion_id, nro_ticket)
                """);
                
                // si no hay productos
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM productos")) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO productos(nombre, precio, orden) VALUES(?,?,?)")) {
                            insert(ps, "Pinta Común", 1800, 1);
                            insert(ps, "Pinta Lupulada", 2000, 2);
                            insert(ps, "Pinta NEIPA", 2200, 3);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error inicializando DB", e);
        }
    }

    private static void insert(PreparedStatement ps, String nombre, double precio, int orden) throws SQLException {
        ps.setString(1, nombre);
        ps.setDouble(2, precio);
        ps.setInt(3, orden);
        ps.executeUpdate();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }
}
