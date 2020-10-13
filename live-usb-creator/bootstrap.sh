#!/bin/bash

# mount the SecWorld + CodeSafe image so we can copy stuff off it to our target image
sudo mkdir /media/SecWorld && sudo mount -t iso9660 -o ro /vagrant/SecWorld_Lin64-12.60.11.iso /media/SecWorld
sudo mkdir /media/CodeSafe && sudo mount -t iso9660 -o ro /vagrant/Codesafe_Lin64-12.63.0.iso  /media/CodeSafe
