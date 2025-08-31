package com.linksplit.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {
    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = BASE62_ALPHABET.length();

    public String encode(long number) {
        if (number == 0) {
            return String.valueOf(BASE62_ALPHABET.charAt(0));
        }

        StringBuilder encoded = new StringBuilder();
        while (number > 0) {
            int remainder = (int) (number % BASE);
            encoded.insert(0, BASE62_ALPHABET.charAt(remainder));
            number = number / BASE;
        }

        return encoded.toString();
    }

    public long decode(String encoded) {
        long result = 0;
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            int digit = BASE62_ALPHABET.indexOf(c);
            if (digit == -1) {
                throw new IllegalArgumentException("Invalid character in encoded string: " + c);
            }
            result = result * BASE + digit;
        }
        return result;
    }
}