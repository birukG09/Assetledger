import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

public class Main {
    static class Asset {
        private final String type;
        private final BigDecimal value;
        private final String owner;

        public Asset(String type, BigDecimal value, String owner) {
            this.type = type;
            this.value = value;
            this.owner = owner;
        }

        public String getType() { return type; }
        public BigDecimal getValue() { return value; }
        public String getOwner() { return owner; }
    }

    static class Token {
        private final UUID tokenId;
        private final Asset asset;

        public Token(Asset asset) {
            this.tokenId = UUID.randomUUID();
            this.asset = asset;
        }

        public UUID getTokenId() { return tokenId; }
        public Asset getAsset() { return asset; }
    }

    static class Transaction {
        static final String GENESIS_HASH = "0".repeat(64);
        private final UUID id;
        private final String tokenId;
        private final String fromOwner;
        private final String toOwner;
        private final Instant timestamp;
        private final String previousHash;
        private final String hash;

        public Transaction(String tokenId, String fromOwner, String toOwner, String previousHash) {
            this.id = UUID.randomUUID();
            this.tokenId = tokenId;
            this.fromOwner = fromOwner;
            this.toOwner = toOwner;
            this.timestamp = Instant.now();
            this.previousHash = previousHash;
            this.hash = computeHash();
        }

        private String computeHash() {
            try {
                String input = tokenId + fromOwner + toOwner + timestamp + previousHash;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) sb.append(String.format("%02x", b));
                return sb.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public String getHash() { return hash; }
        public String getPreviousHash() { return previousHash; }
    }

    static class Ledger {
        private final List<Transaction> transactions = new ArrayList<>();

        public Transaction recordTransfer(String tokenId, String from, String to) {
            String prevHash = transactions.isEmpty() ? Transaction.GENESIS_HASH
                    : transactions.get(transactions.size() - 1).getHash();
            Transaction tx = new Transaction(tokenId, from, to, prevHash);
            transactions.add(tx);
            return tx;
        }

        public boolean validateIntegrity() {
            String expected = Transaction.GENESIS_HASH;
            for (Transaction tx : transactions) {
                if (!tx.getPreviousHash().equals(expected)) return false;
                expected = tx.getHash();
            }
            return true;
        }

        public int size() { return transactions.size(); }
    }

    public static void main(String[] args) {
        Asset asset = new Asset("EQUIPMENT", new BigDecimal("1500.00"), "Northstar");
        Token token = new Token(asset);
        Ledger ledger = new Ledger();

        ledger.recordTransfer(token.getTokenId().toString(), "Northstar", "Arcadia");
        ledger.recordTransfer(token.getTokenId().toString(), "Arcadia", "Vantage");

        System.out.println("Transactions: " + ledger.size());
        System.out.println("Chain valid: " + ledger.validateIntegrity());
    }
}
