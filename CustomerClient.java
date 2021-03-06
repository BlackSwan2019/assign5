/*
 CSCI           470 section 1
 TA:            Priyanka Kondapuram
 Partner 1      Ben Lane
 zID:		    z1806979
 Partner 2:     Jinhong Yao
 zID:		    z178500
 Assignment:    5
 Date Due:	    TBD

 Purpose:       To create and run a client and server. The client can request certain tasks of a server
                that then accesses a database. The server queries, adds, deletes, and updates the database.
 */

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * This class is used to create a client interface and handle forms and server requests.
 */
public class CustomerClient extends JFrame implements ActionListener {
    // Labels for the text fields.
    private JLabel nameLabel = new JLabel("Name:");
    private JLabel AddressLabel = new JLabel("Address:");
    private JLabel ssnLabel = new JLabel("SSN:");
    private JLabel zipLabel = new JLabel("Zip Code:");

    // This label shows status of client/database-actions.
    private JLabel statusLabel = new JLabel("Client started");

    // Text fields for entering data.
    private JTextField nameField = new JTextField();
    private JTextField addressField = new JTextField();
    private JTextField ssnField = new JTextField();
    private JTextField zipField = new JTextField();

    // GUI components
    private JButton connectButton = new JButton("Connect");
    private JButton getAllButton = new JButton("Get All");
    private JButton addButton = new JButton("Add");
    private JButton deleteButton = new JButton("Delete");
    private JButton updateButton = new JButton("Update Address");

    //private JPanel topPanel = new JPanel(new GridLayout(3, 1));
    private JPanel topPanel = new JPanel(new BorderLayout());
    private JPanel subPanel1 = new JPanel(new GridLayout(2, 4, 15, 15));
    private JPanel subPanel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
    private JPanel subPanel3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

    private JScrollPane scrollArea = new JScrollPane();     // Scroll area that contains the table.
    private JTable table = new JTable();                    // Customer table that will be populated by Get All task.

    private Socket socket;                                  // Socket to communicate with server.
    private ObjectInputStream in;                           // For incoming data from server.
    private ObjectOutputStream out;                         // For outgoing data to server.

    private ArrayList<String> message = new ArrayList<>();  // Server requests and database results.

    private ArrayList<String> warnings = new ArrayList<>(); // Errors/warnings

    private static final long serialVersionUID = 1L;        // ID for object serialization.

    public static void main(String[] args) {

        EventQueue.invokeLater(() -> {
            CustomerClient client = new CustomerClient();
            client.createAndShowGUI();
        });
    }

    /**
     * Constructor for CustomerClient class.
     */
    private CustomerClient() {
        super("Customer Database");
    }

