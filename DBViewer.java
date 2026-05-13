import java.sql.*;

public class DBViewer {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("======================================");
            System.out.println("        WEB APP DATABASE CONTENTS             ");
            System.out.println("======================================");

            System.out.println("\n--- REGISTERED USERS ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
                boolean hasUsers = false;
                while (rs.next()) {
                    hasUsers = true;
                    System.out.println("Username: " + rs.getString("username"));
                }
                if (!hasUsers) System.out.println("No registered users.");
            } catch (Exception e) { System.out.println("No user data."); }

            System.out.println("\n--- USER PORTFOLIOS ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM user_portfolio")) {
                while (rs.next()) {
                    System.out.println("User: " + rs.getString("username") + ", Balance: $" + rs.getDouble("balance"));
                }
            } catch (Exception e) { System.out.println("No portfolio data."); }

            System.out.println("\n--- CURRENT HOLDINGS ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM user_holdings")) {
                boolean hasHoldings = false;
                while (rs.next()) {
                    hasHoldings = true;
                    System.out.println("User: " + rs.getString("username") + ", Symbol: " + rs.getString("symbol") + ", Quantity: " + rs.getInt("quantity"));
                }
                if (!hasHoldings) System.out.println("No one owns any stocks yet.");
            } catch (Exception e) { System.out.println("No holdings data."); }

            System.out.println("\n--- RECENT TRANSACTIONS ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM user_transactions ORDER BY id DESC LIMIT 20")) {
                boolean hasTransactions = false;
                while (rs.next()) {
                    hasTransactions = true;
                    System.out.println(rs.getString("timestamp") + " | " + 
                                       rs.getString("username") + " | " + 
                                       rs.getString("type") + " " + 
                                       rs.getInt("quantity") + "x " + 
                                       rs.getString("symbol") + " @ $" + 
                                       String.format("%.2f", rs.getDouble("price")));
                }
                if (!hasTransactions) System.out.println("No transactions yet.");
            } catch (Exception e) { System.out.println("No transaction data."); }
            
            System.out.println("\n======================================");
            
        } catch (SQLException e) {
            System.err.println("Could not connect to the database. Make sure it exists!");
            e.printStackTrace();
        }
    }
}
