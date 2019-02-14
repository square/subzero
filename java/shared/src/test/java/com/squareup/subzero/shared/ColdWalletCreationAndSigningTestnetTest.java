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
import org.bitcoinj.params.TestNet3Params;
import org.junit.Test;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.spongycastle.util.encoders.Hex.toHexString;

/**
 * This tests show cases how to initialize, finalize and sign a transaction.
 *
 * Compared to ColdWalletCreationAndSigningMainnetTest, we broadcast the transaction on testnet.
 * This test is also slightly more complicated, there are two transactions:
 *
 * transaction 1: creates two change outputs.
 * - 1 input from cold wallet (1 non-change)
 * - 2 outputs (2 change, 0 gateway)
 *
 * transaction 2:
 * - 4 inputs from cold wallet (2 non-change, 2 change)
 * - 3 outputs (1 change, 2 gateway)
 *
 * You can see all transactions here:
 * https://live.blockcypher.com/btc-testnet/tx/27efc462460cfafd3223f801e7dd1363c52df74a4b65465124f9a6c7eb45e07b/
 * https://live.blockcypher.com/btc-testnet/tx/c6b69d678c2f5389297301fb8a5290891891ca22465f9e16b8886e9488f0f567/
 * https://live.blockcypher.com/btc-testnet/tx/b7e01f3000d63e9337ebafbb3c589dcfc9c4486030493282eae1d1287d92d2b1/
 */
public class ColdWalletCreationAndSigningTestnetTest {
  private final int walletId = 2;

  @Test public void createInitWalletQrCode() {
    // The first QR code is the request to initialize a wallet
    CommandRequest commandRequest = CommandRequest.newBuilder()
        .setWalletId(walletId)
        .setInitWallet(CommandRequest.InitWalletRequest.newBuilder())
        .build();
    PlutusUtils.validateCommandRequest(commandRequest);

    String initWalletRequest = Base64.toBase64String(commandRequest.toByteArray());
    assertThat(initWalletRequest).isEqualTo("EAIaAA==");

    // This QR code is scanned on each offline machine, and results in 4 responses:
    // initWalletResponse1, initWalletResponse2, initWalletResponse3, and initWalletResponse4
  }

