package com.squareup.subzero.shared;

import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import static java.lang.String.format;

/**
 * Convert to/from Base45.
 *
 * QR codes can support the following encodings:
 * - base 10, numeric (0-9)
 * - base 45, alphanumeric
 * - binary
 * - kanji
 *
 * We picked Base45 because working with binary data is a pain. QR scanners can be configured to
 * emit ASCII sequences. We however risk having rendering issues (most QR code rendering libraries
 * take a string instead of a byte buffer as input). We would also need to support an alternate way
 * to manually type the code (for dev/debugging purpose or as a fallback).
 */
public final class Base45 {
  protected static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";
  private final static Object lock = new Object();
  protected static ImmutableMap<Character, Integer> REVERSE_CHARSET = null;

  /**
   * Fast reverse lookup using ImmutableMap.
   */
  protected static Integer indexOf(char c) {
    // The map needs to be initialized in a thread-safe way.
    synchronized (lock) {
      if (REVERSE_CHARSET == null) {
        HashMap<Character, Integer> map = new HashMap<>(45);
        for (int i = 0; i < 45; i++) {
          map.put(CHARSET.charAt(i), i);
        }
        REVERSE_CHARSET = ImmutableMap.copyOf(map);
      }
    }
    return REVERSE_CHARSET.get(c);
  }

  /**
   * Encodes a byte array to a Base45-encoded String.
   */
  public static String toBase45(byte[] input) {
    // input expands by approximately a factor of 1.5 (1.5 comes from log(256) / log(45))
    StringBuilder output = new StringBuilder((int)(input.length * 1.5) + 3);
    int acc = 0;
    int counter = 0;

    // consume the input buffer
    for (int i = 0; i < input.length; i++) {
      // convert signed byte to unsigned int
      int inputByte = (input[i] & 0xff);
      // read 2 bytes at a time
      acc = (acc << 8) | inputByte;
      counter++;

      if (counter == 2) {
        // emit acc and reset
        int s0 = (acc / 45) / 45;
        int s1 = (acc / 45) % 45;
        int s2 = (acc % 45);
        if (s0 >= 45) {
          throw new IllegalStateException(format("s0 should be <45, but is %d", s0));
        }
        output.append(CHARSET.charAt(s0));
        output.append(CHARSET.charAt(s1));
        output.append(CHARSET.charAt(s2));

        acc = 0;
        counter = 0;
      }
    }

    if (counter > 0) {
      // if we blindly emit the last value, we won't be able to tell even from odd length inputs
      // apart. So we emit 2 chars for odd length inputs.
      int s0 = acc / 45;
      int s1 = acc % 45;
      if (s0 >= 45) {
        throw new IllegalStateException(format("s0 should be <45, but is %d", s0));
      }
      output.append(CHARSET.charAt(s0));
      output.append(CHARSET.charAt(s1));
    }
    return output.toString();
  }

  /**
   * Decodes a Base45-encoded String back to a byte array.
   *
   * Throws IllegalArgumentException if the input contains invalid characters.
   */
  public static byte[] fromBase45(String input) {
    // input shrinks by a factor 1.5
    ByteArrayOutputStream output = new ByteArrayOutputStream((int) (input.length() / 1.5));
    int acc = 0;
    int counter = 0;

    // consume the input string
    for (int i = 0; i < input.length(); i++) {
      Integer inputValue = indexOf(input.charAt(i));
      if (inputValue == null) {
        throw new IllegalArgumentException(format("invalid char (%d) at offset %d", (int)input.charAt(i), i));
      }
      // read 3 chars at a time
      acc = acc * 45 + inputValue;
      counter++;
      if (counter == 3) {
        // emit acc and reset
        int s0 = acc / 256;
        int s1 = acc % 256;
        if (s0 >= 256) {
          throw new IllegalStateException(format("s0 should be <256, but is %d", s0));
        }
        output.write((byte) s0);
        output.write((byte) s1);

        acc = 0;
        counter = 0;
      }
    }

    if (counter > 0) {
      if (acc >= 256) {
        throw new IllegalStateException(format("acc should be <256, but is %d", acc));
      }
      output.write((byte) acc);
    }
    return output.toByteArray();
  }
}
