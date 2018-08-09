The two files in this directory are invoked by anaconda directly after the install process, thanks to directives in rhel7-livemedia.ks:

* 0_post_install_nochroot: run first, from the VM guest, after CentOS install
* 1_post_install_chroot: run second, chrooted into the new system image, after CentOS install
