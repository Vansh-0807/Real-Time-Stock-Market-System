import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class Portfolio {
    private final String username;
    private double balance;
    private final Map<String, Integer> ownedStocks = new ConcurrentHashMap<>();

    public Portfolio(String username) {
        this.username = username;
        this.balance = DatabaseManager.getBalance(username);
        this.ownedStocks.putAll(DatabaseManager.getHoldings(username));
    }

    public synchronized boolean buy(Stock stock, int quantity) {
        double cost = stock.getCurrentPrice() * quantity;
        if (balance >= cost) {
            balance -= cost;
            int newQty = ownedStocks.getOrDefault(stock.getSymbol(), 0) + quantity;
            ownedStocks.put(stock.getSymbol(), newQty);
            
            DatabaseManager.updateBalance(username, balance);
            DatabaseManager.updateHolding(username, stock.getSymbol(), newQty);
            DatabaseManager.logTransaction(username, "Buy", stock.getSymbol(), quantity, stock.getCurrentPrice());
            
            return true;
        }
        return false;
    }

    public synchronized boolean sell(Stock stock, int quantity) {
        int owned = ownedStocks.getOrDefault(stock.getSymbol(), 0);
        if (owned >= quantity) {
            double revenue = stock.getCurrentPrice() * quantity;
            balance += revenue;
            int newQty = owned - quantity;
            if (newQty == 0) {
                ownedStocks.remove(stock.getSymbol());
            } else {
                ownedStocks.put(stock.getSymbol(), newQty);
            }
            
            DatabaseManager.updateBalance(username, balance);
            DatabaseManager.updateHolding(username, stock.getSymbol(), newQty);
            DatabaseManager.logTransaction(username, "Sell", stock.getSymbol(), quantity, stock.getCurrentPrice());
            
            return true;
        }
        return false;
    }

    public synchronized double getBalance() {
        return balance;
    }

    public Map<String, Integer> getHoldings() {
        return ownedStocks;
    }
}