  @Test public void createFinalizeWalletQrCode() throws Exception {
    // We use the 4 initWalletResponses to create a finalize wallet QR code.
    String initWalletResponse1 = "CnMKcQpv3trfyO6S++vP39DJ75n63/7t38icz+6Y7/vSmf/Jz/j/5sntx/7m7v/F+p/D8/nBnMTy592c4Oby3vD86fLm05ve68v9+8udz/jB5M7Dz5nQ8M/p/PPi/+ne/NLN78ni78TE/83sxe7d/eTzy+bLIgA=";
    String initWalletResponse2 = "CnMKcQpv3trfyO6Szsuf7sfbncjeksHDwufgksvE2OiYxf755pLv+9/bwvPN05PT+pjJ+PDYk8Gf7fvk6/mSwsvmycXZ+c+f/p7J4cnQ69zy59ni+N34w82Sye7N6eva+u/ymZ36k83Bz/vT+sL92Z7oz+/JIgA=";
    String initWalletResponse3 = "CnMKcQpv3trfyO6Tz8fZ2u7swJjLz8v9297TkuDD+OLMzpvN7JLwy9/w5t3M+tDzy9D5yMTY5NnJx8zc5+vz8O37mdqSnMjwyNrzwZntk57iksjS4uj8/tjHwOzN/sPk38GYxcefzujwx9382vDCz9zkz+fAIgA=";
    String initWalletResponse4 = "CnMKcQpv3trfyO6S8O+Z/MXcw8jvzMLg2+/DxN7w7evOnc/fw+Lf4ejQzv7d/d7D//2d08WewObr3Zjr//7ok9Kd7MHi5pn9w93m4f/47Mfb+d3H8OL5x+aY5v3p/vvQ/cvs3NqYnsz+5sHHmMfv8N355O7MIgA=";

    EncryptedPubKey encPubKey1 = CommandResponse.parseFrom(Base64.decode(initWalletResponse1))
            .getInitWalletOrThrow()
            .getEncryptedPubKeyOrThrow();
    EncryptedPubKey encPubKey2 = CommandResponse.parseFrom(Base64.decode(initWalletResponse2))
        .getInitWalletOrThrow()
        .getEncryptedPubKeyOrThrow();
    EncryptedPubKey encPubKey3 = CommandResponse.parseFrom(Base64.decode(initWalletResponse3))
        .getInitWalletOrThrow()
        .getEncryptedPubKeyOrThrow();
    EncryptedPubKey encPubKey4 = CommandResponse.parseFrom(Base64.decode(initWalletResponse4))
        .getInitWalletOrThrow()
        .getEncryptedPubKeyOrThrow();

    CommandRequest commandRequest = CommandRequest.newBuilder()
        .setWalletId(walletId)
        .setFinalizeWallet(CommandRequest.FinalizeWalletRequest.newBuilder()
            .addEncryptedPubKeys(encPubKey1)
            .addEncryptedPubKeys(encPubKey2)
            .addEncryptedPubKeys(encPubKey3)
            .addEncryptedPubKeys(encPubKey4))
        .build();
    PlutusUtils.validateCommandRequest(commandRequest);

    String finalizeWalletRequest = Base64.toBase64String(commandRequest.toByteArray());
    assertThat(finalizeWalletRequest).isEqualTo("EAIizAMKcQpv3trfyO6S++vP39DJ75n63/7t38icz+6Y7/vSmf/Jz/j/5sntx/7m7v/F+p/D8/nBnMTy592c4Oby3vD86fLm05ve68v9+8udz/jB5M7Dz5nQ8M/p/PPi/+ne/NLN78ni78TE/83sxe7d/eTzy+bLCnEKb97a38juks7Ln+7H253I3pLBw8Ln4JLLxNjomMX++eaS7/vf28LzzdOT0/qYyfjw2JPBn+375Ov5ksLL5snF2fnPn/6eyeHJ0Ovc8ufZ4vjd+MPNksnuzenr2vrv8pmd+pPNwc/70/rC/dme6M/vyQpxCm/e2t/I7pPPx9na7uzAmMvPy/3b3tOS4MP44szOm83skvDL3/Dm3cz60PPL0PnIxNjk2cnHzNzn6/Pw7fuZ2pKcyPDI2vPBme2TnuKSyNLi6Pz+2MfA7M3+w+TfwZjFx5/O6PDH3fza8MLP3OTP58AKcQpv3trfyO6S8O+Z/MXcw8jvzMLg2+/DxN7w7evOnc/fw+Lf4ejQzv7d/d7D//2d08WewObr3Zjr//7ok9Kd7MHi5pn9w93m4f/47Mfb+d3H8OL5x+aY5v3p/vvQ/cvs3NqYnsz+5sHHmMfv8N355O7M");

    // This QR code is scanned on each offline machine, and results in 4 responses:
    // finalizeWalletResponse1, finalizeWalletResponse2, finalizeWalletResponse3, and finalizeWalletResponse4
  }

