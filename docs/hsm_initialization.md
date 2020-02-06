# HSM initialization

Before any operation can take place, all the HSMs need to be initialized. For various reasons, we decided to enroll
all the HSMs in the same Security World.

A Security World (or World for short) is a concept, which allows HSMs to share key material for redundancy purpose. Two
HSMs enrolled in the same World are essentially identical. You can think of the World as the master encryption key,
which other keys are then encrypted under.

Enrolling a HSM in a World involves administrator smart cards. Blank cards are used to create a new World. Enrolling
a HSM in an existing World requires the corresponding administrator smart cards.

## Wallet World

Using the tools provided with the HSM, we create the wallet World. The smart cards which back this world need to
be stored in a very secure location. They will be needed in the following cases:

- a number of HSMs (or locations) were damaged and the remaining locations no longer form a quorum.
- operators have lost their smart cards (or the passwords thereof) and new smart cards need to be issued.

A 256-bit AES key (pubKey encryption key) is created at the same time as the wallet World. This key is used at wallet
initialization time.

When enrolling HSMs in the wallet World, the administrator need to initialize the nvram for rollback protection purpose.

Typically, administrators will also want to create a staging and a dev World. Creating all security worlds at the same
time is convenient.

## Signing World

A different World is used to sign the Core code. This creates a separation of duty: the engineers who sign and release
the Core code don't have direct access to decrypt wallets.

Ideally, the public part of the signing keys would be exported from the signing World, into the main World (and staging
and dev Worlds as well). This is however not possible with our HSMs. We therefore have to move the signing key from
the signing world to the wallet world. It also implies that the Core code needs to be the signed for each environment.
Typically, this implies having three binaries (dev, staging and production).

We shred the smart card which backs the signing keys in the wallet world. Keep in mind that a quorum of ACS can be
used to recover the shredded card.

## Commands

The following commands create two security worlds, SEE keys, etc. We use a quorum of 1-of-1 as an example. For a
real system, you should carefully think about your quorum needs.

You will need at least 5 blank smartcards.

