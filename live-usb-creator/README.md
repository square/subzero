# live-usb-creator

This project generates a LiveUSB image with CentOS 7.7.1908 and nCipher drivers/support tools for use in machines with
HSMs.

## Building image

Three dependencies need to be fetched out-of-band.

Set the following files in place in the same directory as the `Vagrantfile`.

* CodeSafe-linux64-dev-12.50.2.iso (2.6GB): supplied by the HSM vendor.
* Codesafe_Lin64-12.63.0.iso (281M): supplied by the HSM vendor.
* CentOS-7-x86_64-Everything-1908.iso (10G): `curl -O https://vault.centos.org/7.7.1908/isos/x86_64/CentOS-7-x86_64-Everything-1908.iso`
* kernel-devel-3.10.0-957.12.2.el7.x86_64.rpm (17MB): `curl -L -O http://archive.kernel.org/centos-vault/centos/7.6.1810/updates/x86_64/Packages/kernel-devel-3.10.0-957.12.2.el7.x86_64.rpm`
* protoc-3.14.0-linux-x86_64.zip (1.6MB): `curl -OL https://github.com/protocolbuffers/protobuf/releases/download/v3.14.0/protoc-3.14.0-linux-x86_64.zip`
* six-1.15.0-py2.py3-none-any.whl (11.0kB): `curl -L -O https://files.pythonhosted.org/packages/ee/ff/48bde5c0f013094d729fe4b0316ba2a24774b3ff1c52d924a8a4cb04078a/six-1.15.0-py2.py3-none-any.whl`
* protobuf-3.14.0-cp36-cp36m-manylinux1_x86_64.whl (1MB): `curl -O -L https://files.pythonhosted.org/packages/fe/fd/247ef25f5ec5f9acecfbc98ca3c6aaf66716cf52509aca9a93583d410493/protobuf-3.14.0-cp36-cp36m-manylinux1_x86_64.whl`
* Pillow-8.3.2-cp36-cp36m-manylinux_2_5_x86_64.manylinux1_x86_64.whl (3.0 MB): `curl -O -L  https://files.pythonhosted.org/packages/6f/2b/7c242e58b1b332a123b4a7bf358e2cc7fa7d904b3576b87defc9528e2bfd/Pillow-8.3.2-cp36-cp36m-manylinux_2_5_x86_64.manylinux1_x86_64.whl`
* openjdk-19.0.2_linux-x64_bin.tar.gz: `curl -O -L https://download.java.net/java/GA/jdk19.0.2/fdb695a9d9064ad6b064dc6df578380c/7/GPL/openjdk-19.0.2_linux-x64_bin.tar.gz`

Verify the shasums programmatically(checksum file is the output of the manual command below):

```bash
shasum -a 256 -c checksum
```

Verify the following SHA256 sums manually:

```bash
$ shasum -a 256 CodeSafe-linux64-dev-12.50.2.iso Codesafe_Lin64-12.63.0.iso CentOS-7-x86_64-Everything-1908.iso \
kernel-devel-3.10.0-957.12.2.el7.x86_64.rpm protoc-3.14.0-linux-x86_64.zip \
six-1.15.0-py2.py3-none-any.whl protobuf-3.14.0-cp36-cp36m-manylinux1_x86_64.whl \
Pillow-8.3.2-cp36-cp36m-manylinux_2_5_x86_64.manylinux1_x86_64.whl \
openjdk-19.0.2_linux-x64_bin.tar.gz
23ca2c5fc2476887926409bc69f19b772c99191b1e0cce1a3bace8d1e4488528  CodeSafe-linux64-dev-12.50.2.iso
df928054888f466c263ef1d7de37877bdcf27c632b34c6934b6eee4e8697a6de  Codesafe_Lin64-12.63.0.iso
bd5e6ca18386e8a8e0b5a9e906297b5610095e375e4d02342f07f32022b13acf  CentOS-7-x86_64-Everything-1908.iso
a27c718efb2acec969b20023ea517d06317b838714cb359e4a80e8995ac289fc  kernel-devel-3.10.0-957.12.2.el7.x86_64.rpm
a2900100ef9cda17d9c0bbf6a3c3592e809f9842f2d9f0d50e3fba7f3fc864f0  protoc-3.14.0-linux-x86_64.zip
8b74bedcbbbaca38ff6d7491d76f2b06b3592611af620f8426e82dddb04a5ced  six-1.15.0-py2.py3-none-any.whl
ecc33531a213eee22ad60e0e2aaea6c8ba0021f0cce35dbf0ab03dee6e2a23a1  protobuf-3.14.0-cp36-cp36m-manylinux1_x86_64.whl
8f284dc1695caf71a74f24993b7c7473d77bc760be45f776a2c2f4e04c170550  Pillow-8.3.2-cp36-cp36m-manylinux_2_5_x86_64.manylinux1_x86_64.whl
34cf8d095cc071e9e10165f5c45023f96ec68397fdaabf6c64bfec1ffeee6198  openjdk-19.0.2_linux-x64_bin.tar.gz
```

