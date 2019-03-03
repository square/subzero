package com.squareup.subzero.shared;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.subzero.proto.service.Common.EncryptedPubKey;
import com.squareup.subzero.proto.service.Service.CommandRequest;
import com.squareup.subzero.proto.service.Service.CommandResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * These tests exercise the lifecycle of creating a cold wallet.
 */
public class ColdWalletCreatorTest {
  /**
   * Test `init()`.  Just check the fields are basically set right.
   */
  @Test public void testInit() {
    int walletId = 111;
    String token = "some-token";

    CommandRequest request = ColdWalletCreator.init(token, walletId);

    assertThat(request.getWalletId()).isEqualTo(walletId);
    assertThat(request.getToken()).isEqualTo(token);
    assertThat(request.hasInitWallet()).isTrue();
  }

  /**
   * You'd normally get this from Subzero.  This is a helper for making something
   * that looks like those responses.
   */
  private static CommandResponse initWalletResponse(String token, String pubkey) {
    return CommandResponse.newBuilder()
        .setToken(token)
        .setInitWallet(CommandResponse.InitWalletResponse.newBuilder()
            .setEncryptedPubKey(
                EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFromUtf8(pubkey))
            )).build();
  }

  /**
   * Test `combine()`.
   */
  @Test public void testCombine() throws InvalidProtocolBufferException {
    // The exact values here don't matter too much; we just check they match in the return values.
    byte[] byteArrayA = {1};
    EncryptedPubKey encryptedPubKeyA =
        EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFrom(byteArrayA)).build();
    byte[] byteArrayB = {2};
    EncryptedPubKey encryptedPubKeyB =
        EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFrom(byteArrayB)).build();
    byte[] byteArrayC = {3};
    EncryptedPubKey encryptedPubKeyC =
        EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFrom(byteArrayC)).build();
    byte[] byteArrayD = {4};
    EncryptedPubKey encryptedPubKeyD =
        EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFrom(byteArrayD)).build();
    Map<String, EncryptedPubKey> map = new HashMap<>();
    String tokenA = "token-a";
    map.put(tokenA, encryptedPubKeyA);
    map.put("token-b", encryptedPubKeyB);
    map.put("token-c", encryptedPubKeyC);
    map.put("token-d", encryptedPubKeyD);
    int walletId = 123;

    CommandRequest request = ColdWalletCreator.combine(map, tokenA, walletId);
    assertThat(request.getToken()).isEqualTo(tokenA);
    assertThat(request.getWalletId()).isEqualTo(walletId);
    assertThat(request.getFinalizeWallet().getEncryptedPubKeysCount())
        .isEqualTo(map.size());
    assertThat(request.getFinalizeWallet().getEncryptedPubKeysList())
        .containsExactlyInAnyOrder(encryptedPubKeyA, encryptedPubKeyB, encryptedPubKeyC,
            encryptedPubKeyD);
  }

  /**
   * Test `combine()` where the requested token is not present in the given map.
   */
  @Test public void testCombineWhereTokenNotPresent() {
    // The exact values here don't matter too much; we just check they match in the return values.
    byte[] byteArrayA = {1};
    EncryptedPubKey encryptedPubKeyA =
        EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFrom(byteArrayA)).build();
    byte[] byteArrayB = {2};
    EncryptedPubKey encryptedPubKeyB =
        EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFrom(byteArrayB)).build();
    byte[] byteArrayC = {3};
    EncryptedPubKey encryptedPubKeyC =
        EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFrom(byteArrayC)).build();
    byte[] byteArrayD = {4};
    EncryptedPubKey encryptedPubKeyD =
        EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFrom(byteArrayD)).build();
    Map<String, EncryptedPubKey> map = new HashMap<>();
    String tokenA = "token-a";
    map.put("token-x", encryptedPubKeyA);
    map.put("token-b", encryptedPubKeyB);
    map.put("token-c", encryptedPubKeyC);
    map.put("token-d", encryptedPubKeyD);
    int walletId = 123;

    assertThatThrownBy(() -> ColdWalletCreator.combine(map, tokenA, walletId))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /**
   * Test `combine()` where there are not enough teams present.
   */
  @Test public void testCombineWhereWrongNumberOfElements() {
    // The exact values here don't matter too much; we just check they match in the return values.
    byte[] byteArrayA = {1};
    EncryptedPubKey encryptedPubKeyA =
        EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFrom(byteArrayA)).build();
    Map<String, EncryptedPubKey> map = new HashMap<>();
    String tokenA = "token-a";
    map.put(tokenA, encryptedPubKeyA);
    int walletId = 123;
    assertThat(map.size()).isNotEqualTo(Constants.ENCRYPTED_PUB_KEYS_MAX_COUNT);
    assertThatThrownBy(() -> ColdWalletCreator.combine(map, tokenA, walletId))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /**
   * Test `combine()` where some of the given encrypted pub keys are null
   */
  @Test public void testCombineWithNullKeys() {
    // The exact values here don't matter too much; we just check they match in the return values.
    byte[] byteArrayA = {1};
    EncryptedPubKey encryptedPubKeyA =
        EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFrom(byteArrayA)).build();
    Map<String, EncryptedPubKey> map = new HashMap<>();
    String tokenA = "token-a";
    map.put(tokenA, encryptedPubKeyA);
    map.put("token-b", null);
    map.put("token-c", null);
    map.put("token-d", null);
    int walletId = 123;

    assertThatThrownBy(() -> ColdWalletCreator.combine(map, tokenA, walletId))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /**
   * Test `finalize()`.
   */
  @Test public void testFinalize() {
    String pubkey =
        "tpubD9jBarsLCKot45kvTTu7yWxmNnkBPbpn2CgS1F3yuxZGgohTkamYwJJKenZHrsYwPRJY66dk3ZUt3aZwZqFf7QGsgUUUcNSvvb9NXHFt5Vb";
    CommandResponse fixture = CommandResponse.newBuilder().setFinalizeWallet(
        CommandResponse.FinalizeWalletResponse.newBuilder()
            .setPubKey(ByteString.copyFromUtf8(pubkey))).build();

    String returned = ColdWalletCreator.finalize(fixture);

    assertThat(returned).isEqualTo(pubkey);
  }
}
