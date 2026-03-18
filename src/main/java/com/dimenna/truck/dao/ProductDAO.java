package com.dimenna.truck.dao;

import com.dimenna.truck.core.Database;
import com.dimenna.truck.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    
    public List<Product> getAll() {
        List<Product> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT id, nombre, precio, orden FROM productos ORDER BY orden ASC, id ASC"
             );
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Product(
                    rs.getLong("id"),
                    rs.getString("nombre"),
                    rs.getDouble("precio"),
                    rs.getInt("orden")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo productos", e);
        }
        return list;
    }

    
    public void insert(String nombre, double precio) {
        int nextOrden = nextOrderValue();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO productos(nombre, precio, orden) VALUES (?,?,?)"
             )) {
            ps.setString(1, nombre);
            ps.setDouble(2, precio);
            ps.setInt(3, nextOrden);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando producto", e);
        }
    }

    
    public void update(long id, String nombre, double precio) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE productos SET nombre=?, precio=? WHERE id=?"
             )) {
            ps.setString(1, nombre);
            ps.setDouble(2, precio);
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando producto", e);
        }
    }

   
    public void delete(long id) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM productos WHERE id=?"
             )) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando producto", e);
        }
    }

    
    public void deleteAndCompact(long id) {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement del = c.prepareStatement("DELETE FROM productos WHERE id=?")) {
                del.setLong(1, id);
                del.executeUpdate();
            }
            compactSequentialOrder(c);
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando y compactando orden", e);
        }
    }

    
    public void moveUp(long id) {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);

            int currentOrder = getOrderById(c, id);
            if (currentOrder <= 1) { c.rollback(); return; }

            long upperId = getIdByOrder(c, currentOrder - 1);
            if (upperId <= 0) { c.rollback(); return; }

            swapOrders(c, id, currentOrder, upperId, currentOrder - 1);

            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Error moviendo producto arriba", e);
        }
    }

   
    public void moveDown(long id) {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);

            int currentOrder = getOrderById(c, id);
            int maxOrder = getMaxOrder(c);
            if (currentOrder >= maxOrder) { c.rollback(); return; }

            long lowerId = getIdByOrder(c, currentOrder + 1);
            if (lowerId <= 0) { c.rollback(); return; }

            swapOrders(c, id, currentOrder, lowerId, currentOrder + 1);

            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Error moviendo producto abajo", e);
        }
    }

    
    public void compactSequentialOrder() {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            compactSequentialOrder(c);
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Error compactando orden", e);
        }
    }

    

    private int nextOrderValue() {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COALESCE(MAX(orden), 0) + 1 FROM productos");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 1;
        } catch (SQLException e) {
            throw new RuntimeException("Error calculando siguiente orden", e);
        }
    }

    private int getOrderById(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT orden FROM productos WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private int getMaxOrder(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(MAX(orden),0) FROM productos");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private long getIdByOrder(Connection c, int order) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM productos WHERE orden=?")) {
            ps.setInt(1, order);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        }
    }

    private void swapOrders(Connection c, long idA, int orderA, long idB, int orderB) throws SQLException {
        try (PreparedStatement upd = c.prepareStatement("UPDATE productos SET orden=? WHERE id=?")) {
            upd.setInt(1, orderB);
            upd.setLong(2, idA);
            upd.executeUpdate();

            upd.setInt(1, orderA);
            upd.setLong(2, idB);
            upd.executeUpdate();
        }
    }

    private void compactSequentialOrder(Connection c) throws SQLException {
        
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM productos ORDER BY orden ASC, id ASC")) {
            int idx = 1;
            try (PreparedStatement upd = c.prepareStatement("UPDATE productos SET orden=? WHERE id=?")) {
                while (rs.next()) {
                    upd.setInt(1, idx++);
                    upd.setLong(2, rs.getLong(1));
                    upd.addBatch();
                }
                upd.executeBatch();
            }
        }
    }
}
