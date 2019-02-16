package com.squareup.subzero.shared;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.squareup.protos.subzero.service.Common.Destination;
import com.squareup.protos.subzero.service.Common.Path;
import com.squareup.protos.subzero.service.Common.Signature;
import com.squareup.protos.subzero.service.Common.TxInput;
import com.squareup.protos.subzero.service.Common.TxOutput;
import com.squareup.protos.subzero.service.Service.CommandRequest;
import com.squareup.protos.subzero.service.Service.CommandResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.TestNet3Params;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class ColdWalletTest {
  private ColdWallet coldWallet;


  @Before public void setup() {
    List<String> pubkeys = ImmutableList.of(
        "tpubD9jBarsLCKot45kvTTu7yWxmNnkBPbpn2CgS1F3yuxZGgohTkamYwJJKenZHrsYwPRJY66dk3ZUt3aZwZqFf7QGsgUUUcNSvvb9NXHFt5Vb",
        "tpubD8u5eHk8B6q63G4ekfBB2eVCeBYTrW2uCvi7Z3BF6bY2z33jW14Uzna8f5cFQWS3HFwdAzUWxqESq7j5x2CUQ7uBPgpXnpf8X9FPnh63XYd",
        "tpubD9dkteiWZqa3jL17meqVvy1RSUQsmJvU3hBfAosMzrSa69DacRku8yHy3E2fma1Q4Den25ukcsBL3bYTFyKjKbF8CEWHu86Xg9YXiY6CkeC",
        "tpubD8GrNWdYHDjdJryH5tng8LUzHSrEo4gToaguKnzthEqTxyfF13jTsp4sMtmso4n1VC58R5Wvt4Ua4npZTecR1xaGGYJgLLQj5sQGdD2xh2N");

    String gateway = "tpubD8GrNWdYHDjdJryH5tng8LUzHSrEo4gToaguKnzthEqTxyfF13jTsp4sMtmso4n1VC58R5Wvt4Ua4npZTecR1xaGGYJgLLQj5sQGdD2xh2N";
    coldWallet = new ColdWallet(TestNet3Params.get(), 0x7FFFFFFF, pubkeys, gateway);
  }

  /**
   * MaxSizeTest verifies we can create a transaction with the documented number of inputs and
   * outputs, with the generated proto under the QR code size limit.
   */
  @Test public void MaxSizeTest() {
    Path.Builder path = Path.newBuilder()
        .setAccount(0x7FFFFFFF)
        .setIsChange(false)
        .setIndex(0x7FFFFFFF);

    // This input is as big as possible.
    TxInput input = TxInput.newBuilder()
        .setPrevHash(ByteString.copyFrom(Hex.decode("d37365c6a3c089576b1ea1cb8bee2baa005afc96d24080515635f852c680d127")))
        .setPrevIndex(0x7FFFFFFF)
        .setAmount(0x7FFFFFFF7FFFFFFFL)
        .setPath(path)
        .build();

    TxOutput output = TxOutput.newBuilder()
        .setAmount(0x7FFFFFFF7FFFFFFFL)
        .setDestination(Destination.GATEWAY)
        .setPath(path)
        .build();

    List<TxInput> inputs = new ArrayList<>();
    for(int i = 0; i < Constants.INPUTS_COUNT_AT_LEAST; i++) {
      inputs.add(input);
    }

    List<TxOutput> outputs = new ArrayList<>();
    for(int i = 0; i < Constants.OUTPUTS_COUNT_AT_LEAST; i++) {
      outputs.add(output);
    }

    String token = "TokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenToken";
    CommandRequest request = coldWallet.startTransaction(inputs, outputs, token, null);
    assertThat(request.getSerializedSize()).isLessThan(Constants.MAX_QR_PROTO_BYTES);

    // The response consists of signatures.  We know bitcoin signatures are, in worst-case, 73 bytes
    // So we verify a CommandResponse.SignTxResponse with 73 byte signatures fits.
    List<Signature> signatures = new ArrayList<>();

    for(int i = 0; i < Constants.INPUTS_COUNT_AT_LEAST; i++) {
      signatures.add(Signature.newBuilder()
          .setDer(ByteString.copyFrom(Hex.decode(
                  "3045022100c822d9b754a87ef25a0c9a9ad44eba41bd00822ddab60f759e77e99e80427feb02207edc74ca82fafc6d7b42ef4815d4142ff49fca0bac533449f15543c19264c13901FF"
           ))).build());
    }

    CommandResponse response = CommandResponse.newBuilder()
        .setToken(token)
        .setSignTx(CommandResponse.SignTxResponse.newBuilder()
            .addAllSignatures(signatures))
        .build();

    System.out.println(format("the serialized size is %d (of %d)", response.getSerializedSize(), Constants.MAX_QR_PROTO_BYTES));
    assertThat(response.getSerializedSize()).isLessThan(Constants.MAX_QR_PROTO_BYTES);
  }

  @Test public void testCreateWrappedSegwitMultisigTransaction() throws Exception {
    // Desired transaction: txid 25954ad7739b80d29bfe6593e8e5044aeccc4c5a53e668a3f19ec3fdd5019478

    ByteString txhash = ByteString.copyFrom(Hex.decode("20e02943a743fd5d1eb1092699aa17872d9d064e20ccfe5c35c93ccbb2c33355"));

    List<List<Signature>> signatures = new ArrayList<>();
    List<Signature> hsm1sigs = new ArrayList<>();
    // These are DER-encoded signatures, hexed.
    hsm1sigs.add(Signature.newBuilder()
        .setDer(ByteString.copyFrom(Hex.decode("304402202403513696f18ed56040225cb558a2a068d5aa703ca86ca4f24ee0c56755daa902204756e62b4ccd6b4c90ffdf1a79b4e26fde4848f41156b54e9302b04297aead58")))
        .setHash(txhash)
        .build());
    signatures.add(hsm1sigs);
    List<Signature> hsm2sigs = new ArrayList<>();
    hsm2sigs.add(Signature.newBuilder()
        .setDer(ByteString.copyFrom(Hex.decode("30450221009db16421c956c694498acce44eb4de031cc87187f12860fa11377993e4621a8f02203e80612543f4f7b4d2d7c7e59c3977014fec7ce59f97305e9a2f5ee91dc420b8")))
        .setHash(txhash)
        .build());
    signatures.add(hsm2sigs);

    byte[] prev_hash = Hex.decode("d37365c6a3c089576b1ea1cb8bee2baa005afc96d24080515635f852c680d127");
    int prev_index = 0;
    int lock_time = 0;
    List<DeterministicKey> publicRootKeys = ImmutableList.of(
        "tpubD9jBarsLCKot45kvTTu7yWxmNnkBPbpn2CgS1F3yuxZGgohTkamYwJJKenZHrsYwPRJY66dk3ZUt3aZwZqFf7QGsgUUUcNSvvb9NXHFt5Vb",
        "tpubD8u5eHk8B6q63G4ekfBB2eVCeBYTrW2uCvi7Z3BF6bY2z33jW14Uzna8f5cFQWS3HFwdAzUWxqESq7j5x2CUQ7uBPgpXnpf8X9FPnh63XYd",
        "tpubD9dkteiWZqa3jL17meqVvy1RSUQsmJvU3hBfAosMzrSa69DacRku8yHy3E2fma1Q4Den25ukcsBL3bYTFyKjKbF8CEWHu86Xg9YXiY6CkeC",
        "tpubD8GrNWdYHDjdJryH5tng8LUzHSrEo4gToaguKnzthEqTxyfF13jTsp4sMtmso4n1VC58R5Wvt4Ua4npZTecR1xaGGYJgLLQj5sQGdD2xh2N")
        .stream()
        .map(pub -> DeterministicKey.deserializeB58(pub, TestNet3Params.get()))
        .collect(Collectors.toList());

    List<TxInput> inputs = new ArrayList<>();
    TxInput input = TxInput.newBuilder()
        .setPrevHash(ByteString.copyFrom(prev_hash))
        .setPrevIndex(prev_index)
        .setAmount(1000000)
        .setPath(PlutusUtils.newPath(0, false, 0))
        .build();
    inputs.add(input);

    List<TxOutput> outputs = new ArrayList<>();
    TxOutput output = TxOutput.newBuilder()
        .setAmount(999334)
        .setPath(PlutusUtils.newPath(0, false, 0))
        .setDestination(Destination.GATEWAY)
        .build();
    outputs.add(output);

    DeterministicKey gateway = DeterministicKey.deserializeB58("tpubDA3zCqbkh8BS1h3VGmB6VU1Cj638VJufBcDWSeZVw3aEE9rzvmrNoKyGFqDwr8d9rhf4sh4Yjg8LVwkehVF3Aoyss1KdDFLoiarFJQvqp4R", TestNet3Params.get());

    int sequence = 0xfffffffe;
    String tx = Hex.toHexString(
        coldWallet.createWrappedSegwitMultisigTransaction(
            publicRootKeys, inputs, outputs, gateway, signatures, lock_time, sequence));

    byte[] expected = Hex.decode(""+
        "0100000000010127d180c652f83556518040d296fc5a00aa2bee8bcba11e6b5789c0a3c665"
        + "73d30000000023220020a793b69cdc1ae26fef6c1eb2029f97d00dedc54390829137c928b6"
        + "f83e77c736feffffff01a63f0f00000000001976a91414b8b8a87b0695442f9df6858a42ef"
        + "399db3fab588ac04004830450221009db16421c956c694498acce44eb4de031cc87187f128"
        + "60fa11377993e4621a8f02203e80612543f4f7b4d2d7c7e59c3977014fec7ce59f97305e9a"
        + "2f5ee91dc420b80147304402202403513696f18ed56040225cb558a2a068d5aa703ca86ca4"
        + "f24ee0c56755daa902204756e62b4ccd6b4c90ffdf1a79b4e26fde4848f41156b54e9302b0"
        + "4297aead58018b52210242d1e4eb9d5b8102013c71210aaceb1ef83d400a70dc7103634cdb"
        + "b6fead7c7b21025773e967c3adcafc6d2958254c4609360ef7cb5d0cd3c2858a00bcc060f5"
        + "a99e2102abbc5c832a0e7ed5fd4dedd1618d0bf4052a66a0aab7d0f6716ec8c2a7f56d4221"
        + "02f48bd236291c172b83002f51d859627ea926b7d4b83df4cd85327987cee8da0054ae0000"
        + "0000");

    Assert.assertArrayEquals("Expected transactions to match", expected, Hex.decode(tx));
  }

  @Test
  public void testExchangeRate() {
    Path.Builder path = Path.newBuilder()
        .setAccount(0x7FFFFFFF)
        .setIsChange(false)
        .setIndex(0x7FFFFFFF);

    // This input is as big as possible.
    TxInput input = TxInput.newBuilder()
        .setPrevHash(ByteString.copyFrom(Hex.decode("d37365c6a3c089576b1ea1cb8bee2baa005afc96d24080515635f852c680d127")))
        .setPrevIndex(0x7FFFFFFF)
        .setAmount(0x7FFFFFFF7FFFFFFFL)
        .setPath(path)
        .build();

    TxOutput output = TxOutput.newBuilder()
        .setAmount(0x7FFFFFFF7FFFFFFFL)
        .setDestination(Destination.GATEWAY)
        .setPath(path)
        .build();

    List<TxInput> inputs = new ArrayList<>();
    for(int i = 0; i < Constants.INPUTS_COUNT_AT_LEAST; i++) {
      inputs.add(input);
    }

    List<TxOutput> outputs = new ArrayList<>();
    for(int i = 0; i < Constants.OUTPUTS_COUNT_AT_LEAST; i++) {
      outputs.add(output);
    }

    String token = "TokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenToken";
    double localRate = 0.00123;
    CommandRequest request = coldWallet.startTransaction(inputs, outputs, token, localRate);
    assertThat(request.getSignTx().getLocalRate()).isEqualTo(localRate);
  }
}
