import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.function.UnaryOperator;

public class Main {
    record Asset(String type, BigDecimal value, String owner) {}
    record Token(UUID tokenId, Asset asset) {}
    record Transaction(UUID id, String tokenId, String fromOwner, String toOwner,
                        Instant timestamp, String previousHash, String hash) {
        static final String GENESIS_HASH = "0".repeat(64);
    }

    static final UnaryOperator<String> sha256 = input -> {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    static Transaction transferFn(String tokenId, String from, String to, String prevHash) {
        Instant now = Instant.now();
        String hash = sha256.apply(tokenId + from + to + now + prevHash);
        return new Transaction(UUID.randomUUID(), tokenId, from, to, now, prevHash, hash);
    }

    static List<Transaction> appendTransfer(List<Transaction> chain, String tokenId, String from, String to) {
        String prevHash = chain.isEmpty() ? Transaction.GENESIS_HASH : chain.get(chain.size() - 1).hash();
        Transaction tx = transferFn(tokenId, from, to, prevHash);
        List<Transaction> updated = new ArrayList<>(chain);
        updated.add(tx);
        return List.copyOf(updated);
    }

    static boolean isChainValid(List<Transaction> chain) {
        String[] expected = { Transaction.GENESIS_HASH };
        return chain.stream().allMatch(tx -> {
            boolean valid = tx.previousHash().equals(expected[0]);
            expected[0] = tx.hash();
            return valid;
        });
    }

    public static void main(String[] args) {
        Asset asset = new Asset("EQUIPMENT", new BigDecimal("1500.00"), "Northstar");
        Token token = new Token(UUID.randomUUID(), asset);

        List<Transaction> chain = List.of();
        chain = appendTransfer(chain, token.tokenId().toString(), "Northstar", "Arcadia");
        chain = appendTransfer(chain, token.tokenId().toString(), "Arcadia", "Vantage");

        System.out.println("Transactions: " + chain.size());
        System.out.println("Chain valid: " + isChainValid(chain));
    }
}
