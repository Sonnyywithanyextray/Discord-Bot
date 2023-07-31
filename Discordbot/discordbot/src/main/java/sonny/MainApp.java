package sonny;

import java.sql.Connection;
import sonny.services.DatabaseConnectionService;

public class MainApp {
    public static void main(String[] args) {
        Connection connection = DatabaseConnectionService.getConnection();
        if (connection != null) {
            // Do something with the connection, like CRUD operations...
            try {
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

