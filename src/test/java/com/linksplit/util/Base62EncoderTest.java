package com.linksplit.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {
    
    private Base62Encoder encoder;
    
    @BeforeEach
    void setUp() {
        encoder = new Base62Encoder();
    }
    
    @Test
    @DisplayName("Should encode zero correctly")
    void testEncodeZero() {
        String result = encoder.encode(0);
        assertEquals("0", result);
    }
    
    @Test
    @DisplayName("Should encode small numbers correctly")
    void testEncodeSmallNumbers() {
        assertEquals("1", encoder.encode(1));
        assertEquals("9", encoder.encode(9));
        assertEquals("A", encoder.encode(10));
        assertEquals("Z", encoder.encode(35));
        assertEquals("a", encoder.encode(36));
        assertEquals("z", encoder.encode(61));
    }
    
    @Test
    @DisplayName("Should encode large numbers correctly")
    void testEncodeLargeNumbers() {
        assertEquals("10", encoder.encode(62));
        assertEquals("100", encoder.encode(3844));
        String encoded = encoder.encode(123456789);
        assertNotNull(encoded);
        assertTrue(encoded.length() > 0);
    }
    
    @Test
    @DisplayName("Should decode encoded values correctly")
    void testDecodeEncodedValues() {
        long[] testValues = {0, 1, 10, 61, 62, 100, 1000, 123456789, Long.MAX_VALUE / 2};
        
        for (long value : testValues) {
            String encoded = encoder.encode(value);
            long decoded = encoder.decode(encoded);
            assertEquals(value, decoded, "Failed for value: " + value);
        }
    }
    
    @Test
    @DisplayName("Should handle round-trip encoding and decoding")
    void testRoundTrip() {
        for (int i = 0; i < 10000; i += 100) {
            String encoded = encoder.encode(i);
            long decoded = encoder.decode(encoded);
            assertEquals(i, decoded);
        }
    }
    
    @Test
    @DisplayName("Should throw exception for invalid characters in decode")
    void testDecodeInvalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> encoder.decode("!@#"));
        assertThrows(IllegalArgumentException.class, () -> encoder.decode("abc-def"));
        assertThrows(IllegalArgumentException.class, () -> encoder.decode(" "));
    }
    
    @Test
    @DisplayName("Should handle maximum long value")
    void testMaxLongValue() {
        String encoded = encoder.encode(Long.MAX_VALUE);
        assertNotNull(encoded);
        long decoded = encoder.decode(encoded);
        assertEquals(Long.MAX_VALUE, decoded);
    }
}