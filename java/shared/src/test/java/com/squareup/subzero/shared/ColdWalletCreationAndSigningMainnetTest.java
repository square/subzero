package com.squareup.subzero.shared;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.squareup.protos.subzero.service.Common.Destination;
import com.squareup.protos.subzero.service.Common.EncryptedPubKey;
import com.squareup.protos.subzero.service.Common.Path;
import com.squareup.protos.subzero.service.Common.Signature;
import com.squareup.protos.subzero.service.Common.TxInput;
import com.squareup.protos.subzero.service.Common.TxOutput;
import com.squareup.protos.subzero.service.Service.CommandRequest;
import com.squareup.protos.subzero.service.Service.CommandResponse;
import java.util.LinkedList;
import java.util.List;
import org.bitcoinj.params.MainNetParams;
import org.junit.Test;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.spongycastle.util.encoders.Hex.toHexString;

/**
 * These tests showcase how to initialize, finalize and sign a transaction.
 */
public class ColdWalletCreationAndSigningMainnetTest {
  private final int walletId = 20190131;

  @Test public void createInitWalletRequest() {
    // The first QR code is the request to initialize a wallet
    CommandRequest commandRequest = CommandRequest.newBuilder()
        .setWalletId(walletId)
        .setInitWallet(CommandRequest.InitWalletRequest.newBuilder())
        .build();
    SubzeroUtils.validateCommandRequest(commandRequest);

    String initWalletRequest = Base64.toBase64String(commandRequest.toByteArray());
    assertThat(initWalletRequest).isEqualTo("ELOn0AkaAA==");

    // This QR code is scanned on each offline machine, and results in 4 responses:
    // initWalletResponse1, initWalletResponse2, initWalletResponse3, and initWalletResponse4
  }

