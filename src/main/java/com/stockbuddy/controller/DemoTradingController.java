//src/main/java/com/stockbuddy/controller/DemoTradingController.java

package com.stockbuddy.controller;

import com.stockbuddy.dto.TradeRequest;
import com.stockbuddy.dto.UpdateHoldingsRequest;
import com.stockbuddy.dto.HoldingUpdateItem;
import com.stockbuddy.model.DemoTradingAccount;
import com.stockbuddy.model.Holding;
import com.stockbuddy.model.Transaction;
import com.stockbuddy.model.User;
import com.stockbuddy.model.UserPreferences;
import com.stockbuddy.repository.DemoTradingAccountRepository;
import com.stockbuddy.repository.UserPreferencesRepository;
import com.stockbuddy.repository.UserRepository;
import com.stockbuddy.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.Locale;

@RestController
@RequestMapping("/api/demotrading")
public class DemoTradingController {

    @Autowired
    private DemoTradingAccountRepository tradingRepo;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserPreferencesRepository preferencesRepository;
    @Autowired
    private EmailService emailService;

    // ───────────────────────────────────────────────────────────────
    // GET /api/demotrading/account
    // ───────────────────────────────────────────────────────────────
    @GetMapping("/account")
    public ResponseEntity<?> getAccount(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        try {
            DemoTradingAccount account = tradingRepo.findByUserId(userId)
                    .orElseGet(() -> createDefaultAccount(userId));
            ensureAccountCollections(account);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Server error", "message", e.getMessage()));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // POST /api/demotrading/trade
    // ───────────────────────────────────────────────────────────────
    @PostMapping("/trade")
    public ResponseEntity<?> executeTrade(@RequestBody TradeRequest req,
                                          Authentication auth) {
        String userId = (String) auth.getPrincipal();

        // Validate request fields
        if (req.getSymbol() == null || req.getSymbol().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Symbol is required"));
        if (req.getCompanyName() == null || req.getCompanyName().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Company name is required"));
        if (!"BUY".equals(req.getType()) && !"SELL".equals(req.getType()))
            return ResponseEntity.badRequest().body(Map.of("error", "Trade type must be BUY or SELL"));
        if (req.getQuantity() < 1)
            return ResponseEntity.badRequest().body(Map.of("error", "Quantity must be at least 1"));
        if (req.getPrice() <= 0)
            return ResponseEntity.badRequest().body(Map.of("error", "Price must be a positive number"));

        try {
            DemoTradingAccount account = tradingRepo.findByUserId(userId)
                    .orElseGet(() -> createDefaultAccount(userId));
            ensureAccountCollections(account);

            String symbol      = normalizeSymbol(req.getSymbol());
            String companyName = req.getCompanyName();
            String type        = req.getType();
            int    quantity    = req.getQuantity();
            double price       = req.getPrice();
            double totalAmount = quantity * price;

            if ("BUY".equals(type)) {
                // ── Check sufficient balance ──────────────────────────────
                if (account.getBalance() < totalAmount) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error",     "Insufficient funds",
                            "available", account.getBalance(),
                            "required",  totalAmount));
                }

                account.setBalance(account.getBalance() - totalAmount);

                // Update or create holding
                Optional<Holding> existingOpt = account.getHoldings().stream()
                        .filter(h -> symbol.equals(normalizeSymbol(h.getSymbol())))
                        .findFirst();

                if (existingOpt.isPresent()) {
                    Holding h = existingOpt.get();
                    int    newQty       = h.getQuantity() + quantity;
                    double newTotalVal  = h.getPurchaseValue() + totalAmount;
                    double newAvgPrice  = newTotalVal / newQty;
                    double newCurVal    = newQty * price;
                    double profit       = newCurVal - newTotalVal;

                    h.setQuantity(newQty);
                    h.setAveragePrice(newAvgPrice);
                    h.setPurchaseValue(newTotalVal);
                    h.setCurrentPrice(price);
                    h.setCurrentValue(newCurVal);
                    h.setProfit(profit);
                    h.setProfitPercentage((profit / newTotalVal) * 100);
                    h.setLastUpdated(new Date());
                } else {
                    Holding h = new Holding();
                    h.setSymbol(symbol);
                    h.setCompanyName(companyName);
                    h.setQuantity(quantity);
                    h.setAveragePrice(price);
                    h.setPurchaseValue(totalAmount);
                    h.setCurrentPrice(price);
                    h.setCurrentValue(totalAmount);
                    h.setProfit(0);
                    h.setProfitPercentage(0);
                    h.setLastUpdated(new Date());
                    account.getHoldings().add(h);
                }

            } else { // SELL
                // ── Find holding ──────────────────────────────────────────
                int idx = -1;
                for (int i = 0; i < account.getHoldings().size(); i++) {
                    if (symbol.equals(normalizeSymbol(account.getHoldings().get(i).getSymbol()))) {
                        idx = i;
                        break;
                    }
                }
                if (idx == -1) {
                    return ResponseEntity.badRequest().body(
                            Map.of("error", "You do not own this stock"));
                }

                Holding h = account.getHoldings().get(idx);

                if (h.getQuantity() < quantity) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error",     "Insufficient shares",
                            "available", h.getQuantity(),
                            "required",  quantity));
                }

                account.setBalance(account.getBalance() + totalAmount);

                int newQty = h.getQuantity() - quantity;

                if (newQty == 0) {
                    account.getHoldings().remove(idx);
                } else {
                    double soldValue     = quantity * h.getAveragePrice();
                    double newPurchaseVal = h.getPurchaseValue() - soldValue;
                    double newCurVal      = newQty * price;
                    double profit         = newCurVal - newPurchaseVal;

                    h.setQuantity(newQty);
                    h.setPurchaseValue(newPurchaseVal);
                    h.setCurrentPrice(price);
                    h.setCurrentValue(newCurVal);
                    h.setProfit(profit);
                    h.setProfitPercentage((profit / newPurchaseVal) * 100);
                    h.setLastUpdated(new Date());
                }
            }