    /**
     * Creates and displays the client app's user interface.
     */
    private void createAndShowGUI() {
        // Set up GUI
        setSize(new Dimension(1200, 500));

        add(BorderLayout.PAGE_START, topPanel);

        // Set background color for the top 3 sub-panels.
        subPanel1.setBackground(new Color(238, 238, 238));
        subPanel2.setBackground(new Color(238, 238, 238));
        subPanel3.setBackground(new Color(238, 238, 238));

        // Add sub-panels.
        topPanel.add(BorderLayout.PAGE_START, subPanel1);
        topPanel.add(BorderLayout.CENTER, subPanel2);
        topPanel.add(BorderLayout.PAGE_END, subPanel3);

        // Add margins to the top two sub-panels.
        subPanel1.setBorder(BorderFactory.createEmptyBorder(10,  8,  5,  5));
        subPanel2.setBorder(BorderFactory.createEmptyBorder(10,  8,  5,  5));

        // Add labels and text fields to the interface.
        subPanel1.add(nameLabel);
        subPanel1.add(nameField);
        subPanel1.add(ssnLabel);
        subPanel1.add(ssnField);
        subPanel1.add(AddressLabel);
        subPanel1.add(addressField);
        subPanel1.add(zipLabel);
        subPanel1.add(zipField);

        // Add buttons to the interface.
        subPanel2.add(connectButton);
        subPanel2.add(getAllButton);
        subPanel2.add(addButton);
        subPanel2.add(deleteButton);
        subPanel2.add(updateButton);

        // Task buttons will initially be disabled (until successful connection to server/database).
        getAllButton.setEnabled(false);
        addButton.setEnabled(false);
        deleteButton.setEnabled(false);
        updateButton.setEnabled(false);

        // Add action listeners to buttons.
        connectButton.addActionListener(this);
        getAllButton.addActionListener(this);
        addButton.addActionListener(this);
        updateButton.addActionListener(this);
        deleteButton.addActionListener(this);

        // Add status label to the interface.
        subPanel3.add(statusLabel);

        // Set scroll area properties.
        scrollArea.setPreferredSize(new Dimension(this.getWidth(), this.getHeight() / 2));
        scrollArea.getViewport().setBackground(Color.BLACK);

        // This is used just to get column names displayed before any Get All request.
        String[] columnNames = {"Name", "Social Security Number", "Address", "ZIP Code"};
        Object[][] customerTable = new Object[0][0];
        table = new JTable(new DefaultTableModel(customerTable, columnNames));
        table.setFillsViewportHeight(true);
        scrollArea.setViewportView(table);

        add(BorderLayout.CENTER, scrollArea);

        setVisible(true);
    }

    /**
     * Event listener for the client task buttons.
     *
     * @param e     Action event from a button.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Connect")) {
            connect();
        } else if (e.getActionCommand().equals("Disconnect")) {
            disconnect();
        } else if (e.getSource() == getAllButton) {
            handleGetAll();
        } else if (e.getSource() == addButton) {
            handleAdd();
        } else if (e.getSource() == updateButton) {
            handleUpdate();
        } else if (e.getSource() == deleteButton) {
            handleDelete();
        }
    }

    /**
     * Establishes connection with a server as well as making sure the server has access to the database.
     */
    private void connect() {
        try {
            String handshake;           // Checks to see if client can talk to server and database.
            // Replace 97xx with your port number
            //socket = new Socket("turing.cs.niu.edu", 9732);
            // Set socket on local machine to port 9732.
            socket = new Socket("localhost", 9732);

            System.out.println("LOG: Socket opened");

            // Create in/out object streams.
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("LOG: Streams opened");

            // Write to server and see if there is a responding handshake.
            out.writeObject("HANDSHAKE");
            handshake = (String) in.readObject();

            // If server responds with "HANDSHAKE", it means that the client is connected to the server and the server
            // was able to connect to the database.
            if (handshake.equalsIgnoreCase("HANDSHAKE")) {
                // Update status label.
                statusLabel.setText("Connected");
                statusLabel.setForeground(Color.BLACK);

                // Since client is connected, switch "Connect" button to "Disconnect".
                connectButton.setText("Disconnect");

                // Enable buttons
                getAllButton.setEnabled(true);
                addButton.setEnabled(true);
                deleteButton.setEnabled(true);
                updateButton.setEnabled(true);
            } else {    // Else, client handshake with server failed. Display warning.
                statusLabel.setText(handshake);
                statusLabel.setForeground(Color.RED);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Exception resolving host name: " + e);
            statusLabel.setText("Could not connect to server.");
            statusLabel.setForeground(Color.RED);
        }
    }

    /**
     *  Disconnects client from the server.
     */
    private void disconnect() {
        // Since client is disconnected, switch button label to "Connect".
        connectButton.setText("Connect");

        // Disable buttons
        getAllButton.setEnabled(false);
        addButton.setEnabled(false);
        deleteButton.setEnabled(false);
        updateButton.setEnabled(false);

        // Set status.
        statusLabel.setText("Disconnected");
        statusLabel.setForeground(Color.BLACK);

        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Exception closing socket: " + e);
        }
    }