  @Test public void deriveColdWalletAddress() throws Exception {
    // We use the 4 finalizeWalletResponses to derive a cold wallet address
    String finalizeWalletResponse1 = "EnEKb3RwdWJEOFFBZXV6Y0UzUHVUR3ViNmVEMkVReDNVY2VSVUxjR21UTERVb1A1aVlTazZuWE13NkpMWHRaVkNYTHkxdEFhV1FhN2VSa05kaWUzelplQ1ZZSFVDdFZ4Z0VjSEVublVnRm9Ed1dOWWFMYSIA";
    String finalizeWalletResponse2 = "EnEKb3RwdWJEOGRhNURtcTdidDhraWhNSjhhbnJCMm9UU0w4RVF1cWhZZ3k5eVAyY1JacjlrNUdRTkFTOGhhTGNvc1NlNVQ0Y0tjekF2WE1zSFJ3UmlnOGNEZ0NBcFBFWDM3UDlna2VReVBoV3M0QmVFYyIA";
    String finalizeWalletResponse3 = "EnEKb3RwdWJEOWVtc3BERmoyYWVhV3F0eThKaVJIZmQxZ0Y4WmF1Wkx3ZlB6WWF6U2Juck5zY21mdk1BWVpHUTNwODZiWmJwWWszRzk0SDhieEhCVlRybWpGZ1RpTnVrMm9tNWRCWm13VnBaaGV2TmVNaiIA";
    String finalizeWalletResponse4 = "EnEKb3RwdWJEOFpFM1ZvdmliRWZoSnFFaW50WkdBZDdldWlIdUtCemRUd1d0aVVXN3lvNGpMQXcyQVVUQjl4N0ZrSEwzV2l3TEtVUkZtcVN3bVpIU21MMkxXQ1RReldhRnZwMjRmVExrbTJtRVp3U05EZiIA";

    String pubKey1 = CommandResponse.parseFrom(Base64.decode(finalizeWalletResponse1))
        .getFinalizeWalletOrThrow()
        .getPubKeyOrThrow()
        .toStringUtf8();

    String pubKey2 = CommandResponse.parseFrom(Base64.decode(finalizeWalletResponse2))
        .getFinalizeWalletOrThrow()
        .getPubKeyOrThrow()
        .toStringUtf8();

    String pubKey3 = CommandResponse.parseFrom(Base64.decode(finalizeWalletResponse3))
        .getFinalizeWalletOrThrow()
        .getPubKeyOrThrow()
        .toStringUtf8();

    String pubKey4 = CommandResponse.parseFrom(Base64.decode(finalizeWalletResponse4))
        .getFinalizeWalletOrThrow()
        .getPubKeyOrThrow()
        .toStringUtf8();

    assertThat(pubKey1).isEqualTo("tpubD8QAeuzcE3PuTGub6eD2EQx3UceRULcGmTLDUoP5iYSk6nXMw6JLXtZVCXLy1tAaWQa7eRkNdie3zZeCVYHUCtVxgEcHEnnUgFoDwWNYaLa");
    assertThat(pubKey2).isEqualTo("tpubD8da5Dmq7bt8kihMJ8anrB2oTSL8EQuqhYgy9yP2cRZr9k5GQNAS8haLcosSe5T4cKczAvXMsHRwRig8cDgCApPEX37P9gkeQyPhWs4BeEc");
    assertThat(pubKey3).isEqualTo("tpubD9emspDFj2aeaWqty8JiRHfd1gF8ZauZLwfPzYazSbnrNscmfvMAYZGQ3p86bZbpYk3G94H8bxHBVTrmjFgTiNuk2om5dBZmwVpZhevNeMj");
    assertThat(pubKey4).isEqualTo("tpubD8ZE3VovibEfhJqEintZGAd7euiHuKBzdTwWtiUW7yo4jLAw2AUTB9x7FkHL3WiwLKURFmqSwmZHSmL2LWCTQzWaFvp24fTLkm2mEZwSNDf");

    // Path was picked randomly. It's important to never reuse addresses!
    Path path = Path.newBuilder()
        .setAccount(2519)
        .setIsChange(false)
        .setIndex(8011)
        .build();

    List<String> addresses = ImmutableList.of(pubKey1, pubKey2, pubKey3, pubKey4);
    String gateway = "tpubDA3zCqbkh8BS1h3VGmB6VU1Cj638VJufBcDWSeZVw3aEE9rzvmrNoKyGFqDwr8d9rhf4sh4Yjg8LVwkehVF3Aoyss1KdDFLoiarFJQvqp4R";

    ColdWallet coldWallet = new ColdWallet(TestNet3Params.get(), walletId, addresses, gateway);
    String coldWalletAddress = coldWallet.address(path).toBase58();
    assertThat(coldWalletAddress).isEqualTo("2NG8AE4w9bcaAAVzFF9dyPrqdtFt4rpCgqp");

    // You can see the transaction which funds this address here:
    // https://live.blockcypher.com/btc-testnet/tx/27efc462460cfafd3223f801e7dd1363c52df74a4b65465124f9a6c7eb45e07b/
  }

