import java.util.ArrayList;
import java.util.List;

public class MarketManager {
    private static MarketManager instance;
    private final List<Stock> stocks = new ArrayList<>();

    public MarketManager() {
        stocks.add(new Stock("AAPL", "Apple Inc.", 175.50));
        stocks.add(new Stock("MSFT", "Microsoft Corp.", 330.20));
        stocks.add(new Stock("GOOGL", "Alphabet Inc.", 135.10));
        stocks.add(new Stock("AMZN", "Amazon.com Inc.", 130.00));
        stocks.add(new Stock("TSLA", "Tesla Inc.", 240.50));
        stocks.add(new Stock("NVDA", "NVIDIA Corp.", 450.80));
        stocks.add(new Stock("META", "Meta Platforms", 300.00));
        stocks.add(new Stock("JPM", "JPMorgan Chase", 145.20));
        stocks.add(new Stock("V", "Visa Inc.", 235.10));
        stocks.add(new Stock("NFLX", "Netflix Inc.", 400.60));
    }

    public static synchronized MarketManager getInstance() {
        if (instance == null) {
            instance = new MarketManager();
            instance.startMarket();
        }
        return instance;
    }

    public List<Stock> getStocks() {
        return stocks;
    }

    public Stock getStock(String symbol) {
        for (Stock s : stocks) {
            if (s.getSymbol().equalsIgnoreCase(symbol)) return s;
        }
        return null;
    }

    public void startMarket() {
        for (Stock stock : stocks) {
            Thread t = new Thread(new StockTask(stock));
            t.setDaemon(true);
            t.start();
        }
    }
}
