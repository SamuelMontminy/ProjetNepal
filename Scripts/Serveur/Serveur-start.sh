#!/bin/bash
/opt/vc/bin/tvservice -o        #Désalimente le port HDMI

cd /home/pi/ProjetNepal/
echo '1-1' |sudo tee /sys/bus/usb/drivers/usb/unbind    #Désalimente les ports USB

sudo java Serveur               #Démarre le code
