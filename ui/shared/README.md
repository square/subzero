# plutus-shared

This library encapsulates the logic needed for interacting with plutus.  It is designed for use in
our coordinator service, and in our integration tests.

## API
For more information on each of these methods, see their relevant Javadocs.  This is a high level
overview of this library.  There's two main classes here.

### ColdWalletCreator

ColdWalletCreator handles the creation of a new cold wallet.  There are two offline steps to this
process (InitWallet and FinalizeWallet), which are handled by three static functions here.

The setup flow looks like this:
1. `ColdWallet.init()` returns an `InitWalletRequest`
2. Plutus runs InitWalletRequest and return `InitWalletResponse`
3. `ColdWallet.combine()` on the `InitWalletResponse ` returning `FinalizeWalletRequest`s
4. Plutus runs `FinalizeWalletRequest` and return `FinalizeWalletResponse`s
5. `ColdWallet.finalize()` on those `FinalizeWalletResponse` to obtain the public keys


### ColdWallet

To use a cold wallet, you pass the public keys from setup and other parameters to the ColdWallet
constructor.  The ColdWallet object is for sending money to and from the cold wallet.

`ColdWallet.address` takes a derivation path and returns an address that can be used to send funds
to the wallet.  The derivation path should be saved for use in withdrawing those funds.

Creating a transaction is a two-step process, where the coordinator starts a transaction, it is
signed offline, and then those signatures are combined to produce the final transaction.

`ColdWallet.startTransaction` is for for starting moving money in cold storage.  It creates the
 SendTxRequest command to send to cold storage for signing.  This is called once, and the resulting
 SignTxRequest is distributed to the signers.

 `ColdWallet.createTransaction` takes the signatures from cold storage, along with the parameters
 passed to the corresponding startTransfer, and produces the final transaction for broadcast.  This
 requires input from the required number of participants (ie, 2 of 4).

 ### Constants

 A class with various constants.

 ### PlutusUtils

This is a grab-bag of stuff, used for implementing the above classes.  Ideally the implementation
details here should be considered private and we have a nice API to hand off.
