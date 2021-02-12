# External Crypto

This directory contains external cryptography code (currently Brian Gladman's
AES GCM) that's not part of `trezor-crypto`. We have decided to put foreign
code here so that it will be easier to update `trezor-crypto` in the future.

Code in the follow directories are taken from the following repositories:

- `aes`: https://github.com/BrianGladman/aes/: HEAD commit 8798ad829374cd5ff312f55ba3ccccfcf586fa11
- `modes`: https://github.com/BrianGladman/modes: HEAD commit af9dd1bd01ac80f543fd5ff3e1953f388a601415

We maintain our own CMakeLists.txt.
