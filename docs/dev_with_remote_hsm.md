# Developing with a remote HSM
You can use a nCipher Solo XC connected to a remote host for development or staging purpose — with the assumption that
the remote host is running Linux, has the standard nCipher tools installed, and you have ssh access.

In addition, you will either need the remote host to be in your vicinity (so you can insert various smart cards when
needed) or setup remote readers.

Note: these instructions have not been endorsed by the HSM manufacturer. They have worked for many years, across many
software/firmware releases, including CodeSafe v12.50.2 and firmware 12.50.11.

In order to communicate with the HSM, nCipherKM.jar expects a few files/scripts to be available locally. Run the
following commands on your local host. Replace _[REMOTE HOST]_ with the IP address or hostname of the remote machine.

1. mkdir -p /opt/nfast/kmdata/config
2. mkdir -p /opt/nfast/tcl/bin
3. scp [REMOTE HOST]:/opt/nfast/kmdata/config/config /opt/nfast/kmdata/config/config
4. echo -e '#!/bin/bash\nssh [REMOTE HOST] /opt/nfast/tcl/bin/nfkmcmdadp "$@"' > /opt/nfast/tcl/bin/nfkmcmdadp
5. chmod a+x /opt/nfast/tcl/bin/nfkmcmdadp
6. ssh -L 9000:localhost:9000 -L 9001:localhost:9001 -L 32366:localhost:32366 [REMOTE HOST]

You should now be able to run Subzero's Java GUI with the HSM connected to a remote host. It is recommended to sync the
remote and local /opt/nfast/kmdata/local directories, which can be achieved using a network file system (sshfs, nfs,
afs, smb, etc.) or by running rsync (or scp) in a loop. For example, setting up sshfs on Mac OS X:

1. brew cask install osxfuse
2. brew install sshfs
3. sshfs [REMOTE HOST]:/opt/nfast/kmdata/local /opt/nfast/kmdata
