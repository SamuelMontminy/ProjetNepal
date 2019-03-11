# Projet Népal
## À s'assurer avant de faire fonctionner le projet:
* Brancher les deux Raspberry Pi. Le code Serveur.java devrait ce trouver sur un Pi pouvant faire un "hotspot" (comme un Pi 3b par exemple).
* Les codes clients (ClientEntrepot.java, ClientCentrifugeuse.java & ClientEcremeuse.java) devraient ce trouver sur un Pi qui ne consomme pas beaucoup de courant 
(comme un Pi Zero W par exemple) pour maximiser la longitivité de la batterie. Quand le Pi 3b sera démarré, il va créer un réseau Wi-fi appelé "Fromagerie" sur lequel les Pi Zero vont se connecter automatiquement.

>Tout le code fonctionnel qui est necéssaire pour le projet ce trouve dans la branche "master". Le code qui se retrouve dans les autre branches est dépassé et ne devrait plus être utilisé.

## Procédures utilisés pour la réalisation du projet:
>Des liens pratiques qui nous ont aidés dans la réalisation du projet, qui pourraient s'avérer utiles si jamais il y à un problème.
* [Procédure pour configurer un Pi comme "hotspot"][hotspot]
* [Procédure pour installer l'image "Lite" pour les Pi Zero, et pour faire en sorte qu'ils se connectent au réseau automatiquement][InstallLite]

## Éléments nécéssaires sur l'installation de Raspbian
> L'image déja dans les Pi (et que nous avons en copie), contient déja ces éléments. Il n'est pas nécéssaire de refaire ces manipulations, sauf si il faut repartir d'une image "vanille" de Raspbian.
* Pour les Pi ayant les codes Entrepot et Écrémeuse, [il est nécéssaire d'installer Pi4J pour compiler et éxécuter le code][Pi4j]
* [Il est important de s'assurer que le I2C est bien activé pour les Pi qui en ont de besoin (Entrepot et Écrémeuse)][I2C]
* Pour le serveur, [il faut installer le SDK Hologram][SDK] (pour que le modem LTE fonctionne).
* Pour pouvoir recevoir les données de la carte SIM, celle-ci doit [être enregistrée dans le "dashboard" Hologram][Dashboard]
* Pour recevoir les données dans AWS S3, une route doit être fait dans Hologram pour rediriger les données vers S3 (Voir le tutoriel dans la section "Docs & Files" sur notre projet dans BaseCamp)
* Pour voir les données dans QuickSight, il faut des fichiers "manifests" qui dictent quelles données importer à partir de S3 (Voir le tutoriel dans la section "Docs & Files" sur notre projet dans BaseCamp)

>Java ne ce trouve pas sur l'installation standard de Raspbian, il est nécéssaire de l'installer avec la commande suivante
```sh
sudo apt-get install oracle-java8-jdk
```

## Démarrer les codes sur les différents Raspberry Pi
> Les différents codes s'éxécutent automatiquement dès l'alimentation des Pi. Pour arrêter les codes, nous devons se connecter en ssh, naviguer dans le dossier ou se trouve le script de démarrage avec la commande suivante
```sh
cd /etc/init.d
```
> et effectuer la commande suivante pour arrêter l'éxécution du code
```sh
sudo ./monScript stop
```
> Cette commande est la même pour tout les Pi, c'est-à-dire autant le module central et les Pi ZeroW

## Compiler le code manuellement
> Sur tout les Pi, les codes se retrouvent dans le même dossier, on peut faire la commande suivante pour s'y rendre
```sh
cd /home/pi/ProjetNepal
```
> Pour compiler et éxécuter le code manuellement, il faut se trouver dans le même dossier que le code.
##### Serveur (Pi 3b):
* Compiler le code avec javac
```sh
javac Serveur.java
```
##### Client Entrepot (Pi ZeroW)
* Compiler le code avec Pi4J
```sh
pi4j -c ClientEntrepot.java
```
##### Client Écrémeuse (Pi ZeroW)
* Compiler le code avec Pi4J
```sh
pi4j -c ClientEcremeuse.java
```
##### Client Centrifugeuse (Pi ZeroW)
* Compiler le code avec javac
```sh
javac ClientCentrifugeuse.java
```

## Éxécuter le code manuellement
> Si jamais on le désire, nous pouvons éxécuter les codes manuellement au lieu de "rebooter" les Pi. À noter qu'il est mieux d'arrêter l'éxécution automatique des codes avant de les partir manuellement (voir la section "Démarrer les codes sur les différents Raspberry Pi" pour plus d'informations). Pour éxécuter manuellement les codes, il faut se trouver dans le même répertoire que ceux-ci (voir la section "Compiler le code manuellement" pour plus d'informations). Le code doit avoir déja été compilé au moins une fois avant de pouvoir l'éxécuter.

##### Serveur (Pi 3b)
* Éxécuter le code avec Java (pas d'arguments)
```sh
java Serveur
```
##### Client Entrepot (Pi ZeroW)
* Éxécuter le code avec Pi4J
> 192.168.4.1 est l'adresse du serveur, et 2228 est le port de communication
```sh
pi4j -r ClientEntrepot 192.168.4.1 2228
```
##### Client Écrémeuse (Pi ZeroW)
* Éxécuter le code avec Pi4J
> 192.168.4.1 est l'adresse du serveur, et 2228 est le port de communication
```sh
pi4j -r ClientEcremeuse 192.168.4.1 2228
```
##### Client Centrifugeuse (Pi ZeroW)
* Éxécuter le code avec Java
> 192.168.4.1 est l'adresse du serveur, et 2228 est le port de communication
```sh
java ClientCentrifugeuse 192.168.4.1 2228
```

[hotspot]: <https://www.raspberrypi.org/documentation/configuration/wireless/access-point.md>
[InstallLite]: <https://www.losant.com/blog/getting-started-with-the-raspberry-pi-zero-w-without-a-monitor>
[Pi4j]: <http://pi4j.com/install.html>
[I2C]: <https://learn.adafruit.com/adafruits-raspberry-pi-lesson-4-gpio-setup/configuring-i2c>
[SDK]: <https://www.hackster.io/hologram/add-cellular-to-a-raspberry-pi-with-hologram-nova-ea5926>
   [Dashboard]: <[Dashboard] https://dashboard.hologram.io>
