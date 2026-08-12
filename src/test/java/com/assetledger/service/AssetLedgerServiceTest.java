import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

public class Main {
    record Asset(String type, BigDecimal value, String owner) {}

    record Token(UUID tokenId, Asset asset) {
        static Token create(Asset asset) {
            return new Token(UUID.randomUUID(), asset);
        }
    }

    record Transaction(UUID id, String tokenId, String fromOwner, String toOwner,
                        Instant timestamp, String previousHash, String hash) {

        static final String GENESIS_HASH = "0".repeat(64);

        static Transaction create(String tokenId, String from, String to, String previousHash) {
            Instant now = Instant.now();
            String hash = computeHash(tokenId, from, to, now, previousHash);
            return new Transaction(UUID.randomUUID(), tokenId, from, to, now, previousHash, hash);
        }

        static String computeHash(String tokenId, String from, String to, Instant ts, String prevHash) {
            try {
                String input = tokenId + from + to + ts + prevHash;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) sb.append(String.format("%02x", b));
                return sb.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class Ledger {
        private final List<Transaction> transactions = new ArrayList<>();

        String latestHash() {
            return transactions.isEmpty() ? Transaction.GENESIS_HASH : transactions.get(transactions.size() - 1).hash();
        }

        Transaction recordTransfer(String tokenId, String from, String to) {
            Transaction tx = Transaction.create(tokenId, from, to, latestHash());
            transactions.add(tx);
            return tx;
        }

        boolean validateIntegrity() {
            String expected = Transaction.GENESIS_HASH;
            for (Transaction tx : transactions) {
                if (!tx.previousHash().equals(expected)) return false;
                expected = tx.hash();
            }
            return true;
        }

        int size() {
            return transactions.size();
        }
    }

    static class Registry {
        private final Map<UUID, Token> tokens = new LinkedHashMap<>();
        private final Map<UUID, String> currentOwners = new HashMap<>();

        Token register(Asset asset) {
            Token token = Token.create(asset);
            tokens.put(token.tokenId(), token);
            currentOwners.put(token.tokenId(), asset.owner());
            return token;
        }

        String currentOwner(UUID tokenId) {
            return currentOwners.get(tokenId);
        }

        void setOwner(UUID tokenId, String newOwner) {
            currentOwners.put(tokenId, newOwner);
        }

        int size() {
            return tokens.size();
        }
    }

    public static void main(String[] args) {
        Registry registry = new Registry();
        Ledger ledger = new Ledger();

        Token equipment = registry.register(new Asset("EQUIPMENT", new BigDecimal("1500.00"), "Northstar"));
        Token invoice = registry.register(new Asset("INVOICE", new BigDecimal("2400.00"), "Arcadia"));

        transfer(registry, ledger, equipment.tokenId(), "Vantage");
        transfer(registry, ledger, invoice.tokenId(), "Northstar");
        transfer(registry, ledger, equipment.tokenId(), "Arcadia");

        System.out.println("Registered assets: " + registry.size());
        System.out.println("Ledger transactions: " + ledger.size());
        System.out.println("Chain valid: " + ledger.validateIntegrity());
    }

    static void transfer(Registry registry, Ledger ledger, UUID tokenId, String newOwner) {
        String currentOwner = registry.currentOwner(tokenId);
        if (currentOwner.equals(newOwner)) {
            throw new IllegalArgumentException("Cannot transfer to same owner");
        }
        ledger.recordTransfer(tokenId.toString(), currentOwner, newOwner);
        registry.setOwner(tokenId, newOwner);
    }
}
