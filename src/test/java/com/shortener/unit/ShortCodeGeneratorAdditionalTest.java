package com.shortener.unit;

import com.shortener.core.service.ShortCodeGenerator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ShortCodeGeneratorAdditionalTest {

    @Test
    void testGenerateCodeWithNullInput() {
        ShortCodeGenerator generator = new ShortCodeGenerator(
                ShortCodeGenerator.Algorithm.BASE62, 7
        );

        UUID userId = UUID.randomUUID();

        assertThrows(NullPointerException.class, () ->
                generator.generateCode(null, userId)
        );

        assertThrows(NullPointerException.class, () ->
                generator.generateCode("https://example.com", null)
        );
    }

    @Test
    void testDifferentAlgorithmsProduceDifferentCodes() {
        ShortCodeGenerator randomGen = new ShortCodeGenerator(
                ShortCodeGenerator.Algorithm.RANDOM, 7
        );
        ShortCodeGenerator base62Gen = new ShortCodeGenerator(
                ShortCodeGenerator.Algorithm.BASE62, 7
        );
        ShortCodeGenerator hashGen = new ShortCodeGenerator(
                ShortCodeGenerator.Algorithm.HASH, 7
        );

        String url = "https://example.com/path?query=value";
        UUID userId = UUID.randomUUID();

        String randomCode = randomGen.generateCode(url, userId);
        String base62Code = base62Gen.generateCode(url, userId);
        String hashCode = hashGen.generateCode(url, userId);

        assertEquals(7, randomCode.length());
        assertEquals(7, base62Code.length());
        assertEquals(7, hashCode.length());

        // Codes should be different for different algorithms
        assertNotEquals(randomCode, base62Code);
        assertNotEquals(randomCode, hashCode);
        assertNotEquals(base62Code, hashCode);
    }

    @Test
    void testGenerateCodeWithVeryLongUrl() {
        ShortCodeGenerator generator = new ShortCodeGenerator(
                ShortCodeGenerator.Algorithm.BASE62, 10
        );

        StringBuilder longUrl = new StringBuilder("https://example.com/");
        for (int i = 0; i < 1000; i++) {
            longUrl.append("verylongpath/");
        }
        longUrl.append("end");

        UUID userId = UUID.randomUUID();

        String code = generator.generateCode(longUrl.toString(), userId);

        assertEquals(10, code.length());
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    void testClearCache() {
        ShortCodeGenerator generator = new ShortCodeGenerator(
                ShortCodeGenerator.Algorithm.BASE62, 7
        );

        String url = "https://example.com";
        UUID userId = UUID.randomUUID();

        String code1 = generator.generateCode(url, userId);

        // Same URL and user should return same code
        String code2 = generator.generateCode(url, userId);
        assertEquals(code1, code2);

        // Clear cache and generate again - should be different or same?
        generator.clearCache();
        String code3 = generator.generateCode(url, userId);

        // After clearing cache, might get same or different code
        // Just ensure it's generated without errors
        assertEquals(7, code3.length());
    }

    @Test
    void testGenerateCodeWithSpecialCharactersInUrl() {
        ShortCodeGenerator generator = new ShortCodeGenerator(
                ShortCodeGenerator.Algorithm.BASE62, 8
        );

        String[] urls = {
                "https://example.com/path-with-dashes",
                "https://example.com/path_with_underscores",
                "https://example.com/path%20with%20spaces",
                "https://example.com/path?param=value&other=something",
                "https://example.com/#anchor"
        };

        UUID userId = UUID.randomUUID();

        for (String url : urls) {
            String code = generator.generateCode(url, userId);
            assertNotNull(code);
            assertEquals(8, code.length());
            assertTrue(code.matches("^[a-zA-Z0-9]+$"));
        }
    }
}
