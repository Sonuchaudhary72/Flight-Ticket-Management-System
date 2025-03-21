package pkg1;

import pkg2.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;
import java.sql.SQLException;
import javafx.scene.effect.DropShadow;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserDashboard extends Application {
    private User currentUser;
    private TableView<Flight> flightTable;
    private TableView<Ticket> bookingTable;
    
    public UserDashboard(User user) {
        this.currentUser = user;
    }
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Flight Management System - User Dashboard");
        
        // Create main container with modern design
        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: #f8f9fa;");
        
        // Create header
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #4361ee; -fx-padding: 20;");
        header.setPrefHeight(80);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label welcomeLabel = new Label("Welcome, " + currentUser.getFullName());
        welcomeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        welcomeLabel.setTextFill(Color.WHITE);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button logoutBtn = createStyledButton("Logout", "logout");
        logoutBtn.setOnAction(e -> {
            try {
                Stage mainStage = new Stage();
                MainPage mainPage = new MainPage();
                mainPage.start(mainStage);
                primaryStage.close();
            } catch (Exception ex) {
                showError("Error during logout: " + ex.getMessage());
            }
        });
        
        header.getChildren().addAll(welcomeLabel, spacer, logoutBtn);
        
        // Create tabs with modern styling
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: transparent;");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        Tab searchTab = createSearchTab();
        Tab bookingsTab = createBookingsTab();
        Tab profileTab = createProfileTab();
        
        tabPane.getTabs().addAll(searchTab, bookingsTab, profileTab);
        
        // Style the tabs
        tabPane.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-tab-min-height: 50px; " +
            "-fx-tab-max-height: 50px;"
        );
        
        // Add components to main container
        mainContainer.getChildren().addAll(header, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        
        // Set up the scene
        Scene scene = new Scene(mainContainer, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Load initial data
        loadMyBookings();
    }
    
    private Tab createSearchTab() {
        Tab searchTab = new Tab("Search Flights");
        VBox searchBox = createStyledBox();
        
        // Search form
        GridPane searchForm = new GridPane();
        searchForm.setHgap(10);
        searchForm.setVgap(10);
        searchForm.setAlignment(Pos.CENTER);
        
        TextField originField = new TextField();
        originField.setPromptText("From");
        TextField destinationField = new TextField();
        destinationField.setPromptText("To");
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Departure Date");
        
        Button searchBtn = createStyledButton("Search", "search");
        searchBtn.setOnAction(e -> handleSearch(
            originField.getText().trim(),
            destinationField.getText().trim(),
            datePicker.getValue()
        ));
        
        searchForm.add(new Label("From:"), 0, 0);
        searchForm.add(originField, 1, 0);
        searchForm.add(new Label("To:"), 2, 0);
        searchForm.add(destinationField, 3, 0);
        searchForm.add(new Label("Date:"), 4, 0);
        searchForm.add(datePicker, 5, 0);
        searchForm.add(searchBtn, 6, 0);
        
        // Flight table
        flightTable = new TableView<>();
        setupFlightTable();
        
        Button bookBtn = createStyledButton("Book Selected Flight", "book");
        bookBtn.setOnAction(e -> handleBookFlight());
        
        searchBox.getChildren().addAll(
            createSectionTitle("Search Flights"),
            searchForm,
            flightTable,
            bookBtn
        );
        
        searchTab.setContent(searchBox);
        return searchTab;
    }
    
    private Tab createBookingsTab() {
        Tab bookingsTab = new Tab("My Bookings");
        VBox bookingsBox = createStyledBox();
        
        // Bookings table
        bookingTable = new TableView<>();
        setupBookingTable();
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button viewBtn = createStyledButton("View Details", "view");
        viewBtn.setOnAction(e -> handleViewTicket());
        
        Button cancelBtn = createStyledButton("Cancel Booking", "cancel");
        cancelBtn.setOnAction(e -> handleCancelBooking());
        
        buttonBox.getChildren().addAll(viewBtn, cancelBtn);
        
        bookingsBox.getChildren().addAll(
            createSectionTitle("My Bookings"),
            bookingTable,
            buttonBox
        );
        
        bookingsTab.setContent(bookingsBox);
        return bookingsTab;
    }
    
    private Tab createProfileTab() {
        Tab profileTab = new Tab("Profile");
        VBox profileBox = createStyledBox();
        
        GridPane profileForm = new GridPane();
        profileForm.setHgap(15);
        profileForm.setVgap(10);
        profileForm.setAlignment(Pos.CENTER);
        
        TextField emailField = new TextField(currentUser.getEmail());
        TextField fullNameField = new TextField(currentUser.getFullName());
        TextField phoneField = new TextField(currentUser.getPhone());
        TextField addressField = new TextField(currentUser.getAddress());
        
        emailField.setEditable(false);
        
        profileForm.add(new Label("Email:"), 0, 0);
        profileForm.add(emailField, 1, 0);
        profileForm.add(new Label("Full Name:"), 0, 1);
        profileForm.add(fullNameField, 1, 1);
        profileForm.add(new Label("Phone:"), 0, 2);
        profileForm.add(phoneField, 1, 2);
        profileForm.add(new Label("Address:"), 0, 3);
        profileForm.add(addressField, 1, 3);
        
        Button updateProfileBtn = createStyledButton("Update Profile", "edit");
        updateProfileBtn.setOnAction(e -> handleUpdateProfile(
            fullNameField.getText(),
            phoneField.getText(),
            addressField.getText()
        ));
        
        profileBox.getChildren().addAll(
            createSectionTitle("Personal Information"),
            profileForm,
            updateProfileBtn
        );
        
        profileTab.setContent(profileBox);
        return profileTab;
    }
    
    private void setupFlightTable() {
        // Set minimum height for the table
        flightTable.setMinHeight(300);
        
        TableColumn<Flight, String> flightNumberCol = new TableColumn<>("Flight #");
        flightNumberCol.setCellValueFactory(new PropertyValueFactory<>("flightNumber"));
        flightNumberCol.setPrefWidth(100);
        
        TableColumn<Flight, String> originCol = new TableColumn<>("From");
        originCol.setCellValueFactory(new PropertyValueFactory<>("origin"));
        originCol.setPrefWidth(120);
        
        TableColumn<Flight, String> destinationCol = new TableColumn<>("To");
        destinationCol.setCellValueFactory(new PropertyValueFactory<>("destination"));
        destinationCol.setPrefWidth(120);
        
        TableColumn<Flight, LocalDateTime> departureCol = new TableColumn<>("Departure");
        departureCol.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
        departureCol.setPrefWidth(150);
        departureCol.setCellFactory(column -> new TableCell<Flight, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    try {
                        // Format the date and add day of week
                        String formattedDate = formatter.format(item);
                        String dayOfWeek = item.getDayOfWeek().toString().substring(0, 3);
                        setText(dayOfWeek + ", " + formattedDate);
                        
                        // Style based on whether flight is in past or future
                        LocalDateTime now = LocalDateTime.now();
                        if (item.isBefore(now)) {
                            setStyle("-fx-text-fill: #666666;"); // Past flights in gray
                        } else if (item.isBefore(now.plusHours(24))) {
                            setStyle("-fx-text-fill: #e67e22;"); // Flights within 24 hours in orange
                        } else {
                            setStyle("-fx-text-fill: #2ecc71;"); // Future flights in green
                        }
                    } catch (Exception e) {
                        System.out.println("Error formatting departure date: " + e.getMessage());
                        e.printStackTrace();
                        setText("Invalid Date");
                        setStyle("-fx-text-fill: red;");
                    }
                }
            }
        });
        
        TableColumn<Flight, LocalDateTime> arrivalCol = new TableColumn<>("Arrival");
        arrivalCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        arrivalCol.setPrefWidth(150);
        arrivalCol.setCellFactory(column -> new TableCell<Flight, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    try {
                        // Format the date and add day of week
                        String formattedDate = formatter.format(item);
                        String dayOfWeek = item.getDayOfWeek().toString().substring(0, 3);
                        setText(dayOfWeek + ", " + formattedDate);
                    } catch (Exception e) {
                        System.out.println("Error formatting arrival date: " + e.getMessage());
                        e.printStackTrace();
                        setText("Invalid Date");
                    }
                }
            }
        });
        
        TableColumn<Flight, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(100);
        priceCol.setCellFactory(column -> new TableCell<Flight, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", price));
                }
            }
        });
        
        TableColumn<Flight, Integer> availableSeatsCol = new TableColumn<>("Available Seats");
        availableSeatsCol.setCellValueFactory(new PropertyValueFactory<>("availableSeats"));
        availableSeatsCol.setPrefWidth(120);
        availableSeatsCol.setCellFactory(column -> new TableCell<Flight, Integer>() {
            @Override
            protected void updateItem(Integer seats, boolean empty) {
                super.updateItem(seats, empty);
                if (empty || seats == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(seats));
                    if (seats == 0) {
                        setStyle("-fx-text-fill: red;");
                    } else if (seats < 10) {
                        setStyle("-fx-text-fill: orange;");
                    } else {
                        setStyle("-fx-text-fill: green;");
                    }
                }
            }
        });
        
        TableColumn<Flight, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(column -> new TableCell<Flight, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(status);
                    switch (status.toUpperCase()) {
                        case "SCHEDULED":
                            setStyle("-fx-text-fill: green;");
                            break;
                        case "DELAYED":
                            setStyle("-fx-text-fill: orange;");
                            break;
                        case "CANCELLED":
                            setStyle("-fx-text-fill: red;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        
        // Clear existing columns if any
        flightTable.getColumns().clear();
        
        // Add all columns
        flightTable.getColumns().addAll(
            flightNumberCol, originCol, destinationCol, departureCol, arrivalCol, priceCol, availableSeatsCol, statusCol
        );
        
        // Set selection mode
        flightTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        // Set resize policy
        flightTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Add table style
        flightTable.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5;");
        
        // Load initial data
        try {
            List<Flight> flights = FlightDAO.getAllFlights();
            flightTable.getItems().clear();
            flightTable.getItems().addAll(flights);
            System.out.println("Loaded " + flights.size() + " flights initially");
        } catch (SQLException e) {
            System.out.println("Error loading initial flight data: " + e.getMessage());
            e.printStackTrace();
            showError("Error loading flights: " + e.getMessage());
        }
    }
    
    private void setupBookingTable() {
        TableColumn<Ticket, Integer> ticketIdCol = new TableColumn<>("Ticket #");
        ticketIdCol.setCellValueFactory(new PropertyValueFactory<>("ticketId"));
        
        TableColumn<Ticket, String> flightNumberCol = new TableColumn<>("Flight #");
        flightNumberCol.setCellValueFactory(new PropertyValueFactory<>("flightNumber"));
        
        TableColumn<Ticket, LocalDateTime> bookingTimeCol = new TableColumn<>("Booking Date");
        bookingTimeCol.setCellValueFactory(new PropertyValueFactory<>("bookingDateTime"));
        bookingTimeCol.setCellFactory(column -> new TableCell<Ticket, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatter.format(item));
            }
        });
        
        TableColumn<Ticket, String> seatCol = new TableColumn<>("Seat");
        seatCol.setCellValueFactory(new PropertyValueFactory<>("seatNumber"));
        
        TableColumn<Ticket, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        bookingTable.getColumns().addAll(
            ticketIdCol, flightNumberCol, bookingTimeCol, seatCol, statusCol
        );
        bookingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Add row styling
        bookingTable.setRowFactory(tv -> new TableRow<Ticket>() {
            @Override
            protected void updateItem(Ticket ticket, boolean empty) {
                super.updateItem(ticket, empty);
                if (empty || ticket == null) {
                    setStyle("");
                } else {
                    switch (ticket.getStatus()) {
                        case "CHECKED_IN":
                            setStyle("-fx-background-color: #e8f5e9;");
                            break;
                        case "CANCELLED":
                            setStyle("-fx-background-color: #ffebee;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
    }
    
    private void handleBookFlight() {
        Flight selectedFlight = flightTable.getSelectionModel().getSelectedItem();
        if (selectedFlight == null) {
            showError("Please select a flight to book");
            return;
        }
        
        if (selectedFlight.getAvailableSeats() <= 0) {
            showError("Sorry, this flight is fully booked");
            return;
        }
        
        // Create payment dialog
        Dialog<String> paymentDialog = new Dialog<>();
        paymentDialog.setTitle("Payment Information");
        paymentDialog.setHeaderText("Complete payment to book your ticket");
        
        // Create the payment form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Payment method selection
        ComboBox<String> paymentMethodCombo = new ComboBox<>();
        paymentMethodCombo.getItems().addAll("CREDIT_CARD", "DEBIT_CARD", "NET_BANKING");
        paymentMethodCombo.setValue("CREDIT_CARD");
        
        TextField cardNumberField = new TextField();
        cardNumberField.setPromptText("Card Number");
        
        TextField cvvField = new TextField();
        cvvField.setPromptText("CVV");
        cvvField.setPrefWidth(70);
        
        DatePicker expiryDatePicker = new DatePicker();
        expiryDatePicker.setPromptText("Expiry Date");
        
        // Add fields to grid
        grid.add(new Label("Payment Method:"), 0, 0);
        grid.add(paymentMethodCombo, 1, 0);
        grid.add(new Label("Card Number:"), 0, 1);
        grid.add(cardNumberField, 1, 1);
        grid.add(new Label("CVV:"), 0, 2);
        grid.add(cvvField, 1, 2);
        grid.add(new Label("Expiry Date:"), 0, 3);
        grid.add(expiryDatePicker, 1, 3);
        
        // Add price information
        Label priceLabel = new Label(String.format("Total Amount: $%.2f", selectedFlight.getPrice()));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        grid.add(priceLabel, 0, 4, 2, 1);
        
        paymentDialog.getDialogPane().setContent(grid);
        
        ButtonType payButton = new ButtonType("Pay Now", ButtonBar.ButtonData.OK_DONE);
        paymentDialog.getDialogPane().getButtonTypes().addAll(payButton, ButtonType.CANCEL);
        
        paymentDialog.setResultConverter(dialogButton -> {
            if (dialogButton == payButton) {
                if (cardNumberField.getText().isEmpty() || cvvField.getText().isEmpty() || expiryDatePicker.getValue() == null) {
                    showError("Please fill in all payment details");
                    return null;
                }
                return paymentMethodCombo.getValue();
            }
            return null;
        });
        
        Optional<String> paymentMethod = paymentDialog.showAndWait();
        
        if (paymentMethod.isPresent()) {
            try {
                // Create the ticket
                Ticket ticket = new Ticket();
                ticket.setUserId(currentUser.getUserId());
                ticket.setFlightId(selectedFlight.getFlightId());
                ticket.setFullName(currentUser.getFullName());
                ticket.setFlightNumber(selectedFlight.getFlightNumber());
                ticket.setSeatNumber(generateSeatNumber());
                ticket.setPrice(selectedFlight.getPrice());
                ticket.setBookingDateTime(LocalDateTime.now());
                ticket.setStatus("PENDING");
                ticket.setPaymentMethod(paymentMethod.get());
                
                // Add the ticket
                TicketDAO.addTicket(ticket);
                
                // Create payment record
                Payment payment = new Payment(ticket.getTicketId(), ticket.getPrice(), paymentMethod.get());
                payment.setTransactionId("TXN" + System.currentTimeMillis()); // Generate a transaction ID
                payment.setPaymentStatus("COMPLETED"); // Simulate successful payment
                payment.setPaymentDate(LocalDateTime.now());
                
                // Process payment
                PaymentDAO.createPayment(payment);
                
                showSuccess("Flight booked successfully! Your ticket number is: " + ticket.getTicketId());
                loadMyBookings(); // Refresh bookings table
                
            } catch (SQLException e) {
                showError("Error booking flight: " + e.getMessage());
            }
        }
    }
    
    private String generateSeatNumber() {
        return String.format("%d%c", (int)(Math.random() * 30) + 1, 
                                   (char)('A' + (int)(Math.random() * 6)));
    }
    
    private void handleViewTicket() {
        Ticket selectedTicket = bookingTable.getSelectionModel().getSelectedItem();
        if (selectedTicket == null) {
            showError("Please select a booking to view");
            return;
        }
        
        try {
            Flight flight = FlightDAO.getFlightById(selectedTicket.getFlightId());
            Payment payment = PaymentDAO.getPaymentByTicket(selectedTicket.getTicketId());
            
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Booking Details");
            dialog.setHeaderText("Ticket #" + selectedTicket.getTicketId());
            
            VBox content = new VBox(10);
            content.setPadding(new Insets(20));
            
            // Add booking details
            content.getChildren().addAll(
                new Label("Flight Number: " + selectedTicket.getFlightNumber()),
                new Label("From: " + flight.getOrigin()),
                new Label("To: " + flight.getDestination()),
                new Label("Departure: " + flight.getDepartureTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))),
                new Label("Arrival: " + flight.getArrivalTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))),
                new Label("Seat Number: " + selectedTicket.getSeatNumber()),
                new Label("Price: $" + String.format("%.2f", selectedTicket.getPrice())),
                new Label("Status: " + selectedTicket.getStatus()),
                new Label("Payment Status: " + selectedTicket.getPaymentStatus())
            );
            
            // Add payment details if available
            if (payment != null) {
                content.getChildren().addAll(
                    new Label("Payment Method: " + payment.getPaymentMethod()),
                    new Label("Transaction ID: " + payment.getTransactionId()),
                    new Label("Payment Date: " + (payment.getPaymentDate() != null ? 
                        payment.getPaymentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "Not paid"))
                );
            }
            
            content.getChildren().add(
                new Label("Booking Time: " + selectedTicket.getBookingDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            );
            
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            
            dialog.showAndWait();
        } catch (SQLException e) {
            showError("Error loading ticket details: " + e.getMessage());
        }
    }
    
    private void handleCancelBooking() {
        Ticket selectedTicket = bookingTable.getSelectionModel().getSelectedItem();
        if (selectedTicket == null) {
            showError("Please select a booking to cancel");
            return;
        }
        
        if ("CANCELLED".equals(selectedTicket.getStatus())) {
            showError("This booking is already cancelled");
            return;
        }
        
        if ("CHECKED_IN".equals(selectedTicket.getStatus())) {
            showError("Cannot cancel a checked-in booking");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Booking");
        alert.setHeaderText("Are you sure you want to cancel this booking?");
        alert.setContentText("This action cannot be undone.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                TicketDAO.cancelTicket(selectedTicket.getTicketId());
                loadMyBookings();
                showSuccess("Booking cancelled successfully");
            } catch (SQLException e) {
                showError("Error cancelling booking: " + e.getMessage());
            }
        }
    }
    
    private void handleUpdateProfile(String fullName, String phone, String address) {
        try {
            currentUser.setFullName(fullName);
            currentUser.setPhone(phone);
            currentUser.setAddress(address);
            
            UserDAO.updateUser(currentUser);
            showSuccess("Profile updated successfully!");
        } catch (SQLException e) {
            showError("Error updating profile: " + e.getMessage());
        }
    }
    
    private void loadMyBookings() {
        try {
            List<Ticket> tickets = TicketDAO.getTicketsByUser(currentUser.getUserId());
            bookingTable.getItems().clear();
            bookingTable.getItems().addAll(tickets);
        } catch (SQLException e) {
            showError("Error loading bookings: " + e.getMessage());
        }
    }
    
    private void handleSearch(String origin, String destination, LocalDate date) {
        try {
            System.out.println("Searching for flights with criteria:");
            System.out.println("Origin: " + (origin != null ? origin : "Any"));
            System.out.println("Destination: " + (destination != null ? destination : "Any"));
            System.out.println("Date: " + (date != null ? date : "Any"));
            
            // Clear the table first
            flightTable.getItems().clear();
            
            // Validate inputs
            if ((origin == null || origin.trim().isEmpty()) && 
                (destination == null || destination.trim().isEmpty()) && 
                date == null) {
                showInfo("Please enter at least one search criteria");
                return;
            }
            
            // Convert date to start of day if provided, otherwise null
            LocalDateTime dateTime = null;
            if (date != null) {
                dateTime = date.atStartOfDay();
                System.out.println("Searching for date: " + dateTime);
            }
            
            // Get flights based on search criteria
            List<Flight> flights = FlightDAO.searchFlights(
                origin != null && !origin.trim().isEmpty() ? origin.trim() : null,
                destination != null && !destination.trim().isEmpty() ? destination.trim() : null,
                dateTime
            );
            
            System.out.println("Found " + flights.size() + " flights matching criteria");
            
            // Debug output for first flight if available
            if (!flights.isEmpty()) {
                try {
                    Flight first = flights.get(0);
                    System.out.println("Sample flight details:");
                    System.out.println("  Flight Number: " + first.getFlightNumber());
                    System.out.println("  Route: " + first.getOrigin() + " -> " + first.getDestination());
                    System.out.println("  Departure: " + first.getDepartureTime());
                    System.out.println("  Available Seats: " + first.getAvailableSeats());
                    System.out.println("  Status: " + first.getStatus());
                } catch (Exception e) {
                    System.out.println("Error printing sample flight details: " + e.getMessage());
                }
            }
            
            // Update the table with search results
            flightTable.getItems().addAll(flights);
            
            // Show appropriate message based on results
            if (flights.isEmpty()) {
                showInfo("No flights found matching your search criteria.\nTry different dates or locations.");
            } else {
                String message = String.format("Found %d flight%s matching your criteria", 
                    flights.size(), flights.size() == 1 ? "" : "s");
                if (date != null && date.isBefore(LocalDate.now())) {
                    message += "\nNote: Showing historical flight data";
                }
                showSuccess(message);
                flightTable.refresh();
            }
            
        } catch (SQLException e) {
            System.out.println("Database error during search: " + e.getMessage());
            e.printStackTrace();
            showError("Error searching flights: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error during search: " + e.getMessage());
            e.printStackTrace();
            showError("An unexpected error occurred while searching flights");
        }
    }
    
    private Button createStyledButton(String text, String type) {
        Button button = new Button(text);
        String baseStyle = "-fx-font-weight: bold; -fx-font-size: 14; -fx-cursor: hand;";
        
        switch (type) {
            case "header":
                button.setStyle(baseStyle + "-fx-background-color: transparent; -fx-text-fill: white; -fx-padding: 10 20;");
                break;
            case "search":
                button.setStyle(baseStyle + "-fx-background-color: #4361ee; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
                break;
            case "book":
                button.setStyle(baseStyle + "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
                break;
            case "view":
                button.setStyle(baseStyle + "-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
                break;
            case "cancel":
                button.setStyle(baseStyle + "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
                break;
            case "edit":
                button.setStyle(baseStyle + "-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
                break;
            case "logout":
                button.setStyle(baseStyle + "-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
                break;
            default:
                button.setStyle(baseStyle + "-fx-background-color: #4361ee; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
        }
        
        // Add hover effect
        button.setOnMouseEntered(e -> button.setStyle(button.getStyle() + "-fx-opacity: 0.8;"));
        button.setOnMouseExited(e -> button.setStyle(button.getStyle().replace("-fx-opacity: 0.8;", "")));
        
        return button;
    }
    
    private VBox createStyledBox() {
        VBox box = new VBox(20);
        box.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 10; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);"
        );
        box.setPadding(new Insets(30));
        return box;
    }
    
    private Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        label.setTextFill(Color.rgb(44, 62, 80));
        label.setPadding(new Insets(0, 0, 20, 0));
        return label;
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 