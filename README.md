[![Build Status](https://github.com/square/subzero/workflows/CodeQL/badge.svg)](https://github.com/square/subzero/actions?query=workflow%3ACodeQL)
[![Documentation Status](https://readthedocs.org/projects/subzero/badge/?version=master)](https://subzero.readthedocs.io/en/master/?badge=master)
[![Dependabot Status](https://api.dependabot.com/badges/status?host=github&repo=square/subzero)](https://dependabot.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/square/subzero/blob/master/LICENSE)

<img src="logo.png" width="100">

# HSM Cold Storage

For security, Square stores a reserve of Bitcoins in an offline setting. By having these funds offline, we
reduce attack surface and risk of theft.

Square's solution is unique, specifically, we leverage FIPS certified Hardware Security Modules (HSMs) to protect the
private key material. We decided to use such HSMs because we already own, operate, and trust these devices for other payment-related needs.

Funds can be sent from online systems to the cold storage at any time. Moving funds out of cold storage requires a
multi-party signing ceremony. In addition, the offline HSMs are able to enforce business logic rules, for instance we
only allow sending funds to Square-owned addresses. Such a scheme is usually called defense in depth or an onion model.
We maintain the online/offline isolation by importing transaction metadata and exporting signatures using QR codes.

HSMs have the ability to share key material. This enables the ability to store our backups in encrypted form and
restore a wallet at any location.

This repo contains our design documents as well as specific technical information. We are sharing our source code, with
the caveat that the code is currently only useful if you have the exact same hardware setup. We are willing to make the
code more modular over time, as long as the broader community shows interest to implement support for additional
hardware vendors.

See also [Open Sourcing Subzero (blog post)](https://developer.squareup.com/blog/open-sourcing-subzero)

# Documentation

[https://subzero.readthedocs.io](https://subzero.readthedocs.io)

# License


    Copyright 2018 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