    /**
     * Handles the Get All customers task.
     */
    private void handleGetAll() {
        try {
            // Reset the client-server message object.
            message = new ArrayList<>();

            // Set request type to "GETALL" and add to message. Send request to server.
            message.add("GETALL");
            out.writeObject(message);

            // Read in the returned results for "GETALL" and store in message.
            message = (ArrayList) in.readObject();

            // Get status of query (a string) and store it. Set status label to that string.
            String queryStatus = message.get(message.size() - 1);
            statusLabel.setText(queryStatus);
            statusLabel.setForeground(Color.BLACK);

            // Scrub the request-type and query status from the message object.
            message.remove(0);
            message.remove(message.size() - 1);

            // Set table column names.
            String[] columnNames = {table.getColumnName(0),
                                    table.getColumnName(1),
                                    table.getColumnName(2),
                                    table.getColumnName(3)};

            // Set the number of rows and columns in the table.
            Object[][] customerTable = new Object[message.size() / 4][4];

            int row = 0;    // row counter
            int col = 0;    // column counter

            // Loop through the message, adding its strings to the table.
            for (String s : message) {
                customerTable[row][col] = s;

                col++;

                if (col == 4) {
                    col = 0;
                    row++;
                }
            }

            // Reset table and set its properties.
            table = new JTable(new DefaultTableModel(customerTable, columnNames));
            table.setRowSelectionAllowed(false);
            table.setDefaultEditor(Object.class, null);
            table.setDefaultRenderer(Object.class, new VisitorRenderer());
            table.setFillsViewportHeight(true);

            // Remove scrollArea and update it.
            this.remove(scrollArea);
            scrollArea = new JScrollPane(table);
            scrollArea.setPreferredSize(new Dimension(this.getWidth(), this.getHeight() / 2));
            add(BorderLayout.CENTER, scrollArea);

        } catch(IOException | ClassNotFoundException e) {
            System.out.println("Couldn't read from server");
        }
    }

