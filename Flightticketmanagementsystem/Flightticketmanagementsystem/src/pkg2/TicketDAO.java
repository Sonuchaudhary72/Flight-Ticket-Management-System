package pkg2;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TicketDAO {
    private static final String INSERT_TICKET = "INSERT INTO tickets (user_id, flight_id, full_name, flight_number, seat_number, status, payment_status, price, booking_datetime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_TICKET = "UPDATE tickets SET user_id = ?, flight_id = ?, full_name = ?, flight_number = ?, seat_number = ?, status = ?, payment_status = ?, price = ?, booking_datetime = ? WHERE ticket_id = ?";
    private static final String DELETE_TICKET = "DELETE FROM tickets WHERE ticket_id = ?";
    private static final String SELECT_ALL_TICKETS = "SELECT t.*, p.payment_status as payment_status, p.payment_method, p.transaction_id, p.payment_date FROM tickets t LEFT JOIN payments p ON t.ticket_id = p.ticket_id ORDER BY t.booking_datetime DESC";
    private static final String SELECT_TICKET_BY_ID = "SELECT t.*, p.payment_status as payment_status, p.payment_method, p.transaction_id, p.payment_date FROM tickets t LEFT JOIN payments p ON t.ticket_id = p.ticket_id WHERE t.ticket_id = ?";
    private static final String SELECT_TICKETS_BY_USER = "SELECT t.*, p.payment_status as payment_status, p.payment_method, p.transaction_id, p.payment_date FROM tickets t LEFT JOIN payments p ON t.ticket_id = p.ticket_id WHERE t.user_id = ? ORDER BY t.booking_datetime DESC";
    private static final String SELECT_TICKETS_BY_FLIGHT = "SELECT t.*, p.payment_status as payment_status, p.payment_method, p.transaction_id, p.payment_date FROM tickets t LEFT JOIN payments p ON t.ticket_id = p.ticket_id WHERE t.flight_id = ? ORDER BY t.booking_datetime DESC";
    private static final String UPDATE_TICKET_STATUS = "UPDATE tickets SET status = ? WHERE ticket_id = ? AND payment_status = 'COMPLETED'";
    
    public static List<Ticket> getAllTickets() throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_TICKETS)) {
            
            while (rs.next()) {
                tickets.add(createTicketFromResultSet(rs));
            }
        }
        
        return tickets;
    }
    
    public static void addTicket(Ticket ticket) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_TICKET, Statement.RETURN_GENERATED_KEYS)) {
            
            // Start transaction
            conn.setAutoCommit(false);
            try {
                // First decrement available seats
                FlightDAO.decrementAvailableSeats(ticket.getFlightId());
                
                // Then insert the ticket
                setTicketParameters(stmt, ticket);
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating ticket failed, no rows affected.");
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        ticket.setTicketId(generatedKeys.getInt(1));
                        
                        // Create payment record
                        Payment payment = new Payment(ticket.getTicketId(), ticket.getPrice(), ticket.getPaymentMethod());
                        PaymentDAO.createPayment(payment);
                    } else {
                        throw new SQLException("Creating ticket failed, no ID obtained.");
                    }
                }
                
                // If everything is successful, commit the transaction
                conn.commit();
            } catch (SQLException e) {
                // If there's an error, rollback the transaction
                conn.rollback();
                throw e;
            } finally {
                // Reset auto-commit to true
                conn.setAutoCommit(true);
            }
        }
    }
    
    public static void updateTicket(Ticket ticket) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_TICKET)) {
            
            setTicketParameters(stmt, ticket);
            stmt.setInt(9, ticket.getTicketId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating ticket failed, no rows affected.");
            }
        }
    }
    
    public static void deleteTicket(int ticketId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_TICKET)) {
            
            stmt.setInt(1, ticketId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting ticket failed, no rows affected.");
            }
        }
    }
    
    public static Ticket getTicketById(int ticketId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_TICKET_BY_ID)) {
            
            stmt.setInt(1, ticketId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return createTicketFromResultSet(rs);
                }
            }
        }
        return null;
    }
    
    public static List<Ticket> getTicketsByUser(int userId) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_TICKETS_BY_USER)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tickets.add(createTicketFromResultSet(rs));
                }
            }
        }
        
        return tickets;
    }
    
    public static List<Ticket> getTicketsByFlight(int flightId) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_TICKETS_BY_FLIGHT)) {
            
            stmt.setInt(1, flightId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tickets.add(createTicketFromResultSet(rs));
                }
            }
        }
        
        return tickets;
    }
    
    public static void checkInTicket(int ticketId) throws SQLException {
        // Only allow check-in if payment is completed
        updateTicketStatus(ticketId, "CHECKED_IN");
    }
    
    public static void cancelTicket(int ticketId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update ticket status
                updateTicketStatus(ticketId, "CANCELLED");
                
                // Get the flight ID for this ticket
                Ticket ticket = getTicketById(ticketId);
                if (ticket != null) {
                    // Increment available seats
                    FlightDAO.incrementAvailableSeats(ticket.getFlightId());
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
    
    private static void updateTicketStatus(int ticketId, String status) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_TICKET_STATUS)) {
            
            stmt.setString(1, status);
            stmt.setInt(2, ticketId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating ticket status failed. Either ticket not found or payment not completed.");
            }
        }
    }
    
    private static void setTicketParameters(PreparedStatement stmt, Ticket ticket) throws SQLException {
        stmt.setInt(1, ticket.getUserId());
        stmt.setInt(2, ticket.getFlightId());
        stmt.setString(3, ticket.getFullName());
        stmt.setString(4, ticket.getFlightNumber());
        stmt.setString(5, ticket.getSeatNumber());
        stmt.setString(6, ticket.getStatus());
        stmt.setString(7, ticket.getPaymentStatus());
        stmt.setDouble(8, ticket.getPrice());
        stmt.setTimestamp(9, Timestamp.valueOf(ticket.getBookingDateTime()));
    }
    
    private static Ticket createTicketFromResultSet(ResultSet rs) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setTicketId(rs.getInt("ticket_id"));
        ticket.setUserId(rs.getInt("user_id"));
        ticket.setFlightId(rs.getInt("flight_id"));
        ticket.setFullName(rs.getString("full_name"));
        ticket.setFlightNumber(rs.getString("flight_number"));
        ticket.setSeatNumber(rs.getString("seat_number"));
        ticket.setStatus(rs.getString("status"));
        ticket.setPaymentStatus(rs.getString("payment_status"));
        ticket.setPrice(rs.getDouble("price"));
        ticket.setBookingDateTime(rs.getTimestamp("booking_datetime").toLocalDateTime());
        
        // Add payment information if available
        String paymentMethod = rs.getString("payment_method");
        if (paymentMethod != null) {
            ticket.setPaymentMethod(paymentMethod);
            ticket.setTransactionId(rs.getString("transaction_id"));
            Timestamp paymentDate = rs.getTimestamp("payment_date");
            if (paymentDate != null) {
                ticket.setPaymentDate(paymentDate.toLocalDateTime());
            }
        }
        
        return ticket;
    }
} 