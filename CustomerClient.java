import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.swing.*;

public class CustomerClient extends JFrame implements ActionListener {
    private JLabel nameLabel = new JLabel("Name:");
    private JLabel AddressLabel = new JLabel("Address:");
    private JLabel ssnLabel = new JLabel("SSN:");
    private JLabel zipLabel = new JLabel("Zip Code:");
    private JLabel statusLabel = new JLabel("Client started");
    private JLabel errors = new JLabel();

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

    private JTextArea outputArea = new JTextArea();
    private JScrollPane scrollArea = new JScrollPane();

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private ArrayList<String> message = new ArrayList<>();

    private ArrayList<String> warnings = new ArrayList<>();

    private static final long serialVersionUID = 1L;

    public static void main(String[] args) {

        EventQueue.invokeLater(() -> {
            CustomerClient client = new CustomerClient();
            client.createAndShowGUI();
        });
    }

    private CustomerClient() {
        super("Customer Database");
    }

    private void createAndShowGUI() {
        // Set up GUI
        setSize(new Dimension(1200, 400));

        add(BorderLayout.CENTER, topPanel);

        subPanel1.setBackground(new Color(238, 238, 238));
        subPanel2.setBackground(new Color(238, 238, 238));
        subPanel3.setBackground(new Color(238, 238, 238));

        topPanel.add(BorderLayout.PAGE_START, subPanel1);
        topPanel.add(BorderLayout.CENTER, subPanel2);
        topPanel.add(BorderLayout.PAGE_END, subPanel3);

        subPanel1.setBorder(BorderFactory.createEmptyBorder(10,  8,  5,  5));
        subPanel2.setBorder(BorderFactory.createEmptyBorder(10,  8,  5,  5));

        subPanel1.add(nameLabel);
        subPanel1.add(nameField);
        subPanel1.add(ssnLabel);
        subPanel1.add(ssnField);
        subPanel1.add(AddressLabel);
        subPanel1.add(addressField);
        subPanel1.add(zipLabel);
        subPanel1.add(zipField);

        subPanel2.add(connectButton);
        subPanel2.add(getAllButton);
        subPanel2.add(addButton);
        subPanel2.add(deleteButton);
        subPanel2.add(updateButton);

        getAllButton.setEnabled(false);
        addButton.setEnabled(false);
        deleteButton.setEnabled(false);
        updateButton.setEnabled(false);

        connectButton.addActionListener(this);
        getAllButton.addActionListener(this);
        addButton.addActionListener(this);
        updateButton.addActionListener(this);
        deleteButton.addActionListener(this);

        subPanel3.add(statusLabel);
        subPanel3.add(errors);

        //add(BorderLayout.CENTER, topPanel);
        scrollArea = new JScrollPane(outputArea);
        scrollArea.setPreferredSize(new Dimension(this.getWidth(), this.getHeight() / 2));
        add(BorderLayout.PAGE_END, scrollArea);

        setVisible(true);
    }

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

    private void connect() {
        try {
            // Replace 97xx with your port number
            //socket = new Socket("turing.cs.niu.edu", 9732);
            socket = new Socket("localhost", 9732);

            System.out.println("LOG: Socket opened");

            // Create in/out object streams.
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("LOG: Streams opened");

            connectButton.setText("Disconnect");

            // Update status label.
            statusLabel.setText("Connected");

            // Enable buttons
            getAllButton.setEnabled(true);
            addButton.setEnabled(true);
            deleteButton.setEnabled(true);
            updateButton.setEnabled(true);

        } catch (UnknownHostException e) {
            System.err.println("Exception resolving host name: " + e);
        } catch (IOException e) {
            System.err.println("Exception establishing socket connection: " + e);
        }
    }