The CentOS's GPG signature can also be verified to confirm the image has been signed by the CentOS team.

This workflow uses the tool [Vagrant](https://www.vagrantup.com/) to orchestrate the creation and provisioning of a VirtualBox VM from a CentOS-provided base VM.

Note that VirtualBox DOES NOT work on Apple Silicon Macs - you need an x86_86 Mac or Linux machine.

Build the image in a CentOS VM guest as follows:

* Install the toolchain (Mac OS, x86_64 only):

  ```bash
  # install virtualbox using brew cask if virtualbox is not installed
  # Post install, make sure you click "allow" in settings->security and privacy for virtualbox.
  # That is required for virtual box to work properly.
  # After enabling that you will have to reboot your machine.
  brew install virtualbox --cask
  # install vagrant using brew cask if vagrant is not installed
  brew install vagrant --cask
  # install vagrant-vbguest if vagrant-vbguest is not installed
  vagrant plugin install vagrant-vbguest
  ```

* Install the toolchain (Linux, x86_64 only):
    - Go to https://www.virtualbox.org/wiki/Linux_Downloads and follow the instructions to download and install VirtualBox for your linux distro.
    - Go to https://www.virtualbox.org/wiki/Downloads and download and install the "VirtualBox Extension Pack".
    - Go to https://developer.hashicorp.com/vagrant/downloads and follow the instructions to download and install Vagrant for your Linux distro.
    - Run `vagrant plugin install vagrant-vbguest` in a terminal.

* Build the ISO:

  ```bash
  # remove the existing centos/7 image, if one exists
  vagrant box remove -f centos/7
  # install centos 7 vagrant box version 1905.1 (checksum via https://cloud.centos.org/centos/7/vagrant/x86_64/images/sha256sum.txt.asc)
  vagrant box add --checksum-type sha256 --checksum de768cf0180d712a6eac1944bd78870c060999a8b6617f8f9af98ddbb9f2d271 --provider virtualbox --box-version 1905.1 centos/7
  # boot up a virtualbox VM to run the install in (Note: this takes a while)
  vagrant up
  # enter the VM
  vagrant ssh
  # kick off the build, from the VM (takes about 5-25 minutes), to build a release image. For building a development image, use `/vagrant/build.sh dev` instead
  /vagrant/build.sh
  # will create boot.iso (1.1G) in the current directory (i.e. outside the VM) when finished
  ```

* Rebuid the ISO:

  ```bash
  # Logout from the VM
  exit
  # Halt the VM
  vagrant halt
  # Remove the existing centos/7 image
  vagrant box remove -f centos/7
  # Clean up the git repo, preserving the artifacts obtained out-of-band (gitignored)
  git clean -fd
  # Following the steps in "Build the ISO" above to rebuild the ISO
  ```

## Writing image to USB drive

1. Identify device path to disk (such as `/dev/disk3`) with `diskutil list external physical`. We will call it `/dev/diskN` in the rest of the document.
2. Unmount all volumes on that disk with `diskutil unmountDisk /dev/diskN`
3. Identify the “raw” device path by replacing `disk` with `rdisk` in the device path. (E.g. `/dev/disk3` becomes `/dev/rdisk3`.) This speeds up writes by 3–4× in very informal testing.
3. Write ISO to that “raw” device path with `sudo dd bs=1m if=boot.iso of=/dev/rdiskN`. Remember that on MacOS you can check in on progress by sending SIGINFO with Ctrl+T. (Takes about 3 minutes in one test.)
4. Eject the disk with `diskutil eject /dev/diskN`
5. Remember to hold F12 during startup to get a boot menu. You’ll then select something like `UEFI: SanDisk`.

## Build process

In general there are 7 phases of the build process, all kicked off in sequence when `livemedia-creator` is invoked in `build.sh`:

1. creation of an ext4 filesystem image (at e.g. `/tmp/build/diskLdYfQA.img`) in the `make_image` function of `/sbin/livemedia-creator`
2. installation of CentOS 7 onto that image by `anaconda`, as driven by the `rhel7-livemedia.ks` kickstart, and as triggered in `novirt_install` function of `/sbin/livemedia-creator`
3. creation of a compressed squashfs image (at e.g. `/var/tmp/tmpkd2g30/images/install.img`) from that ext4 image, as triggered in the `make_squashfs` function of `/sbin/livemedia-creator`
4. bundling of that squashfs image with some live media bootstrapping stuff by the `pylorax.TreeBuilder` as driven by the template `/usr/share/lorax/live/x86.tmpl`
5. invocation of `mkisofs` (also by `pylorax.TreeBuilder`) to slurp all of the files from the previous step into a bootable ISO (at e.g. `/var/tmp/tmpkd2g30/images/boot.iso`)
6. post-processing that ISO image with `isohybrid` to allow hybrid booting as a CD-ROM or as a hard disk.
7. embedding of an MD5 checksum by `implantisomd5` into an unused section of that ISO

### Checking build progress

One of the most time-intensive (and quiet) stages of this process is the `mksquashfs` operation (`Creating 4.0 filesystem on /var/tmp/tmp??????/images/install.img, block size 131072.`). You can get a rough check on progress by comparing `install.img` size to `install.img` size in a previous build, in a separate `vagrant ssh` session (will output some value between 0.0% and 100.0%, where 100.0% is completely done with the creation of the install image, assuming output sizes are in fact identical):

```bash
sudo sh -c "stat --printf='%s' /var/tmp/tmp*/images/install.img | python -c 'import sys; print \"%0.1f%%\" % (float(int(sys.stdin.read())) / 1045090304.0 * 100.0)'"
```

## Hacking on this project

Here are the useful files you may want to tweak.

VM related stuff:

* `Vagrantfile`: Provisions a VirtualBox VM in which the live system ISO will be built using the Vagrant tool.
* `bootstrap.sh`: A shell script that runs at the very end of VM bringup. (Currently just mounts the CodeSafe ISO.)
* `build.sh`: Manually run by the user to kick off the ISO build.
Currently includes some patches for Lorax and Kickstart files applied
before running the build proper with `livemedia-creator`. Two image
ypes, development and release, can be built.
  * Release build: `build.sh` or `build.sh release`.

    A release image is to be used by cold storage operators who do not need
    to have access to an interactive shell. For a release image to boot into
    a root shell successfully,
    * An SCSI/ATA hard drive needs to be available, with a partition
    '/dev/sda1' used as persistent storage for the cold
    wallet. The partition can be formatted in ext3 or vfat for example. The
    device node `/dev/sda1` is required to give the operators a seamless
    experience when booting up the system.
    * A provisioned HSM needs to be operational on the machine.
    * A few other files, including the HSM world and encrypted secrets, and
    the compiled subzero code (in .jar and signed .sar), are not checked in
    to the project repository, and need to be added to the appropriate
    locations in the subzero repository's `live-usb-creator` directory before
    building a release image that will work out of the box. Specifically,

      * `data_app_subzero/` should contain `subzero-cli.jar`,
      `subzero-signed.sar`, `subzero-userdata-signed.sar`, where
      `subzero-cli.jar` is `gui-1.0.0-SNAPSHOT-shaded.jar` renamed, and the latter
      two `.sar` files are the signed artifacts of `make prod` on a properly
      configured machine with an HSM.
      * `/dev/sda1` when mounted to `/hd`, should have the HSM and wallets
      files contained in `/hd/local` and `/hd/wallets`, respectively.

  * Development build: `build.sh dev`.

    A development image is to be used by subzero developers for HSM and cold
    wallet development. A developer can boot into an interactive root shell
    without performing the above mentioned steps. The subzero development
    toolchain including HSM software is automatically installed in the
    development image to allow a subzero developer to set up a customizable
    development and testing environment with a minimal hardware requirement.
    A developer can manually provision the HSM, set up persistent storage
    for the cold wallet and HSM files, execute the .jar and .sar files for
    development and testing.

  Note that right now an interactive shell for the `liveuser` user is
  available for a debugging purpose, in both the `dev` and `release` images.
  The default booting behavior is different between these two image flavors.

Install related stuff:

* `rhel7-livemedia.ks`: This is the “kickstart” file that scripts the
CentOS install by anaconda, for the `release` image. Modified from the
version that exists on the VM guest under the path
`/usr/share/doc/lorax-*/rhel7-livemedia.ks`. I’ve tried to factor out
most of the interesting stuff so you don’t have to touch this gross
file. See my changes versus the CentOS/Fedora example liveiso
kickstart by running `diff rhel7-livemedia.ks.orig
rhel7-livemedia.ks`. Mostly I removed the GNOME desktop/X11, added
couple packages to %packages, and added in hooks for the below.
* `rhel7-livemedia-dev.ks`: This is the anaconda kickstart file used to
build the `dev` (development) image.
* `install_scripts/0_post_install_nochroot`: This bash script is executed by anaconda (the CentOS installer) directly after install (but before `install_scripts/1_post_install_chroot`). From the point of view of the script, the newly installed CentOS (soon to be implanted in our live ISO) lives at `/mnt/sysimage/opt/`, and this directory (the one containing the `Vagrantfile`) lives at `/vagrant/`. That makes this bash script the ideal place to copy files onto the root filesystem of the newly installed CentOS.
* `install_scripts/1_post_install_chroot`: This bash script is executed by anaconda (the CentOS installer) directly after install (but after `install_scripts/0_post_install_nochroot`). From the point of view of the script, the newly installed CentOS (soon to be implanted in our live ISO) is chrooted to `/`. That makes this bash script the ideal place to run Linux binaries (like useradd, systemctl, yum-config-manager, etc.) that change system state.

Pretty bootloader related stuff:

* `patches/usr/share/lorax/live/`: includes changes to SYSLINUX/ISOLINUX and GRUB2 configs.
* `otf_fonts/`: OTF fonts (now just SQ Market Bold)
* `grub_fonts/`: PF2 versions of those fonts (PF2 is a proprietary GRUB bitmap font format)
* `grub_themes/`: contains the pretty SQ GRUB theme.
* `syslinux-splash.png`: contains the pretty SQ SYSLINUX/ISOLINUX background.

Live system related stuff:

* `live_scripts/`: All the files in this directory (except README.md) will be copied into `/usr/local/bin/` on the live system image, putting them in-$PATH. Useful for utility scripts that should be available on the live system.
* `live_scripts/install_protobuf`: This script is invoked and only invoked for the development image type at startup (via /usr/local/bin/startup and /etc/rc.d/init.d/livesys).
* `live_scripts/install_nfast_tools_{dev|release}`: Depending on image type (development or release), the script `live_scripts/install_nfast_tools_dev` or `live_scripts/install_nfast_tools_release` is run at startup (via /usr/local/bin/startup and /etc/rc.d/init.d/livesys), and builds the nfast kernel module in /opt/nfast/driver and runs the /opt/nfast/sbin/install script. This has to be done from the live system because the install script behaves differently when no PCI card is attached to the system. The contents of this file block entering an interactive shell, thanks to `nfast_block_shell` and a bash profile addition.
* `live_scripts/startup`: This script is run at startup (via /etc/rc.d/init.d/livesys). The contents of this file DO NOT block entering an interactive shell (they run async).
* `live_scripts/nfast_block_shell`: Blocks until the existence of the path /.nfast-configured, which is intended to be touched after NFast Tools have been installed with `install_nfast_tools`.

## Bootloader

BIOS boot uses SYSLINUX/ISOLINUX. SYSLINUX/ISOLINUX config lives at `/usr/share/lorax/live/config_files/x86/isolinux.cfg`.

EFI boot uses GRUB2. GRUB2 config template lives at `/usr/share/lorax/live/config_files/x86/grub2-efi.cfg`.

## Troubleshooting Tips

Normally, operators and developers log onto the LiveUSB system as `root`. For
LiveUSB image testing, you may log in as the `liveuser` user, become `root`
(with `su`), for debugging.