  @Test public void createSignTxQrCode1() {
    Path inputPath = Path.newBuilder()
        .setAccount(2519)
        .setIsChange(false)
        .setIndex(8011)
        .build();

    Path outputPath1 = Path.newBuilder()
        .setAccount(2519)
        .setIsChange(true)
        .setIndex(4284)
        .build();

    Path outputPath2 = Path.newBuilder()
        .setAccount(2519)
        .setIsChange(true)
        .setIndex(4285)
        .build();

    List<String> addresses = new LinkedList<>();
    addresses.add("tpubD8QAeuzcE3PuTGub6eD2EQx3UceRULcGmTLDUoP5iYSk6nXMw6JLXtZVCXLy1tAaWQa7eRkNdie3zZeCVYHUCtVxgEcHEnnUgFoDwWNYaLa");
    addresses.add("tpubD8da5Dmq7bt8kihMJ8anrB2oTSL8EQuqhYgy9yP2cRZr9k5GQNAS8haLcosSe5T4cKczAvXMsHRwRig8cDgCApPEX37P9gkeQyPhWs4BeEc");
    addresses.add("tpubD9emspDFj2aeaWqty8JiRHfd1gF8ZauZLwfPzYazSbnrNscmfvMAYZGQ3p86bZbpYk3G94H8bxHBVTrmjFgTiNuk2om5dBZmwVpZhevNeMj");
    addresses.add("tpubD8ZE3VovibEfhJqEintZGAd7euiHuKBzdTwWtiUW7yo4jLAw2AUTB9x7FkHL3WiwLKURFmqSwmZHSmL2LWCTQzWaFvp24fTLkm2mEZwSNDf");

    String gateway = "tpubDA3zCqbkh8BS1h3VGmB6VU1Cj638VJufBcDWSeZVw3aEE9rzvmrNoKyGFqDwr8d9rhf4sh4Yjg8LVwkehVF3Aoyss1KdDFLoiarFJQvqp4R";

    ColdWallet coldWallet = new ColdWallet(TestNet3Params.get(), walletId, addresses, gateway);
    CommandRequest commandRequest = coldWallet.startTransaction(
        ImmutableList.of(TxInput.newBuilder()
            .setPrevHash(ByteString.copyFrom(
                Hex.decode("27efc462460cfafd3223f801e7dd1363c52df74a4b65465124f9a6c7eb45e07b")))
            .setPrevIndex(1)
            .setAmount(130000000)
            .setPath(inputPath)
            .build()),
        ImmutableList.of(
            TxOutput.newBuilder()
                .setAmount(43000000)
                .setDestination(Destination.CHANGE)
                .setPath(outputPath1)
                .build(),
            TxOutput.newBuilder()
                .setAmount(86000000)
                .setDestination(Destination.CHANGE)
                .setPath(outputPath2)
                .build()
            ),
        null);
    PlutusUtils.validateCommandRequest(commandRequest);

    String signTxRequest = Base64.toBase64String(commandRequest.toByteArray());
    assertThat(signTxRequest).isEqualTo("EAIqXQozCiAn78RiRgz6/TIj+AHn3RNjxS33SktlRlEk+abH60XgexABGIDJ/j0iCAjXExAAGMs+EhEIwMHAFBABGggI1xMQARi8IRIRCICDgSkQARoICNcTEAEYvSEYAA==");

    // This QR code is scanned on two offline machine, and results in 2 responses:
    // signTxResponse1, and signTxResponse2
  }

  @Test public void createTransaction1() throws Exception {
    // We use signTxResponse1 and signTxResponse2 to create the transaction.
    // The gateway address we use here must match what's hardcoded in Plutus' CodeSafe module or
    // the signature will fail to verify.
    CommandRequest signTxRequest = CommandRequest.parseFrom(Base64.decode(
        "EAIqXQozCiAn78RiRgz6/TIj+AHn3RNjxS33SktlRlEk+abH60XgexABGIDJ/j0iCAjXExAAGMs+EhEIwMHAFBABGggI1xMQARi8IRIRCICDgSkQARoICNcTEAEYvSEYAA=="));

    String signTxResponse1 = "GmwKagpGMEQCIAnUcilM+YJpC7eR5qX8DiCbLRLVJMyM/GXIalZKpk3zAiAxxoiMiojn28CIXuQ4UEUt7HewogYyHa/NhITtJoBHEhIgITGcMIuroefqZcIHbvGPaXPw3aRg7+tgSrByIghfcSEiAA==";
    String signTxResponse2 = "GmwKagpGMEQCIAkCljaQwhYcR6KgUFUtjQZTolzVdgKIOV8ss2vi73ftAiAN3BGDkFKXLYjanGBE3EhraS0vrS8sERdfQU0XM/kBSRIgITGcMIuroefqZcIHbvGPaXPw3aRg7+tgSrByIghfcSEiAA==";

    CommandResponse sig1 = CommandResponse.parseFrom(Base64.decode(signTxResponse1));
    CommandResponse sig2 = CommandResponse.parseFrom(Base64.decode(signTxResponse2));

    List<String> addresses = new LinkedList<>();
    addresses.add("tpubD8QAeuzcE3PuTGub6eD2EQx3UceRULcGmTLDUoP5iYSk6nXMw6JLXtZVCXLy1tAaWQa7eRkNdie3zZeCVYHUCtVxgEcHEnnUgFoDwWNYaLa");
    addresses.add("tpubD8da5Dmq7bt8kihMJ8anrB2oTSL8EQuqhYgy9yP2cRZr9k5GQNAS8haLcosSe5T4cKczAvXMsHRwRig8cDgCApPEX37P9gkeQyPhWs4BeEc");
    addresses.add("tpubD9emspDFj2aeaWqty8JiRHfd1gF8ZauZLwfPzYazSbnrNscmfvMAYZGQ3p86bZbpYk3G94H8bxHBVTrmjFgTiNuk2om5dBZmwVpZhevNeMj");
    addresses.add("tpubD8ZE3VovibEfhJqEintZGAd7euiHuKBzdTwWtiUW7yo4jLAw2AUTB9x7FkHL3WiwLKURFmqSwmZHSmL2LWCTQzWaFvp24fTLkm2mEZwSNDf");

    String gateway = "tpubDA3zCqbkh8BS1h3VGmB6VU1Cj638VJufBcDWSeZVw3aEE9rzvmrNoKyGFqDwr8d9rhf4sh4Yjg8LVwkehVF3Aoyss1KdDFLoiarFJQvqp4R";

    List<List<Signature>> signatures = ImmutableList.of(sig1.getSignTx().getSignaturesList(), sig2.getSignTx().getSignaturesList());

    ColdWallet coldWallet = new ColdWallet(TestNet3Params.get(), walletId, addresses, gateway);
    String transaction =
        toHexString(coldWallet.createTransaction(signTxRequest.getSignTxOrThrow().getInputsList(),
            signTxRequest.getSignTxOrThrow().getOutputsList(), signatures));
    assertThat(transaction).isEqualTo(
        "010000000001017be045ebc7a6f9245146654b4af72dc56313dde701f82332fdfa0c4662c4ef2701000000232200205211be1c60bb65d02a0fbc2b0d9ce77c83f680a1414248ca7a36232e2292ea4afeffffff02c02090020000000017a91416872e5bd0e766876696531e68a87605fd082c4987804120050000000017a9142e820d410e36fb6ab1b272eb62a37282b95e2f2087040047304402200902963690c2161c47a2a050552d8d0653a25cd5760288395f2cb36be2ef77ed02200ddc11839052972d88da9c6044dc486b692d2fad2f2c11175f414d1733f9014901473044022009d472294cf982690bb791e6a5fc0e209b2d12d524cc8cfc65c86a564aa64df3022031c6888c8a88e7dbc0885ee43850452dec77b0a206321dafcd8484ed26804712018b5221022dc4f3706655e8c11685bfe1978e930bcbba1d83269f385c5242b00905a3253c2102359b50d66f4439fc135e558e4f09631aa8d1688553449cf794bf12f88d0877962102fd5283f39419f559dbc16a5ea6a721f22de21d224bc665b64c59102dfcc5253021036dd870c3272a0d1f1f33ee382ad9f3d7b370021e16b78528220907db41aa7d2454ae00000000");

    // You can see this transaction here:
    // https://live.blockcypher.com/btc-testnet/tx/c6b69d678c2f5389297301fb8a5290891891ca22465f9e16b8886e9488f0f567/
  }

