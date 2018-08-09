# Software Components

## Coordinator service

A Coordinator service is required to manage UTXO, initiate a signing ceremony and merge signatures.

The coodrinator service and the Core need to agree on the gateway wallet.

## Core

The Core code (C code) runs inside the HSM. The code opens a TCP socket and receives commands encoded using protobufs.

The Core code performs AES encryption/decryption using a nCipher specific mechanism called Tickets.

The HSM's endian-ness differs from our development environment. As a result, we wrote the code to be endianness agnostic.

The Core and the UI need to agree on the version and multisig parameters. The Core and the Coordinator service need to
agree on the gateway wallet.

## UI

The UI (Java code) runs outside the HSM. The UI encodes commands using protobufs and sends them over the
TCP socket. The application renders graphics by directly drawing to a Linux framebuffer.

The UI also contains a bunch of utility functions, such as a base45 encoder/decoder. Code to merge the
signatures and build a Segwit + Multisig transaction, etc.

Java was chosen for two reasons:
1. Square maintains a large Java codebase. Engineers are comfortable using Java.
2. Languages other than C, Python, or Java, would require writing bindings to interact with the HSM.

The UI and the Core need to agree on the version and multisig parameters.

## QR codes

Bespoke QR codes are used to trigger operations and read the results. The QR codes contain base45-encoded protobufs.

## Live USB Creator

Scripts which leverage Vagrant to generate a DVD. The DVD contains a signed artifact of the Core code and the UI as a
jar.

# Beancounter

Go program to audit balance of gateway and cold wallets.

## DVD Label

Go program used to print DVD labels.