    /**
     * Handles the Add customer task.
     */
    private void handleAdd() {
        try {
            // Reset client-server message.
            message = new ArrayList<>();

            // Add request type to beginning of message.
            message.add("ADD");

            // Reset warnings list.
            warnings = new ArrayList<>();

            // Set invalid-field flag to false.
            boolean warningFlag = false;

            // Created regex pattern for name field.
            java.util.regex.Pattern namePattern = java.util.regex.Pattern.compile("^(?!\\s)[a-zA-Z\\s]{1,20}$");
            java.util.regex.Matcher nameMatcher = namePattern.matcher(nameField.getText());

            // Compare name field's data to empty or regex pattern. If invalid data, set warning flag to true.
            if (nameField.getText().equalsIgnoreCase("")) {
                warnings.add("No name entered.");
                warningFlag = true;
            }
            else if (!nameMatcher.matches()){
                warnings.add("Improper name entered.");
                warningFlag = true;
            }
            // Created regex pattern for SSN field.
            java.util.regex.Pattern ssnPattern = java.util.regex.Pattern.compile("^(?!000|666|\\s)[0-8][0-9]{2}-(?!00)[0-9]{2}-(?!0000)[0-9]{4}$");
            java.util.regex.Matcher ssnMatcher = ssnPattern.matcher(ssnField.getText());

            // Compare SSN field's data to empty or regex pattern. If invalid data, set warning flag to true.
            if (ssnField.getText().equalsIgnoreCase("")) {
                warnings.add("No SSN entered.");
                warningFlag = true;
            }
            else if (!ssnMatcher.matches()){
                warnings.add("Incorrectly formatted SSN.");
                warningFlag = true;
            }

            // Created regex pattern for address field.
            java.util.regex.Pattern addressPattern = java.util.regex.Pattern.compile("^(?!\\s)[\\w\\s. ]{1,40}$");
            java.util.regex.Matcher addressMatcher = addressPattern.matcher(addressField.getText());

            // Compare address field's data to empty or regex pattern. If invalid data, set warning flag to true.
            if (addressField.getText().equalsIgnoreCase("")) {
                warnings.add("No address entered.");
                warningFlag = true;
            }
            else if (!addressMatcher.matches()) {
                warnings.add("Improper address entered.");
                warningFlag = true;
            }

            // Created regex pattern for ZIP Code field.
            java.util.regex.Pattern zipPattern = java.util.regex.Pattern.compile("^(?!\\s)[0-9]{5}$");
            java.util.regex.Matcher zipMatcher = zipPattern.matcher(zipField.getText());

            // Compare ZIP Code field's data to empty or regex pattern. If invalid data, set warning flag to true.
            if (zipField.getText().equalsIgnoreCase("")) {
                warnings.add("No zip code entered.");
                warningFlag = true;
            }
            else if (!zipMatcher.matches()) {
                warnings.add("Improper zip code entered.");
                warningFlag = true;
            }

            // If there are any incorrectly-filled fields (warningFlag is true), give warnings and do NOT add customer to database.
            if (warningFlag) {
                StringBuilder warning = new StringBuilder();

                // Loop through array list of built-up warnings and add them to StringBuilder.
                for (String s : warnings) {
                    warning.append(s);
                    warning.append(" ");
                }

                // Convert StringBuilder warning to String.
                String warningString = warning.toString();

                // Set status label to display warnings.
                statusLabel.setText(warningString);
                statusLabel.setForeground(Color.RED);

                this.setVisible(true);
            } else {  // Else, add all the field values to the message and send it to server for processing.
                message.add(nameField.getText());
                message.add(ssnField.getText());
                message.add(addressField.getText());
                message.add(zipField.getText());

                out.writeObject(message);

                // Read in last section of returned message to obtain result status.
                message = (ArrayList) in.readObject();
                String queryStatus = message.get(message.size() - 1);

                // If SSN already is in the database, MySQL will naturally reject it. Give warning.
                if ((message.get(message.size() - 1).equalsIgnoreCase("SSN already exists."))) {
                    statusLabel.setText(queryStatus);
                    statusLabel.setForeground(Color.RED);
                }
                // Else, SSN is new, so insert the new customer.
                else {
                    statusLabel.setText(queryStatus);
                    statusLabel.setForeground(Color.BLACK);
                }
            }
        } catch(Exception e) {
            System.out.println("Could not add customer to database: " + e.getMessage());
        }
    }

