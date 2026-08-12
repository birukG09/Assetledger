import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

public class Main {
    interface HashStrategy {
        String hash(String input);
    }

    static class Sha256Strategy implements HashStrategy {
        public String hash(String input) {
            try {
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

    interface LedgerEntry {
        String getHash();
        String getPreviousHash();
    }

    static class Transaction implements LedgerEntry {
        static final String GENESIS_HASH = "0".repeat(64);
        final String tokenId, fromOwner, toOwner, previousHash, hash;

        Transaction(String tokenId, String from, String to, String previousHash, HashStrategy strategy) {
            this.tokenId = tokenId;
            this.fromOwner = from;
            this.toOwner = to;
            this.previousHash = previousHash;
            this.hash = strategy.hash(tokenId + from + to + Instant.now() + previousHash);
        }

        public String getHash() { return hash; }
        public String getPreviousHash() { return previousHash; }
    }

    static class Ledger {
        private final List<LedgerEntry> entries = new ArrayList<>();
        private final HashStrategy hashStrategy;

        Ledger(HashStrategy hashStrategy) {
            this.hashStrategy = hashStrategy;
        }

        void recordTransfer(String tokenId, String from, String to) {
            String prevHash = entries.isEmpty() ? Transaction.GENESIS_HASH : entries.get(entries.size() - 1).getHash();
            entries.add(new Transaction(tokenId, from, to, prevHash, hashStrategy));
        }

        boolean validateIntegrity() {
            String expected = Transaction.GENESIS_HASH;
            for (LedgerEntry entry : entries) {
                if (!entry.getPreviousHash().equals(expected)) return false;
                expected = entry.getHash();
            }
            return true;
        }

        int size() { return entries.size(); }
    }

    public static void main(String[] args) {
        Ledger ledger = new Ledger(new Sha256Strategy());
        ledger.recordTransfer("TOKEN-1", "Northstar", "Arcadia");
        ledger.recordTransfer("TOKEN-1", "Arcadia", "Vantage");

        System.out.println("Transactions: " + ledger.size());
        System.out.println("Chain valid: " + ledger.validateIntegrity());
    }
}
