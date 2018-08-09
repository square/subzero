# Audit

## Source code

_At some point, we will perform a formal code audit and share the findings._

## Security World and keys

We plan to add support in the UI to list all the available keys and their protection mechanisms. Until that
functionality is baked in the UI, auditors can use the standard set of tools supplied by the HSM vendor.

## Balance

[beancounter](https://github.com/square/beancounter/) is a tool we wrote which can be used to list the balances
available in the gateway and cold wallets. The tool derives addresses associated with a given wallet and then contacts
Electrum servers or a personal Btcd node to query transaction history. The transactions are then processed to compute a
balance.
