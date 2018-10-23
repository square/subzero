# HSM initialization

Before any operation can take place, all the HSMs need to be initialized. For various reasons, we decided to enroll
all the HSMs in the same Security World.

A Security World (or World for short) is a concept, which allows HSMs to share key material for redundancy purpose. Two
HSMs enrolled in the same World are essentially identical. You can think of the World as the master encryption key,
which other keys are then encrypted under.

Enrolling a HSM in a World involves administrator smart cards. Blank cards are used to create a new World. Enrolling
a HSM in an existing World requires the corresponding administrator smart cards.

## Main World

Using the tools provided with the HSM, we create the main World. The smart cards which back this world need to
be stored in a very secure location. They will be needed in the following cases:

- a number of HSMs (or locations) were damaged and the remaining locations no longer form a quorum.
- operators have lost their smart cards (or the passwords thereof) and new smart cards need to be issued.

A 256-bit AES key (pubKey encryption key) is created at the same time as the main World. This key is used at wallet
initialization time.

When enrolling HSMs in the main World, the administrator need to initialize the nvram for rollback protection purpose.

Typically, administrators will want to create a staging and a dev World. Creating all three Worlds at the same time
is convenient.

## Signing World

A different World is used to sign the Core code. This creates a separation of duty: the engineers who sign and release
the Core code don't have direct access to decrypt wallets.

Ideally, the public part of the signing keys would be exported from the signing World, into the main World (and staging
and dev Worlds as well). This is however not possible with our HSMs. We therefore move the signing keys to the other
worlds. The smart card which backs the signing keys in the non-signing Worlds is then shredded.