  @Test public void createFinalizeWalletRequest() throws Exception {
    // We use the 4 initWalletResponses to create a finalize wallet QR code.
    String initWalletResponse1 = "CnMKcQpv0trfyJyS+f3Y/szeyPCf/enBmPr+6MfH4pL8/M+TyO+Yx5PB7dPCy/vFnNmZ6M7eyeDkk8Lu6+vgntvBwduS8PvfwfjBxO/fx/DQzJnI+cLJycXnk86T5+vQzOiY4vuT78LYmJ3n3sT9zMPT+dvpIgA=";
    String initWalletResponse2 = "CnMKcQpv0trfyJyS0tydwvDFxJ/7w5vM4ZPE0OTv7pzr85nb3sDs/vDDnpjE3OjcwJOfm/PPmfmT/Ono3p/iy//S/dvh/8Cczvjw/sTDxcLJ/8Sbzd/r+8fA7J3e5uTs8Mzh8uDJwfzN8/j82fzo4MP438iYIgA=";
    String initWalletResponse3 = "CnMKcQpv0trfyJyTm8/L7eD+/efn5sHMmeDd85vA2+zLnpLsktDe4fDr8M/h6N2b2NDO/MDC592Y/M/8zOLc++bZ6OHf/sSb0v2S4evswcX96czbzvvw0/uYndzQ6MPnzOzHwM7ey9jLzPKfxOvN78P5wMfzIgA=";
    String initWalletResponse4 = "CnMKcQpv0trfyJySmMWb29vs7sPDzsOY0pzAnfrS8/7tn8/Z3dPSksDO29mfw8797cfg8+3h+NCZ0PiZyez+5N7Z0vz5+tnF4t7Onszrn//IyPrrne3HxJ6em8+Y4ezIw+b+2t2bxOz54pPFydjc4Obk693PIgA=";

    EncryptedPubKey encPubKey1 = CommandResponse.parseFrom(Base64.decode(initWalletResponse1))
            .getInitWallet()
            .getEncryptedPubKey();
    EncryptedPubKey encPubKey2 = CommandResponse.parseFrom(Base64.decode(initWalletResponse2))
        .getInitWallet()
        .getEncryptedPubKey();
    EncryptedPubKey encPubKey3 = CommandResponse.parseFrom(Base64.decode(initWalletResponse3))
        .getInitWallet()
        .getEncryptedPubKey();
    EncryptedPubKey encPubKey4 = CommandResponse.parseFrom(Base64.decode(initWalletResponse4))
        .getInitWallet()
        .getEncryptedPubKey();

    CommandRequest commandRequest = CommandRequest.newBuilder()
        .setWalletId(walletId)
        .setFinalizeWallet(CommandRequest.FinalizeWalletRequest.newBuilder()
            .addEncryptedPubKeys(encPubKey1)
            .addEncryptedPubKeys(encPubKey2)
            .addEncryptedPubKeys(encPubKey3)
            .addEncryptedPubKeys(encPubKey4))
        .build();
    SubzeroUtils.validateCommandRequest(commandRequest);

    String finalizeWalletRequest = Base64.toBase64String(commandRequest.toByteArray());
    assertThat(finalizeWalletRequest).isEqualTo("ELOn0AkizAMKcQpv0trfyJyS+f3Y/szeyPCf/enBmPr+6MfH4pL8/M+TyO+Yx5PB7dPCy/vFnNmZ6M7eyeDkk8Lu6+vgntvBwduS8PvfwfjBxO/fx/DQzJnI+cLJycXnk86T5+vQzOiY4vuT78LYmJ3n3sT9zMPT+dvpCnEKb9La38icktLcncLwxcSf+8ObzOGTxNDk7+6c6/OZ297A7P7ww56YxNzo3MCTn5vzz5n5k/zp6N6f4sv/0v3b4f/AnM748P7Ew8XCyf/Em83f6/vHwOyd3ubk7PDM4fLgycH8zfP4/Nn86ODD+N/ImApxCm/S2t/InJObz8vt4P795+fmwcyZ4N3zm8Db7MuekuyS0N7h8Ovwz+Ho3ZvY0M78wMLn3Zj8z/zM4tz75tno4d/+xJvS/ZLh6+zBxf3pzNvO+/DT+5id3NDow+fM7MfAzt7L2MvM8p/E683vw/nAx/MKcQpv0trfyJySmMWb29vs7sPDzsOY0pzAnfrS8/7tn8/Z3dPSksDO29mfw8797cfg8+3h+NCZ0PiZyez+5N7Z0vz5+tnF4t7Onszrn//IyPrrne3HxJ6em8+Y4ezIw+b+2t2bxOz54pPFydjc4Obk693P");

    // This QR code is scanned on each offline machine, and results in 4 responses:
    // finalizeWalletResponse1, finalizeWalletResponse2, finalizeWalletResponse3, and finalizeWalletResponse4
  }

