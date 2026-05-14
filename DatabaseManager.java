import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/stock_market?createDatabaseIfNotExist=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "#boney@6264464754";
    public static String lastError = "";

   public static Connection getConnection() throws SQLException {
    try {
        Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
        throw new SQLException("MySQL Driver not found.", e);
    }

    return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
   }

    public static void initDatabase() {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (Exception ignored) {}

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                         "username VARCHAR(255) PRIMARY KEY, " +
                         "password VARCHAR(255) NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS user_portfolio (" +
                         "username VARCHAR(255) PRIMARY KEY, " +
                         "balance REAL NOT NULL, " +
                         "FOREIGN KEY(username) REFERENCES users(username))");

            stmt.execute("CREATE TABLE IF NOT EXISTS user_holdings (" +
                         "username VARCHAR(255), " +
                         "symbol VARCHAR(50), " +
                         "quantity INTEGER NOT NULL, " +
                         "PRIMARY KEY(username, symbol), " +
                         "FOREIGN KEY(username) REFERENCES users(username))");

            stmt.execute("CREATE TABLE IF NOT EXISTS user_transactions (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY, " +
                         "username VARCHAR(255), " +
                         "type VARCHAR(20) NOT NULL, " +
                         "symbol VARCHAR(50) NOT NULL, " +
                         "quantity INTEGER NOT NULL, " +
                         "price REAL NOT NULL, " +
                         "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                         "FOREIGN KEY(username) REFERENCES users(username))");
                         
            System.out.println("[Database] Multi-user schema initialized.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String registerUser(String username, String password) {
        String sqlUser = "INSERT INTO users (username, password) VALUES (?, ?)";
        String sqlPort = "INSERT INTO user_portfolio (username, balance) VALUES (?, 10000.0)";
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(sqlUser);
                 PreparedStatement ps2 = conn.prepareStatement(sqlPort)) {
                ps1.setString(1, username);
                ps1.setString(2, password);
                ps1.executeUpdate();
                
                ps2.setString(1, username);
                ps2.executeUpdate();
                
                conn.commit();
                return null;
            } catch (SQLException ex) {
                conn.rollback();
                if (ex.getMessage().contains("Duplicate entry")) {
                    return "User already exists. Please login instead.";
                }
                return "SQL Error: " + ex.getMessage();
            }
        } catch (SQLException e) {
            return "Connection Error: " + e.getMessage();
        }
    }

    public static String loginUser(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (rs.getString("password").equals(password)) {
                        return null;
                    } else {
                        return "Invalid password.";
                    }
                } else {
                    return "User does not exist. Please sign up.";
                }
            }
        } catch (SQLException e) {
            return "Connection Error: " + e.getMessage();
        }
    }

    public static double getBalance(String username) {
        String sql = "SELECT balance FROM user_portfolio WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("balance");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 10000.0;
    }

    public static void updateBalance(String username, double newBalance) {
        String sql = "UPDATE user_portfolio SET balance = ? WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static Map<String, Integer> getHoldings(String username) {
        Map<String, Integer> holdings = new ConcurrentHashMap<>();
        String sql = "SELECT symbol, quantity FROM user_holdings WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    holdings.put(rs.getString("symbol"), rs.getInt("quantity"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return holdings;
    }

    public static void updateHolding(String username, String symbol, int quantity) {
        try (Connection conn = getConnection()) {
            if (quantity <= 0) {
                String sql = "DELETE FROM user_holdings WHERE username = ? AND symbol = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ps.setString(2, symbol);
                    ps.executeUpdate();
                }
            } else {
                String sql = "INSERT INTO user_holdings (username, symbol, quantity) VALUES (?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ps.setString(2, symbol);
                    ps.setInt(3, quantity);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void logTransaction(String username, String type, String symbol, int quantity, double price) {
        String sql = "INSERT INTO user_transactions (username, type, symbol, quantity, price) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, type);
            ps.setString(3, symbol);
            ps.setInt(4, quantity);
            ps.setDouble(5, price);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static List<Map<String, String>> getTransactions(String username) {
        List<Map<String, String>> txs = new ArrayList<>();
        String sql = "SELECT type, symbol, quantity, price, timestamp FROM user_transactions WHERE username = ? ORDER BY id DESC LIMIT 50";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> tx = new HashMap<>();
                    tx.put("type", rs.getString("type"));
                    tx.put("sym", rs.getString("symbol"));
                    tx.put("qty", String.valueOf(rs.getInt("quantity")));
                    tx.put("price", String.valueOf(rs.getDouble("price")));
                    tx.put("date", rs.getString("timestamp"));
                    txs.add(tx);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return txs;
    }
}
