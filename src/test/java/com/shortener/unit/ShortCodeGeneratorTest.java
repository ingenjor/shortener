package com.shortener.unit;

import com.shortener.core.service.ShortCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ShortCodeGeneratorTest {
    private ShortCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ShortCodeGenerator(
                ShortCodeGenerator.Algorithm.BASE62, 7
        );
        generator.clearCache();
    }

    @Test
    void testGenerateCode_UniqueForDifferentUsers() {
        String url = "https://example.com";
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        String code1 = generator.generateCode(url, user1);
        String code2 = generator.generateCode(url, user2);

        assertNotNull(code1);
        assertNotNull(code2);
        assertEquals(7, code1.length());
        assertEquals(7, code2.length());
        assertNotEquals(code1, code2, "Different users should get different codes for same URL");
    }

    @Test
    void testGenerateCode_SameUserSameUrl_ReturnsSameCode() {
        String url = "https://example.com";
        UUID user = UUID.randomUUID();

        String code1 = generator.generateCode(url, user);
        String code2 = generator.generateCode(url, user);

        assertEquals(code1, code2, "Same user should get same code for same URL");
    }

    @Test
    void testGenerateCode_Uniqueness() {
        Set<String> codes = new HashSet<>();
        UUID user = UUID.randomUUID();

        for (int i = 0; i < 1000; i++) {
            String url = "https://example.com/" + i;
            String code = generator.generateCode(url, user);
            assertTrue(codes.add(code), "Duplicate code generated: " + code);
        }
    }

    @Test
    void testDifferentAlgorithms() {
        ShortCodeGenerator randomGen = new ShortCodeGenerator(
                ShortCodeGenerator.Algorithm.RANDOM, 7
        );
        ShortCodeGenerator hashGen = new ShortCodeGenerator(
                ShortCodeGenerator.Algorithm.HASH, 7
        );

        String url = "https://example.com";
        UUID user = UUID.randomUUID();

        String randomCode = randomGen.generateCode(url, user);
        String hashCode = hashGen.generateCode(url, user);

        assertEquals(7, randomCode.length());
        assertEquals(7, hashCode.length());
        assertNotEquals(randomCode, hashCode);
    }

    @Test
    void testGenerateCode_InvalidInput() {
        String url = "https://example.com";
        UUID user = UUID.randomUUID();

        assertThrows(NullPointerException.class, () ->
                generator.generateCode(null, user)
        );

        // тестируем с не-null параметрами
        assertDoesNotThrow(() -> generator.generateCode(url, UUID.randomUUID()));
    }
}
