Brancher les deux Raspberry Pi. Le code Serveur.java devrait ce trouver sur un Pi pouvant faire un "hotspot" (comme un Pi 3b par exemple).
Procédure pour configurer un Pi comme "hotspot": https://www.raspberrypi.org/documentation/configuration/wireless/access-point.md
Les codes clients (ClientEntrepot.java, ClientCentrifugeuse.java & ClientEcremeuse.java) devraient ce trouver sur un Pi qui ne consomme pas beaucoup de courant 
(comme un Pi Zero W par exemple) pour maximiser la longitivité de la batterie. Quand le Pi 3b sera démarré, il va créer un réseau Wi-fi appelé "Fromagerie" sur lequel les Pi Zero vont se connecter automatiquement.
Procédure pour installer l'image "Lite" pour les Pi Zero, et pour faire en sorte qu'ils se connectent au réseau automatiquement: https://www.losant.com/blog/getting-started-with-the-raspberry-pi-zero-w-without-a-monitor
Java ne ce trouve pas sur l'installation standard de Raspbian, il est nécéssaire de l'installer. Installer avec la commande suivante: sudo apt-get install oracle-java8-jdk
Pour les Pi ayant les codes Entrepot et Écrémeuse, il est nécéssaire d'installer Pi4J pour compiler et éxécuter le code. Suivre la procédure suivante: http://pi4j.com/install.html
De plus, il est important de s'assurer que le I2C est bien activé pour les Pi qui en ont de besoin (Entrepot et Écrémeuse). Suivre la procédure suivante pour l'activer: https://learn.adafruit.com/adafruits-raspberry-pi-lesson-4-gpio-setup/configuring-i2c
Pour le serveur, il est nécéssaire d'installer le SDK Hologram (pour que le modem LTE fonctionne). Suivre la procédure suivante: https://www.hackster.io/hologram/add-cellular-to-a-raspberry-pi-with-hologram-nova-ea5926
Pour pouvoir recevoir les données de la carte SIM, celle-ci doit être enregistrée dans le "dashboard" Hologram: https://dashboard.hologram.io
Pour recevoir les données dans AWS S3, une route doit être fait dans Hologram pour rediriger les données vers S3 (Tutoriel à venir)
Pour voir les données dans QuickSight, il faut des fichiers "manifests" qui dictent quelles données importer à partir de S3 (Tutoriel à venir)

Serveur:
	Compiler:
	javac Serveur.java

	Lancer:
	java Serveur

Clients
	Entrepot:
		Compiler:
		pi4j -c ClientEntrepot.java

		Lancer:
		//192.168.4.1 est l'adresse du serveur, et 2228 est le port de communication
		pi4j -r ClientEntrepot 192.168.4.1 2228

	Écrémeuse:
		Compiler:
		pi4j -c ClientEcremeuse.java

		Lancer:
		//192.168.4.1 est l'adresse du serveur, et 2228 est le port de communication
		pi4j -r ClientEcremeuse 192.168.4.1 2228

	Centrifugeuse:
		Compiler:
		javac ClientCentrifugeuse.java

		Lancer:
		//192.168.4.1 est l'adresse du serveur, et 2228 est le port de communication
		java ClientCentrifugeuse 192.168.4.1 2228