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
          "\"ioBg3WF2BntMnGae6PyWbp1VG4r446PUYVZnt1BzOOVQzHy3XeaqmBXS6tMbE9fsB0sR+Vi9xP" +
          "gJcayN2uJsJNjEw7S77h9oUUpu0zWrYvl6iRAI4fcezOxbRcc=\"},\"encrypted_pub_keys\"" +
          ":[{\"encrypted_pub_key\":\"OytXbV6n2L0l50yLegnaP6ea9jRDfFM0I6J/tJQzvnc2+E2Bl" +
          "eqvh4ZaIoTd7Nm6j9XRag1WYni/K0uoek/0rLnLNGZbrrQLNt5lkfTTcMZ72mEKTRkvRWbJwd8H+" +
          "p86GLqSqgvofDSE5E5EkgYGhIGSkFy8dLpXK4jpYxAQGrIQ2tNeXKKw2nNPOQ==\"},{\"encryp" +
          "ted_pub_key\":\"YxgmbmaiwGON1uHpp6cp7sWxMNNJYbX4tqtEJwbOYqfKWW9k56V/uguQrliI" +
          "waG2X7ca6VJ01YQiiMdJciQzb3w182R/HsGiYYdMuHP0PNjVk9ScYby38ofTUfjW8ihUFFjM6FSs" +
          "7WzZAFCuQ04bNNATuGfdXQK8pgoCHKWKTJ2c3alaZvIauwzkfQ==\"},{\"encrypted_pub_key" +
          "\":\"i36ne27C7pv1psRFttz3oNBVZVwgh/t6sQO3DUDfb6Edw3GvDAea3oPQ3Fm5No3JBWp5/SA" +
          "RPva29lPdi4X4mz+qde2nPYMvIJtW0ndAUGU2kw9dhzVY/FZ8XGnIH33otuKE2i+HxOYwxk6+EqS" +
          "1WEoWEqRe2LO8h1DTg9GsYzzTyjSj2OKIOGc02A==\"},{\"encrypted_pub_key\":\"s/1O2n" +
          "YnApJd+Mlc10rvGsMghE8AmhfIBXDBW52GBrjML07IVF3pZsgPKt4mLpsf2aUcHYn4P276jrdN1r" +
          "CCxlkz1haxZawNOD0RUdocg5/h6GjaeOqJVxI6hgD3xqJRT+8e2OjVLwJWSmwbX2ckeKz+u76bFN" +
          "xiCP2g+UCT94s8amrAeQTLXwF9lg==\"}]}";

  // TODO: Implement test wallet for nChipher testing
  protected static final String ncipherTestWallet = "TO BE IMPLEMENTED";
}
