import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    static class Product {
        String sku, name, category;
        double price;
        int stock;
        Product(String sku, String name, double price, int stock, String category) {
            this.sku = sku; this.name = name; this.price = price; this.stock = stock; this.category = category;
        }
    }

    static final Path INVENTORY_PATH = Paths.get("data", "inventory.csv");
    static final Path RECEIPTS_DIR   = Paths.get("data", "receipts");
    static final Map<String, Product> INVENTORY = new LinkedHashMap<>();
    static final LinkedHashMap<String, Integer> CART = new LinkedHashMap<>();
    static final Scanner IN = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("SwiftPOS booting…");
        try {
            loadInventory();
            Files.createDirectories(RECEIPTS_DIR);
        } catch (Exception e) {
            System.out.println("ERROR: Inventory failed to load: " + e.getMessage());
            return;
        }

        while (true) {
            System.out.println("\n=== SwiftPOS ===");
            System.out.println("1) List products");
            System.out.println("2) Add to cart (SKU & qty)");
            System.out.println("3) View cart");
            System.out.println("4) Checkout (print receipt)");
            System.out.println("5) Clear cart");
            System.out.println("0) Exit");
            System.out.print("Choice: ");
            String choice = IN.nextLine().trim();

            switch (choice) {
                case "1" -> listProducts();
                case "2" -> addToCart();
                case "3" -> showCart();
                case "4" -> checkout();
                case "5" -> { CART.clear(); System.out.println("Cart cleared."); }
                case "0" -> { System.out.println("Goodbye!"); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    static void loadInventory() throws IOException {
        INVENTORY.clear();
        List<String> lines = Files.readAllLines(INVENTORY_PATH);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] p = line.split(",", -1);
            if (p.length < 5) continue;
            String sku = p[0].trim();
            String name = p[1].trim();
            double price = Double.parseDouble(p[2].trim());
            int stock = Integer.parseInt(p[3].trim());
            String cat = p[4].trim();
            INVENTORY.put(sku, new Product(sku, name, price, stock, cat));
        }
    }

    static void listProducts() {
        System.out.printf("%-6s | %-24s | %8s | %5s | %-10s%n", "SKU", "NAME", "PRICE", "STOCK", "CAT");
        System.out.println("---------------------------------------------------------------------");
        for (Product p : INVENTORY.values()) {
            System.out.printf("%-6s | %-24s | %8.2f | %5d | %-10s%n",
                    p.sku, truncate(p.name,24), p.price, p.stock, truncate(p.category,10));
        }
    }

    static void addToCart() {
        System.out.print("SKU: ");
        String sku = IN.nextLine().trim();
        Product p = INVENTORY.get(sku);
        if (p == null) { System.out.println("SKU not found."); return; }
        System.out.print("Quantity: ");
        String qtyStr = IN.nextLine().trim();
        int qty;
        try { qty = Integer.parseInt(qtyStr); }
        catch (Exception e) { System.out.println("Invalid quantity."); return; }
        if (qty <= 0) { System.out.println("Quantity must be > 0."); return; }
        if (qty > p.stock) { System.out.println("Insufficient stock. In stock: " + p.stock); return; }
        CART.put(sku, CART.getOrDefault(sku, 0) + qty);
        System.out.println(qty + " x " + p.name + " added to cart.");
    }

    static void showCart() {
        if (CART.isEmpty()) { System.out.println("Cart is empty."); return; }
        double subtotal = 0;
        System.out.printf("%-6s | %-24s | %5s | %8s%n", "SKU", "NAME", "QTY", "LINE");
        System.out.println("---------------------------------------------------------");
        for (var e : CART.entrySet()) {
            Product p = INVENTORY.get(e.getKey());
            int qty = e.getValue();
            double line = p.price * qty;
            subtotal += line;
            System.out.printf("%-6s | %-24s | %5d | %8.2f%n", p.sku, truncate(p.name,24), qty, line);
        }
        double tax = subtotal * 0.08;
        double total = subtotal + tax;
        System.out.printf("Subtotal: %.2f  | Tax(8%%): %.2f  | Total: %.2f%n", subtotal, tax, total);
    }

    static void checkout() {
        if (CART.isEmpty()) { System.out.println("Cart is empty."); return; }
        showCart();
        System.out.print("Cash paid (e.g., 10.00): ");
        double paid;
        try { paid = Double.parseDouble(IN.nextLine().trim()); }
        catch (Exception e) { System.out.println("Invalid amount."); return; }

        double subtotal = CART.entrySet().stream()
                .mapToDouble(e -> INVENTORY.get(e.getKey()).price * e.getValue()).sum();
        double tax = subtotal * 0.08;
        double total = round2(subtotal + tax);

        if (paid + 1e-9 < total) { System.out.println("Insufficient payment. Needed: " + total); return; }
        double change = round2(paid - total);

        try {
            Path receipt = writeReceipt(total, tax, paid, change);
            System.out.println("Payment received. Change: " + change);
            System.out.println("Receipt saved: " + receipt.toString());
            CART.clear();
        } catch (Exception e) {
            System.out.println("Receipt write error: " + e.getMessage());
        }
    }

    static Path writeReceipt(double total, double tax, double paid, double change) throws IOException {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path file = RECEIPTS_DIR.resolve("receipt_" + ts + ".txt");
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            w.write("=== SwiftPOS Receipt ===\n");
            w.write("Date: " + LocalDateTime.now() + "\n\n");
            w.write(String.format("%-6s | %-24s | %5s | %8s%n", "SKU","NAME","QTY","LINE"));
            w.write("---------------------------------------------------------\n");
            double subtotal = 0;
            for (var e : CART.entrySet()) {
                Product p = INVENTORY.get(e.getKey());
                int qty = e.getValue();
                double line = p.price * qty;
                subtotal += line;
                w.write(String.format("%-6s | %-24s | %5d | %8.2f%n",
                        p.sku, truncate(p.name,24), qty, line));
            }
            w.write("\n");
            w.write(String.format("Subtotal: %.2f%n", subtotal));
            w.write(String.format("Tax (8%%): %.2f%n", tax));
            w.write(String.format("TOTAL  : %.2f%n", total));
            w.write(String.format("Paid   : %.2f%n", paid));
            w.write(String.format("Change : %.2f%n", change));
            w.write("\nThank you!\n");
        }
        return file;
    }

    static String truncate(String s, int n) { return s.length() <= n ? s : s.substring(0, n-1) + "…"; }
    static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
