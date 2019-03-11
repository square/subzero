# Building and Running the code without HSMs

Subzero can be used without HSMs. This is useful for evaluation purpose as well as development. The wallet is however
not encrypted -- for production, Subzero's security model assumes you are using a HSM.

The instructions in this page work with Mac OS X. Linux users can simply skip irrelevant parts. Windows users are
advised to use a virtualization layer (e.g. Ubuntu inside VirtualBox).

## Installing the tools and building the code

This section goes over the minimal set of tools needed to build and run Subzero. If you would like to develop and
contribute to Subzero, we recommend installing [Intellij IDEA](https://www.jetbrains.com/idea/) for the Java piece
and [CLion](https://www.jetbrains.com/clion/) for the C core.

    # Open Terminal and install Homebrew
    /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"

    # install Java
    brew update
    brew cask install java

    # install Maven
    brew install maven

    # install cmake
    brew install cmake

    # install protobuf
    brew install protobuf

    # close + re-open the Terminal window for some changes to take effect

    # clone the repo
    git clone --recursive https://github.com/square/subzero.git
    cd subzero

    # build the Java code
    cd java
    mvn package

    # build the C code, using testnet transactions.
    cd ../core
    mkdir build
    cd build
    TARGET=dev CURRENCY=btc-testnet cmake ../
    make

    # create the wallets directory
    sudo mkdir -p /data/app/subzero/wallets/
    sudo chown $USER /data/app/subzero/wallets/
    touch /data/app/subzero/wallets/.subzero_702e63a9

## Running the servers and user interface

See also [sample output](core_sample_output.md).

    # start the core (listens on port 32366 by default)
    # when the Core starts, it runs several self checks. Some number of red lines is thus expected
    ./subzero/core/build/subzero

    # in a fresh Terminal tab, start the dev/demo server (listens on port 8080)
    ./subzero/java/server/target/server-1.0.0-SNAPSHOT.jar server

    # in a fresh Terminal tab, open http://localhost:8080/.
    open http://localhost:8080/

    # start the GUI
    ./subzero/java/gui/target/gui-1.0.0-SNAPSHOT-shaded.jar

Your environment should look as following (from top left, going clockwise): the GUI, a web browser, and a Terminal.
<img src="../dev_setup.png">

## Creating a wallet and signing your first transaction

By default, Subzero is configured to work with 2-of-4 signatures. Creating a wallet will therefore require 4
initialization and 4 finalization steps.

### Initialization
On the web browser, click on "generate QR code" under Initialize a wallet. Unless if you set a wallet id, the QR code
should be "EAAaAA==".

<img src="../init_wallet_dev_server.png">

You can paste this code in the GUI, which will change the GUI to the approval step. note: the GUI currently does not
support clipboard operations. You can instead use the Terminal tab to copy-paste data.

<img src="../init_wallet_gui.png">

Type "yes" + [enter] to continue. A QR code is displayed, but you can also copy the data from the Terminal. You will
want to save this response in TextEdit.

<img src="../init_wallet_gui_done.png">

Before you can repeat the wallet initialization process 3 more times, you need to move the wallet file out of the way:

    # In a fresh Terminal tab
    cd /data/app/subzero/wallets
    mkdir initialized
    mv subzero-0.wallet initialized/subzero-0.wallet-1

Once you are done initializing the 4 shares, you'll have 4 files in /data/app/subzero/wallets/initialized:

<img src="../init_wallet_initialized.png">

And 4 responses in TextEdit:
<img src="../init_wallet_text_edit.png">

### Finalization

You can now finalize each share. Using the Finalize a wallet section in the web browser, fill out each initialize
response and click on "generate QR code":

<img src="../finalize_wallet.png">

You can now finalize each wallet file. You will have to, one-by-one, copy wallet files from /data/app/subzero/wallets/initialized
to /data/app/subzero/wallets/ and move the result to /data/app/subzero/wallets/finalized/.

    mv initialized/subzero-0.wallet-1 subzero-0.wallet

<img src="../finalize_wallet_gui.png">

Type "yes" + [enter] to continue. Again, make sure you record each response.

<img src="../finalize_wallet_gui_done.png">

Move the wallet file out of the way and finalize the remaining 3 shares.

    mv subzero-0.wallet finalized/subzero-0.wallet-1

Once you are done finalizing the 4 shares, you'll have 4 files in /data/app/subzero/wallets/finalized:

<img src="../finalize_wallet_finalized.png">

And 4 more responses in TextEdit:
<img src="../finalize_wallet_text_edit.png">

### Reveal wallet xpubs

Using the 4 responses from the finalization step and the web browser, you can get the xpubs:

<img src="../xpubs.png">
