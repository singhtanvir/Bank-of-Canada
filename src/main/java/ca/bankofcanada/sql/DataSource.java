package ca.bankofcanada.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DataSource {
    public static DataSource instance;
    private final Connection connection;

    private DataSource() throws ClassNotFoundException, SQLException {
        final String DRIVER_NAME = "com.mysql.cj.jdbc.Driver";
        Class.forName(DRIVER_NAME);
        final String URL = "jdbc:mysql://localhost:3306/forex?" +
                "useUnicode=true&" +
                "useJDBCCompliantTimezoneShift=true&" +
                "useLegacyDatetimeCode=false&serverTimezone=UTC";
        final String USERNAME = "root";
        final String PASSWORD = "YOUR_DATABASE_PASSWORD";

        this.connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    public static DataSource getInstance() throws SQLException, ClassNotFoundException {
        if (DataSource.instance == null) {
            DataSource.instance = new DataSource();
        } else if (DataSource.instance.getConnection().isClosed()) {
            DataSource.instance = new DataSource();
        }
        return DataSource.instance;
    }

    public Connection getConnection() {
        return this.connection;
    }

}
