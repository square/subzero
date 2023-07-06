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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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

    IllegalStateException e1 = assertThrows(IllegalStateException.class, () -> walletLoader.ensureDoesNotExist(1234));
    assertTrue(e1.getMessage().contains("marker file not found"));

    // Should pass with empty directory + marker file + wallet file
    writeMarkerFile(tempDir);

    walletLoader.save(1234, Wallet.newBuilder().build());

    IllegalStateException e2 = assertThrows(IllegalStateException.class, () -> walletLoader.ensureDoesNotExist(1234));
    assertTrue(e2.getMessage().contains("already exists"));

    // Should pass with empty directory + marker file
    walletLoader.ensureDoesNotExist(9999);
  }

  @Test
  public void save() throws Exception {
    File tempDir = Files.createTempDir();
    tempDir.deleteOnExit();

    WalletLoader walletLoader = new WalletLoader(tempDir.toPath());
    // Saving should fail because there's no marker file
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> walletLoader.save(1234, wallet));
    assertTrue(e.getMessage().contains("marker file not found"));

    // Add marker file
    writeMarkerFile(tempDir);

    // Save and check file's content
    walletLoader.save(1234, wallet);

    byte[] bytes = Files.toByteArray(tempDir.toPath().resolve("subzero-1234.wallet").toFile());
    byte[] expected = Resources.toByteArray(Resources.getResource("fixture.wallet"));

    System.out.println(new String(bytes));

    assertArrayEquals(expected, bytes);
  }

  @Test
  public void load() throws Exception {
    File tempDir = Files.createTempDir();
    tempDir.deleteOnExit();
    WalletLoader walletLoader = new WalletLoader(tempDir.toPath());

    // Loading should fail because there's no marker file
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> walletLoader.load(1234));
    assertTrue(e.getMessage().contains("marker file not found"));

    writeMarkerFile(tempDir);

    // Save and reload the file
    walletLoader.save(1234, wallet);
    Wallet roundtrip = walletLoader.load(1234);

    assertEquals(wallet, roundtrip);
  }

  @Test
  public void walletLoaderBadDirectory() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> new WalletLoader("\0"));
  }

  @Test
  public void walletLoaderDirectoryDoesNotExist() throws Exception {
    WalletLoader walletLoader = new WalletLoader("/abc");

    // Loading should fail because the directory does not exist and subsequently no marker file.
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> walletLoader.load(1234));
    assertTrue(e.getMessage().contains("marker file not found"));
  }

  @Test
  public void walletLoaderDirectoryIsFile() throws Exception {
    File tempDir = Files.createTempDir();
    tempDir.deleteOnExit();
    Path f = tempDir.toPath().resolve("filename");
    FileWriter fileWriter = new FileWriter(f.toFile());
    fileWriter.write("");
    fileWriter.close();
    WalletLoader walletLoader = new WalletLoader(f);

    // Loading should fail because the path points to a file and subsequently no marker file.
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> walletLoader.load(1234));
    assertTrue(e.getMessage().contains("marker file not found"));
  }


  private static void writeMarkerFile(File tempDir) throws IOException {
    Path f = tempDir.toPath().resolve(WalletLoader.MARKER_FILE);
    FileWriter fileWriter = new FileWriter(f.toFile());
    fileWriter.write("");
    fileWriter.close();
  }
}
