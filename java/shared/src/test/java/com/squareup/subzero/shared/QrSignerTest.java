package com.squareup.subzero.shared;

import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.interfaces.ECPrivateKey;

import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;
import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.util.io.pem.PemReader;
import org.spongycastle.util.io.pem.PemWriter;
import javax.security.auth.DestroyFailedException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static java.util.Arrays.asList;

public class QrSignerTest {

    @Before
    public void setup() {
        //private key with MSB 0.
        /*
        String file_content = "-----BEGIN PRIVATE KEY-----\n" +
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgMZroVD6Hy2ql7wDQ\n" +
                "DVVNPZuXUwrx2R4dgYyUmNiln+OhRANCAATK4eoZu+QwEI97Kjhm62cL+fd2XIR/\n" +
                "QzFe9JqUiiW23rexw0HvNnpM3hXn89H6SCqQjJVN986Up9PP/MDd0gF9\n" +
                "-----END PRIVATE KEY-----";
        */
        //private key with MSB 1.
        String file_content = "-----BEGIN PRIVATE KEY-----\n" +
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg65jRx/BQnYcR+A7Z\n" +
                "1DBYx/o1zabedQm5u+p9dbv/dKuhRANCAAR85NGDEhy1NwYZqqDg8nbd+XKjrgH9\n" +
                "UlYzrL3xrmB28vDSUeegT6v6/dK/SyDpUTJ3qFCrMd2Ca0dOpb9zQl02\n" +
                "-----END PRIVATE KEY-----";

        try {
            Files.write(Paths.get("/tmp/secret"), file_content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void SignTestDeterministic() throws DestroyFailedException {
        QrSigner signer = new QrSigner();
        byte[] bytes_to_sign = "Subzero".getBytes(StandardCharsets.UTF_8);
        byte[] signature = signer.sign(bytes_to_sign);
        byte[] signature2 = signer.sign(bytes_to_sign);
        assertArrayEquals(signature, signature2);
        assertTrue(signer.verify(signature, bytes_to_sign));
        assertEquals(65, signer.dumpPublicKey().length);
        System.out.println("bytes to sign = " + Hex.toHexString(bytes_to_sign));
        System.out.println("public key = " + Hex.toHexString(signer.dumpPublicKey()));
        System.out.println("Signature = " + Hex.toHexString(signature));
        assertEquals(
            "d4c868713c414cd962641a798c3ae874a1d0e3e4793208b4e086e66a35f5f1f38cbd683a578c8b47033748cdaee54e8f25305011f7902f71f993bf0e051b2a14",
            Hex.toHexString(signature));
        assertEquals(
            "047ce4d183121cb5370619aaa0e0f276ddf972a3ae01fd525633acbdf1ae6076f2f0d251e7a04fabfafdd2bf4b20e9513277a850ab31dd826b474ea5bf73425d36",
            Hex.toHexString(signer.dumpPublicKey()));
        signer.destroy();
    }

    /*
    This test exhaustively demonstrates the earlier bug and also demonstrates the fix as working.
    The bug is that the sign method was parsing the 32 byte private key as a signed integer.
    It is an unsigned integer.
     */
    @Test
    public void SignTestError() throws IOException {
        String file_content = "-----BEGIN PRIVATE KEY-----\n" +
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgMZroVD6Hy2ql7wDQ\n" +
                "DVVNPZuXUwrx2R4dgYyUmNiln+OhRANCAATK4eoZu+QwEI97Kjhm62cL+fd2XIR/\n" +
                "QzFe9JqUiiW23rexw0HvNnpM3hXn89H6SCqQjJVN986Up9PP/MDd0gF9\n" +
                "-----END PRIVATE KEY-----";
        try {
            Files.write(Paths.get("/tmp/secret"), file_content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte [] bytes_to_sign = Hex.decode("ffaabb");
        QrSigner signer = new QrSigner();

        //public keys match because MSB of key is 0.
        assertArrayEquals(signer.dumpIncorrectPublicKeyTest(), signer.dumpPublicKey());
        // signatures match because MSB of key is 0.
        assertArrayEquals(signer.incorrectSignTest(bytes_to_sign), signer.sign(bytes_to_sign));

        String file_content_msb_1 = "-----BEGIN PRIVATE KEY-----\n" +
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg65jRx/BQnYcR+A7Z\n" +
                "1DBYx/o1zabedQm5u+p9dbv/dKuhRANCAAR85NGDEhy1NwYZqqDg8nbd+XKjrgH9\n" +
                "UlYzrL3xrmB28vDSUeegT6v6/dK/SyDpUTJ3qFCrMd2Ca0dOpb9zQl02\n" +
                "-----END PRIVATE KEY-----";

        Files.write(Paths.get("/tmp/secret"), file_content_msb_1.getBytes(StandardCharsets.UTF_8));

        QrSigner signer_msb1 = new QrSigner();
        String correct_pubkey = "047ce4d183121cb5370619aaa0e0f276ddf972a3ae01fd525633acbdf1ae6076f2f0d251e7a04fabfafdd2bf4b20e9513277a850ab31dd826b474ea5bf73425d36";
        //incorrect pubkey generated.
        assertNotEquals(correct_pubkey, Hex.toHexString(signer_msb1.dumpIncorrectPublicKeyTest()));
        //correct pubkey generated.
        assertEquals(correct_pubkey, Hex.toHexString(signer_msb1.dumpPublicKey()));
        // just to check again.
        assertNotEquals(asList(signer_msb1.dumpPublicKey()), asList(signer_msb1.dumpIncorrectPublicKeyTest()));
        //different signatures generated.
        assertNotEquals(asList(signer_msb1.sign(bytes_to_sign)), asList(signer_msb1.incorrectSignTest(bytes_to_sign)));

        System.out.println("SignTestError: This Test Ran");
    }

    @Test
    public void SignerTestRandomKeyRandomBytes() throws DestroyFailedException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());

        KeyPair pair = gen.generateKeyPair();
        PemWriter writer = new PemWriter(new FileWriter(QrSigner.secret_path));
        writer.writeObject(new PemObject("PRIVATE KEY", pair.getPrivate().getEncoded()));
        writer.close();
        QrSigner signer = new QrSigner();
        byte[] bytes_to_sign = new byte[100];
        new SecureRandom().nextBytes(bytes_to_sign);

        byte[] signature = signer.sign(bytes_to_sign);
        byte[] signature2 = signer.sign(bytes_to_sign);
        assertArrayEquals(signature, signature2);
        assertTrue(signer.verify(signature, bytes_to_sign));
        assertEquals(65, signer.dumpPublicKey().length);
        assertEquals(64, signature.length);

        signer.destroy();
    }
}
