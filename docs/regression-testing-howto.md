# How to set up and do transaction signing regression (black box) testing for subzero core

*Currently, we have only implemented regression testing for a subzero core
'dev' target. Support of the nCipher target will be added later. Some ideas
about how to do it are described in Section "Future Work" of this document.*

## How to run regression tests

1. Build subzero code (core, development server and user interface) as
instructed in [subzero
documentation](https://subzero.readthedocs.io/en/master/running_without_hsm/).
2. Start subzero core in a terminal

   ```bash
   # under subzero repository directory root
   ./core/build/subzero
   ```

3. In another terminal, invoke subzero GUI with option `--signtx-test`

   ```bash
   # under subzero repository directory root
   ./java/gui/target/gui-1.0.0-SNAPSHOT-shaded.jar --signtx-test
   ```

## How to create test vectors

An outline of the test vector creation process is as follows.

1. Run subzero components locally exactly as described in [subzero
documentation](https://subzero.readthedocs.io/en/master/running_without_hsm/).
Put the follow content in `/data/app/subzero/wallets/subzero-1492.wallet`

   ```text
   {"currency":"TEST_NET","encrypted_master_seed":{"encrypted_master_seed":"ioBg3WF2BntMnGaebOtI+HDcTHVkaMonIplOcp+6i83P9Cjb6r7+5T9KDrlN8Np9MyIm6vh74M348X7oJMkCG6cY5endbgf/sofKn5OcpOw86+rtI8fCAyLNddM="},"encrypted_pub_keys":[{"encrypted_pub_key":"OytXbV6n2L0l50yLa5fXe0z8N84MU1Ci3jQyOy87Id1zMJCG33Wqg5rVv6KJED8fmjg8s0rsMJaiCe9o47OHrFg9SH37SxIa4IGVU3WEjuFVTOAxFbOJkY0gE2XlSzGUtO+MGzvcayS4JSqDdsRHN2pKBvJDDi92oxMMpi0C8uuu9vUiIYa9IA8f5g=="},{"encrypted_pub_key":"YxgmbmaiwGON1uHpO1U51f9BcZAruJn436kjCZZxQBXqbcxJcd2L0k7HNDhGK7Q7g+bbqKnhEoS+Cqjz4FqSv+yGmfzLqwvy9g/XIu44B37VSXLh5bsyBl1hh8sljNVIwbYWRWjhiCGE57g0P2o2Uj63cBE9wRpwKUa6RTlRWb2Mmw3GnL+xn9n0Bw=="},{"encrypted_pub_key":"i36ne27C7pv1psRF7iF3FtA9D+DYzL9dGLNG2al96iZYOhXW3fWxB1WmgapH4Qjwp32cNosGJ5xIqPGcOebe/lUYfJy9nAJktqNb+9m9cIXRAEBR2Aem07Y3iVBPXwbrsEGrwaIlghXSU35d8s3nGgmJHpb/+ILUK+XipCW6f6laVM8jC39lq8od5Q=="},{"encrypted_pub_key":"s/1O2nYnApJd+MlcGpLARYL7pudrwDSrF/xLc6Lj5WgaKquFlJFFlTblYtOSyA8O7jiz253gvzFmK1oVmr2hqw7rW0kAliBzpQ/sbiZnQYHnGVw5582vRTmixQmJD7EtlOXsuY3zcniZZjJm6oMRMxO0QGQVmewgW3jyfnB8ed5Nh3jCTs/eMwWWNQ=="}]}
   ```

2. Start subzero core and the development server as normal

   In one terminal

   ```bash
   # under subzero repository directory root
   ./core/build/subzero
   ```

   In another terminal

   ```bash
   # under subzero repository directory root
   ./java/server/target/server-1.0.0-SNAPSHOT.jar server
   ```

3. Run python script `txsign_testcase_expect_scrypt_gen.py` to generate an
[expect script](https://core.tcl-lang.org/expect/index) that will later be
used to generate test vector files.

   ```bash
   # under subzero repository directory root
   python3 ./utils/txsign_testcase_expect_script_gen.py > /tmp/subzero_expect.sh
   chmod +x /tmp/subzero_expect.sh
   ```

   Under the hood in `txsign_testcase_expect_script_gen.py`:

   - Generate 255 (17x5x3) test vectors in JSON based on a fixed RNG seed
   (1492), as well as the number of inputs (MAX 17), the number of outputs
   (MAX 5) and the size (LARGE, MEDIUM or SMALL) of satoshi amount in a
   transaction to be signed.
   - POST the auto-generated JSON payloads in HTTP requests to the local
   development server's `generate-qr-code/sign-tx-request` API endpoint to
   retrieve base64-encoded, proto-buffer-formatted sign-transaction requests
   for the GUI to send to subzero core later.
   - Construct an expect script using the base64-encoded requests to
   interact with the GUI automatically, and output the expect script to
   STDOUT.

4. Run the expect script generated in the last step to create a log file
`expect_subzero.log`.

   ```bash
   # under subzero repository directory root
   # remove existing log file, if any
   rm expect_subzero.log
   # invoke expect script, pointing to the GUI jar
   /tmp/subzero_expect.sh ./java/gui/target/gui-1.0.0-SNAPSHOT-shaded.jar
   # 'expect_subzero.log' should show up below
   ls -l expect_subzero.log
   ```

5. Run python script `txsign_testcase_from_expect_log.py` to parse the
expect script log, and generate test vector files in `/tmp/out_dir`

   ```bash
   # under subzero repository directory root
   mkdir -p /tmp/out_dir
   python3 utils/txsign_testcase_from_expect_log.py -i ./expect_subzero.log -o /tmp/out_dir
   # test vector files should now show up
   ls -l /tmp/out_dir
   ```

Here is the content of a sample test vector (`cat /tmp/out_dir/valid-0000`).

```text
request:ENQLKlMKMgogxDWtt0dgwAmN4ISc0MOWwGTTmUUiiIo4nckDVeaiA/QQABjVutiZprYJIgQQABgGEhAIh6nDxsznCBACGgQQABgDGAAiACkAAAAAAAAAAA==
response:GmwKagpGMEQCIGqjMmRmArvztBhGKo8pVoAB/F1CF95+k7QpHrqM71ChAiADWXQUdPM27W7YkvEe7EEqnM1edgmOe82T44uNkC+4xBIg/Ej2ebikCgjve0pWlpog295vbxDH8+p+Dewiwe8qs/8=
```

The request and response in the above example can be displayed in JSON using
the development server's "Pretty print" utility.

![Subzero server: pretty print](./subzero-server-pprint.png)

## How to add new test vectors for regression test

- How to create new test vectors. There are multiple ways

  - Rotate the RNG seed in `txsign_testcase_expect_script_gen.py` to
  generate another set of 255 test vectors
  - Hack up `txsign_testcase_expect_script_gen.py` to add new test vector
  generation logic
  - Hand craft test transactions in JSON, and convert them into test vectors
  following a procedure similar how `txsign_testcase_expect_script_gen.py`
  does it, as described in the previous section

- How to make test vectors available for subzero GUI

  - Copy all test vector files to subzero's source directory
  `java/gui/src/main/resources/txsign-testvectors/`. Prefix valid (positive)
  test vector file names with "valid-" (e.g., "valid-0042"), and prefix invalid
  (negative) test vector file names with "invalid".
  - Rebuild the GUI jar, and run the regression test
  - Check the test vectors into the subzero repository

## A few words on the design of this testing approach

We decided to use version controlled test vector files as oppose to
generating them dynamically during the test, for the following reasons:

- Version controlled test vectors more auditable and easily reproducible
- It makes it possible to separate the test vector generation process (a
one-time thing) from the GUI logic
- It makes it possible to re-use existing subzero components (server, core)
for test vector creation, avoiding duplication of functionality

## Future work

- [Preferred] Add support of regression testing for the nCipher target (in
staging)
  - We need to generate test vectors for the HSM target. We can use a known
  dummy master key seed for both dev and staging testing, and use (dummy)
  master key encryption key (which happens to be the same as the public key
  encryption key for the nCipher target) known to the staging world.
  - It's possible to use the same test vectors for both the dev and nCipher
  (staging) target

- [Not preferred] Use wallet generated on the fly instead of a hardcoded one

  - Instead of hardcoding a wallet for the test, we can hardcode the dummy
  master key encryption (as well as the public key encryption key) in the
  GUI, generate the master keys, the wallets, and the associated test vectors
  dynamically. Because the wallet handling logic is relatively simple, the
  increased test coverage seems to be marginal with this approach. Therefore,
  we lean toward not to go in this direction to avoid adding complexity to
  the test design.
