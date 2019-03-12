# Address derivation

After each location has finalized their wallet, the Coordinate service is able to use all the xpub to derive
addresses.

The following derivation scheme is used inside the HSM:

```no-highlight
m / coin_type' / change / address_index
```

The Coordinator service thus computes the `change / address_index` derivation.

# Multisig + segwit

A multisig script is then computed. For example, with the sample transaction:

```no-highlight
echo '5221022dc4f3706655e8c11685bfe1978e930bcbba1d83269f385c5242b00905a3253c2102359b50d66f4439fc135e558e4f09631aa8d1688553449cf794bf12f88d0877962102fd5283f39419f559dbc16a5ea6a721f22de21d224bc665b64c59102dfcc5253021036dd870c3272a0d1f1f33ee382ad9f3d7b370021e16b78528220907db41aa7d2454ae' | bx script-decode
2 [022dc4f3706655e8c11685bfe1978e930bcbba1d83269f385c5242b00905a3253c] [02359b50d66f4439fc135e558e4f09631aa8d1688553449cf794bf12f88d087796] [02fd5283f39419f559dbc16a5ea6a721f22de21d224bc665b64c59102dfcc52530] [036dd870c3272a0d1f1f33ee382ad9f3d7b370021e16b78528220907db41aa7d24] 4 checkmultisig
```

This script is wrapped in a pay-to-script-hash (P2SH), with the resulting address `a914fa...`:
```no-highlight
echo '5221022dc4f3706655e8c11685bfe1978e930bcbba1d83269f385c5242b00905a3253c2102359b50d66f4439fc135e558e4f09631aa8d1688553449cf794bf12f88d0877962102fd5283f39419f559dbc16a5ea6a721f22de21d224bc665b64c59102dfcc5253021036dd870c3272a0d1f1f33ee382ad9f3d7b370021e16b78528220907db41aa7d2454ae' | bx sha256
5211be1c60bb65d02a0fbc2b0d9ce77c83f680a1414248ca7a36232e2292ea4a
echo '00205211be1c60bb65d02a0fbc2b0d9ce77c83f680a1414248ca7a36232e2292ea4a' | bx bitcoin160  
faf4d7354aae5846569530283b31875e8439a2f1
echo 'hash160 [faf4d7354aae5846569530283b31875e8439a2f1] equal' | bx script-encode
a914faf4d7354aae5846569530283b31875e8439a2f187
```
