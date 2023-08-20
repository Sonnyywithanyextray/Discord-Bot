package sonny.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.PreparedStatement;

public class DatabaseConnectionService {

    public static Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:test.db");
            System.out.println("Database connection path: " + connection.getMetaData().getURL());
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

        // Modified Bugs table structure
        String createBugsTable = "CREATE TABLE IF NOT EXISTS Bugs (" +
            "id INTEGER PRIMARY KEY," +
            "title TEXT NOT NULL," +
            "description TEXT NOT NULL," +
            "footer TEXT," +
            "thumbnail TEXT," +
            "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createSuggestionsTable);
            stmt.execute(createBugsTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    //this was a dummybug test method I used to test if #1) the database connection was established, and 2), why suggestions and bugs were not inserted in the database correctly.
    public static void insertDummyBug() {
        System.out.println("Inserting dummy bug...");
        String sql = "INSERT INTO Bugs (title, description) VALUES ('Dummy Bug', 'This is a dummy bug description.')";

        try (Connection connection = getConnection();
            Statement stmt = connection.createStatement()) {
            int rowsInserted = stmt.executeUpdate(sql);

            System.out.println("Dummy bug inserted! Rows inserted: " + rowsInserted);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error executing the SQL query: " + e.getMessage());
        }
    }

    public static void runTestQuery() {
        try (Connection connection = getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 2;")) {

            if (rs.next()) {
                int result = rs.getInt(1);
                System.out.println("Test query result: " + result);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error executing the test query: " + e.getMessage());
        }
    }

    public static void insertBug(String bugDescription, String authorDisplayName, String avatarUrl) {
        String title = "Bug Reported";
        String footer = "Reported by: " + authorDisplayName;
        String thumbnail = avatarUrl;
        String sql = "INSERT INTO Bugs (title, description, footer, thumbnail) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConnectionService.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, bugDescription);
            pstmt.setString(3, footer);
            pstmt.setString(4, thumbnail);
            pstmt.executeUpdate();
            System.out.println("Bug inserted successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error inserting the bug: " + e.getMessage());
        }
    }
    public static void insertSuggestion(String suggestionText, String authorDisplayName, String avatarUrl) {
        String title = "Suggestion Received";
        String footer = "Suggested by: " + authorDisplayName;
        String thumbnail = avatarUrl;
        String sql = "INSERT INTO Suggestions (title, suggestion, footer, thumbnail) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConnectionService.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, suggestionText);
            pstmt.setString(3, footer);
            pstmt.setString(4, thumbnail);
            pstmt.executeUpdate();
            System.out.println("Suggestion inserted successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error inserting the suggestion: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        runTestQuery(); // Execute the test query first
        insertDummyBug(); // Call the insertDummyBug() method separately
    }
}