  @Test public void createSignTxQrCode2() {
    Path inputPath1 = Path.newBuilder()
        .setAccount(2519)
        .setIsChange(false)
        .setIndex(8012)
        .build();

    Path inputPath2 = Path.newBuilder()
        .setAccount(2519)
        .setIsChange(false)
        .setIndex(8013)
        .build();

    Path inputPath3 = Path.newBuilder()
        .setAccount(2519)
        .setIsChange(true)
        .setIndex(4284)
        .build();

    Path inputPath4 = Path.newBuilder()
        .setAccount(2519)
        .setIsChange(true)
        .setIndex(4285)
        .build();

    Path outputPath1 = Path.newBuilder()
        .setAccount(2519)
        .setIsChange(false)
        .setIndex(7836)
        .build();

    Path outputPath2 = Path.newBuilder()
        .setAccount(2519)
        .setIsChange(false)
        .setIndex(7837)
        .build();


    List<String> addresses = new LinkedList<>();
    addresses.add("tpubD8QAeuzcE3PuTGub6eD2EQx3UceRULcGmTLDUoP5iYSk6nXMw6JLXtZVCXLy1tAaWQa7eRkNdie3zZeCVYHUCtVxgEcHEnnUgFoDwWNYaLa");
    addresses.add("tpubD8da5Dmq7bt8kihMJ8anrB2oTSL8EQuqhYgy9yP2cRZr9k5GQNAS8haLcosSe5T4cKczAvXMsHRwRig8cDgCApPEX37P9gkeQyPhWs4BeEc");
    addresses.add("tpubD9emspDFj2aeaWqty8JiRHfd1gF8ZauZLwfPzYazSbnrNscmfvMAYZGQ3p86bZbpYk3G94H8bxHBVTrmjFgTiNuk2om5dBZmwVpZhevNeMj");
    addresses.add("tpubD8ZE3VovibEfhJqEintZGAd7euiHuKBzdTwWtiUW7yo4jLAw2AUTB9x7FkHL3WiwLKURFmqSwmZHSmL2LWCTQzWaFvp24fTLkm2mEZwSNDf");

    String gateway = "tpubDA3zCqbkh8BS1h3VGmB6VU1Cj638VJufBcDWSeZVw3aEE9rzvmrNoKyGFqDwr8d9rhf4sh4Yjg8LVwkehVF3Aoyss1KdDFLoiarFJQvqp4R";

    ColdWallet coldWallet = new ColdWallet(TestNet3Params.get(), walletId, addresses, gateway);
    CommandRequest commandRequest = coldWallet.startTransaction(
        ImmutableList.of(
            TxInput.newBuilder()
                .setPrevHash(ByteString.copyFrom(Hex.decode("2037e3440e84ccd602643f354f79e1e596821ea927f0d342dc94e3e48f2d4652")))
                .setPrevIndex(1)
                .setAmount(130000000)
                .setPath(inputPath1)
                .build(),
            TxInput.newBuilder()
                .setPrevHash(ByteString.copyFrom(Hex.decode("3ddb8236769fe679341a89523bad15e14c4c673671292ce1575cb9474d16e73f")))
                .setPrevIndex(0)
                .setAmount(65000000)
                .setPath(inputPath2)
                .build(),
            TxInput.newBuilder()
                .setPrevHash(ByteString.copyFrom(Hex.decode("c6b69d678c2f5389297301fb8a5290891891ca22465f9e16b8886e9488f0f567")))
                .setPrevIndex(0)
                .setAmount(43000000)
                .setPath(inputPath3)
                .build(),
            TxInput.newBuilder()
                .setPrevHash(ByteString.copyFrom(Hex.decode("c6b69d678c2f5389297301fb8a5290891891ca22465f9e16b8886e9488f0f567")))
                .setPrevIndex(1)
                .setAmount(86000000)
                .setPath(inputPath4)
                .build()),
        ImmutableList.of(
            TxOutput.newBuilder()
                .setAmount(107666666)
                .setDestination(Destination.GATEWAY)
                .setPath(outputPath1)
                .build(),
            TxOutput.newBuilder()
                .setAmount(215333332)
                .setDestination(Destination.GATEWAY)
                .setPath(outputPath2)
                .build()
        ),
        null);
    PlutusUtils.validateCommandRequest(commandRequest);

    String signTxRequest = Base64.toBase64String(commandRequest.toByteArray());
    assertThat(signTxRequest).isEqualTo("EAIq/AEKMwogIDfjRA6EzNYCZD81T3nh5ZaCHqkn8NNC3JTj5I8tRlIQARiAyf49IggI1xMQABjMPgozCiA924I2dp/meTQaiVI7rRXhTExnNnEpLOFXXLlHTRbnPxAAGMCk/x4iCAjXExAAGM0+CjMKIMa2nWeML1OJKXMB+4pSkIkYkcoiRl+eFriIbpSI8PVnEAAYwMHAFCIICNcTEAEYvCEKMwogxradZ4wvU4kpcwH7ilKQiRiRyiJGX54WuIhulIjw9WcQARiAg4EpIggI1xMQARi9IRIRCOq5qzMQAhoICNcTEAAYnD0SEQjU89ZmEAIaCAjXExAAGJ09GAA=");

    // This QR code is scanned on two offline machine, and results in 2 responses:
    // signTxResponse1, and signTxResponse2
  }

