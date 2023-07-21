package com.squareup.subzero.observers;

import com.squareup.subzero.proto.service.Internal;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * <p>Interface for an observer that writes each
 * {@link com.squareup.subzero.proto.service.Internal.InternalCommandRequest} as a separate file to an output directory,
 * before the request is sent to Subzero Core by {@link com.squareup.subzero.InternalCommandConnector}.</p>
 *
 * <p>This is used together with the --signtx-test and --generate-wallet-files-test options to build an initial
 * LLVM libfuzzer corpus.</p>
 *
 * <p>Each file is named as the hex-encoded SHA-1 hash of its contents, which is the same naming convention that
 * LLVM's libfuzzer itself uses for naming novel inputs as they are discovered.</p>
 */
public class CreateFuzzerCorpusObserver implements InternalCommandRequestObserver {
    private final Path outputDir;

    public CreateFuzzerCorpusObserver(final Path outputDir) {
        this.outputDir = outputDir;
    }

    @Override
    public void observe(Internal.InternalCommandRequest internalRequest) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            internalRequest.writeDelimitedTo(baos);
            byte[] serializedRequest = baos.toByteArray();
            // LLVM's libfuzzer names each file as sha1(file contents).
            // Let's do the same when seeding the initial corpus.
            String sha1hex = sha1(serializedRequest);
            FileOutputStream output = new FileOutputStream(outputDir.resolve(sha1hex).toFile());
            output.write(serializedRequest);
            output.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String sha1(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(data);
            return org.bitcoinj.core.Utils.HEX.encode(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
