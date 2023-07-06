package com.squareup.subzero.shared;

import java.util.Random;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class Base45Test {
  @Test
  public void testCharset() {
    assertEquals(45, Base45.CHARSET.length());
  }

  @Test
  public void testEmptyArray() {
    byte[] buf1 = new byte[] {};
    assertArrayEquals(buf1, Base45.fromBase45(Base45.toBase45(buf1)));
  }

  @Test
  public void testAllOneByte() {
    byte[] buf1 = new byte[1];

    for (int i=0; i<256; i++) {
      buf1[0] = (byte)i;

      byte[] buf2 = Base45.fromBase45(Base45.toBase45(buf1));
      assertArrayEquals(buf1, buf2);
    }
  }

  @Test
  public void testAllTwoBytes() {
    byte[] buf1 = new byte[2];

    for (int i=0; i<256; i++) {
      buf1[0] = (byte)i;
      for (int j=0; j<256; j++) {
        buf1[1] = (byte)j;

        byte[] buf2 = Base45.fromBase45(Base45.toBase45(buf1));
        assertArrayEquals(buf1, buf2);
      }
    }
  }

  @Test
  public void testAllThreeBytes() {
    byte[] buf1 = new byte[3];

    for (int i=0; i<256; i++) {
      buf1[0] = (byte)i;
      for (int j=0; j<256; j++) {
        buf1[1] = (byte)j;
        for (int k=0; k<256; k++) {
          buf1[2] = (byte)k;

          byte[] buf2 = Base45.fromBase45(Base45.toBase45(buf1));
          assertArrayEquals(buf1, buf2);
        }
      }
    }
  }

  @Test
  public void testRandomBuffers() {
    // we create increasing long strings, up to 12k
    for (int l=0; l<12000; l++) {
      // Create a random string
      byte[] buf1 = new byte[l];
      new Random().nextBytes(buf1);

      byte[] buf2 = Base45.fromBase45(Base45.toBase45(buf1));
      assertArrayEquals(buf1, buf2);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidBase45() {
    Base45.fromBase45("foobar");
  }
}