  @Test public void deriveColdWalletAddress() throws Exception {
    // We use the 4 finalizeWalletResponses to derive a cold wallet address
    String finalizeWalletResponse1 = "EnEKb3hwdWI2OFNXclRmdGJaNVdDazJQVEJtbUg4VlZlOWJFMm05a0d5aGFRbzZzM0JkdGNKTjloREFBSjRxa2txOFpRdWtSa25FdW1aemYzYlNoY2NvTTlkOU1BemZCMkhROUVocjI3TXRuV2ZpeVNxQyIA";
    String finalizeWalletResponse2 = "EnEKb3hwdWI2OHh2N2hab241UWkxZks5bnpORUQ2QVkzcXRqRlRaaTQybnZCdmo5NTFZZTNTOVZDQnQ1SGFVeFdxS1VqNmRSWlRuaW9oY1VuMWd1QVFtakY3dExORlpmS1hKY2tWZ1lSVnNWQkppUnViMiIA";
    String finalizeWalletResponse3 = "EnEKb3hwdWI2OTFlYUdKVFdNTUxrZjNKd1kxanFGYTQ4Rjh6dEtaQVplS0J3MXJ6ZFZqaE13MlZlVmZIdlFMc0JLdVRuMXhXOEtBRmtvV0NmcWRRWnlRMjd2ekJpTWZGbWpkdGFyYWZYNW5BZ0VpU2ptWSIA";
    String finalizeWalletResponse4 = "EnEKb3hwdWI2ODJvMXFxRkRpaWRpMng2ajdQeFlURzVlc3d5eDhqZHFzNWlkV0dtSllHS1J6M3pSM2NGVE50c3hWU1Bzb0h0ZDRmQTVVYmJQQTdHbW40NDFlMktGYmlMVHB3MW5GU0g5b2NydkpMTkF3ZSIA";

    String pubKey1 = CommandResponse.parseFrom(Base64.decode(finalizeWalletResponse1))
        .getFinalizeWallet()
        .getPubKey()
        .toStringUtf8();

    String pubKey2 = CommandResponse.parseFrom(Base64.decode(finalizeWalletResponse2))
        .getFinalizeWallet()
        .getPubKey()
        .toStringUtf8();

    String pubKey3 = CommandResponse.parseFrom(Base64.decode(finalizeWalletResponse3))
        .getFinalizeWallet()
        .getPubKey()
        .toStringUtf8();

    String pubKey4 = CommandResponse.parseFrom(Base64.decode(finalizeWalletResponse4))
        .getFinalizeWallet()
        .getPubKey()
        .toStringUtf8();

    assertThat(pubKey1).isEqualTo("xpub68SWrTftbZ5WCk2PTBmmH8VVe9bE2m9kGyhaQo6s3BdtcJN9hDAAJ4qkkq8ZQukRknEumZzf3bShccoM9d9MAzfB2HQ9Ehr27MtnWfiySqC");
    assertThat(pubKey2).isEqualTo("xpub68xv7hZon5Qi1fK9nzNED6AY3qtjFTZi42nvBvj951Ye3S9VCBt5HaUxWqKUj6dRZTniohcUn1guAQmjF7tLNFZfKXJckVgYRVsVBJiRub2");
    assertThat(pubKey3).isEqualTo("xpub691eaGJTWMMLkf3JwY1jqFa48F8ztKZAZeKBw1rzdVjhMw2VeVfHvQLsBKuTn1xW8KAFkoWCfqdQZyQ27vzBiMfFmjdtarafX5nAgEiSjmY");
    assertThat(pubKey4).isEqualTo("xpub682o1qqFDiidi2x6j7PxYTG5eswyx8jdqs5idWGmJYGKRz3zR3cFTNtsxVSPsoHtd4fA5UbbPA7Gmn441e2KFbiLTpw1nFSH9ocrvJLNAwe");

    // Path was picked randomly. It's important to never reuse addresses!
    Path path = Path.newBuilder()
        .setIsChange(false)
        .setIndex(3172)
        .build();

    List<String> addresses = ImmutableList.of(pubKey1, pubKey2, pubKey3, pubKey4);
    String gateway = "xpub68Jb6w8Rt39nCkDgMaaSiTCCEyZTx6vjeFJHfKgvxz4JkePneCfRpe82vECwHxfWkAdUysRxa4d59RbC8RqVFDKYB5hWQ8ebUecbS5qwHN7";

    ColdWallet coldWallet = new ColdWallet(MainNetParams.get(), walletId, addresses, gateway);
    String coldWalletAddress = coldWallet.address(path).toBase58();
    assertThat(coldWalletAddress).isEqualTo("38j52zh1XCpjWEgAgEQXaEdfwHV1gSBSQY");

    // You can see the transaction which funds this address here:
    // https://live.blockcypher.com/btc/tx/356ef5e00c897257e56c9bef9cadbe1858cac648faf3072caa471e4e8378b92d/
  }

