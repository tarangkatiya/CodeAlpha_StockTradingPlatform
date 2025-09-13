import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;


public class StockTradingPlatform {

    

    static class Stock implements Serializable {
        private static final long serialVersionUID = 1L;
        String symbol;
        String name;
        BigDecimal price; // price per share

        public Stock(String symbol, String name, BigDecimal price) {
            this.symbol = symbol.toUpperCase();
            this.name = name;
            this.price = price.setScale(2, RoundingMode.HALF_UP);
        }

        public String toString() {
            return String.format("%s (%s): %s", symbol, name, price.toPlainString());
        }
    }

    static class Market {
        Map<String, Stock> stocks = new HashMap<>();
        Random rnd = new Random();

        public Market() {
            // Seed with some example equities
            addStock(new Stock("AAPL", "Apple Inc.", new BigDecimal("170.00")));
            addStock(new Stock("GOOG", "Alphabet Inc.", new BigDecimal("145.50")));
            addStock(new Stock("TSLA", "Tesla Inc.", new BigDecimal("240.00")));
            addStock(new Stock("INFY", "Infosys Ltd.", new BigDecimal("18.50")));
            addStock(new Stock("RELI", "Reliance Industries", new BigDecimal("90.00")));
        }

        public void addStock(Stock s) { stocks.put(s.symbol, s); }

        public Stock get(String symbol) { return stocks.get(symbol.toUpperCase()); }

        public Collection<Stock> all() { return stocks.values(); }

        
        public void tick() {
            for (Stock s : stocks.values()) {
                // generate percentage change between -3% and +3%
                double pct = (rnd.nextDouble() * 6.0) - 3.0;
                BigDecimal factor = BigDecimal.valueOf(1 + pct / 100.0);
                BigDecimal newPrice = s.price.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                if (newPrice.compareTo(new BigDecimal("0.01")) < 0) newPrice = new BigDecimal("0.01");
                s.price = newPrice;
            }
        }

        public void showMarket() {
            System.out.println("--- MARKET DATA ---");
            List<Stock> list = new ArrayList<>(stocks.values());
            list.sort(Comparator.comparing(st -> st.symbol));
            for (Stock s : list) {
                System.out.printf("%6s | %20s | %8s\n", s.symbol, s.name, s.price.toPlainString());
            }
            System.out.println();
        }
    }

    static enum Side { BUY, SELL }

    static class Transaction implements Serializable {
        private static final long serialVersionUID = 1L;
        LocalDateTime time;
        Side side;
        String symbol;
        int qty;
        BigDecimal price; 
        public Transaction(Side side, String symbol, int qty, BigDecimal price) {
            this.time = LocalDateTime.now();
            this.side = side;
            this.symbol = symbol.toUpperCase();
            this.qty = qty;
            this.price = price.setScale(2, RoundingMode.HALF_UP);
        }

        public String toString() {
            return String.format("%s | %s %d @ %s", time, side, qty, price.toPlainString());
        }
    }

    static class Portfolio implements Serializable {
        private static final long serialVersionUID = 1L;
        Map<String, Integer> holdings = new HashMap<>(); 
        BigDecimal cash;
        List<Transaction> history = new ArrayList<>();

        public Portfolio(BigDecimal initialCash) {
            this.cash = initialCash.setScale(2, RoundingMode.HALF_UP);
        }

        public int getQty(String symbol) { return holdings.getOrDefault(symbol.toUpperCase(), 0); }

        public BigDecimal marketValue(Market m) {
            BigDecimal value = cash;
            for (Map.Entry<String, Integer> e : holdings.entrySet()) {
                Stock s = m.get(e.getKey());
                if (s != null) {
                    BigDecimal pos = s.price.multiply(BigDecimal.valueOf(e.getValue()));
                    value = value.add(pos);
                }
            }
            return value.setScale(2, RoundingMode.HALF_UP);
        }

        public void buy(Market m, String symbol, int qty) throws IllegalArgumentException {
            if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive");
            Stock s = m.get(symbol);
            if (s == null) throw new IllegalArgumentException("Unknown stock: " + symbol);
            BigDecimal cost = s.price.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            if (cash.compareTo(cost) < 0) throw new IllegalArgumentException("Insufficient cash. Need " + cost + " available " + cash);
            cash = cash.subtract(cost);
            holdings.put(s.symbol, getQty(s.symbol) + qty);
            Transaction t = new Transaction(Side.BUY, s.symbol, qty, s.price);
            history.add(t);
            System.out.println("Executed: " + t);
        }

