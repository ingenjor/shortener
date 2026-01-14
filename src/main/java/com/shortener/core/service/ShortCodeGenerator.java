package com.shortener.core.service;

import org.apache.commons.lang3.RandomStringUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ShortCodeGenerator {
    private static final String BASE62_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int DEFAULT_CODE_LENGTH = 7;

    private final Map<String, String> urlUserCache = new HashMap<>();
    private final Set<String> generatedCodes = new HashSet<>();

    public enum Algorithm {
        RANDOM,
        BASE62,
        HASH
    }

    private final Algorithm algorithm;
    private final int codeLength;

    public ShortCodeGenerator(Algorithm algorithm, int codeLength) {
        this.algorithm = algorithm;
        this.codeLength = codeLength > 0 ? codeLength : DEFAULT_CODE_LENGTH;
    }

    public String generateCode(String input, UUID userId) {
        // Добавляем проверки на null
        if (input == null) {
            throw new NullPointerException("input cannot be null");
        }
        if (userId == null) {
            throw new NullPointerException("userId cannot be null");
        }

        String cacheKey = userId.toString() + ":" + input;

        if (urlUserCache.containsKey(cacheKey)) {
            return urlUserCache.get(cacheKey);
        }

        String code;
        int attempts = 0;
        int maxAttempts = 100;

        do {
            if (attempts++ > maxAttempts) {
                throw new RuntimeException("Failed to generate unique code after " + maxAttempts + " attempts");
            }

            String uniqueInput = input + ":" + userId.toString() + ":" + attempts;

            switch (algorithm) {
                case RANDOM:
                    code = generateRandomCode();
                    break;
                case BASE62:
                    code = generateBase62Code(uniqueInput);
                    break;
                case HASH:
                    code = generateHashCode(uniqueInput);
                    break;
                default:
                    code = generateRandomCode();
            }
        } while (generatedCodes.contains(code));

        generatedCodes.add(code);
        urlUserCache.put(cacheKey, code);

        return code;
    }

    private String generateRandomCode() {
        return RandomStringUtils.randomAlphanumeric(codeLength);
    }

    private String generateBase62Code(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, digest);

            StringBuilder code = new StringBuilder();
            while (number.compareTo(BigInteger.ZERO) > 0 && code.length() < codeLength) {
                BigInteger[] divmod = number.divideAndRemainder(BigInteger.valueOf(62));
                code.insert(0, BASE62_CHARS.charAt(divmod[1].intValue()));
                number = divmod[0];
            }

            while (code.length() < codeLength) {
                code.append(BASE62_CHARS.charAt(
                        (int) (Math.random() * BASE62_CHARS.length())
                ));
            }

            return code.substring(0, codeLength);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    private String generateHashCode(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return encoded.substring(0, Math.min(codeLength, encoded.length()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public void clearCache() {
        generatedCodes.clear();
        urlUserCache.clear();
    }
}
