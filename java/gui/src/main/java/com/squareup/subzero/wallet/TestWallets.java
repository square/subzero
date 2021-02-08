package com.squareup.subzero.wallet;

/**
 * Hardcoded wallets for blackbox regression testing of subzero transaction signing
 * <p>
 * Instead of loading a test wallet from a file, we have it loaded from this class
 */

public final class TestWallets {
  // Test wallet 1492, share 1
  protected static final String devTestWallet =
      "{\"currency\":\"TEST_NET\",\"encrypted_master_seed\":{\"encrypted_master_seed\":" +
          "\"pvG4sZJ5bbAdbFzfDmwJ3Ebh8EKju7VNTGOvp0EPPOmPURQfH3KDkkdpkGMDD0AJzybVQ9C4pA" +
          "ze1HWLtQk7yQ==\"},\"encrypted_pub_keys\":[{\"encrypted_pub_key\":\"3trfyO6S/" +
          "JOexdnE79rCnN7L65ndzczPnen4mP/w//jFztDgkpv658vm583hwdDo3s6dmN7/7vqe7sSezZney" +
          "Jv47p/c5PrmyeycwN/B5uftzMDLnsubwJPEz+/b693dzc+SmNzky8Lyn8OZwfD5\"},{\"encryp" +
          "ted_pub_key\":\"3trfyO6S2eTZ5p3c5M3a7J3inMXZm+346JLinp+Sx8n7wvjPxfLo6d3i3ODY" +
          "xJzg4Of56+zv4v7ywf+f0tzFmPLs2cjY8P/aw+vT2NDcmJnCxfvD2sCdm5zzn+zk+s7w++La5sDH" +
          "057B2tzLzt6e\"},{\"encrypted_pub_key\":\"3trfyO6T4emS08ji7eDJwNjC6NjL/f/kksf" +
          "J+8XO/eyYw9Dmx/DZzemd7v/pzNL56MDk4P7dwtPa2Nv985P4m/r6+82bxMzN3ebc4pz73Nn42/v" +
          "tw5zSn/+Z6O/DnpLs+pPM4cCc/fjgzsvn/djF\"},{\"encrypted_pub_key\":\"3trfyO6Swu" +
          "fA8/CYyf7/x+7c4eTY4MDp+cDk/8fDwN7828vEnc3s+834w/36ntrr7e3dkvzTw+HB3NvH096ZnJ" +
          "/++ZvL8sLh0Jz6x+bm2d6S5NvT6/Ke3MXm8ML4zcjc+/zZk/ve8pjCxZPnmNzM\"}]}";

  // TODO: Implement test wallet for nChipher testing
  protected static final String ncipherTestWallet = "TO BE IMPLEMENTED";
}
