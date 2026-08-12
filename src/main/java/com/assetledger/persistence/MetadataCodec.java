package com.assetledger.persistence;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class MetadataCodec {
    private MetadataCodec() {
    }

    static String encode(Map<String, String> metadata) {
        return metadata.entrySet().stream()
                .map(entry -> encodePart(entry.getKey()) + "=" + encodePart(entry.getValue()))
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
    }

    static Map<String, String> decode(String encoded) {
        Map<String, String> metadata = new LinkedHashMap<>();
        if (encoded == null || encoded.isBlank()) {
            return metadata;
        }
        for (String pair : encoded.split(";")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                metadata.put(decodePart(parts[0]), decodePart(parts[1]));
            }
        }
        return metadata;
    }

    private static String encodePart(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decodePart(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}