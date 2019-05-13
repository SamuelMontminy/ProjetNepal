#!/bin/bash

/opt/vc/bin/tvservice --off #Désalimente le port HDMI

sudo pigpiod                #Pour activer pigpio

raspi-gpio set 5 pd         #pull-down sur gpio5
raspi-gpio set 6 pd         #pull-down sur gpio6
raspi-gpio set 13 pd        #pull-down sur gpio13

cd /home/pi/ProjetNepal/

pi4j -r ClientCentrifugeuse.java 192.168.4.1 2228   #Démarre le code
