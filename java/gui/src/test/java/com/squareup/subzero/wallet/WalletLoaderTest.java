package com.squareup.subzero.wallet;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.squareup.subzero.proto.service.Common.EncryptedMasterSeed;
import com.squareup.subzero.proto.service.Common.EncryptedPubKey;
import com.squareup.subzero.proto.wallet.WalletProto.Wallet;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Test;

import static com.google.protobuf.ByteString.copyFromUtf8;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class WalletLoaderTest {
  private Wallet wallet;

  @Before
  public void setup() {
    wallet = Wallet.newBuilder()
        .setCurrency(Wallet.Currency.TEST_NET)
        .setOcsId("7d5b29b6b093f0b444f56fe32e5edfe67aa3d24d")
        .setOcsCardsFile(copyFromUtf8("ocs_cards_file"))
        .setOcsCardOneFile(copyFromUtf8("ocs_card_one"))
        .setMasterSeedEncryptionKeyId("5a3cf4ffa75083f30547eaf669816ddc079c844f")
        .setMasterSeedEncryptionKeyFile(copyFromUtf8("master_seed_encryption_key"))
        .setEncryptedMasterSeed(EncryptedMasterSeed.newBuilder().setEncryptedMasterSeed(copyFromUtf8("encrypted_master_seed")))
        .addEncryptedPubKeys(EncryptedPubKey.newBuilder().setEncryptedPubKey(copyFromUtf8("encrypted_pub_key_1")))
        .addEncryptedPubKeys(EncryptedPubKey.newBuilder().setEncryptedPubKey(copyFromUtf8("encrypted_pub_key_2")))
        .addEncryptedPubKeys(EncryptedPubKey.newBuilder().setEncryptedPubKey(copyFromUtf8("encrypted_pub_key_3")))
        .addEncryptedPubKeys(EncryptedPubKey.newBuilder().setEncryptedPubKey(copyFromUtf8("encrypted_pub_key_4")))
        .build();
  }

  @Test
  public void ensureDoesNotExist() throws Exception {
    File tempDir = Files.createTempDir();
    tempDir.deleteOnExit();

    // Should fail with empty directory
    WalletLoader walletLoader = new WalletLoader(tempDir.toPath());
    assertThatThrownBy(() -> walletLoader.ensureDoesNotExist(1234))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("marker file not found");

    // Should pass with empty directory + marker file + wallet file
    writeMarkerFile(tempDir);

    walletLoader.save(1234, Wallet.newBuilder().build());

    assertThatThrownBy(() -> walletLoader.ensureDoesNotExist(1234))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already exists");

    // Should pass with empty directory + marker file
    walletLoader.ensureDoesNotExist(9999);
  }

  @Test
  public void save() throws Exception {
    File tempDir = Files.createTempDir();
    tempDir.deleteOnExit();

    WalletLoader walletLoader = new WalletLoader(tempDir.toPath());
    // Saving should fail because there's no marker file
    assertThatThrownBy(() -> walletLoader.save(1234, wallet))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("marker file not found");

    // Add marker file
    writeMarkerFile(tempDir);

    // Save and check file's content
    walletLoader.save(1234, wallet);

    byte[] bytes = Files.toByteArray(tempDir.toPath().resolve("subzero-1234.wallet").toFile());
    byte[] expected = Resources.toByteArray(Resources.getResource("fixture.wallet"));

    System.out.println(new String(bytes));

    assertThat(bytes).isEqualTo(expected);
  }

  @Test
  public void load() throws Exception {
    File tempDir = Files.createTempDir();
    tempDir.deleteOnExit();
    WalletLoader walletLoader = new WalletLoader(tempDir.toPath());

    // Loading should fail because there's no marker file
    assertThatThrownBy(() -> walletLoader.load(1234))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("marker file not found");

    writeMarkerFile(tempDir);

    // Save and reload the file
    walletLoader.save(1234, wallet);
    Wallet roundtrip = walletLoader.load(1234);

    assertThat(roundtrip).isEqualTo(wallet);
  }

  @Test
  public void walletLoaderBadFilePaths() throws Exception {
    assertThatThrownBy(() -> new WalletLoader("\0"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static void writeMarkerFile(File tempDir) throws IOException {
    Path f = tempDir.toPath().resolve(WalletLoader.MARKER_FILE);
    FileWriter fileWriter = new FileWriter(f.toFile());
    fileWriter.write("");
    fileWriter.close();
  }
}