        public void sell(Market m, String symbol, int qty) throws IllegalArgumentException {
            if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive");
            Stock s = m.get(symbol);
            if (s == null) throw new IllegalArgumentException("Unknown stock: " + symbol);
            int have = getQty(s.symbol);
            if (have < qty) throw new IllegalArgumentException("Not enough shares to sell. Have " + have);
            BigDecimal proceeds = s.price.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            cash = cash.add(proceeds);
            int remain = have - qty;
            if (remain == 0) holdings.remove(s.symbol); else holdings.put(s.symbol, remain);
            Transaction t = new Transaction(Side.SELL, s.symbol, qty, s.price);
            history.add(t);
            System.out.println("Executed: " + t);
        }

        public void printPortfolio(Market m) {
            System.out.println("--- PORTFOLIO ---");
            System.out.println("Cash: " + cash.toPlainString());
            System.out.println("Holdings:");
            if (holdings.isEmpty()) System.out.println("  (no positions)");
            else {
                System.out.printf("%6s | %8s | %10s | %10s\n", "SYM", "QTY", "Price", "Value");
                for (Map.Entry<String, Integer> e : holdings.entrySet()) {
                    Stock s = m.get(e.getKey());
                    BigDecimal price = s != null ? s.price : BigDecimal.ZERO;
                    BigDecimal value = price.multiply(BigDecimal.valueOf(e.getValue())).setScale(2, RoundingMode.HALF_UP);
                    System.out.printf("%6s | %8d | %10s | %10s\n", e.getKey(), e.getValue(), price.toPlainString(), value.toPlainString());
                }
            }
            System.out.println("Total portfolio value (cash + positions): " + marketValue(m));
            System.out.println();
        }

        public void printHistory() {
            System.out.println("--- TRANSACTION HISTORY ---");
            if (history.isEmpty()) System.out.println("  (none)");
            else history.forEach(t -> System.out.println(t));
            System.out.println();
        }
    }

    static class User implements Serializable {
        private static final long serialVersionUID = 1L;
        String username;
        Portfolio portfolio;

        public User(String username, BigDecimal initialCash) {
            this.username = username;
            this.portfolio = new Portfolio(initialCash);
        }
    }

    

    static class Storage {
        public static void savePortfolio(Portfolio p, String filename) throws IOException {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
                oos.writeObject(p);
            }
        }

        public static Portfolio loadPortfolio(String filename) throws IOException, ClassNotFoundException {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
                return (Portfolio) ois.readObject();
            }
        }
    }

    /* ---------------------- Simple Application ---------------------- */

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Market market = new Market();
        System.out.print("Enter your name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) name = "Trader";
        User user = new User(name, new BigDecimal("10000.00"));

        String saveFile = "portfolio_" + name.replaceAll("\\W+", "_") + ".dat";

        menuLoop:
        while (true) {
            System.out.println("\nHello " + user.username + " - Cash: " + user.portfolio.cash.toPlainString());
            System.out.println("1) Show market  2) Buy  3) Sell  4) Portfolio  5) History  6) Advance Market  7) Save  8) Load  9) Exit");
            System.out.print("Choose: ");
            String cmd = sc.nextLine().trim();
            try {
                switch (cmd) {
                    case "1":
                        market.showMarket();
                        break;
                    case "2":
                        System.out.print("Enter symbol: ");
                        String symB = sc.nextLine().trim().toUpperCase();
                        System.out.print("Enter qty: ");
                        int qB = Integer.parseInt(sc.nextLine().trim());
                        user.portfolio.buy(market, symB, qB);
                        break;
                    case "3":
                        System.out.print("Enter symbol: ");
                        String symS = sc.nextLine().trim().toUpperCase();
                        System.out.print("Enter qty: ");
                        int qS = Integer.parseInt(sc.nextLine().trim());
                        user.portfolio.sell(market, symS, qS);
                        break;
                    case "4":
                        user.portfolio.printPortfolio(market);
                        break;
                    case "5":
                        user.portfolio.printHistory();
                        break;
                    case "6":
                        market.tick();
                        System.out.println("Market advanced (random tick). New prices:");
                        market.showMarket();
                        break;
                    case "7":
                        Storage.savePortfolio(user.portfolio, saveFile);
                        System.out.println("Saved portfolio to " + saveFile);
                        break;
                    case "8":
                        try {
                            Portfolio p = Storage.loadPortfolio(saveFile);
                            user.portfolio = p;
                            System.out.println("Loaded portfolio from " + saveFile);
                        } catch (FileNotFoundException fnf) {
                            System.out.println("No saved portfolio found (" + saveFile + ")");
                        }
                        break;
                    case "9":
                        System.out.println("Goodbye!");
                        break menuLoop;
                    default:
                        System.out.println("Unknown option. Try again.");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }

        sc.close();
    }
}