  @Test public void createSignTxRequest() {
    Path inputPath = Path.newBuilder()
        .setIsChange(false)
        .setIndex(3172)
        .build();

    Path outputPath = Path.newBuilder()
        .setIsChange(false)
        .setIndex(0)
        .build();

    List<String> addresses = new LinkedList<>();
    addresses.add("xpub68SWrTftbZ5WCk2PTBmmH8VVe9bE2m9kGyhaQo6s3BdtcJN9hDAAJ4qkkq8ZQukRknEumZzf3bShccoM9d9MAzfB2HQ9Ehr27MtnWfiySqC");
    addresses.add("xpub68xv7hZon5Qi1fK9nzNED6AY3qtjFTZi42nvBvj951Ye3S9VCBt5HaUxWqKUj6dRZTniohcUn1guAQmjF7tLNFZfKXJckVgYRVsVBJiRub2");
    addresses.add("xpub691eaGJTWMMLkf3JwY1jqFa48F8ztKZAZeKBw1rzdVjhMw2VeVfHvQLsBKuTn1xW8KAFkoWCfqdQZyQ27vzBiMfFmjdtarafX5nAgEiSjmY");
    addresses.add("xpub682o1qqFDiidi2x6j7PxYTG5eswyx8jdqs5idWGmJYGKRz3zR3cFTNtsxVSPsoHtd4fA5UbbPA7Gmn441e2KFbiLTpw1nFSH9ocrvJLNAwe");

    String gateway = "xpub68Jb6w8Rt39nCkDgMaaSiTCCEyZTx6vjeFJHfKgvxz4JkePneCfRpe82vECwHxfWkAdUysRxa4d59RbC8RqVFDKYB5hWQ8ebUecbS5qwHN7";

    ColdWallet coldWallet = new ColdWallet(MainNetParams.get(), walletId, addresses, gateway);
    CommandRequest commandRequest = coldWallet.startTransaction(
        ImmutableList.of(TxInput.newBuilder()
            .setPrevHash(ByteString.copyFrom(
                Hex.decode("356ef5e00c897257e56c9bef9cadbe1858cac648faf3072caa471e4e8378b92d")))
            .setPrevIndex(0)
            .setAmount(100000)
            .setPath(inputPath)
            .build()),
        ImmutableList.of(TxOutput.newBuilder()
            .setAmount(93200)
            .setDestination(Destination.GATEWAY)
            .setPath(outputPath)
            .build()),
        null, null);
    SubzeroUtils.validateCommandRequest(commandRequest);

    String signTxRequest = Base64.toBase64String(commandRequest.toByteArray());
    assertThat(signTxRequest).isEqualTo("ELOn0AkqQQovCiA1bvXgDIlyV+Vsm++crb4YWMrGSPrzByyqRx5Og3i5LRAAGKCNBiIFEAAY5BgSDAiQ2AUQAhoEEAAYABgA");

    // This QR code is scanned on two offline machine, and results in 2 responses:
    // signTxResponse1, and signTxResponse2
  }

