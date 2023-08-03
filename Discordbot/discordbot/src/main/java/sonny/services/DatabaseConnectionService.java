package sonny.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseConnectionService {
    
    public static Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:test.db");
            System.out.println("Database connection established!");
            setupTables(connection);
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private static void setupTables(Connection connection) {
        String createSuggestionsTable = "CREATE TABLE IF NOT EXISTS Suggestions (" +
        "id INTEGER PRIMARY KEY," +
        "suggestion TEXT NOT NULL," +
         "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
         ");";

        String createBugsTable = "CREATE TABLE IF NOT EXISTS Bugs (" +
        "id INTEGER PRIMARY KEY," +
        "bug_description TEXT NOT NULL," +
        "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
        ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createSuggestionsTable);
            stmt.execute(createBugsTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void insertDummyBug() {
        System.out.println("Inserting dummy bug...");
        String sql = "INSERT INTO Bugs (bug_description) VALUES ('This is a dummy bug description.')";

        try (Connection connection = getConnection();
            Statement stmt = connection.createStatement()) {
            int rowsInserted = stmt.executeUpdate(sql);
            connection.commit(); // Commit the changes

            System.out.println("Dummy bug inserted! Rows inserted: " + rowsInserted);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error executing the SQL query: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        try (Connection connection = getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1;")) {
            
            if (rs.next()) {
                int result = rs.getInt(1);
                System.out.println("Test query result: " + result);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error executing the test query: " + e.getMessage());
        }
    }
}