  @Test public void createTransaction2() throws Exception {
    // We use signTxResponse1 and signTxResponse2 to create the transaction.
    // The gateway address we use here must match what's hardcoded in Plutus' CodeSafe module or
    // the signature will fail to verify.
    CommandRequest signTxRequest = CommandRequest.parseFrom(Base64.decode("EAIq/AEKMwogIDfjRA6EzNYCZD81T3nh5ZaCHqkn8NNC3JTj5I8tRlIQARiAyf49IggI1xMQABjMPgozCiA924I2dp/meTQaiVI7rRXhTExnNnEpLOFXXLlHTRbnPxAAGMCk/x4iCAjXExAAGM0+CjMKIMa2nWeML1OJKXMB+4pSkIkYkcoiRl+eFriIbpSI8PVnEAAYwMHAFCIICNcTEAEYvCEKMwogxradZ4wvU4kpcwH7ilKQiRiRyiJGX54WuIhulIjw9WcQARiAg4EpIggI1xMQARi9IRIRCOq5qzMQAhoICNcTEAAYnD0SEQjU89ZmEAIaCAjXExAAGJ09GAA="));

    String signTxResponse1 = "GrADCmkKRTBDAiA5DpybwCcBv5GKvfhMDZxDWZ0iQk1NvNnqk8AKF8+HjwIfBdKfTmMzM73/gYW2cFFjZXA2XzWDPcKMxRZnbxe29xIgu6yjQkDPyyFHqr09H4EhsSO/DJv6gcj36WlKsZKKyvoKagpGMEQCIDHmtIGG9xB5qBuGhrXfhPYntPPb5OA0ntortKtCdfiWAiBzKMck8ILvG4trDgUQtB3vW0clubmVhzj2Fs+Zxuaq8xIgZpDSzig+wfPZNokqJUFhSUwakrOyI6cBopb5NFtBclsKawpHMEUCIQCSD5Aq1NdoRQ63lQCApNCkauU6/b4LLbsRRphuIOqfBQIgXp3lZ/i6lnR0TY803cXOszr7E5yrarotrj9/pjVgx54SIEeksTQh653AbI7UY39hU5pv1jBjSY8UiQ9fbq4rtWsZCmoKRjBEAiBgTKsrYOc0EW0gOShoxzM5wZmcWNbj7/E4/SfYUVGGIgIgfjf1LTxaFHVIBZ7S5ju5yJ6rKnFarNbAdmXhjrPe9gISIHlUdXo+epL0pK1Wrswl6d/ZxfU7ejh24qIlPL0HGrmkIgA=";
    String signTxResponse2 = "GrIDCmsKRzBFAiEAnCqLS6tIaJvIhMGoxix/KOo27UfZ01+Z5x3j5HgYQfkCIFJz4mE+b5U1C/eKeiIxmemyD0wdnmfyLXQNAqXOzFL2EiC7rKNCQM/LIUeqvT0fgSGxI78Mm/qByPfpaUqxkorK+gprCkcwRQIhAOZ+mmOH1LITwIT742LRgqN/W5uTB7rrud1MxGtMMX4FAiA3oMTZmU3EM54wJK5h0oLaqdy/hqWPGJKQVchXLa+K3RIgZpDSzig+wfPZNokqJUFhSUwakrOyI6cBopb5NFtBclsKagpGMEQCICQ0oMSyV0MpMnU0GzvzYAg7wlEG7bDcKPyl4Od7DqXvAiAiBauxb6qPMbyJAc2wFmbP1ivwITDE+0WwRbng8lzT1RIgR6SxNCHrncBsjtRjf2FTmm/WMGNJjxSJD19uriu1axkKagpGMEQCIHgJmE6xDJpTjiSn788+KaIaNkA8irln1auh1E6f5ztFAiAwmGLVtBQGwk+IyImphNxUwvdb9fbmL0vqT7jEYWL2LRIgeVR1ej56kvSkrVauzCXp39nF9Tt6OHbioiU8vQcauaQiAA==";

    CommandResponse sig1 = CommandResponse.parseFrom(Base64.decode(signTxResponse1));
    CommandResponse sig2 = CommandResponse.parseFrom(Base64.decode(signTxResponse2));

    List<String> addresses = new LinkedList<>();
    addresses.add("tpubD8QAeuzcE3PuTGub6eD2EQx3UceRULcGmTLDUoP5iYSk6nXMw6JLXtZVCXLy1tAaWQa7eRkNdie3zZeCVYHUCtVxgEcHEnnUgFoDwWNYaLa");
    addresses.add("tpubD8da5Dmq7bt8kihMJ8anrB2oTSL8EQuqhYgy9yP2cRZr9k5GQNAS8haLcosSe5T4cKczAvXMsHRwRig8cDgCApPEX37P9gkeQyPhWs4BeEc");
    addresses.add("tpubD9emspDFj2aeaWqty8JiRHfd1gF8ZauZLwfPzYazSbnrNscmfvMAYZGQ3p86bZbpYk3G94H8bxHBVTrmjFgTiNuk2om5dBZmwVpZhevNeMj");
    addresses.add("tpubD8ZE3VovibEfhJqEintZGAd7euiHuKBzdTwWtiUW7yo4jLAw2AUTB9x7FkHL3WiwLKURFmqSwmZHSmL2LWCTQzWaFvp24fTLkm2mEZwSNDf");

    String gateway = "tpubDA3zCqbkh8BS1h3VGmB6VU1Cj638VJufBcDWSeZVw3aEE9rzvmrNoKyGFqDwr8d9rhf4sh4Yjg8LVwkehVF3Aoyss1KdDFLoiarFJQvqp4R";

    List<List<Signature>> signatures = ImmutableList.of(sig1.getSignTx().getSignaturesList(), sig2.getSignTx().getSignaturesList());

    ColdWallet coldWallet = new ColdWallet(TestNet3Params.get(), walletId, addresses, gateway);
    String transaction =
        toHexString(coldWallet.createTransaction(signTxRequest.getSignTxOrThrow().getInputsList(),
            signTxRequest.getSignTxOrThrow().getOutputsList(), signatures));
    assertThat(transaction).isEqualTo(
        "0100000000010452462d8fe4e394dc42d3f027a91e8296e5e1794f353f6402d6cc840e44e33720010000002322002056d2540f628278bd5499bdadeb985ca38313dc5c8168c3d9843df7e741e37385feffffff3fe7164d47b95c57e12c297136674c4ce115ad3b52891a3479e69f763682db3d0000000023220020443a40372ac775413a1e2221682507df00a276315170af508cdf964fc3648a14feffffff67f5f088946e88b8169e5f4622ca91188990528afb01732989532f8c679db6c60000000023220020bc205be4127bf8c89461b3575148e96283c61226106d6a1f86a15a68ec090cdcfeffffff67f5f088946e88b8169e5f4622ca91188990528afb01732989532f8c679db6c60100000023220020134bdff192d1b32e31e77d102aee730a960cd39c73e739e8a04d42ad78a4dd6ffeffffff02eadc6a06000000001976a914d18cf0f37b2ad9f3f6b76436d686de3fa00c19f488acd4b9d50c000000001976a9144c99523a8c82ac245d902ca7c7ce2fe4fd0e6e1288ac04004630430220390e9c9bc02701bf918abdf84c0d9c43599d22424d4dbcd9ea93c00a17cf878f021f05d29f4e633333bdff8185b67051636570365f35833dc28cc516676f17b6f7014830450221009c2a8b4bab48689bc884c1a8c62c7f28ea36ed47d9d35f99e71de3e4781841f902205273e2613e6f95350bf78a7a223199e9b20f4c1d9e67f22d740d02a5cecc52f6018b5221029ae6b2d2b5013d871e4c8ad7286624bd98527bbf244c3e7f7d27ab50a98750d22102de6429157b443d950ca19505dc9bbc88a9034adecda032c57369676e0c1363692103b5248b2058ed937665145442137e86d69f3746e856bc25271dc38e0a65d767b72103d58631ca77f46a65ccd36f045db015382152dca5d47f4e41d3849eae16e19cf154ae0400483045022100e67e9a6387d4b213c084fbe362d182a37f5b9b9307baebb9dd4cc46b4c317e05022037a0c4d9994dc4339e3024ae61d282daa9dcbf86a58f18929055c8572daf8add01473044022031e6b48186f71079a81b8686b5df84f627b4f3dbe4e0349eda2bb4ab4275f89602207328c724f082ef1b8b6b0e0510b41def5b4725b9b9958738f616cf99c6e6aaf3018b522102956d156f4f2958987ab361be3143e42295fbdce63cd96d658c9221c4d04822212102ab1ca44d28cf2e8fc8aaadb83df1ed1dbed9968a50e5ee594a52110003d399be210357334cb095793d0bcac5ab50234583b15bc6c205c8711fa373a2ef06056395602103926ea6b5d2a6e46dc6d87c1b73f974f675b2e8bac47accbf35209d5310e7205254ae0400483045022100920f902ad4d768450eb7950080a4d0a46ae53afdbe0b2dbb1146986e20ea9f0502205e9de567f8ba9674744d8f34ddc5ceb33afb139cab6aba2dae3f7fa63560c79e0147304402202434a0c4b25743293275341b3bf360083bc25106edb0dc28fca5e0e77b0ea5ef02202205abb16faa8f31bc8901cdb01666cfd62bf02130c4fb45b045b9e0f25cd3d5018b52210230ad6a5fdb3741cc77d3f2908004a98514f978fefd5baa6097293233963b4e7f21028729354bf6c02633d4371011411e1ffb2dbe96b88961fb4bae7333c67cb25913210302204d2f596e1c1ccc6512b54dc962ca253cd40b932fc3f69e81ca6411ef976b2103901a6340c5d9bc62a823ea7fab388a03bc0332c5296418560b0852dbc94c35f854ae040047304402207809984eb10c9a538e24a7efcf3e29a21a36403c8ab967d5aba1d44e9fe73b450220309862d5b41406c24f88c889a984dc54c2f75bf5f6e62f4bea4fb8c46162f62d014730440220604cab2b60e734116d20392868c73339c1999c58d6e3eff138fd27d85151862202207e37f52d3c5a147548059ed2e63bb9c89eab2a715aacd6c07665e18eb3def602018b5221020d3e4abc0d03aa98ea5bce00b7f3799cb962e67b7cf82c398f3e159c05bd554821028fbab3268c179f1e89feea55a3572f6e86f192faec9cb00524814e23b6768a722103258c00cb6b1f0170bb089f180edab17b3c66cdc5c5968dd34de8db0e437e8bd721034acf94f3f4cfb977d3b5835ce9341fd96782768753401f2396cd4359894b9b6654ae00000000");

    // You can see this transaction here:
    // https://live.blockcypher.com/btc-testnet/tx/b7e01f3000d63e9337ebafbb3c589dcfc9c4486030493282eae1d1287d92d2b1/
  }
}