```
# These instructions are going to assume your tools are installed in the default location. The instructions have been
# tested on software v12.20.51 (which is the newest software version which is not severely broken!) with firmware
[~]$ cd /opt/nfast/bin


# If needed, use ./fet to load SEE license.
[bin]$ ./fet
... follow instructions on screen ...


# Put HSM in initialization mode. Assumes remote mode override is enabled (jumper E in off position)
[bin]$ ./nopclearfail -I -m 1
Module 1, command ClearUnitEx: OK


# Create the signing security world + signing ACS. Command prompts for blank card(s).
# Note: expect the first command after running nopclearfail to be (very) slow.
[bin]$ ./new-world -i -m 1 -Q 1/1

Create Security World:
Module 1: 0 cards of 1 written
Module 1 slot 0: empty
Module 1 slot 0: unformatted card
Module 1 slot 0:- passphrase specified - writing card
Card writing complete.

security world generated on module #1; hknso = 59875f07a8b506b0063662c2628956d75960da5f


# Remove and label the card(s) (e.g. "Subzero Signing ACS #1 (1-of-1)").
∅


# Put HSM in operational mode.
[bin]$ ./nopclearfail -O -m 1
Module 1, command ClearUnitEx: OK


# Create signing OCS. Command prompts for blank card(s).
[bin]$ ./createocs -m 1 -Q 1/1 -N subzero-signing-ocs


Creating Cardset:
Module 1: 0 cards of 1 written
Module 1 slot 0: empty
Module 1 slot 0: unformatted card
Module 1 slot 0:- passphrase specified - writing card
Card writing complete.

cardset created; hkltu = 38a06dc96028c226c897863f81e6ec4478f6b010


# Create machine signing key
[bin]$ ./generatekey seeinteg cardset=subzero-signing-ocs plainname=subzerocodesigner recovery=yes type=RSA size=2048 nvram=no pubexp=
key generation parameters:
 operation    Operation to perform               generate
 application  Application                        seeinteg
 protect      Protected by                       token
 slot         Slot to read cards from            0
 recovery     Key recovery                       yes
 verify       Verify security of key             yes
 type         Key type                           RSA
 size         Key size                           2048
 pubexp       Public exponent for RSA key (hex)
 plainname    Key name                           subzerocodesigner
 nvram        Blob in NVRAM (needs ACS)          no

Loading `subzero-signing-ocs':
 Module 1: 0 cards of 1 read
 Module 1 slot 0: `subzero-signing-ocs' #1
 Module 1 slot 0:- passphrase supplied - reading card
Card reading complete.

Key successfully generated.
Path to key: /opt/nfast/kmdata/local/key_seeinteg_subzerocodesigner


# Create userdata signing key
[bin]$ ./generatekey seeinteg cardset=subzero-signing-ocs plainname=subzerodatasigner recovery=yes type=RSA size=2048 nvram=no pubexp=
key generation parameters:
 operation    Operation to perform               generate
 application  Application                        seeinteg
 protect      Protected by                       token
 slot         Slot to read cards from            0
 recovery     Key recovery                       yes
 verify       Verify security of key             yes
 type         Key type                           RSA
 size         Key size                           2048
 pubexp       Public exponent for RSA key (hex)
 plainname    Key name                           subzerodatasigner
 nvram        Blob in NVRAM (needs ACS)          no

Loading `subzero-signing-ocs':
 Module 1: 0 cards of 1 read
 Module 1 slot 0: `subzero-signing-ocs' #1
 Module 1 slot 0:- passphrase supplied - reading card
Card reading complete.

Key successfully generated.
Path to key: /opt/nfast/kmdata/local/key_seeinteg_subzerodatasigner


# Remove and label the card(s) (e.g. "Subzero Signing OCS #1 (1-of-1)").
∅


# Backup the files for the signing security world
[bin]$ cp -r /opt/nfast/kmdata/local ~/subzero_signing


# For reference, the subzero_signing folder should contain the following files:
[bin]$ ls -l ~/subzero_signing/
total 48
-rw-r--r--. 1 alok nfast   104 Dec 21 11:23 card_38a06dc96028c226c897863f81e6ec4478f6b010_1
-rw-r--r--. 1 alok nfast   120 Dec 21 11:23 cards_38a06dc96028c226c897863f81e6ec4478f6b010
-rw-r--r--. 1 alok nfast  5192 Dec 21 11:29 key_seeinteg_subzerocodesigner
-rw-r--r--. 1 alok nfast  5192 Dec 21 11:29 key_seeinteg_subzerodatasigner
-rw-r--r--. 1 alok nfast   860 Dec 21 11:16 module_D006-02E0-D947
-rw-r--r--. 1 alok nfast 18256 Dec 21 11:16 world


# Perform a code signing ceremony
[bin]$ cd ~/subzero/core/build/
[build]$ /opt/nfast/bin/tct2 --sign-and-pack --key=subzerocodesigner --is-machine --machine-type=PowerPCELF --infile ~/subzero/core/build/subzero --outfile ~/subzero/core/build/subzero-signed.sar
No module specified, using 1
Signing machine as `PowerPCELF'.

Loading `subzero-signing-ocs':
 Module 1: 0 cards of 1 read
 Module 1 slot 0: Admin Card #1
 Module 1 slot 0: empty
 Module 1 slot 0: `subzero-signing-ocs' #1
 Module 1 slot 0:- passphrase supplied - reading card
Card reading complete.


[build]$ echo dummy > dummy
[build]$ /opt/nfast/bin/cpioc subzero.cpio dummy
F dummy
Written 'subzero.cpio': 1 files, 0 directories, 0 errors


[build]$ /opt/nfast/bin/tct2 --sign-and-pack --key=subzerodatasigner --machine-key-ident=subzerocodesigner --infile subzero.cpio --outfile subzero-userdata-signed.sar
No module specified, using 1

Loading `subzero-signing-ocs':
 Module 1: 0 cards of 1 read
 Module 1 slot 0: `subzero-signing-ocs' #1
 Module 1 slot 0:- passphrase supplied - reading card
Card reading complete.


# Put HSM in initialization mode.
[bin]$ ./nopclearfail -I -m 1
Module 1, command ClearUnitEx: OK


