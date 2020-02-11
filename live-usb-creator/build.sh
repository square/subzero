#!/bin/bash

# variant of build.sh that does not include the CentOS Everything yum repo

###############################################################################
# Customize SYSLINUX and GRUB2 bootloaders
###############################################################################
sudo cp /vagrant/patches/usr/share/lorax/live/config_files/x86/grub2-efi.cfg /usr/share/lorax/live/config_files/x86/grub2-efi.cfg
sudo cp /vagrant/patches/usr/share/lorax/live/x86.noeverything.tmpl /usr/share/lorax/live/x86.tmpl
sudo cp /vagrant/patches/usr/share/lorax/live/efi.tmpl /usr/share/lorax/live/efi.tmpl

###############################################################################
# Actually build the ISO
###############################################################################

# livemedia-creator refuses to run if results_dir exists
sudo rm -rf /tmp/build

sudo livemedia-creator --logfile=/vagrant/livemedia-creator.log --make-iso --ks=/vagrant/rhel7-livemedia.ks --resultdir="/tmp/build" --no-virt --project="CentOS" --releasever="7.7.1908" --volid="CentOS 7 (1908) + nCipher x86_64"

###############################################################################
# Copy ISO back to host
###############################################################################
sudo cp /tmp/build/images/boot.iso /vagrant/boot.iso
