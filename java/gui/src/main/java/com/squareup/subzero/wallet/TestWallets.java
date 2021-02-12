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
          "\"ioBg3WF2BntMnGaebOtI+HDcTHVkaMonIplOcp+6i83P9Cjb6r7+5T9KDrlN8Np9MyIm6vh74M" +
          "348X7oJMkCG6cY5endbgf/sofKn5OcpOw86+rtI8fCAyLNddM=\"},\"encrypted_pub_keys\"" +
          ":[{\"encrypted_pub_key\":\"OytXbV6n2L0l50yLa5fXe0z8N84MU1Ci3jQyOy87Id1zMJCG3" +
          "3Wqg5rVv6KJED8fmjg8s0rsMJaiCe9o47OHrFg9SH37SxIa4IGVU3WEjuFVTOAxFbOJkY0gE2XlS" +
          "zGUtO+MGzvcayS4JSqDdsRHN2pKBvJDDi92oxMMpi0C8uuu9vUiIYa9IA8f5g==\"},{\"encryp" +
          "ted_pub_key\":\"YxgmbmaiwGON1uHpO1U51f9BcZAruJn436kjCZZxQBXqbcxJcd2L0k7HNDhG" +
          "K7Q7g+bbqKnhEoS+Cqjz4FqSv+yGmfzLqwvy9g/XIu44B37VSXLh5bsyBl1hh8sljNVIwbYWRWjh" +
          "iCGE57g0P2o2Uj63cBE9wRpwKUa6RTlRWb2Mmw3GnL+xn9n0Bw==\"},{\"encrypted_pub_key" +
          "\":\"i36ne27C7pv1psRF7iF3FtA9D+DYzL9dGLNG2al96iZYOhXW3fWxB1WmgapH4Qjwp32cNos" +
          "GJ5xIqPGcOebe/lUYfJy9nAJktqNb+9m9cIXRAEBR2Aem07Y3iVBPXwbrsEGrwaIlghXSU35d8s3" +
          "nGgmJHpb/+ILUK+XipCW6f6laVM8jC39lq8od5Q==\"},{\"encrypted_pub_key\":\"s/1O2n" +
          "YnApJd+MlcGpLARYL7pudrwDSrF/xLc6Lj5WgaKquFlJFFlTblYtOSyA8O7jiz253gvzFmK1oVmr" +
          "2hqw7rW0kAliBzpQ/sbiZnQYHnGVw5582vRTmixQmJD7EtlOXsuY3zcniZZjJm6oMRMxO0QGQVme" +
          "wgW3jyfnB8ed5Nh3jCTs/eMwWWNQ==\"}]}";

  // TODO: Implement test wallet for nChipher testing
  protected static final String ncipherTestWallet = "TO BE IMPLEMENTED";
}
