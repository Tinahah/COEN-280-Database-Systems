
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author yanping kang
 */
public class DBConnection {

    private static final String url = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String user = "system", pwd = "oraclepwd";

    //Connect DB 
    public static Connection getConnection() {
        System.out.println("Connecting to DB...");
        Connection con = null;
        try {
            con = DriverManager.getConnection(url, user, pwd);
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
            System.out.println("Database connection is successfully established.");
        } catch (SQLException ex) {
            System.out.println("Database connection failed!");   
             ex.printStackTrace();
        }
        return con;
    }

    //Close DB connection.
    public static void closeConnection(Connection con) throws SQLException {
        con.close();
        System.out.println("DB Connection closed successfully.");
    }
}
