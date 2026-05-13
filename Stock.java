import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a stock entity.
 * Uses ReadWriteLock to ensure thread-safe access to price updates and history.
 */
public class Stock {
    private final String symbol;
    private final String name;
    private final double initialPrice;
    private double currentPrice;
    private final List<Double> priceHistory;
    
    // Lock to ensure thread safety between the simulator thread and UI thread
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public Stock(String symbol, String name, double initialPrice) {
        this.symbol = symbol;
        this.name = name;
        this.initialPrice = initialPrice;
        this.currentPrice = initialPrice;
        this.priceHistory = new ArrayList<>();
        this.priceHistory.add(initialPrice);
    }

    
    
    public void updatePrice(double newPrice) {
        lock.writeLock().lock();
        try {
            this.currentPrice = newPrice;
            this.priceHistory.add(newPrice);
            // Cap history size to prevent memory leaks over long runs
            if (this.priceHistory.size() > 500) {
                this.priceHistory.remove(0);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    
    public double getCurrentPrice() {
        lock.readLock().lock();
        try {
            return currentPrice;
        } finally {
            lock.readLock().unlock();
        }
    }

    public double getChange() {
        lock.readLock().lock();
        try {
            return currentPrice - initialPrice;
        } finally {
            lock.readLock().unlock();
        }
    }

    public double getChangePercent() {
        lock.readLock().lock();
        try {
            return ((currentPrice - initialPrice) / initialPrice) * 100;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Double> getPriceHistory() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(priceHistory);
        } finally {
            lock.readLock().unlock();
        }
    }
}
