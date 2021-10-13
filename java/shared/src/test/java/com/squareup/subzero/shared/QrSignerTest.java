package com.squareup.subzero.shared;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;
import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.util.io.pem.PemReader;
import org.spongycastle.util.io.pem.PemWriter;
import javax.security.auth.DestroyFailedException;

public class QrSignerTest {

    @Before
    public void setup() {
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
    }
    private void writeRandomKey(){

    }
    @Test
    public void SignTestDeterministic() {
        QrSigner signer = new QrSigner();
        byte[] bytes_to_sign = "Subzero".getBytes(StandardCharsets.UTF_8);
        byte[] signature = signer.sign(bytes_to_sign);
        byte[] signature2 = signer.sign(bytes_to_sign);
        assertThat(signature.equals(signature2));
        assertThat(signer.verify(signature, bytes_to_sign));
        assertThat(signer.dumpPublicKey().length == 65);
        System.out.println("bytes to sign = " + Hex.toHexString(bytes_to_sign));
        System.out.println("public key = " + Hex.toHexString(signer.dumpPublicKey()));
        System.out.println("Signature = " + Hex.toHexString(signature));
        assertThat(Hex.toHexString(signature).equals("63ce6fb96b6ee7c0b488304da55bedbd12dd5c19c15cf047df6af56d64f6316062e6b12842919135f498b7d7a963b14c4192ed489ae24f2fe56274408502dd63"));
        try {
            signer.destroy();
        } catch (DestroyFailedException e){
            assertThat(false);
        }
    }

    @Test
    public void SignerTestRandomKeyRandomBytes() {
        try {
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
            assertThat(signature.equals(signature2));
            assertThat(signer.verify(signature, bytes_to_sign));
            assertThat(signer.dumpPublicKey().length == 65);
            assertThat(signature.length == 64);

            try {
                signer.destroy();
            } catch (DestroyFailedException e){
                assertThat(false);
            }

        }catch(NoSuchAlgorithmException | InvalidAlgorithmParameterException | IOException e){
            throw new RuntimeException(e);
        }
    }



}
