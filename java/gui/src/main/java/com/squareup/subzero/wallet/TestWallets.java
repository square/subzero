package com.squareup.subzero.wallet;

/**
 * Hardcoded wallets for blackbox regression testing of subzero transaction signing
 * <p>
 * Instead of loading a test wallet from a file, we have it loaded from this class
 */

public final class TestWallets {
  // Test wallet 1492, share 1, for off-target dev testing.
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

  // Test wallet for on-target nChipher testing. Note that ncipherTestWallet differs
  // from devTestWallet only in its extra "ocs_id" and "master_seed_encryption_key_id"
  // values. We could use the string below for both test wallets, but keep them separate
  // so that the code is easier to understand.
  protected static final String ncipherTestWallet =
      "{\"currency\":\"TEST_NET\",\"ocs_id\":\"adb1c4d63095d578b60d7fa3ef44f2acc435c821" +
          "\",\"master_seed_encryption_key_id\":\"masterseedenckey128\", \"encrypted_ma" +
          "ster_seed\":{\"encrypted_master_seed\":\"ioBg3WF2BntMnGae6PyWbp1VG4r446PUYVZ" +
          "nt1BzOOVQzHy3XeaqmBXS6tMbE9fsB0sR+Vi9xPgJcayN2uJsJNjEw7S77h9oUUpu0zWrYvl6iRA" +
          "I4fcezOxbRcc=\"},\"encrypted_pub_keys\":[{\"encrypted_pub_key\":\"OytXbV6n2L" +
          "0l50yLegnaP6ea9jRDfFM0I6J/tJQzvnc2+E2Bleqvh4ZaIoTd7Nm6j9XRag1WYni/K0uoek/0rL" +
          "nLNGZbrrQLNt5lkfTTcMZ72mEKTRkvRWbJwd8H+p86GLqSqgvofDSE5E5EkgYGhIGSkFy8dLpXK4" +
          "jpYxAQGrIQ2tNeXKKw2nNPOQ==\"},{\"encrypted_pub_key\":\"YxgmbmaiwGON1uHpp6cp7" +
          "sWxMNNJYbX4tqtEJwbOYqfKWW9k56V/uguQrliIwaG2X7ca6VJ01YQiiMdJciQzb3w182R/HsGiY" +
          "YdMuHP0PNjVk9ScYby38ofTUfjW8ihUFFjM6FSs7WzZAFCuQ04bNNATuGfdXQK8pgoCHKWKTJ2c3" +
          "alaZvIauwzkfQ==\"},{\"encrypted_pub_key\":\"i36ne27C7pv1psRFttz3oNBVZVwgh/t6" +
          "sQO3DUDfb6Edw3GvDAea3oPQ3Fm5No3JBWp5/SARPva29lPdi4X4mz+qde2nPYMvIJtW0ndAUGU2" +
          "kw9dhzVY/FZ8XGnIH33otuKE2i+HxOYwxk6+EqS1WEoWEqRe2LO8h1DTg9GsYzzTyjSj2OKIOGc0" +
          "2A==\"},{\"encrypted_pub_key\":\"s/1O2nYnApJd+Mlc10rvGsMghE8AmhfIBXDBW52GBrj" +
          "ML07IVF3pZsgPKt4mLpsf2aUcHYn4P276jrdN1rCCxlkz1haxZawNOD0RUdocg5/h6GjaeOqJVxI" +
          "6hgD3xqJRT+8e2OjVLwJWSmwbX2ckeKz+u76bFNxiCP2g+UCT94s8amrAeQTLXwF9lg==\"}]}";
}
