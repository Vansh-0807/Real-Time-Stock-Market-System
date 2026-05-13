import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class WebServer {
    public static void main(String[] args) throws Exception {
        DatabaseManager.initDatabase();
        MarketManager.getInstance(); // starts background thread

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            
            server.createContext("/", new StaticFileHandler());
            server.createContext("/api/signup", new AuthHandler(true));
            server.createContext("/api/login", new AuthHandler(false));
            server.createContext("/api/market", new MarketHandler());
            server.createContext("/api/portfolio", new PortfolioHandler());
            server.createContext("/api/buy", new TradeHandler(true));
            server.createContext("/api/sell", new TradeHandler(false));
            server.createContext("/api/admin/db", new AdminDbHandler());
            
            server.setExecutor(null);
            server.start();
            System.out.println("[WebServer] Server started on http://localhost:8080");
        } catch (java.net.BindException e) {
            System.err.println("\n[ERROR] Port 8080 is already in use!");
            System.err.println("Another instance of the Java backend is likely already running.");
            System.err.println("Please close the previous terminal/process and try again.\n");
            System.exit(1);
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static boolean handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type,Authorization");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream()) {
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static String extractJsonStr(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start == -1) return null;
        start += marker.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
    
    private static int extractJsonInt(String json, String field) {
        String marker = "\"" + field + "\":";
        int start = json.indexOf(marker);
        if (start == -1) return -1;
        start += marker.length();
        int end1 = json.indexOf(",", start);
        int end2 = json.indexOf("}", start);
        int end = (end1 != -1 && end1 < end2) ? end1 : end2;
        try {
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Exception e) { return -1; }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            
            try {
                byte[] fileBytes = Files.readAllBytes(Paths.get("." + path));
                exchange.getResponseHeaders().set("Content-Type", path.endsWith(".html") ? "text/html" : "text/plain");
                exchange.sendResponseHeaders(200, fileBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(fileBytes);
                }
            } catch (Exception e) {
                String res = "404 Not Found";
                exchange.sendResponseHeaders(404, res.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(res.getBytes()); }
            }
        }
    }

    static class AuthHandler implements HttpHandler {
        private boolean isSignup;
        public AuthHandler(boolean isSignup) { this.isSignup = isSignup; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = readBody(exchange);
                String user = extractJsonStr(body, "username");
                String pass = extractJsonStr(body, "password");
                
                if (user == null || pass == null) {
                    sendResponse(exchange, 400, "{\"success\":false, \"message\":\"Missing fields\"}");
                    return;
                }
                
                String errorMsg;
                if (isSignup) errorMsg = DatabaseManager.registerUser(user, pass);
                else errorMsg = DatabaseManager.loginUser(user, pass);
                
                if (errorMsg == null) {
                    sendResponse(exchange, 200, "{\"success\":true}");
                } else {
                    errorMsg = errorMsg.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
                    sendResponse(exchange, 401, "{\"success\":false, \"message\":\"" + errorMsg + "\"}");
                }
            }
        }
    }

    static class MarketHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            List<Stock> stocks = MarketManager.getInstance().getStocks();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < stocks.size(); i++) {
                Stock s = stocks.get(i);
                json.append(String.format("{\"symbol\":\"%s\", \"name\":\"%s\", \"price\":%f}", s.getSymbol(), s.getName(), s.getCurrentPrice()));
                if (i < stocks.size() - 1) json.append(",");
            }
            json.append("]");
            sendResponse(exchange, 200, json.toString());
        }
    }

    static class PortfolioHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.startsWith("username=")) {
                sendResponse(exchange, 400, "{\"error\":\"missing username\"}");
                return;
            }
            String user = query.split("=")[1];
            
            Portfolio p = new Portfolio(user);
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"balance\":").append(p.getBalance()).append(",");
            
            // Holdings
            json.append("\"holdings\":{");
            Map<String, Integer> h = p.getHoldings();
            int c = 0;
            for (Map.Entry<String, Integer> e : h.entrySet()) {
                json.append("\"").append(e.getKey()).append("\":").append(e.getValue());
                if (++c < h.size()) json.append(",");
            }
            json.append("},");
            
            // Transactions
            json.append("\"transactions\":[");
            List<Map<String, String>> txs = DatabaseManager.getTransactions(user);
            for (int i = 0; i < txs.size(); i++) {
                Map<String, String> tx = txs.get(i);
                json.append(String.format("{\"type\":\"%s\", \"sym\":\"%s\", \"qty\":%s, \"price\":%s, \"date\":\"%s\"}", 
                        tx.get("type"), tx.get("sym"), tx.get("qty"), tx.get("price"), tx.get("date")));
                if (i < txs.size() - 1) json.append(",");
            }
            json.append("]");
            json.append("}");
            
            sendResponse(exchange, 200, json.toString());
        }
    }

    static class TradeHandler implements HttpHandler {
        private boolean isBuy;
        public TradeHandler(boolean isBuy) { this.isBuy = isBuy; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = readBody(exchange);
                String user = extractJsonStr(body, "username");
                String symbol = extractJsonStr(body, "symbol");
                int qty = extractJsonInt(body, "qty");
                
                if (user == null || symbol == null || qty <= 0) {
                    sendResponse(exchange, 400, "{\"success\":false, \"message\":\"Invalid payload\"}");
                    return;
                }
                
                Stock stock = MarketManager.getInstance().getStock(symbol);
                if (stock == null) {
                    sendResponse(exchange, 400, "{\"success\":false, \"message\":\"Invalid stock symbol\"}");
                    return;
                }
                
                Portfolio p = new Portfolio(user);
                boolean success = isBuy ? p.buy(stock, qty) : p.sell(stock, qty);
                
                if (success) {
                    sendResponse(exchange, 200, "{\"success\":true}");
                } else {
                    sendResponse(exchange, 400, "{\"success\":false, \"message\":\"Insufficient funds or shares\"}");
                }
            }
        }
    }
    static class AdminDbHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            StringBuilder json = new StringBuilder("{");
            try (java.sql.Connection conn = DatabaseManager.getConnection()) {
                // Users
                json.append("\"users\":[");
                try (java.sql.Statement stmt = conn.createStatement(); java.sql.ResultSet rs = stmt.executeQuery("SELECT username FROM users")) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"username\":\"%s\"}", rs.getString("username")));
                        first = false;
                    }
                }
                json.append("],");
                
                // Portfolios
                json.append("\"portfolios\":[");
                try (java.sql.Statement stmt = conn.createStatement(); java.sql.ResultSet rs = stmt.executeQuery("SELECT * FROM user_portfolio")) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"username\":\"%s\",\"balance\":%f}", rs.getString("username"), rs.getDouble("balance")));
                        first = false;
                    }
                }
                json.append("],");
                
                // Holdings
                json.append("\"holdings\":[");
                try (java.sql.Statement stmt = conn.createStatement(); java.sql.ResultSet rs = stmt.executeQuery("SELECT * FROM user_holdings")) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"username\":\"%s\",\"symbol\":\"%s\",\"quantity\":%d}", rs.getString("username"), rs.getString("symbol"), rs.getInt("quantity")));
                        first = false;
                    }
                }
                json.append("],");
                
                // Transactions
                json.append("\"transactions\":[");
                try (java.sql.Statement stmt = conn.createStatement(); java.sql.ResultSet rs = stmt.executeQuery("SELECT * FROM user_transactions ORDER BY id DESC")) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"id\":%d,\"username\":\"%s\",\"type\":\"%s\",\"symbol\":\"%s\",\"quantity\":%d,\"price\":%f,\"timestamp\":\"%s\"}",
                                rs.getInt("id"), rs.getString("username"), rs.getString("type"), rs.getString("symbol"), rs.getInt("quantity"), rs.getDouble("price"), rs.getString("timestamp")));
                        first = false;
                    }
                }
                json.append("]");
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
                return;
            }
            json.append("}");
            sendResponse(exchange, 200, json.toString());
        }
    }
}