    private void disconnect() {
        connectButton.setText("Connect");

        // Disable buttons
        getAllButton.setEnabled(false);
        addButton.setEnabled(false);
        deleteButton.setEnabled(false);
        updateButton.setEnabled(false);

        statusLabel.setText("Disconnected");

        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Exception closing socket: " + e);
        }
    }

    private void handleGetAll() {
        try {
            message = new ArrayList<>();

            message.add("GETALL");

            out.writeObject(message);

            message = (ArrayList) in.readObject();

            String queryStatus = message.get(message.size() - 1);

            statusLabel.setText(queryStatus);

            message.remove(0);
            message.remove(message.size() - 1);

            int i = 0;

            for (String s : message)
                if (i < 3) {
                    outputArea.append(s + "; ");
                    i++;
                }
                else {
                    outputArea.append(s + "; \n");
                    i = 0;
                }
        } catch(IOException | ClassNotFoundException e) {
            System.out.println("Couldn't read from server");
        }
    }

    private void handleAdd() {
        try {
            String addSSN;
            String addName;
            String addAddress;
            String addZip;

            // Re-instantiate client-server message.
            message = new ArrayList<>();

            // Add request type to beginning of message.
            message.add("ADD");

            warnings = new ArrayList<>();

            boolean warningFlag = false;

            java.util.regex.Pattern namePattern = java.util.regex.Pattern.compile("^(?!\\s)[a-zA-Z\\s]{1,20}$");
            java.util.regex.Matcher nameMatcher = namePattern.matcher(nameField.getText());

            if (nameField.getText().equalsIgnoreCase("")) {
                warnings.add("No name entered.");
                warningFlag = true;
            }
            else if (!nameMatcher.matches()){
                warnings.add("Improper name entered.");
                warningFlag = true;
            }

            java.util.regex.Pattern ssnPattern = java.util.regex.Pattern.compile("^(?!000|666|\\s)[0-8][0-9]{2}-(?!00)[0-9]{2}-(?!0000)[0-9]{4}$");
            java.util.regex.Matcher ssnMatcher = ssnPattern.matcher(ssnField.getText());

            if (ssnField.getText().equalsIgnoreCase("")) {
                warnings.add("No SSN entered.");
                warningFlag = true;
            }
            else if (!ssnMatcher.matches()){
                warnings.add("Incorrectly formatted SSN.");
                warningFlag = true;
            }

            java.util.regex.Pattern addressPattern = java.util.regex.Pattern.compile("^(?!\\s)[\\w\\s. ]{1,40}$");
            java.util.regex.Matcher addressMatcher = addressPattern.matcher(addressField.getText());

            if (addressField.getText().equalsIgnoreCase("")) {
                warnings.add("No address entered.");
                warningFlag = true;
            }
            else if (!addressMatcher.matches()) {
                warnings.add("Improper address entered.");
                warningFlag = true;
            }

            java.util.regex.Pattern zipPattern = java.util.regex.Pattern.compile("^(?!\\s)[0-9]{5}$");
            java.util.regex.Matcher zipMatcher = zipPattern.matcher(zipField.getText());

            if (zipField.getText().equalsIgnoreCase("")) {
                warnings.add("No zip code entered.");
                warningFlag = true;
            }
            else if (!zipMatcher.matches()) {
                warnings.add("Improper zip code entered.");
                warningFlag = true;
            }

            // If there are any incorrectly-filled fields, give warnings and do NOT add customer to database.
            if (warningFlag) {
                StringBuilder warning = new StringBuilder();

                for (String s : warnings) {
                    warning.append(s);
                    warning.append(" ");
                }

                String warningString = warning.toString();

                statusLabel.setText(warningString);

                statusLabel.setForeground(Color.RED);

                this.setVisible(true);
            }
            // Else, add all the field values to the message and send it to server for processing.
            else {
                addName  = nameField.getText();
                addSSN = ssnField.getText();
                addAddress = addressField.getText();
                addZip = zipField.getText();

                message.add(addName);
                message.add(addSSN);
                message.add(addAddress);
                message.add(addZip);

                out.writeObject(message);

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

    private void handleDelete() {
        try {
            // Re-instantiate client-server message.
            message = new ArrayList<>();

            // Add request type to beginning of message.
            message.add("DELETE");

            warnings = new ArrayList<>();

            boolean warningFlag = false;

            java.util.regex.Pattern ssnPattern = java.util.regex.Pattern.compile("^(?!000|666|\\s)[0-8][0-9]{2}-(?!00)[0-9]{2}-(?!0000)[0-9]{4}$");
            java.util.regex.Matcher ssnMatcher = ssnPattern.matcher(ssnField.getText());

            if (ssnField.getText().equalsIgnoreCase("")) {
                warnings.add("No SSN entered.");
                warningFlag = true;
            }
            else if (!ssnMatcher.matches()){
                warnings.add("Incorrectly formatted SSN.");
                warningFlag = true;
            }

            if (warningFlag) {
                StringBuilder warning = new StringBuilder();

                for (String s : warnings) {
                    warning.append(s);
                    warning.append(" ");
                }

                String warningString = warning.toString();

                statusLabel.setText(warningString);

                statusLabel.setForeground(Color.RED);

                this.setVisible(true);
            } else {
                message.add(ssnField.getText());

                out.writeObject(message);

                message = (ArrayList) in.readObject();

                String queryStatus = message.get(message.size() - 1);

                // If SSN already is in the database, MySQL will naturally reject it. Give warning.
                if ((message.get(message.size() - 1).equalsIgnoreCase("No customer with specified SSN. No customer deleted."))) {
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

    private void handleUpdate() {
        try {
            // Re-instantiate client-server message.
            message = new ArrayList<>();

            // Add request type to beginning of message.
            message.add("UPDATE");

            warnings = new ArrayList<>();

            boolean warningFlag = false;

            java.util.regex.Pattern ssnPattern = java.util.regex.Pattern.compile("^(?!000|666|\\s)[0-8][0-9]{2}-(?!00)[0-9]{2}-(?!0000)[0-9]{4}$");
            java.util.regex.Matcher ssnMatcher = ssnPattern.matcher(ssnField.getText());

            if (ssnField.getText().equalsIgnoreCase("")) {
                warnings.add("No SSN entered.");
                warningFlag = true;
            }
            else if (!ssnMatcher.matches()){
                warnings.add("Incorrectly formatted SSN.");
                warningFlag = true;
            }

            java.util.regex.Pattern addressPattern = java.util.regex.Pattern.compile("^(?!\\s)[\\w\\s. ]{1,40}$");
            java.util.regex.Matcher addressMatcher = addressPattern.matcher(addressField.getText());

            if (addressField.getText().equalsIgnoreCase("")) {
                warnings.add("No address entered.");
                warningFlag = true;
            }
            else if (!addressMatcher.matches()) {
                warnings.add("Improper address entered.");
                warningFlag = true;
            }

            if (warningFlag) {
                StringBuilder warning = new StringBuilder();

                for (String s : warnings) {
                    warning.append(s);
                    warning.append(" ");
                }

                String warningString = warning.toString();

                System.out.println(warning.length());

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
                if ((message.get(message.size() - 1).equalsIgnoreCase("No such customer with specified SSN. No customer updated."))) {
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
}