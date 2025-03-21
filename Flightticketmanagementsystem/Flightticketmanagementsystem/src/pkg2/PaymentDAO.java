package pkg2;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {
    private static final String INSERT_PAYMENT = "INSERT INTO payments (ticket_id, amount, payment_status, payment_method, transaction_id, payment_date) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_PAYMENT = "UPDATE payments SET payment_status = ?, transaction_id = ?, payment_date = ? WHERE payment_id = ?";
    private static final String SELECT_PAYMENT_BY_TICKET = "SELECT * FROM payments WHERE ticket_id = ?";
    
    public static void createPayment(Payment payment) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_PAYMENT, Statement.RETURN_GENERATED_KEYS)) {
            
            conn.setAutoCommit(false);
            try {
                stmt.setInt(1, payment.getTicketId());
                stmt.setDouble(2, payment.getAmount());
                stmt.setString(3, payment.getPaymentStatus());
                stmt.setString(4, payment.getPaymentMethod());
                stmt.setString(5, payment.getTransactionId());
                stmt.setTimestamp(6, payment.getPaymentDate() != null ? 
                    Timestamp.valueOf(payment.getPaymentDate()) : null);
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating payment failed, no rows affected.");
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        payment.setPaymentId(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("Creating payment failed, no ID obtained.");
                    }
                }
                
                // Update ticket payment status
                updateTicketPaymentStatus(conn, payment.getTicketId(), payment.getPaymentStatus());
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
    
    public static void updatePaymentStatus(int paymentId, String status, String transactionId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_PAYMENT)) {
            
            conn.setAutoCommit(false);
            try {
                stmt.setString(1, status);
                stmt.setString(2, transactionId);
                stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setInt(4, paymentId);
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Updating payment failed, no rows affected.");
                }
                
                // Get ticket ID for this payment
                Payment payment = getPaymentById(conn, paymentId);
                if (payment != null) {
                    // Update ticket payment status
                    updateTicketPaymentStatus(conn, payment.getTicketId(), status);
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
    
    public static Payment getPaymentByTicket(int ticketId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_PAYMENT_BY_TICKET)) {
            
            stmt.setInt(1, ticketId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return createPaymentFromResultSet(rs);
                }
            }
        }
        return null;
    }
    
    private static Payment getPaymentById(Connection conn, int paymentId) throws SQLException {
        String sql = "SELECT * FROM payments WHERE payment_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, paymentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return createPaymentFromResultSet(rs);
                }
            }
        }
        return null;
    }
    
    private static void updateTicketPaymentStatus(Connection conn, int ticketId, String paymentStatus) throws SQLException {
        String sql = "UPDATE tickets SET payment_status = ?, status = ? WHERE ticket_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, paymentStatus);
            // If payment is completed, set ticket status to BOOKED
            stmt.setString(2, "COMPLETED".equals(paymentStatus) ? "BOOKED" : "PENDING");
            stmt.setInt(3, ticketId);
            stmt.executeUpdate();
        }
    }
    
    private static Payment createPaymentFromResultSet(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setPaymentId(rs.getInt("payment_id"));
        payment.setTicketId(rs.getInt("ticket_id"));
        payment.setAmount(rs.getDouble("amount"));
        payment.setPaymentStatus(rs.getString("payment_status"));
        payment.setPaymentMethod(rs.getString("payment_method"));
        payment.setTransactionId(rs.getString("transaction_id"));
        Timestamp paymentDate = rs.getTimestamp("payment_date");
        if (paymentDate != null) {
            payment.setPaymentDate(paymentDate.toLocalDateTime());
        }
        return payment;
    }
} 