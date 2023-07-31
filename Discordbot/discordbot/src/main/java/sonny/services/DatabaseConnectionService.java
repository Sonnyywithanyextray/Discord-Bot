package sonny.services;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnectionService {
    
    public static Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:test.db");
            System.out.println("Database connection established!");
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
