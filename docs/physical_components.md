# Physical components

Sensitive key material is only ever available in clear text inside the HSM. As a result, the other physical components
aren't sensitive. We do however practice defense in depth.

## Choosing secure locations

You should consult with a physical security specialist for best practices. Using dedicated
rooms (or buildings), renting space in commercial data centers, or renting bank vaults all provide various tradeoffs.

We recommend picking physical locations which are unlikely to be simultaneously affected by a disaster.

## Hardware Security Module (HSM)

We leverage nCipher Security's **nShield Solo XC General Purpose** HSMs ([product page](https://www.ncipher.com/products/general-purpose-hsms/nshield-solo)). We recommend picking the
XC Base model, but the software works with any of the XC models (XC Base, XC Mid, or XC High). Our latest build uses
the vendor supplied CodeSafe-linux64-dev-12.50.2.iso and firmware 12.50.11.

We picked this HSM for the following reasons:

- FIPS certified.
- Ability to run custom, signed code.

Our HSM's form factor is PCIe. The same hardware is available as an external USB device (nShield Edge) or a
network-attached device (nShield Connect). These different form factors _should_ function in an identical way.

A smart card reader (supplied with the HSM) and smart cards are required for various administrative tasks, as well as
operational tasks.

The vendor sells licenses to enable various functionalities. The CodeSafe license is required to run the Subzero Core
component inside the HSM. The ECC license is *not* required (we leverage Trezor's cryptographic library).

Note: while the device is marketed as nShield, all the vendor provided software is called nCipher. We therefore refer
to our nShield devices as nCiphers.

The code is currently tightly coupled to the nShield. We are open to contributions enabling support for alternative
vendors.

## DVD reader

The code which runs on the servers is distributed using DVDs. We picked DVDs for the following reasons:

- Immutable (mostly).
- Makes upgrading remote locations easier.

## QR-code scanner

Each remote location has two QR-code scanners. The QR-code scanner emulates a keyboard, which removes the need to
deal with custom hardware drivers. One QR-code scanner is connected to the offline server, the other scanner is used
to upload the signatures.

# Linux server

We use standard Linux servers. Our servers need one PCIe slot (for the HSM), a DVD drive bay, a keyboard,
an additional USB port (for the QR-code scanner) and a monitor. A hard drive is needed to store the wallet in
its encrypted form.

# Monitor

Standard monitor which plugs into the server. The monitor is used to display confirmation messages. The monitor's
sensitivity ranges from medium to high, depending on what business logic rules are enforced within the HSM.

# Keyboard

Standard keyboard which plugs into the server. The keyboard is used to confirm actions displayed by the UI. The keyboard
is also used to enter the operator smart card password.

# Battery

Powering the Linux server off a battery while a signing ceremony is taking place provides power isolation.
