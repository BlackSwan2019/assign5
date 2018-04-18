import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.*;

public class CustomerClient extends JFrame implements ActionListener {
    private JLabel nameLabel = new JLabel("Name:");
    private JLabel AddressLabel = new JLabel("Address:");
    private JLabel ssnLabel = new JLabel("SSN:");
    private JLabel zipLabel = new JLabel("Zip Code:");
    private JLabel statusLabel = new JLabel("Client started");

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
        subPanel1.add(AddressLabel);
        subPanel1.add(addressField);
        subPanel1.add(ssnLabel);
        subPanel1.add(ssnField);
        subPanel1.add(zipLabel);
        subPanel1.add(zipField);

        subPanel2.add(connectButton);
        subPanel2.add(getAllButton);
        subPanel2.add(addButton);
        subPanel2.add(deleteButton);
        subPanel2.add(updateButton);

        connectButton.addActionListener(this);
        getAllButton.addActionListener(this);

        subPanel3.add(statusLabel);

        //add(BorderLayout.CENTER, topPanel);
        scrollArea = new JScrollPane(outputArea);
        scrollArea.setPreferredSize(new Dimension(this.getWidth(), this.getHeight() / 2));
        add(BorderLayout.PAGE_END, scrollArea);



        //outputArea.setBackground(new Color(255, 255, 255));

        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Connect")) {
            connect();
        /*
        } else if (e.getActionCommand().equals("Disconnect")) {
            disconnect();
        */
        } else if (e.getSource() == getAllButton) {
            handleGetAll();
        /*
        } else if (e.getSource() == addButton) {
            handleAdd();
        } else if (e.getSource() == updateButton) {
            handleUpdate();
        } else if (e.getSource() == deleteButton) {
            handleDelete();
        }
        */
        }
    }

    private void connect() {
        try {
            // Replace 97xx with your port number
            //socket = new Socket("turing.cs.niu.edu", 9732);
            socket = new Socket("localhost", 9734);

            System.out.println("LOG: Socket opened");

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("LOG: Streams opened");

            connectButton.setText("Disconnect");

            // Enable buttons

        } catch (UnknownHostException e) {
            System.err.println("Exception resolving host name: " + e);
        } catch (IOException e) {
            System.err.println("Exception establishing socket connection: " + e);
        }
    }

/*
    private void disconnect() {
        connectButton.setText("Connect");

        // Disable buttons

        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Exception closing socket: " + e);
        }
    }
*/
    private void handleGetAll() {
        try {
            out.write(5);
            out.flush();

            System.out.println(in.readInt());
        } catch (IOException e) {
            System.out.println("Write to client error.");
        }
    }
/*
    private void handleAdd() {
    }

    private void handleDelete() {
    }

    private void handleUpdate() {
    }
    */
}