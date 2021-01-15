#!/bin/bash

# variant of build.sh that does not include the CentOS Everything yum repo

###############################################################################
# Customize SYSLINUX and GRUB2 bootloaders
###############################################################################
sudo cp /vagrant/patches/usr/share/lorax/live/config_files/x86/grub2-efi.cfg /usr/share/lorax/live/config_files/x86/grub2-efi.cfg
sudo cp /vagrant/patches/usr/share/lorax/live/x86.noeverything.tmpl /usr/share/lorax/live/x86.tmpl
sudo cp /vagrant/patches/usr/share/lorax/live/efi.tmpl /usr/share/lorax/live/efi.tmpl

###############################################################################
# Customize live scripts based on build type: release (default) or dev
###############################################################################
rm -rf /vagrant/live_scripts/.isotype_rel /vagrant/live_scripts/.isotype_dev
if [ "$1" = "dev" ]; then
  BUILD_TYPE="Dev"
  cp /vagrant/live_scripts/install_nfast_tools_dev /vagrant/live_scripts/install_nfast_tools
  touch /vagrant/live_scripts/.isotype_dev
elif [ "$1" = "release" ] || [ "$#" -eq 0 ]; then
  BUILD_TYPE="Rel"
  cp /vagrant/live_scripts/install_nfast_tools_release /vagrant/live_scripts/install_nfast_tools
  touch /vagrant/live_scripts/.isotype_rel
else
  echo "Error occurred. Usage: $0 [release|dev]." >&2
  exit 1
fi

##############################################################################
# Actually build the ISO
###############################################################################

# livemedia-creator refuses to run if results_dir exists
sudo rm -rf /tmp/build

sudo livemedia-creator --logfile=/vagrant/livemedia-creator.log --make-iso --ks=/vagrant/rhel7-livemedia.ks --resultdir="/tmp/build" --no-virt --project="CentOS" --releasever="7.7.1908" --volid="CentOS 7 (1908) + nCipher ($BUILD_TYPE)"

###############################################################################
# Copy ISO back to host
###############################################################################
sudo cp /tmp/build/images/boot.iso /vagrant/boot.iso
