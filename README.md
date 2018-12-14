Brancher les deux Raspberry Pi. Le code Serveur.java devrait ce trouver sur un Pi pouvant faire un "hotspot" (comme un Pi 3b par exemple). Le code client.java
devrait ce trouver sur un Pi qui ne consomme pas beaucoup de courant (Comme un Pi Zero W par exemple). Quand le Pi 3b sera démarré, il va créer un réseau Wi-fi
appelé "Fromagerie" sur lequel le Pi Zero va se connecter.

Serveur:
	Pour compiler le code: Naviguer dans le répertoire ou le code ce trouve. (Commande cd dans une fenêtre de commande)
	Compiler avec le compilateur java dans une fenêtre de commande: javac Serveur.java
	
	Pour lancer le code, ouvrir une fenêtre de commande dans le répertoire ou le code ce trouve, effectuer la commande:
	java Serveur

Client
	Pour compiler le code: Naviguer dans le répertoire ou le code ce trouve. (Commande cd)
	Compiler avec le compilateur java dans une fenêtre de commande: javac Client.java

	Pour lancer le code, ouvrir une fenêtre de commande dans le répertoire ou le code ce trouve, effectuer la commande:
	java Client 192.168.4.1 2228