  @Test public void createTransaction() throws Exception {
    // We use signTxResponse1 and signTxResponse2 to create the transaction.
    // The gateway address we use here must match what's hardcoded in Subzero's CodeSafe module or
    // the signature will fail to verify.
    CommandRequest signTxRequest = CommandRequest.parseFrom(Base64.decode("ELOn0AkqQQovCiA1bvXgDIlyV+Vsm++crb4YWMrGSPrzByyqRx5Og3i5LRAAGKCNBiIFEAAY5BgSDAiQ2AUQAhoEEAAYABgA"));

    String signTxResponse1 = "Gm0KawpHMEUCIQDUXBg5iwrPZM/cAksdOeS9zqcyQYYlNuRBLuWkM5G12AIgJGg432LwX24k769nxPKwpb2qq3NML+AWkE4D5YWXzkoSIMOwIFJ866tzZeeuY6yqkA7dfGiSWbz/Lzk3eF5ffqMsIgA=";
    String signTxResponse2 = "GmwKagpGMEQCIDytUkc3PR7YqOnEapeDV2sG9oarzo8Y9gyvSeqx1Uv0AiAlfWitoSHkhPo65ub+Ef0qBPdP6K7cGw+zp50UVwaQxRIgw7AgUnzrq3Nl565jrKqQDt18aJJZvP8vOTd4Xl9+oywiAA==";

    CommandResponse sig1 = CommandResponse.parseFrom(Base64.decode(signTxResponse1));
    CommandResponse sig2 = CommandResponse.parseFrom(Base64.decode(signTxResponse2));

    List<String> addresses = new LinkedList<>();
    addresses.add("xpub68SWrTftbZ5WCk2PTBmmH8VVe9bE2m9kGyhaQo6s3BdtcJN9hDAAJ4qkkq8ZQukRknEumZzf3bShccoM9d9MAzfB2HQ9Ehr27MtnWfiySqC");
    addresses.add("xpub68xv7hZon5Qi1fK9nzNED6AY3qtjFTZi42nvBvj951Ye3S9VCBt5HaUxWqKUj6dRZTniohcUn1guAQmjF7tLNFZfKXJckVgYRVsVBJiRub2");
    addresses.add("xpub691eaGJTWMMLkf3JwY1jqFa48F8ztKZAZeKBw1rzdVjhMw2VeVfHvQLsBKuTn1xW8KAFkoWCfqdQZyQ27vzBiMfFmjdtarafX5nAgEiSjmY");
    addresses.add("xpub682o1qqFDiidi2x6j7PxYTG5eswyx8jdqs5idWGmJYGKRz3zR3cFTNtsxVSPsoHtd4fA5UbbPA7Gmn441e2KFbiLTpw1nFSH9ocrvJLNAwe");

    String gateway = "xpub68Jb6w8Rt39nCkDgMaaSiTCCEyZTx6vjeFJHfKgvxz4JkePneCfRpe82vECwHxfWkAdUysRxa4d59RbC8RqVFDKYB5hWQ8ebUecbS5qwHN7";

    List<List<Signature>> signatures = ImmutableList.of(sig1.getSignTx().getSignaturesList(), sig2.getSignTx().getSignaturesList());

    ColdWallet coldWallet = new ColdWallet(MainNetParams.get(), walletId, addresses, gateway);
    String transaction =
        toHexString(coldWallet.createTransaction(signTxRequest.getSignTx().getInputsList(),
            signTxRequest.getSignTx().getOutputsList(), signatures));
    assertThat(transaction).isEqualTo(
        "010000000001012db978834e1e47aa2c07f3fa48c6ca5818bead9cef9b6ce55772890ce0f56e350000000023220020c35e8fbe611a5a157ddfe54d1c6e8d9cbe7edd76a9a9578fb39b0390a52b6e07feffffff01106c0100000000001976a914dd12ae4d57cfa4bcb78214f9669aaf9e6215664f88ac0400483045022100d45c18398b0acf64cfdc024b1d39e4bdcea73241862536e4412ee5a43391b5d80220246838df62f05f6e24efaf67c4f2b0a5bdaaab734c2fe016904e03e58597ce4a0147304402203cad5247373d1ed8a8e9c46a9783576b06f686abce8f18f60caf49eab1d54bf40220257d68ada121e484fa3ae6e6fe11fd2a04f74fe8aedc1b0fb3a79d14570690c5018b5221021fe5210be064b91d356a02a0e42cae57c9ceadcde5a500af2a2f44a525985587210254d20970c867d6abc582716731221a6fa9aa6e92621d28cda71d541939290594210379d0c27c46d6f9ea3b741ac7cfeef110f6e8849a827620e57d0ac5affe3c96412103b78cfc42d8285f73ea8e57a86d62e588f9833dd8a69f1d4da3e029aac922d1b054ae00000000");

    // You can see this transaction here:
    // https://live.blockcypher.com/btc/tx/0283e8d4c2385ecd89beb7062581d32b990c3f2055a16ba1934221a872e22a9a/
  }
}
