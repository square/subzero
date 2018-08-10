# Plutus

<img src="logo.png" width="120" height="120">

Square's Bitcoin Cold Storage solution. Cryptographic operations take place on a hardware security modules
(HSMs) in an offline setting. The HSM can be programmed with additional business logic rules.

We use QR codes to import information related to a transaction being signed and use QR codes to export the resulting
signature.

The purpose of this repo is to document various technical design decisions (such as the use of SegWit or Multisig
addresses), as well as to share our code. It is likely that our code only works with our specific HSM model, i.e. you should
treat the code as building blocks and not a finished off-the-shelf solution.

# Architecture
Plutus boots off a Live Linux DVD. A Java GUI application handles scanning and displaying QR codes. The Java application
communicates with the HSM using sockets. The HSM runs the core code, which performs Bitcoin specific operation
(private key derivation, ECC signatures).

      O              +--------+          +------------------------+
     /|\   QR codes  | Java   |  Socket  | Crypto                 |
      |   <--------> | Gui    | <------> | Operations             |
     / \             +--------+          +------------------------+
     Operator         Live DVD            Hardware Security Module

For development and demonstration purpose, we support running the core C code on Mac OS X and Linux, instead of the HSM.

# Trying it out
See demo.iso

# Directory layout

| Directory                | Purpose              |
|--------------------------|----------------------|
| /design-docs             | Design documents     |
| /gui                     | Java GUI             |
| /core                    | C code (runs on HSM) |
| /centos-live-usb-creator | Builds Linux DVD     |