# Create the wallet security world + wallet ACS. Command prompts for blank card(s).
[bin]$ ./new-world -i -m 1 -Q 1/1
14:28:21 WARNING: Module #1: preemptively erasing module to see its slots!

Create Security World:
 Module 1: 0 cards of 1 written
 Module 1 slot 0: blank card
 Module 1 slot 0:- passphrase specified - writing card
Card writing complete.

security world generated on module #1; hknso = 1913e2840d87fb4a5f0cf8b055e5cafe33636a03


# Remove + label the card(s) (e.g. "Subzero wallet ACS #1 (1-of-1)").
∅


# Put HSM in operational mode.
[bin]$ ./nopclearfail -O -m 1
Module 1, command ClearUnitEx: OK


# Create temporary OCS to transfer SEE keys from signing to wallet security world.
[bin]$ ./createocs -m 1 -Q 1/1 -N transfer

Creating Cardset:
 Module 1: 0 cards of 1 written
 Module 1 slot 0: empty
 Module 1 slot 0: blank card
 Module 1 slot 0:- passphrase specified - writing card
Card writing complete.

cardset created; hkltu = 97ed34e6c060a9c1e07cd6c9016c62fae38a13ee


# Reprogram HSM to allow key transfer from signing to wallet world.
# Prompts for insertion of wallet ACS, followed by signing ACS.
# (keep in mind that mk-reprogram requires hitting "enter" after inserting each card and prior to typing each password!)
[bin]$ ./mk-reprogram add ~/subzero_signing
Loading ACS for target module's owning Security World ...
Please insert administrator card in module #1.
Please enter passphrase for this card:
Loading ACS for /home/alok/subzero_signing ...
Please insert administrator card in module #1.
Please enter passphrase for this card:
Module key from /home/alok/subzero_signing added.


# Transfer the SEE keys from the signing ACS world to the wallet ACS world.
# Prompts for insertion of signing ACS, followed by temporary OCS.
# As mentioned previously, it's very unfortunate that the HSM doesn't allow just exporting a public key ¯\_(ツ)_/¯
# (keep in mind that key-xfer-im requires hitting "enter" after inserting each card and prior to typing each password!)
[alok@hsmdev bin]$ ./key-xfer-im ~/subzero_signing/ /opt/nfast/kmdata/local --cardset 97ed34e6c060a9c1e07cd6c9016c62fae38a13ee ~/subzero_signing/key_seeinteg_subzerocodesigner ~/subzero_signing/key_seeinteg_subzerodatasigner
Loading source recovery keys and ACL override authorisation ...
Please insert administrator card in module #1.
Please enter passphrase for this card:
Loading destination world data ...
Loading destination cardset 'transfer' as requested ...
Please insert operator card in module #1.
Please enter passphrase for this card:
 transferred seeinteg subzerocodesigner key(RSAPrivate)
 transferred seeinteg subzerodatasigner key(RSAPrivate)
Transferred 2 keys.


# Store the signing ACS in a safe place. You will need the card(s) each time you wish to enroll a HSM
# for a signing ceremony. We recommend erasing the HSM after each signing ceremony.
∅


# Erase the transfer OCS
# note: this will also erase the files associated with the OCS.
[bin]$ ./bulkerase

Examining/erasing card(s):
Module 1 slot 0: `transfer' #1
Module 1 slot 0:- confirmed - erasing card
Module 1 slot 0: `transfer' #1: Card erased
Module 1 slot 0: blank card

Card erasure complete.


# Create a softcard for pubKeyEncryptionKey
[bin]$ ./ppmk --new subzero
Enter new pass phrase:
Enter new pass phrase again:
New softcard created: HKLTU aa6bfaa5f222407b10fea9b30b68129d6fb7a3e4


# Create pubKeyEncryptionKey
[bin]$ ./generatekey --no-verify simple type=AES protect=softcard softcard=subzero recovery=yes size=256 ident=pubkeyenckey plainname= seeintegname=subzerodatasigner nvram=no


# The HSM is now setup for performing wallet operations.
# Note: you will have to update the Java yaml file and burn a DVD with the two .sar files. Then run the Java GUI with
# --init-nvram the first time to initialize the nvram (will prompt for the ACS).
```
