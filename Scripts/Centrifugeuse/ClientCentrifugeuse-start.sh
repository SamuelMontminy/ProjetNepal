#!/bin/bash

/opt/vc/bin/tvservice --off #Désalimente le port HDMI

sudo pigpiod                #Pour activer pigpio

pigs pud 5 d                #pull-down sur gpio5
pigs pud 6 d                #pull-down sur gpio6
pigs pud 13 d               #pull-down sur gpio13

cd /home/pi/ProjetNepal/

pi4j -r ClientCentrifugeuse.java 192.168.4.1 2228   #Démarre le code
