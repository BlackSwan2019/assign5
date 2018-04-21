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

public class CustomerServer extends Thread {
    private ServerSocket listenSocket;

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

    private Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    BufferedOutputStream outStream;

    // Where JavaCustXX is your database name
    //private static final String URL = "jdbc:mysql://courses:3306/JavaCustXX";     ************************************
    private static final String URL = "jdbc:mysql://localhost:3306/falcon9";
    private static final String name = "user";
    private static final String password = "user";

    private Statement getAllStatement = null;
    private PreparedStatement addStatement = null;
    private PreparedStatement deleteStatement = null;
    private PreparedStatement updateStatement = null;

    private ResultSet resultSet = null;

    private Connection connection = null;

    private ArrayList<String> message = new ArrayList<>();

    /**
     * Constructor
     *
     * Initialize the streams and start the thread.
     */
    Conversation(Socket socket) {
        clientSocket = socket;

        try {
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
            String handshake = (String) in.readObject();

            if (handshake.equalsIgnoreCase("HANDSHAKE")) {
                System.out.println("LOG: Trying to create database connection");
                connection = DriverManager.getConnection(URL, name, password);

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
                // Read and process input from the client. (HANDLE REQUESTS)
                message = (ArrayList) in.readObject();

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
            int rowsReturned = 0;

            getAllStatement = connection.createStatement();

            resultSet = getAllStatement.executeQuery("SELECT * FROM customer");

            while (resultSet.next()) {
                message.add(resultSet.getString("name"));
                message.add(resultSet.getString("ssn"));
                message.add(resultSet.getString("address"));
                message.add(resultSet.getString("zipCode"));
                rowsReturned++;
            }

            message.add(rowsReturned + " records found.");

            out.writeObject(message);
        } catch(Exception e) {
            System.out.println("Error in creating sql statement.");
            System.out.println(e.getMessage());
        }
    }

    private void handleAdd(ArrayList<String> clientMsg) {
        try {
            addStatement = connection.prepareStatement("insert into customer values (?, ?, ?, ?)");

            addStatement.setString(1, clientMsg.get(0));
            addStatement.setString(2, clientMsg.get(1));
            addStatement.setString(3, clientMsg.get(2));
            addStatement.setString(4, clientMsg.get(3));

            try {
                addStatement.executeUpdate();
            } catch (SQLException e) {
                message.add("SSN already exists.");
                out.writeObject(message);
                return;
            }

            message.add("Customer added");

            out.writeObject(message);
        } catch (SQLException | IOException e) {
            System.out.println("Error in adding to database: " + e.getMessage());
        }
    }

    private void handleDelete(ArrayList<String> clientMsg) {
        try {
            deleteStatement = connection.prepareStatement("delete from customer where ssn = ?");

            deleteStatement.setString(1, clientMsg.get(0));

            try {
                if (deleteStatement.executeUpdate() == 0) {
                    message.add("No customer with specified SSN. No customer deleted.");
                    out.writeObject(message);
                    return;
                }

            } catch (SQLException e) {
                message.add("Could not delete customer.");
                out.writeObject(message);
                return;
            }

            message.add("Customer successfully deleted.");

            out.writeObject(message);

        } catch(SQLException | IOException e) {
            System.out.println("Error at handleDelete()" + e.getMessage());
        }
    }

    private void handleUpdate(ArrayList<String> clientMsg) {
        try {
            updateStatement = connection.prepareStatement("update customer set address = ? where ssn = ?");

            updateStatement.setString(1, clientMsg.get(1));
            updateStatement.setString(2, clientMsg.get(0));

            try {
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

            message.add("Customer address successfully updated.");

            out.writeObject(message);

        } catch(SQLException | IOException e) {
            System.out.println("Error at handleUpdate()" + e.getMessage());
        }
    }
}