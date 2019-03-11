# Core sample output

When the Core starts, the code runs several self checks. We do this because test/integration frameworks typically don't
have HSMs attached to them.

Some number of red lines is therefore expected.

<div style="color: #34bc26; background-color: #000000">[DEBUG] src/main.c:28 in main</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/dev/no_rollback.c:34 reading version from /tmp/.subzero-no-rollback</div>
<div style="color: #afad24; background-color: #000000">[INFO] src/dev/no_rollback.c:48 version match.</div>
<div style="color: #afad24; background-color: #000000">[INFO] src/main.c:40 running self checks.</div>
<div style="color: #afad24; background-color: #000000">[INFO] src/checks/verify_mix_entropy.c:55 verify_mix_entropy: ok</div>
<div style="color: #afad24; background-color: #000000">[INFO] src/checks/verify_protect_pubkey.c:31 verify_protect_pubkey: ok</div>
<div style="color: #afad24; background-color: #000000">[INFO] src/checks/bip32.c:93 verify_bip32 ok</div>
<div style="color: #afad24; background-color: #000000">[INFO] src/checks/check_sign_tx.c:104 checking sign_tx.</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:507 Loaded pubkey 0: tpubD9jBarsLCKot45kvTTu7yWxmNnkBPbpn2CgS1F3yuxZGgohTkamYwJJKenZHrsYwPRJY66
dk3ZUt3aZwZqFf7QGsgUUUcNSvvb9NXHFt5Vb</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:507 Loaded pubkey 1: tpubD8u5eHk8B6q63G4ekfBB2eVCeBYTrW2uCvi7Z3BF6bY2z33jW14Uzna8f5cFQWS3HFwdA
zUWxqESq7j5x2CUQ7uBPgpXnpf8X9FPnh63XYd</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:507 Loaded pubkey 2: tpubD9dkteiWZqa3jL17meqVvy1RSUQsmJvU3hBfAosMzrSa69DacRku8yHy3E2fma1Q4Den25
ukcsBL3bYTFyKjKbF8CEWHu86Xg9YXiY6CkeC</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:507 Loaded pubkey 3: tpubD8GrNWdYHDjdJryH5tng8LUzHSrEo4gToaguKnzthEqTxyfF13jTsp4sMtmso4n1VC58R5W
vt4Ua4npZTecR1xaGGYJgLLQj5sQGdD2xh2N</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:510 Gateway: tpubDA3zCqbkh8BS1h3VGmB6VU1Cj638VJufBcDWSeZVw3aEE9rzvmrNoKyGFqDwr8d9rhf4s
h4Yjg8LVwkehVF3Aoyss1KdDFLoiarFJQvqp4R</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:28 Computing prevout hash, input 0</div>
<div style="color: #34bc26; background-color: #000000">27d180c652f83556518040d296fc5a00aa2bee8bcba11e6b5789c0a3c66573d3</div>
<div style="color: #34bc26; background-color: #000000">00000000</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:516 prevoutsHash</div>
<div style="color: #34bc26; background-color: #000000">1beaae4b6bb87b2892b4ce4ffe0ec228602a8f4ead2d436d029f7fb299b4d31e</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:524 seqHash</div>
<div style="color: #34bc26; background-color: #000000">18606b350cd8bf565266bc352f0caddcf01e8fa789dd8a15386327cf8cabe198</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:308 public_key:<span class="Apple-converted-space"> </span></div>
<div style="color: #34bc26; background-color: #000000">03967ef5011ba1dfa415ad1ea511d206398ba8bbeb50999f4251b878f5b204f197</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:349 hashing output script:</div>
<div style="color: #34bc26; background-color: #000000">1976a914e7df301555cf53bcf6a3aa14f0c1ffcc715d312188ac</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:533 outputHash</div>
<div style="color: #34bc26; background-color: #000000">25289753ae4bd7e1736ad52ad995f6e1fad064c816c3e03972e4ddf0cb348f6a</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:242 hash_input</div>
<div style="color: #34bc26; background-color: #000000">010000001beaae4b6bb87b2892b4ce4ffe0ec228602a8f4ead2d436d029f7fb299b4d31e</div>
<div style="color: #34bc26; background-color: #000000">18606b350cd8bf565266bc352f0caddcf01e8fa789dd8a15386327cf8cabe198</div>
<div style="color: #34bc26; background-color: #000000">27d180c652f83556518040d296fc5a00aa2bee8bcba11e6b5789c0a3c66573d3</div>
<div style="color: #34bc26; background-color: #000000">00000000[DEBUG] src/sign.c:270 script.data:</div>
<div style="color: #34bc26; background-color: #000000">8b522102bda02a4e74ead06632cda4125b9791a6101019ca0c61b718c1d2b6abe42d40472102d5
eeb7618e981f3c7a0fd1fff63839563c73412afca97979a688ad6ec5df6dec210311bd1c8096dbb9f
c08fd52b3fb0ba8fe5ce0319b1dbbf9c0e427af7f73347b0e210364f4cee898bc7048c81593e825
a8989b58b1249aa5cf8337a9c35b55dd181ba754ae</div>
<div style="color: #34bc26; background-color: #000000">40420f0000000000feffffff25289753ae4bd7e1736ad52ad995f6e1fad064c816c3e03972e4ddf0c
b348f6a</div>
<div style="color: #34bc26; background-color: #000000">0000000001000000</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:295 That's it folks. Final input hash:</div>
<div style="color: #34bc26; background-color: #000000">12a32fc89d9ff2fe293465a6b018a6f2d2a4f9bba9be79f71154517c965c50a0</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:569 Signing for pubkey 0</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:596 Successfully validated with public key:</div>
<div style="color: #34bc26; background-color: #000000">0311bd1c8096dbb9fc08fd52b3fb0ba8fe5ce0319b1dbbf9c0e427af7f73347b0e</div>
<div style="color: #34bc26; background-color: #000000">[DEBUG] src/sign.c:603 Signature:</div>
<div style="color: #34bc26; background-color: #000000">3044022052cb0ee5dcd64504044125be2943c13a5df8a703c828f0cab484681bb911fa07022067
fc72678ebfba949b05f47d96ea913396564e44debf17268ebddb519270edfd</div>
<div style="color: #afad24; background-color: #000000">[INFO] src/checks/check_sign_tx.c:130 sign_tx passed</div>
<div style="color: #c33720; background-color: #000000">[ERROR] src/checks/validate_fees.c:118 (next line is expected to show red text...)</div>
<div style="color: #c33720; background-color: #000000">[ERROR] src/sign.c:220 validate_fees: fee underflow for output 1.</div>
<div style="color: #afad24; background-color: #000000">[INFO] src/checks/validate_fees.c:127 verify_validate_fees: ok</div>
<div style="color: #afad24; background-color: #000000">[INFO] src/main.c:47 self-checks passed.</div>
<div style="color: #afad24; background-color: #000000">[INFO] src/main.c:94 waiting for client.</div>
