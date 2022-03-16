package com.squareup.subzero.shared;

import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.signers.ECDSASigner;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.signers.HMacDSAKCalculator;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.io.pem.PemReader;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

/**
 * Bespoke class to provide signing capabilities for Qr Codes.
 * Only supports NIST P256 ECDSA signing
 * TODO?: Make this more generic and an abstraction for curves.
 */
public class QrSigner implements Destroyable {
    private boolean is_destroyed;
    private byte[] key;
    private ECDomainParameters domain;
    // equivalent of NISTP256 in the trezor crypto library on the HSM.
    //https://tools.ietf.org/search/rfc4492#appendix-A
    private String curve_name = "secp256r1";
    public static String secret_path = "/tmp/secret";

    /**
     * Zeroize what can be done.
     * TODO: BigInteger is still there somewhere and it needs to be handled.
     *
     * @throws DestroyFailedException
     */
    @Override
    public void destroy() throws DestroyFailedException {
        Arrays.fill(this.key, (byte) 0);
        this.is_destroyed = true;
    }

    @Override
    public boolean isDestroyed() {
        return is_destroyed;
    }

    /**
     * Read a pkcs8 encoded private key file.
     * @return raw key bytes.
     */
    private static byte[] readSecret() {
        try {
            PemReader re = new PemReader(new FileReader(QrSigner.secret_path));
            KeyFactory fac = KeyFactory.getInstance("EC");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(re.readPemObject().getContent());
            ECPrivateKey key =  (ECPrivateKey) fac.generatePrivate(spec);
            //assure that the secret is zero padded.
            return BigIntegers.asUnsignedByteArray(32,key.getS());
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            //should always be able to read the secret path.
            //should always be a key in the expected format.
            throw new RuntimeException(e);
        }
    }

    /**
     * @param path a String representing the absolute path.
     */
    public static void setSecretPath(String path) {
        QrSigner.secret_path = path;
    }

    public QrSigner() {
        this(QrSigner.readSecret());
    }

    /**
     * @param key bytes for the secret scalar. Big Endian.
     */
    public QrSigner(byte[] key) {
        if (key.length != 32) {
            throw new RuntimeException("Private Key Length not as expected.");
        }
        this.key = key;

        X9ECParameters curve = SECNamedCurves.getByName(curve_name);
        this.domain = new ECDomainParameters(curve.getCurve(), curve.getG(), curve.getN(), curve.getH());
        this.is_destroyed = false;
    }

    /**
     * @param data helper to hash bytes.
     * @return sha256 digest.
     */
    private byte[] sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sign the input bytes using the ECDSA algorithm for the NIST 256 CURVE.
     * Input bytes should not be pre hashed.
     * Signing is deterministic. No random k.
     *
     * @param data bytes to sign
     * @return raw signature bytes. Always of length 64.
     */
    public byte[] sign(byte[] data) {
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        //BigIntegers.fromUnsignedByteArray is the way to do it.
        signer.init(true, new ECPrivateKeyParameters(BigIntegers.fromUnsignedByteArray(this.key, 0, 32), this.domain));
        byte[] hashed_data = this.sha256(data);

        BigInteger[] signature = signer.generateSignature(hashed_data);
        ByteArrayOutputStream ret = new ByteArrayOutputStream();
        ret.write(BigIntegers.asUnsignedByteArray(32, signature[0]),0, 32);
        ret.write(BigIntegers.asUnsignedByteArray(32, signature[1]),0, 32);
        return ret.toByteArray();
    }

    // Only added to demonstrate error
    public byte[] incorrectSignTest(byte[] data) {
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        // BigInteger is signed. This is wrong. Just for Test.
        signer.init(true, new ECPrivateKeyParameters(new BigInteger(this.key), this.domain));
        byte[] hashed_data = this.sha256(data);

        BigInteger[] signature = signer.generateSignature(hashed_data);
        ByteArrayOutputStream ret = new ByteArrayOutputStream();
        ret.write(BigIntegers.asUnsignedByteArray(32, signature[0]),0, 32);
        ret.write(BigIntegers.asUnsignedByteArray(32, signature[1]),0, 32);
        return ret.toByteArray();
    }

    /**
     * @param signature raw signature bytes as obtained from `sign`.
     * @param data      bytes to verify the signature against.
     * @return
     */
    public boolean verify(byte[] signature, byte[] data) {
        if (signature.length != 64) {
            throw new RuntimeException("Input signature of incorrect length.");
        }
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        signer.init(false, new ECPublicKeyParameters(this.domain.getG().multiply(BigIntegers.fromUnsignedByteArray(this.key, 0 ,32)), this.domain));

        byte[] hashed_data = sha256(data);

        return signer.verifySignature(hashed_data, BigIntegers.fromUnsignedByteArray(signature, 0, 32), BigIntegers.fromUnsignedByteArray(signature, 32, 32));
    }

    public byte[] dumpPublicKey() {
        ECPoint base = this.domain.getG();
        //BigIntegers.fromUnsignedByteArray is the way to do it.
        ECPoint pub = base.multiply(BigIntegers.fromUnsignedByteArray(this.key, 0, 32));
        return pub.getEncoded(false);

    }
    // Only added to demonstrate and test for a prior Bug.
    public byte[] dumpIncorrectPublicKeyTest() {
        ECPoint base = this.domain.getG();
        // BigInteger is signed. This is wrong. Just for Test.
        ECPoint pub = base.multiply(new BigInteger(this.key));
        return pub.getEncoded(false);

    }
}
