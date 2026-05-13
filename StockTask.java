import java.util.Random;

public class StockTask implements Runnable {
    private final Stock stock;
    private final Random random = new Random();
    private volatile boolean running = true;

    public StockTask(Stock stock) {
        this.stock = stock;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                // Wait for ~1 to 2 seconds to simulate continuous market update
                Thread.sleep(1000 + random.nextInt(1000)); 
                
                double currentPrice = stock.getCurrentPrice();
                
                // Random fluctuation between -2% and +2% to give zig-zag real feel
                double fluctuation = (random.nextDouble() * 0.04) - 0.02;
                double newPrice = currentPrice * (1 + fluctuation);
                
                // Ensure price doesn't drop below a minimum threshold
                newPrice = Math.max(1.0, newPrice);
                
                stock.updatePrice(newPrice);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt status
                break;
            }
        }
    }
}