            // Add transaction record
            Transaction tx = new Transaction();
            tx.setSymbol(symbol);
            tx.setCompanyName(companyName);
            tx.setType(type);
            tx.setQuantity(quantity);
            tx.setPrice(price);
            // totalAmount is negative for BUY, positive for SELL (mirrors Node.js)
            tx.setTotalAmount("BUY".equals(type) ? -totalAmount : totalAmount);
            tx.setDate(new Date());
            account.getTransactions().add(tx);

            // Recalculate equity / profit-loss
            account.recalculate();
            tradingRepo.save(account);

            notifyTradeByEmail(userId, type, symbol, companyName, quantity, price, totalAmount, account);

            return ResponseEntity.ok(account);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Server error", "message", e.getMessage()));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // GET /api/demotrading/transactions
    // ───────────────────────────────────────────────────────────────
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        try {
            Optional<DemoTradingAccount> opt = tradingRepo.findByUserId(userId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("msg", "No trading account found"));
            }

            DemoTradingAccount acc = opt.get();
            ensureAccountCollections(acc);
            List<Transaction> sorted = new ArrayList<>(acc.getTransactions());
            sorted.sort((a, b) -> b.getDate().compareTo(a.getDate()));

            return ResponseEntity.ok(sorted);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Server error", "message", e.getMessage()));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // PUT /api/demotrading/holdings/update
    // ───────────────────────────────────────────────────────────────
    @PutMapping("/holdings/update")
    public ResponseEntity<?> updateHoldings(@RequestBody UpdateHoldingsRequest req,
                                            Authentication auth) {
        String userId = (String) auth.getPrincipal();

        if (req.getHoldings() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Holdings data is required"));
        }

        try {
            Optional<DemoTradingAccount> opt = tradingRepo.findByUserId(userId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("msg", "No trading account found"));
            }

            DemoTradingAccount account = opt.get();
            ensureAccountCollections(account);

            for (HoldingUpdateItem update : req.getHoldings()) {
                String uSym = normalizeSymbol(update.getSymbol());
                account.getHoldings().stream()
                        .filter(h -> uSym.equals(normalizeSymbol(h.getSymbol())))
                        .findFirst()
                        .ifPresent(h -> {
                            h.setCurrentPrice(update.getCurrentPrice());
                            h.setCurrentValue(h.getQuantity() * update.getCurrentPrice());
                            h.setProfit(h.getCurrentValue() - h.getPurchaseValue());
                            h.setProfitPercentage(
                                    (h.getProfit() / h.getPurchaseValue()) * 100);
                            h.setLastUpdated(new Date());
                        });
            }

            account.recalculate();
            tradingRepo.save(account);

            return ResponseEntity.ok(account);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Server error", "message", e.getMessage()));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // POST /api/demotrading/reset
    // ───────────────────────────────────────────────────────────────
    @PostMapping("/reset")
    public ResponseEntity<?> resetAccount(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        try {
            DemoTradingAccount account = tradingRepo.findByUserId(userId)
                    .orElseGet(() -> createDefaultAccount(userId));

            account.setBalance(account.getInitialBalance());
            account.setHoldings(new ArrayList<>());
            account.setTransactions(new ArrayList<>());
            account.setEquity(account.getInitialBalance());
            account.setTotalProfitLoss(0);
            account.setTotalProfitLossPercentage(0);
            account.setLastUpdated(new Date());

            tradingRepo.save(account);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Trading account reset successfully",
                    "account", account));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Server error", "message", e.getMessage()));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // GET /api/demotrading/portfolio-history
    // ───────────────────────────────────────────────────────────────
    @GetMapping("/portfolio-history")
    public ResponseEntity<?> getPortfolioHistory(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        try {
            Optional<DemoTradingAccount> opt = tradingRepo.findByUserId(userId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("msg", "No trading account found"));
            }

            DemoTradingAccount account = opt.get();
            ensureAccountCollections(account);
            List<Transaction> transactions = account.getTransactions();
            Date now = new Date();

            // No transactions — return single snapshot
            if (transactions.isEmpty()) {
                double equity = account.getEquity() > 0
                        ? account.getEquity() : account.getInitialBalance();
                Map<String, Object> snap = new LinkedHashMap<>();
                snap.put("date",                 now);
                snap.put("equity",               equity);
                snap.put("balance",              account.getBalance());
                snap.put("holdingsValue",        equity - account.getBalance());
                snap.put("dayChange",            0);
                snap.put("dayChangePercentage",  0);
                snap.put("weekChange",           0);
                snap.put("weekChangePercentage", 0);
                snap.put("monthChange",          0);
                snap.put("monthChangePercentage",0);
                snap.put("yearChange",           0);
                snap.put("yearChangePercentage", 0);
                return ResponseEntity.ok(List.of(snap));
            }

            // Sort transactions oldest → newest
            List<Transaction> sorted = new ArrayList<>(transactions);
            sorted.sort(Comparator.comparing(Transaction::getDate));

            // Build daily snapshots by replaying transactions
            List<Map<String, Object>> dailySnapshots = new ArrayList<>();
            double currentBalance = account.getInitialBalance();
            List<Map<String, Object>> currentHoldings = new ArrayList<>();

            // Initial snapshot at account creation
            Map<String, Object> initSnap = new LinkedHashMap<>();
            initSnap.put("date",          account.getCreatedAt());
            initSnap.put("equity",        account.getInitialBalance());
            initSnap.put("balance",       account.getInitialBalance());
            initSnap.put("holdingsValue", 0.0);
            initSnap.put("holdings",      new ArrayList<>());
            dailySnapshots.add(initSnap);

            Date lastDate = account.getCreatedAt();

            for (Transaction tx : sorted) {
                String txSym = normalizeSymbol(tx.getSymbol());
                if ("BUY".equals(tx.getType())) {
                    currentBalance += tx.getTotalAmount(); // negative for BUY

                    // Find existing holding (symbols normalized for replay)
                    Optional<Map<String, Object>> existH = currentHoldings.stream()
                            .filter(h -> txSym.equals(normalizeSymbol(String.valueOf(h.get("symbol")))))
                            .findFirst();

                    if (existH.isPresent()) {
                        Map<String, Object> h = existH.get();
                        int    newQty   = (int) h.get("quantity") + tx.getQuantity();
                        double avgCost  = (double) h.get("averageCost");
                        double qty      = (double)(int) h.get("quantity");
                        double newTotal = avgCost * qty - tx.getTotalAmount();
                        h.put("quantity",     newQty);
                        h.put("averageCost",  newTotal / newQty);
                        h.put("currentPrice", tx.getPrice());
                        h.put("currentValue", newQty * tx.getPrice());
                    } else {
                        Map<String, Object> h = new LinkedHashMap<>();
                        h.put("symbol",       txSym);
                        h.put("companyName",  tx.getCompanyName());
                        h.put("quantity",     tx.getQuantity());
                        h.put("averageCost",  tx.getPrice());
                        h.put("currentPrice", tx.getPrice());
                        h.put("currentValue", tx.getQuantity() * tx.getPrice());
                        currentHoldings.add(h);
                    }

                } else if ("SELL".equals(tx.getType())) {
                    currentBalance += tx.getTotalAmount(); // positive for SELL

                    Iterator<Map<String, Object>> it = currentHoldings.iterator();
                    while (it.hasNext()) {
                        Map<String, Object> h = it.next();
                        if (!txSym.equals(normalizeSymbol(String.valueOf(h.get("symbol"))))) {
                            continue;
                        }
                        int newQty = (int) h.get("quantity") - tx.getQuantity();
                        if (newQty <= 0) {
                            it.remove();
                        } else {
                            h.put("quantity", newQty);
                            h.put("currentPrice", tx.getPrice());
                            h.put("currentValue", newQty * tx.getPrice());
                        }
                        break;
                    }
                }

                double holdingsValue = currentHoldings.stream()
                        .mapToDouble(h -> (double) h.get("currentValue"))
                        .sum();
                double equity = currentBalance + holdingsValue;

                Map<String, Object> snap = new LinkedHashMap<>();
                snap.put("date",          tx.getDate());
                snap.put("equity",        equity);
                snap.put("balance",       currentBalance);
                snap.put("holdingsValue", holdingsValue);
                snap.put("holdings",      new ArrayList<>(currentHoldings));
                dailySnapshots.add(snap);

                lastDate = tx.getDate();
            }

            // Add current snapshot if time has passed since last transaction
            long daysSinceLast = (now.getTime() - lastDate.getTime()) / (1000 * 60 * 60 * 24);
            if (daysSinceLast > 0) {
                double holdingsValue = account.getHoldings().stream()
                        .mapToDouble(Holding::getCurrentValue).sum();
                Map<String, Object> curSnap = new LinkedHashMap<>();
                curSnap.put("date",          now);
                curSnap.put("equity",        account.getEquity());
                curSnap.put("balance",       account.getBalance());
                curSnap.put("holdingsValue", holdingsValue);
                curSnap.put("holdings",      account.getHoldings());
                dailySnapshots.add(curSnap);
            }

            // ── Performance calculations ──────────────────────────────────
            Date oneDayAgo   = daysAgo(now, 1);
            Date oneWeekAgo  = daysAgo(now, 7);
            Date oneMonthAgo = monthsAgo(now, 1);
            Date oneYearAgo  = yearsAgo(now, 1);

            Map<String, Object> current = dailySnapshots.get(dailySnapshots.size() - 1);
            double currentEquity = (double) current.get("equity");

            double dayEquity   = findClosestEquity(dailySnapshots, oneDayAgo);
            double weekEquity  = findClosestEquity(dailySnapshots, oneWeekAgo);
            double monthEquity = findClosestEquity(dailySnapshots, oneMonthAgo);
            double yearEquity  = findClosestEquity(dailySnapshots, oneYearAgo);

            double[] day   = change(dayEquity,   currentEquity);
            double[] week  = change(weekEquity,  currentEquity);
            double[] month = change(monthEquity, currentEquity);
            double[] year  = change(yearEquity,  currentEquity);
            double[] total = change(account.getInitialBalance(), currentEquity);

            Map<String, Object> performance = new LinkedHashMap<>();
            performance.put("current", Map.of(
                    "date", current.get("date"),
                    "equity", currentEquity,
                    "balance", current.get("balance"),
                    "holdingsValue", current.get("holdingsValue")));
            performance.put("day",   Map.of("change", day[0],   "percentage", day[1]));
            performance.put("week",  Map.of("change", week[0],  "percentage", week[1]));
            performance.put("month", Map.of("change", month[0], "percentage", month[1]));
            performance.put("year",  Map.of("change", year[0],  "percentage", year[1]));
            performance.put("total", Map.of("change", total[0], "percentage", total[1]));

            // Persist dayChange to account
            account.setDayChange(day[0]);
            account.setDayChangePercentage(day[1]);
            tradingRepo.save(account);

            return ResponseEntity.ok(Map.of(
                    "history",     dailySnapshots,
                    "performance", performance));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Server error", "message", e.getMessage()));
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────

    private DemoTradingAccount createDefaultAccount(String userId) {
        DemoTradingAccount account = new DemoTradingAccount();
        account.setUserId(userId);
        account.setBalance(100000.0);
        account.setInitialBalance(100000.0);
        account.setEquity(100000.0);
        account.setHoldings(new ArrayList<>());
        account.setTransactions(new ArrayList<>());
        account.setCreatedAt(new Date());
        account.setLastUpdated(new Date());
        return tradingRepo.save(account);
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return "";
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static void ensureAccountCollections(DemoTradingAccount account) {
        if (account.getHoldings() == null) {
            account.setHoldings(new ArrayList<>());
        }
        if (account.getTransactions() == null) {
            account.setTransactions(new ArrayList<>());
        }
    }

    private void notifyTradeByEmail(String userId, String type, String symbol, String companyName,
                                    int quantity, double price, double totalAmount, DemoTradingAccount account) {
        try {
            UserPreferences prefs = preferencesRepository.findByUserId(userId).orElse(null);
            if (prefs == null || !prefs.isEmailNotifications()) {
                return;
            }
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return;
            }
            String email = userOpt.get().getEmail();
            if (email == null || email.isBlank()) {
                return;
            }
            emailService.sendDemoTradeEmail(email.trim(), type, symbol, companyName, quantity, price, totalAmount, account);
        } catch (Exception ignored) {
        }
    }

    private double findClosestEquity(List<Map<String, Object>> snapshots, Date target) {
        Map<String, Object> closest = snapshots.get(0);
        long minDiff = Math.abs(((Date) closest.get("date")).getTime() - target.getTime());

        for (Map<String, Object> snap : snapshots) {
            long diff = Math.abs(((Date) snap.get("date")).getTime() - target.getTime());
            if (diff < minDiff) {
                minDiff = diff;
                closest = snap;
            }
        }
        return (double) closest.get("equity");
    }

    private double[] change(double oldVal, double newVal) {
        if (oldVal == 0) return new double[]{0, 0};
        double c = newVal - oldVal;
        double p = (c / oldVal) * 100;
        return new double[]{c, p};
    }

    private Date daysAgo(Date from, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(from);
        c.add(Calendar.DAY_OF_YEAR, -days);
        return c.getTime();
    }

    private Date monthsAgo(Date from, int months) {
        Calendar c = Calendar.getInstance();
        c.setTime(from);
        c.add(Calendar.MONTH, -months);
        return c.getTime();
    }

    private Date yearsAgo(Date from, int years) {
        Calendar c = Calendar.getInstance();
        c.setTime(from);
        c.add(Calendar.YEAR, -years);
        return c.getTime();
    }
}
