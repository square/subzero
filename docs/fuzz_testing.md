# Fuzz testing

Subzero without HSMs can be fuzz-tested with [LLVM's libfuzzer](https://llvm.org/docs/LibFuzzer.html).

First, follow the instructions [here](./running_without_hsm.md) and set up your HSM-less Subzero developer environment.
If you are on MacOS, you will also have to install the full LLVM toolchain, because Apple's Clang doesn't include libfuzzer.
For linux users, the following is not necessary and simply installing llvm from your package manager should suffice.

## Install the full LLVM toolchain with Homebrew (MacOS only)

    # Install the full LLVM toolchain
    brew install llvm

    # Heed the warning from Homebrew and copy it somewhere - it tells you how to manipulate your environment
    # to use the Homebrew-installed Clang, which will not take precedence over Apple's Clang by default.
    # On my machine, it looks something like this:
    # export PATH="/opt/homebrew/opt/llvm/bin:$PATH"
    # export LDFLAGS="-L/opt/homebrew/opt/llvm/lib/c++ -Wl,-rpath,/opt/homebrew/opt/llvm/lib/c++ -L/opt/homebrew/opt/llvm/lib"
    # export CPPFLAGS="-I/opt/homebrew/opt/llvm/include"
    # export CXXFLAGS="-I/opt/homebrew/opt/llvm/include"
    # export CFLAGS="-I/opt/homebrew/opt/llvm/include"
    # export CC=`which clang`
    # export CXX=`which clang`

    # It's not a bad idea to put the above into a script that you can source on demand when needed:
    touch ~/use-brew-llvm.sh
    echo 'export PATH="/opt/homebrew/opt/llvm/bin:$PATH"' >> ~/use-brew-llvm.sh
    echo 'export LDFLAGS="-L/opt/homebrew/opt/llvm/lib/c++ -Wl,-rpath,/opt/homebrew/opt/llvm/lib/c++ -L/opt/homebrew/opt/llvm/lib"' >> ~/use-brew-llvm.sh
    echo 'export CPPFLAGS="-I/opt/homebrew/opt/llvm/include"' >> ~/use-brew-llvm.sh
    echo 'export CXXFLAGS="-I/opt/homebrew/opt/llvm/include"' >> ~/use-brew-llvm.sh
    echo 'export CFLAGS="-I/opt/homebrew/opt/llvm/include"' >> ~/use-brew-llvm.sh
    echo 'export CC=`which clang`' >> ~/use-brew-llvm.sh
    echo 'export CXX=`which clang`' >> ~/use-brew-llvm.sh

    # Now you can enable Homebrew Clang with a simple command:
    source ~/use-brew-llvm.sh

    # You can check that it worked by running the following command. You should see "Homebrew clang" rather than "Apple clang" in the first line of output:
    clang --version

## Generate the initial corpus

Fuzzing will go much better if you provide the fuzzer with an initial set of inputs to start from. Let's generate them now.
For this, you will need a standard subzero core binary and the java GUI.

    # Build and run subzero core in Terminal 1
    cd core
    mkdir build
    cd build
    TARGET=dev CURRENCY=btc-testnet cmake ../
    make
    ./subzero

    # Build the GUI and use it to generate the inital corpus in Terminal 2
    # You will need an empty wallet directory for the generate-wallets test.
    # You will also need an output directory for the initial fuzzer corpus.
    cd java
    mkdir fuzzing_corpus
    mkdir wallets
    touch wallets/.subzero_702e63a9
    ./gradlew build

    # Run the generate wallet files test, writing the internal requests into the corpus directory
    # This will prompt you to type "yes" + Enter 8 times, then it will hang waiting for more input. Ctrl-C when done.
    java -jar gui/build/libs/gui-1.0.0-SNAPSHOT-shaded.jar --generate-wallet-files-test --wallet-dir wallets --generate-fuzzing-corpus --fuzzing-corpus-output-dir fuzzing_corpus

    # Check our work ... this should output "8" because we just wrote 8 internal request files into the corpus
    ls -1 fuzzing_corpus | wc -l

    # Run the signtx test, writing the internal requests into the corpus directory.
    # This might take a couple minutes
    java -jar gui/build/libs/gui-1.0.0-SNAPSHOT-shaded.jar --signtx-test --generate-fuzzing-corpus --fuzzing-corpus-output-dir fuzzing_corpus

    # Check our work again ... this should output "265" because we just wrote 257 more internal request files into the corpus
    ls -1 fuzzing_corpus | wc -l

    # Go back to Terminal 1 and ctrl-C the subzero process. We're done with it for now.

## Build the fuzzer binary

Now that we have generated the initial corpus, we can build the fuzzer binary and do some fuzzing.

    # Build the fuzzer binary. We want to fuzz with ASAN + UBSAN enabled, since that will catch more bugs.
    cd core
    mkdir -p build
    cd build
    TARGET=dev CURRENCY=btc-testnet cmake ../ -DENABLE_ASAN=ON -DENABLE_UBSAN=ON -DENABLE_FUZZER=ON
    make

## Minimize the initial corpus

Many of the inputs in our initial corpus trigger the exact same code paths, therefore they are redundant and not very useful.
We can minimize the initial corpus before kicking off the real fuzz test. Here is how to do that.

    # Create an output directory for the minimized corpus
    cd core
    mkdir fuzzing_corpus

    # Run the fuzzer once with -merge=1 to produce a minimized corpus. This will take a bit of time.
    build/subzero_fuzzer -merge=1 fuzzing_corpus ../java/fuzzing_corpus

## Run the fuzz test

Now we are finally ready to run the fuzz test! Here we go:

    # Fuzz test some code paths. Leave this running for a while, then check the directory for crash-* files.
    # If any are generated, those are the inputs which caused a crash.
    # To stop the fuzzer, ctrl-Z then run 'kill -9 %1'
    build/subzero_fuzzer -forks=10 -workers=10 -jobs=10 -ignore_ooms=1 -ignore_timeouts=1 -ignore_crashes=1 fuzzing_corpus

## Reproducing a failure

If any crashes / ooms / timeouts / etc were found, libfuzzer will generate corresponding fuzz output files outside the corpus,
in the directory from which the fuzzer was run (i.e. subzero/core). You can run the fuzzer with a single input to reproduce the crash.
If a crash is found, it's probably a good idea to fix it and save the bad input for a future regression test.

    # Running the fuzzer with a single bad crash input
    build/subzero_fuzzer crash-017668d340dca49646f38fab6c044145fbfadb76
    # Crash output follows

## A note on logic changes when fuzz testing

When you build the fuzz-test binary, there are a few logic changes from a production build:
- All logging output is disabled
- Invalid QR code signatures are ignored
- AES-GCM decryption failures are ignored
