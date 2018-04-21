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
                that then accesses a database. The client interacts with the server to query, add, delete,
                and update customers in a customer database table.
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * This class creates and runs the server and establishes connection with the customer database.
 */
public class CustomerServer extends Thread {
    private ServerSocket listenSocket;      // Socket for listening for new connections.

    public static void main(String args[]) {
        new CustomerServer();
    }

    private CustomerServer() {
        // Listening port
        int port = 9732;
        try {
            listenSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Exception creating server socket: " + e);
            System.exit(1);
        }

        System.out.println("LOG: Server listening on port " + port);
        this.start();
    }

    /**
     * run()
     * The body of the server thread. Loops forever, listening for and
     * accepting connections from clients. For each connection, create a
     * new Conversation object to handle the communication through the
     * new Socket.
     */

    public void run() {
        try {
            while (true) {
                // Accept client and make a socket for them.
                Socket clientSocket = listenSocket.accept();

                System.out.println("LOG: Client connected");

                // Create a Conversation object to handle this client and pass
                // it the Socket to use.  If needed, we could save the Conversation
                // object reference in an ArrayList. In this way we could later iterate
                // through this list looking for "dead" connections and reclaim
                // any resources.
                new Conversation(clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Exception listening for connections: " + e);
        }
    }
}

/**
 * The Conversation class handles all communication with a client.
 */
class Conversation extends Thread {

    private Socket clientSocket;        // Socket for the client to speak to the server with.
    private ObjectInputStream in;       // Incoming messages from client.
    private ObjectOutputStream out;     // Outgoing messages to the client.
    BufferedOutputStream outStream;

    // Where JavaCustXX is your database name
    //private static final String URL = "jdbc:mysql://courses:3306/JavaCustXX";     ************************************
    private static final String URL = "jdbc:mysql://localhost:3306/falcon9";    // Host, port, and database to connect to.
    private static final String name = "user";                                  // Username for database.
    private static final String password = "user";                              // Password for database.

    private Statement getAllStatement = null;                                   // MySQL query for getting all customers in database.
    private PreparedStatement addStatement = null;                              // MySQL statement for adding a customer.
    private PreparedStatement deleteStatement = null;                           // MySQL statement for deleting a customer.
    private PreparedStatement updateStatement = null;                           // MySQL statement for updating a customer.

    private ResultSet resultSet = null;                                         // Results returned from the database.

    private Connection connection = null;                                       // Database connection.

    private ArrayList<String> message = new ArrayList<>();                      // Object for storing and transporting messages between server and client.

    /**
     * Constructor
     *
     * Initialize the streams and start the thread.
     */
    Conversation(Socket socket) {
        clientSocket = socket;          // Initialize client socket with passed in client socket.

        try {
            // Initialize object streams with client's socket information.
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            System.out.println("LOG: Streams opened");
        } catch (IOException e) {
            try {
                clientSocket.close();
            } catch (IOException e2) {
                System.err.println("Exception closing client socket: " + e2);
            }

            System.err.println("Exception getting socket streams: " + e);
            return;
        }

        try {
            // Obtain "HANDSHAKE" string indicating that client wants to connect.
            String handshake = (String) in.readObject();

            // If the string is indeed "HANDSHAKE", attempt to connect to the database.
            if (handshake.equalsIgnoreCase("HANDSHAKE")) {
                System.out.println("LOG: Trying to create database connection");
                connection = DriverManager.getConnection(URL, name, password);

                // Send back a confirmation of handshake to client.
                out.writeObject("HANDSHAKE");

                System.out.println("LOG: Connected to database");
            }

        } catch (SQLException | IOException | ClassNotFoundException e) {
            try {
                out.writeObject("Could not connect to database.");
            } catch (IOException ex) {
                System.out.println("Could not connect to database.");
            }
            System.err.println("Exception connecting to database manager: " + e);
            return;
        }

        // Start the run loop.
        System.out.println("LOG: Connection achieved, starting run loop");

        this.start();
    }

    /**
     * run()
     *
     * Reads and processes input from the client until the client disconnects.
     */
    @Override
    public void run() {
        System.out.println("LOG: Thread running");

        try {
            while(true) {
                // Read and process input from the client.
                message = (ArrayList) in.readObject();

                // Determine which request was send by client and call appropriate method.
                switch (message.get(0)) {
                    case "GETALL":
                        handleGetAll();
                        break;
                    case "ADD":
                        message.remove(0);
                        handleAdd(message);
                        break;
                    case "UPDATE":
                        message.remove(0);
                        handleUpdate(message);
                        break;
                    case "DELETE":
                        message.remove(0);
                        handleDelete(message);
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("IOException: " + e);
            System.out.println("LOG: Client disconnected");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Exception closing client socket: " + e);
            }
        }
    }

    private void handleGetAll() {
        try {
            int rowsReturned = 0;       // How many rows are return after query.

            // Initialize MySQL statement using connection's create statement method.
            getAllStatement = connection.createStatement();

            // Execute query and store results in the result set.
            resultSet = getAllStatement.executeQuery("SELECT * FROM customer");

            // While there are rows to go through, add each row's fields to message object.
            while (resultSet.next()) {
                message.add(resultSet.getString("name"));
                message.add(resultSet.getString("ssn"));
                message.add(resultSet.getString("address"));
                message.add(resultSet.getString("zipCode"));

                // Increment row counter to keep track of how many customers were found.
                rowsReturned++;
            }

            // Add the string that displays how many customers found to message object.
            message.add(rowsReturned + " records found.");

            // Send message back to client.
            out.writeObject(message);
        } catch(Exception e) {
            System.out.println("Error in creating sql statement.");
            System.out.println(e.getMessage());
        }
    }

    /**
     * Handles the Add customer task.
     *
     * @param clientMsg     A single customer's information. Contains all four data fields.
     */
    private void handleAdd(ArrayList<String> clientMsg) {
        try {
            // Prepare a MySQL statement via the connection's prepare statement method.
            addStatement = connection.prepareStatement("insert into customer values (?, ?, ?, ?)");

            // Bind each field of customer to a question mark in the prepared statement.
            addStatement.setString(1, clientMsg.get(0));
            addStatement.setString(2, clientMsg.get(1));
            addStatement.setString(3, clientMsg.get(2));
            addStatement.setString(4, clientMsg.get(3));

            try {
                // Execute the update.
                addStatement.executeUpdate();
            } catch (SQLException e) {
                message.add("SSN already exists.");
                out.writeObject(message);
                return;
            }

            // Add status to message object that customer was added.
            message.add("Customer added");

            // Send message back to client.
            out.writeObject(message);
        } catch (SQLException | IOException e) {
            System.out.println("Error in adding to database: " + e.getMessage());
        }
    }

    /**
     * Handles Delete customer task.
     *
     * @param clientMsg     A single customer to delete. Contains only SSN.
     */
    private void handleDelete(ArrayList<String> clientMsg) {
        try {
            // Prepare a MySQL statement via the connection's prepare statement method.
            deleteStatement = connection.prepareStatement("delete from customer where ssn = ?");

            // Bind each field of customer to a question mark in the prepared statement.
            deleteStatement.setString(1, clientMsg.get(0));

            try {
                // If the number of rows affected is 0, no customer was deleted because SSN doesn't exist in the database.
                if (deleteStatement.executeUpdate() == 0) {
                    // Send client message that the specified SSN doesn't exist and no customer was deleted.
                    message.add("No customer with specified SSN. No customer deleted.");

                    // Send message back to client.
                    out.writeObject(message);
                    return;
                }

            } catch (SQLException e) {
                message.add("Could not delete customer.");
                out.writeObject(message);
                return;
            }

            // Add status to message object.
            message.add("Customer successfully deleted.");

            // Send message object back to client.
            out.writeObject(message);

        } catch(SQLException | IOException e) {
            System.out.println("Error at handleDelete()" + e.getMessage());
        }
    }

    /**
     * Handles Update customer task. Updates only address of a customer.
     *
     * @param clientMsg     A single customer to update. Contains only SSN and address.
     */
    private void handleUpdate(ArrayList<String> clientMsg) {
        try {
            // Prepare a MySQL statement via the connection's prepare statement method.
            updateStatement = connection.prepareStatement("update customer set address = ? where ssn = ?");

            // Bind each field of customer to a question mark in the prepared statement.
            updateStatement.setString(1, clientMsg.get(1));
            updateStatement.setString(2, clientMsg.get(0));

            try {
                // If the number of rows affected is 0, no customer was updated because SSN doesn't exist in the database.
                if (updateStatement.executeUpdate() == 0) {
                    message.add("No customer with specified SSN. No customer updated.");
                    out.writeObject(message);
                    return;
                }

            } catch (SQLException e) {
                message.add("Could not update customer.");
                out.writeObject(message);
                return;
            }

            // Add status to message object.
            message.add("Customer address successfully updated.");

            // Send message back to client.
            out.writeObject(message);

        } catch(SQLException | IOException e) {
            System.out.println("Error at handleUpdate()" + e.getMessage());
        }
    }
}