    /**
     * Handles the Delete customer task.
     */
    private void handleDelete() {
        try {
            // Reset client-server message.
            message = new ArrayList<>();

            // Add request type to beginning of message.
            message.add("DELETE");

            // Reset warnings list.
            warnings = new ArrayList<>();

            // Set invalid-field warning flag to false.
            boolean warningFlag = false;

            // Created regex pattern for SSN field.
            java.util.regex.Pattern ssnPattern = java.util.regex.Pattern.compile("^(?!000|666|\\s)[0-8][0-9]{2}-(?!00)[0-9]{2}-(?!0000)[0-9]{4}$");
            java.util.regex.Matcher ssnMatcher = ssnPattern.matcher(ssnField.getText());

            // Compare SSN field's data to empty or regex pattern. If invalid data, set warning flag to true.
            if (ssnField.getText().equalsIgnoreCase("")) {
                warnings.add("No SSN entered.");
                warningFlag = true;
            }
            else if (!ssnMatcher.matches()){
                warnings.add("Incorrectly formatted SSN.");
                warningFlag = true;
            }

            // If there are any incorrectly-filled fields (warningFlag is true), give warnings and do NOT delete customer from database.
            if (warningFlag) {
                StringBuilder warning = new StringBuilder();

                // Loop through array list of built-up warnings and add them to StringBuilder.
                for (String s : warnings) {
                    warning.append(s);
                    warning.append(" ");
                }

                // Convert StringBuilder warning to String.
                String warningString = warning.toString();

                // Set status label to display warnings.
                statusLabel.setText(warningString);
                statusLabel.setForeground(Color.RED);

                this.setVisible(true);
            } else {    // Else, delete the customer from the table.
                message.add(ssnField.getText());

                out.writeObject(message);

                message = (ArrayList) in.readObject();

                String queryStatus = message.get(message.size() - 1);

                // If SSN already is in the database, MySQL will naturally reject it. Give warning.
                if ((message.get(message.size() - 1).equalsIgnoreCase("No customer with specified SSN. No customer deleted."))) {
                    statusLabel.setText(queryStatus);
                    statusLabel.setForeground(Color.RED);
                } else {    // Else, SSN is new, so insert the new customer.
                    statusLabel.setText(queryStatus);
                    statusLabel.setForeground(Color.BLACK);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    /**
     * Handles the Update customer task.
     */
    private void handleUpdate() {
        try {
            // Reset client-server message.
            message = new ArrayList<>();

            // Add request type to beginning of message.
            message.add("UPDATE");

            warnings = new ArrayList<>();

            boolean warningFlag = false;

            // Created regex pattern for SSN field.
            java.util.regex.Pattern ssnPattern = java.util.regex.Pattern.compile("^(?!000|666|\\s)[0-8][0-9]{2}-(?!00)[0-9]{2}-(?!0000)[0-9]{4}$");
            java.util.regex.Matcher ssnMatcher = ssnPattern.matcher(ssnField.getText());

            // Compare SSN field's data to empty or regex pattern. If invalid data, set warning flag to true.
            if (ssnField.getText().equalsIgnoreCase("")) {
                warnings.add("No SSN entered.");
                warningFlag = true;
            }
            else if (!ssnMatcher.matches()){
                warnings.add("Incorrectly formatted SSN.");
                warningFlag = true;
            }

            // Created regex pattern for address field.
            java.util.regex.Pattern addressPattern = java.util.regex.Pattern.compile("^(?!\\s)[\\w\\s. ]{1,40}$");
            java.util.regex.Matcher addressMatcher = addressPattern.matcher(addressField.getText());

            // Compare address field's data to empty or regex pattern. If invalid data, set warning flag to true.
            if (addressField.getText().equalsIgnoreCase("")) {
                warnings.add("No address entered.");
                warningFlag = true;
            }
            else if (!addressMatcher.matches()) {
                warnings.add("Improper address entered.");
                warningFlag = true;
            }

            // If there are any incorrectly-filled fields (warningFlag is true), give warnings and do NOT delete customer from database.
            if (warningFlag) {
                StringBuilder warning = new StringBuilder();

                // Loop through array list of built-up warnings and add them to StringBuilder.
                for (String s : warnings) {
                    warning.append(s);
                    warning.append(" ");
                }

                // Convert StringBuilder warning to String.
                String warningString = warning.toString();

                // Set status label to display warnings.
                statusLabel.setText(warningString);
                statusLabel.setForeground(Color.RED);

                this.setVisible(true);
            } else {
                message.add(ssnField.getText());
                message.add(addressField.getText());

                out.writeObject(message);

                message = (ArrayList) in.readObject();

                String queryStatus = message.get(message.size() - 1);

                // If SSN already is in the database, MySQL will naturally reject it. Give warning.
                if ((message.get(message.size() - 1).equalsIgnoreCase("No customer with specified SSN. No customer updated."))) {
                    statusLabel.setText(queryStatus);
                    statusLabel.setForeground(Color.RED);
                }
                // Else, SSN is new, so insert the new customer.
                else {
                    statusLabel.setText(queryStatus);
                    statusLabel.setForeground(Color.BLACK);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    /**
     * This inner-class is used to override the table's cell renderer so that cells have no border when selected.
     */
    public class VisitorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(noFocusBorder);
            return this;
        }
    }
}