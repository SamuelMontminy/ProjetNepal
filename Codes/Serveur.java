/**
 * @file   Serveur.java
 * @author Samuel Montminy & Harri Laliberté
 * @date   Février 2019
 * @brief  Code qui permet de recevoir des données envoyées par le client tcp/ip pour ensuite les envoyer sur le dashboard Hologram par LTE
 *         Le code doit être compilé avec /javac Serveur.java et doit être lancé avec /java Serveur
 *
 * @version 1.0 : Première version
 * @version 1.1 : Enregistre les données dans le système avant de les envoyer une fois par jour pour sauver du courant.
 * @version 1.2 : Un cavalier permet de décider si on est en mode debug ou normal. Le mode debug envoie directement les données par LTE quand elles sont reçues
 * @version 1.3 : Le Pi mets à jour son heure interne au démarrage par 2G/3G avec le module Hologram
 * Environnement de developpement: GitKraken / Notepad++
 * Compilateur: javac (Java version 1.8)
 * Matériel: Raspberry Pi Zero W, Module LTE Hologram (+ carte SIM)
 */

import java.net.*;              //Pour la communication par socket
import java.io.*;               //Pour les gpio
import java.util.regex.*;       //Pour l'analyse des trames des clients
import java.nio.file.*;         //Pour enregistrer les trames dans un fichier

public class Serveur implements Runnable
{
    private static final long TEMPS_30S = 30000;

    final static int NB_OCTETS = 1000;                              //Constante pour le nombre d'octets du tampon memoire du miniserveur
    int m_nPort = 2228;                                             //Numéro du port utilise par le miniserveur (doit être entré comme argument lorsque les codes clients sont lancés)
    ServerSocket m_ssServeur;                                       //Reference vers l'objet ServerSocket
    Thread m_tService;                                              //Reference vers l'objet Thread

    //"Pattern" en Regex qui sert à vérifier si la trame reçue correspond à ce qu'on attend
    String Pattern_TrameClient = "^(\\w{2}),(\\d+\\.?(?:\\d+)?),(\\d+\\.?(?:\\d+)?),(\\d+\\.?(?:\\d+)?),(\\d+\\.?(?:\\d+)?)$";
    String Pattern_Location = "^Location: \\{\"altitude\": \"(-?(?:\\d+)\\.?(?:\\d+)?)\", \"uncertainty\": \"(-?(?:\\d+)\\.?(?:\\d+)?)\", \"longitude\": \"(-?(?:\\d+)\\.?(?:\\d+)?)\", \"latitude\": \"(-?(?:\\d+)\\.?(?:\\d+)?)\", \"time\": \"(\\d+:\\d+:\\d+\\.\\d+)\", \"date\": \"(\\d+\\/\\d+\\/\\d+)\"\\}$";
    String Pattern_Heure = "^(\\d+):(\\d+):(\\d+)\\.(\\d+)$";
    String Pattern_Date = "^(\\d+)/(\\d+)/(\\d+)$";

    public static final String NAME_GPIO = "gpio21";                //Nom du GPIO pour le kernel Raspbian

    public EnvoieInformations m_objInformations;                    //Référence du thread qui sert à envoyer les informations
    public LectureCavalier m_objCavalier;                           //Référence du thread qui sert si le cavalier est en mode debug

    int ModeDebug = 2;                                              //Mode debug = 1, Mode normal = 0, on le mets à 2 au début pour forcer la lecture du cavalier au démarrage                            
    public boolean TimeUpdated = false;                             //Pour savoir si le temps à été mis à jour

    public Serveur()
    {		
        try
        {
            m_ssServeur = new ServerSocket(m_nPort, NB_OCTETS);     //Création du miniserveur au port specifie (m_nPort = 2228)
                                                                    //Le miniserveur a un tampon mémoire de 1000 octets (NB_OCTETS)
            m_tService = new Thread(this);                          //Creation et démarrage de la tâche d'écoute du miniserveur sur le port 2228
            m_tService.start();

            m_objInformations = new EnvoieInformations(this);       //Démarre le thread qui sert à envoyer les informations
            m_objCavalier = new LectureCavalier(this);              //Démarre le thread qui fait la lecture de la position du cavalier

            System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");

            gpioUnexport("20");          						//Déffectation du GPIO #20 (au cas ou ce GPIO est déjè défini par un autre programme)
			gpioExport("20");            						//Affectation du GPIO #20
			gpioSetdir("gpio20", "out");   						//Place GPIO #20 en sortie (Pour agir comme un 5V pour la lecture du cavalier)

            gpioUnexport("21");          						//Déffectation du GPIO #21 (au cas ou ce GPIO est déjè défini par un autre programme)
			gpioExport("21");            						//Affectation du GPIO #21
			gpioSetdir("gpio21", "in");   						//Place GPIO #21 en entrée (Lecture du cavalier pour le mode debug)

            System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        }
        
        catch (IOException e)
        {
            System.out.println(e.toString());
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
        }
    }
    
    //Tâche d'écoute sur le port 2228
    public void run()
    {   
		String Informations = "";                                                           //Les données vont être reçues en string dans cette variable
        String json = "";                                                                   //La trame un coup qu'elle sera prête à être envoyée
        String Temps = "";                                                                  //Le temps reçu de "modem location" mais dans le bon format pour la commande "timedatectl"
        String Date = "";                                                                   //La date reçue de "modem location" mais dans le bon format pour la commande "timedatectl"
        String retour7 = "";                                                                //Pour le retour de la commande/process 7 (modem location)

        try
        {
            //Ce bloc permet de d'activer l'alimentation sur les ports USB                  //<- DÉBUT DU BLOC
            String s8 = "echo '1-1' |sudo tee /sys/bus/usb/drivers/usb/bind";    			//Commande bash a etre executee
            String[] sCmd8 = {"/bin/bash", "-c", s8};             			                //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

            System.out.println(sCmd8[0] + " " + sCmd8[1] + " " + sCmd8[2]);                 //Affiche la commande a executer dans la console Java
            Process p8 = Runtime.getRuntime().exec(sCmd8);        			                //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

            p8.waitFor();                                                                   //Attend que la commande soit éxécutée soit terminée

            if (p8.getErrorStream().available() > 0)        					            //Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
            {
                //Affiche l'erreur survenue
                BufferedReader brCommand8 = new BufferedReader(new InputStreamReader(p8.getErrorStream()));
                System.out.println(brCommand8.readLine());
                brCommand8.close();
            }                                                                               //<-FIN DU BLOC

            System.out.println("Début de l'acquisition de la date et de l'heure par 2G/3G...");

            while (retour7.contains("altitude") == false)                                   //Regarde si la commande à réussie (ne contient pas "altitude si elle échoue")
            {
                //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
                Thread.sleep(TEMPS_30S);                                                    //Réessaie la commande chaque 30 secondes
                //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

                String s7 = "sudo hologram modem location";    			                    //Commande bash a etre executee
                String[] sCmd7 = {"/bin/bash", "-c", s7};             			            //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

                System.out.println(sCmd7[0] + " " + sCmd7[1] + " " + sCmd7[2]);             //Affiche la commande a executer dans la console Java
                Process p7 = Runtime.getRuntime().exec(sCmd7);        			            //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

                //p7.waitFor();                                                               //Attend que la commande soit éxécutée soit terminée

                BufferedReader reader1 = new BufferedReader(new InputStreamReader(p7.getInputStream()));        //Objet pour la lecture du retour
                
                retour7 = reader1.readLine();                                               //Lis ce que la commande retourne dans le terminal
                
                if (retour7 == null)
                {
                    retour7 = "";
                }

                else
                {
                    System.out.println("Ligne trouvée: " + retour7);
                }
            }

            System.out.println("Acquisition de la date et de l'heure réussie");

            //Ce bloc permet de de désactiver l'alimentation sur les ports USB              //<- DÉBUT DU BLOC
            String s9 = "echo '1-1' |sudo tee /sys/bus/usb/drivers/usb/unbind";    			//Commande bash a etre executee
            String[] sCmd9 = {"/bin/bash", "-c", s9};             			                //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

            System.out.println(sCmd9[0] + " " + sCmd9[1] + " " + sCmd9[2]);                 //Affiche la commande a executer dans la console Java
            Process p9 = Runtime.getRuntime().exec(sCmd9);        			                //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

            p9.waitFor();                                                                   //Attend que la commande soit éxécutée soit terminée

            if (p9.getErrorStream().available() > 0)        					            //Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
            {
                //Affiche l'erreur survenue
                BufferedReader brCommand9 = new BufferedReader(new InputStreamReader(p9.getErrorStream()));
                System.out.println(brCommand9.readLine());
                brCommand9.close();
            }                                                                               //<- FIN DU BLOC	

            Pattern r2 = Pattern.compile(Pattern_Location);          //Compile le "pattern" en Regex déclaré plus tôt (pour voir s'il y a des erreurs)
            Matcher m2 = r2.matcher(retour7);                        //Crée un objet de type matcher, qui va permettre de comparer la trame que l'on reçoit avec le pattern défini
            
            if (m2.find( ))                                          //Regarde si la trame reçue correspond au "pattern"       
            {
                //Affiche les valeurs trouvés dans les groupes du pattern regex. Chaque groupe correspond à la valeur reçue de la commande location
                System.out.println("Altitude: "      + m2.group(1));
                System.out.println("Incertitude: "   + m2.group(2));
                System.out.println("Longitude: "     + m2.group(3));
                System.out.println("Latitude: "      + m2.group(4));
                System.out.println("Temps: "         + m2.group(5));
                System.out.println("Date: "          + m2.group(6));

                String ATemps = m2.group(5);        //Mets seulement le temps dans la variable, qui n'est pas encore dans le bon format pour la commande "timedatectl"
                String ADate = m2.group(6);         //Mets seulement la date dans la variable, qui n'est pas encore dans le bon format pour la commande "timedatectl"

                Pattern r3 = Pattern.compile(Pattern_Heure);        //Compile le "pattern" en Regex déclaré plus tôt (pour voir s'il y a des erreurs)
                Matcher m3 = r3.matcher(ATemps);                    //Crée un objet de type matcher, qui va permettre de comparer l'heure acquise avec le pattern défini

                if (m3.find( ))                                     //Regarde si la l'heure reçue correspond au "pattern"     
                {
                    Temps = m3.group(1) + ":" + m3.group(2) + ":" + m3.group(3);        //Mets l'heure dans le bon format pour la commande "timedatectl"
                }

                Pattern r4 = Pattern.compile(Pattern_Date);         //Compile le "pattern" en Regex déclaré plus tôt (pour voir s'il y a des erreurs)
                Matcher m4 = r4.matcher(ADate);                     //Crée un objet de type matcher, qui va permettre de comparer la date acquise avec le pattern défini

                if (m4.find( ))                                     //Regarde si la la date reçue correspond au "pattern"
                {
                    Date = m4.group(3) + "-" + m4.group(2) + "-" + m4.group(1);         //Mets la date dans le bon format pour la commande "timedatectl"
                }

                //Ce bloc permet de de mettre à jour l'heure du Pi                                  //<- DÉBUT DU BLOC
                String s10 = "sudo timedatectl set-time \"" + Date + " " + Temps + "\"";    	    //Commande bash a etre executee
                String[] sCmd10 = {"/bin/bash", "-c", s10};             			                //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

                System.out.println(sCmd10[0] + " " + sCmd10[1] + " " + sCmd10[2]);                  //Affiche la commande a executer dans la console Java
                Process p10 = Runtime.getRuntime().exec(sCmd10);        			                //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

                p10.waitFor();                                                                      //Attend que la commande soit éxécutée soit terminée
                                    
                if (p10.getErrorStream().available() > 0)        					                //Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
                {
                    //Affiche l'erreur survenue
                    BufferedReader brCommand10 = new BufferedReader(new InputStreamReader(p10.getErrorStream()));
                    System.out.println(brCommand10.readLine());
                    brCommand10.close();
                }

                TimeUpdated = true;                                                                 //<- FIN DU BLOC
            }                                                                       	

            while(m_tService != null)                                           //Boucle principale du thread d'écoute du socket TCP/IP
            {
				Informations = "Erreur de lecture du client";                   //Permet de savoir si il y a eu une erreur de lecture du message (la variable va rester inchangée)
                System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
                System.out.println("Attente d'une connexion au serveur...");    //Le miniserveur attend une connexion réseau... -> BLOQUANT! <-
                Socket sConnexion = m_ssServeur.accept();

                System.out.println("Connexion au client établie!");             //Ce message est affiché si une connexion avec un client est établie
                
                InputStream isIn = sConnexion.getInputStream();                 //Objet pour la reception des données
                ObjectInputStream oisIn = new ObjectInputStream(isIn);          //Reçoit les données envoyés par le client
                Informations = (String)oisIn.readObject();               		//Lit le contenu des données reçues et les mets dans Informations
               
                oisIn.close();                                                  //Fermeture des objets de flux de données
                isIn.close();

                System.out.println(Informations + " -> à été reçu d'un client");
            
                Pattern r1 = Pattern.compile(Pattern_TrameClient);              //Compile le "pattern" en Regex déclaré plus tôt
                Matcher m1 = r1.matcher(Informations);                          //Crée un objet de type matcher, qui va permettre de comparer la trame que l'on reçoit avec le pattern

                if (m1.find( ))                                                 //Regarde si la trame reçue correspond au "pattern"       
                {
                    //Affiche les valeurs trouvés dans les groupes du pattern regex. Chaque groupe correspond à la valeur d'un capteur (ID, T, P, H, R)
                    System.out.println("ID: "          + m1.group(1));
                    System.out.println("Température: " + m1.group(2));
                    System.out.println("Pression: "    + m1.group(3));
                    System.out.println("Humidité: "    + m1.group(4));
                    System.out.println("RPM: "         + m1.group(5));

                    //Ajoute la date comme sixième argument de la trame json
                    json = "{ \\\"ID\\\":\\\"" + m1.group(1) + "\\\", \\\"T\\\":\\\"" + m1.group(2) + "\\\", \\\"P\\\":\\\"" + m1.group(3) + "\\\", \\\"H\\\":\\\"" + m1.group(4) + "\\\", \\\"R\\\":\\\"" + m1.group(5) + "\\\", \\\"D\\\":\\\"" + java.time.LocalDateTime.now() + "\\\" }";
                    System.out.println("Trame json crée: " + json);

                    json = "\"" + json + "\"";      //Pour échapper les crochets au début et à la fin de la trame
                    json += "\r";

                    if (ModeDebug == 0)             //Accumule les données dans un fichier .txt
                    {
                        //Spécifie dans quel fichier enregistrer la trame, CREATE pour créer le fichier s'il n'existe pas déja, APPEND pour ajouter l'information dans le fichier au lieu de l'écraser
                        Files.write(Paths.get("/home/pi/ProjetNepal/Data.txt"), json.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        System.out.println("Mode debug désactivé, " + json + " à été sauvegardé dans le fichier Data.txt");
                    }

                    else if (ModeDebug == 1)        //Envoie tout de suite les données par LTE
                    {
                        System.out.println("Mode debug activé, " + json + " -> sera envoyé directement à Hologram");

                        String s2 = "sudo hologram send " + json;    			                    //Commande bash a etre executee
                        String[] sCmd2 = {"/bin/bash", "-c", s2};             			            //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

                        System.out.println(sCmd2[0] + " " + sCmd2[1] + " " + sCmd2[2]);             //Affiche la commande a executer dans la console Java
                        Process p2 = Runtime.getRuntime().exec(sCmd2);        			            //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

                        p2.waitFor();                                                               //Attend que la commande soit éxécutée soit terminée

                        if (p2.getErrorStream().available() > 0)        					        //Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
                        {
                            //Affiche l'erreur survenue
                            BufferedReader brCommand2 = new BufferedReader(new InputStreamReader(p2.getErrorStream()));
                            System.out.println(brCommand2.readLine());
                            brCommand2.close();
                        }

                        System.out.println("Mode debug activé, " + json + " à été envoyé directement à Hologram");
                    }
                }
                
                else
                {
                    System.out.println("Trame inconnue reçue: " + Informations);
                }
            }
        }

        catch (IOException e)
        {
            System.out.println(e.toString());                               //Probleme de communication reseau
        }
        catch (ClassNotFoundException e)
        {
            System.out.println(e.toString());                               //Objet indefinie pour la serialisation...
        }
        catch (Exception e)
        {
            System.out.println(e.toString());                               //Affiche l'erreur survenue en Java
        }
    }
    
    public static void main(String[] args)
    {
        new Serveur();                                                    		//Appelle le constructeur de la classe ServeurSocket (pas d'arguments)
    }

    //Pour lire l'état du GPIO
	//Fonction faite par Pierre Bergeron (Modifiée par Samuel Montminy)
    public Integer gpioReadBit(String name_gpio)
    {
        String sLecture;

        try
        {
            FileInputStream fis = new FileInputStream("/sys/class/gpio/" + name_gpio + "/value");           //Sélection de la destination du flux de
                                                                                                            //données (sélection du fichier d'entrée)
                                                                                                            
            DataInputStream dis = new DataInputStream(fis);                                                 //Canal vers le fichier (entrée en "streaming")
            sLecture = dis.readLine();                                                                      //Lecture du fichier                
                                                                                                            
            dis.close();                                                                                    //Fermeture du canal
            fis.close();                                                                                    //Fermeture du flux de données
        }
		
        catch (Exception e)
        {
            // Affiche l'erreur survenue en Java
            sLecture = "-1";
            System.out.println("Error on gpio readbits" + name_gpio + " :");
            System.out.println(e.toString());
        }
		
        return new Integer(sLecture);  												//Retourne l'état "supposé" de la sortie
    }
	
	//Pour désaffecter le GPIO par kernel
	//Fonction faite par Pierre Bergeron (Modifiée par Samuel Montminy)
    public boolean gpioUnexport(String gpioid)   
    {  
        boolean bError = true;  													//Pour gestion des erreurs
		
        try
        {
            String sCommande = "echo \"" + gpioid + "\">/sys/class/gpio/unexport";  //Commande bash à être exécutée
            String[] sCmd = {"/bin/bash", "-c", sCommande};                       	//Spécifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande à exécuter suit
                                                                                    
            System.out.println(sCmd[0] + " " + sCmd[1] + " " + sCmd[2]);            //Affiche la commande à exécuter dans la console Java
            Process p = Runtime.getRuntime().exec(sCmd);                            //Exécute la commande par le système Linux (le programme Java
                                                                                    //doit être démarré par le root pour les accès aux GPIO)
 
            if(p.getErrorStream().available() > 0)                                  //Vérification s'il y a une erreur d'exécution par l'interpreteur de commandes BASH
            {
                // Affiche l'erreur survenue
                bError = false;
                BufferedReader brCommand = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                System.out.println(brCommand.readLine());
                brCommand.close();
			}
			
			Thread.sleep(20);   												//Délai pour laisser le temps au kernel d'agir
        }
		
        catch (Exception e)      												//Traitement de l'erreur par la VM Java (différent de l'erreur par l'interpreteur BASH)
        {
			//Affiche l'erreur survenue en Java
            bError = false;
            System.out.println("Error on export GPIO :" + gpioid);
            System.out.println(e.toString());
        }
		
        return  bError;
    }
	
	//Pour affecter le GPIO par kernel
	//Fonction faite par Pierre Bergeron (Modifiée par Samuel Montminy)
    public boolean gpioExport(String gpioid)   
    {  
        boolean bError = true;  												//Pour gestion des erreurs
        
		try
        {
            String s = "echo \"" + gpioid + "\">/sys/class/gpio/export";        //Commande bash à être exécutée
            String[] sCmd = {"/bin/bash", "-c", s};                            	//Spécifie que l'interpreteur de commandes est BASH. Le "-c"
                                                                                //indique que la commande à exécuter suit
                                                                                
            System.out.println(sCmd[0] + " " + sCmd[1] + " " + sCmd[2]);        //Affiche la commande à exécuter dans la console Java
            Process p = Runtime.getRuntime().exec(sCmd);                        //Exécute la commande par le système Linux (le programme Java 
                                                                                //doit être démarré par le root pour les accès aux GPIO)
     
            if (p.getErrorStream().available() > 0)        						//Vérification s'il y a une erreur d'exécution par l'interpréteur de commandes BASH
            {
                //Affiche l'erreur survenue
                bError = false;
                BufferedReader brCommand = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                System.out.println(brCommand.readLine());
                brCommand.close();
            }
            Thread.sleep(100);      											//Délai pour laisser le temps au kernel d'agir
        }
		 
		catch (Exception e)         											//Traitement de l'erreur par la VM Java (différent de l'erreur par l'interpreteur BASH)
		{
			//Affiche l'erreur survenue en Java
			bError = false;
			System.out.println("Error on export GPIO :" + gpioid);
			System.out.println(e.toString());
		}
		 
        return bError;
    }  
	
	//Configure la direction du GPIO
    //name_gpio : nom associé au répertoire créé par le kernel (gpio +  no i/o du port : Ex: GPIO 2 ==> pgio2)
    //sMode : Configuration de la direction du GPIO("out" ou "in")
	//Fonction faite par Pierre Bergeron (Modifiée par Samuel Montminy)
    public boolean gpioSetdir(String name_gpio, String sMode)   
    {  
        boolean bError = true;  												//Pour gestion des erreurs
		
        try
        {
			String sCommande = "echo \"" + sMode + "\" >/sys/class/gpio/" + name_gpio + "/direction";   //Commande bash à être exécutée
            String[] sCmd = { "/bin/bash", "-c", sCommande };                                           //Spécifie que l'interpreteur de commandes est BASH. Le "-c"
                                                                                                        //Indique que la commande à exécuter suit
                                                                                    
            System.out.println(sCmd[0] + " " + sCmd[1] + " " + sCmd[2]);    	//Affiche la commande à exécuter dans la console Java
            Process p = Runtime.getRuntime().exec(sCmd);                    	//Exécute la commande par le système Linux (le programme Java doit 
																				//être démarré par le root pour les accès aux GPIO)
     
            if(p.getErrorStream().available() > 0)        						//Vérification s'il y a une erreur d'exécution par l'interpreteur de commandes BASH
            {
                //Affiche l'erreur survenue
                bError = false;
                BufferedReader brCommand = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                sCommande = brCommand.readLine();
                System.out.println(sCommande);
                brCommand.close();
            }
			
            Thread.sleep(100);      											//Délai pour laisser le temps au kernel d'agir
	    }
		
	    catch (Exception e)
	    {
			//Affiche l'erreur survenue en Java
			bError = false;
			System.out.println("Error on direction setup :");
			System.out.println(e.toString());
	    }
		
		return bError;
    }

	//Change l'état du GPIO
    //name_gpio : nom associé au répertoire créé par le kernel (gpio +  no i/o du port : Ex: GPIO 2 ==> pgio2)
    //value : état à placer sur la ligne
	//Fonction faite par Pierre Bergeron (Modifiée par Samuel Montminy)
    public Integer gpioSetBit(String name_gpio, String value)   
    {       
        try
        {
            FileOutputStream fos = new FileOutputStream("/sys/class/gpio/" + name_gpio + "/value");         //Sélection de la destination du flux de
                                                                                                            //données (sélection du fichier de sortie)
                                                                                                            
            DataOutputStream dos = new DataOutputStream(fos);                                               //Canal vers le fichier (sortie en "streaming")
            dos.write(value.getBytes(), 0, 1);                                                              //Écriture dans le fichier
                                                                                                            //(changera l'état du GPIO: 0 ==> niveau bas et différent de 0 niveau haut)
                                                                                                            
            //System.out.println("/sys/class/gpio/" + name_gpio + "/value = " + value);                       //Affiche l'action réalisée dans la console Java
            dos.close();                                                                                    //Fermeture du canal
            fos.close();                                                                                    //Fermeture du flux de données
        }
		
        catch(Exception e)																					//Affiche l'erreur survenue en Java
        {
            value = "-1";
            System.out.println("Error on gpio setbits" + name_gpio + " :");
            System.out.println(e.toString());
        }
		
        return new Integer(value);  																		//Retourne l'état "supposé" de la sortie
	}
}

class LectureCavalier implements Runnable
{
    private static final long TEMPS_5S = 5000;
    private static final long TEMPS_1M30 = 90000;

    Thread m_Thread;
    private Serveur m_Parent;

    public LectureCavalier(Serveur Parent)
    {
        try
        {
            m_Parent = Parent;

            m_Thread = new Thread(this);
            m_Thread.start();
        }

        catch (Exception e)
        {
            System.out.println(e.toString());
        }
    }

    public void run()
    {
        try
        {
            while (m_Parent.TimeUpdated == false)
            {
                Thread.sleep(100);
            }

            //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
            Thread.sleep(TEMPS_5S);                 //Le temps que la classe principale crée le socket TCP/IP et écoute sur le port
            //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            m_Parent.gpioSetBit("gpio20", "1");     //Mets le gpio21 à 5V pour que nous puissons lire un niveau haut sur le gpio21 quand on est en mode debug
            Thread.sleep(250);                      //Laisser le temps à la pin de ce mettre à un niveau haut avant de faire une lecture

            while (true)
            {
                if (m_Parent.gpioReadBit(m_Parent.NAME_GPIO) == 1)          //Mode debug (envoie les données directement par LTE quand elles sont reçues)
                {
                    if (m_Parent.ModeDebug != 1)                            //Pour que ça éxécute le code seulement une fois
                    {
                        m_Parent.ModeDebug = 1;                             //Active le mode debug
                        System.out.println("Mode debug: on");

                        //Ce bloc permet de d'activer l'alimentation sur les ports USB                  //<- DÉBUT DU BLOC
                        String s1 = "echo '1-1' |sudo tee /sys/bus/usb/drivers/usb/bind";    			//Commande bash a etre executee
                        String[] sCmd1 = {"/bin/bash", "-c", s1};             			                //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

                        System.out.println(sCmd1[0] + " " + sCmd1[1] + " " + sCmd1[2]);                 //Affiche la commande a executer dans la console Java
                        Process p1 = Runtime.getRuntime().exec(sCmd1);        			                //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

                        if (p1.getErrorStream().available() > 0)        					            //Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
                        {
                            //Affiche l'erreur survenue
                            BufferedReader brCommand1 = new BufferedReader(new InputStreamReader(p1.getErrorStream()));
                            System.out.println(brCommand1.readLine());
                            brCommand1.close();
                        }                                                                               //<-FIN DU BLOC
                                                                                                        
                        //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
                        Thread.sleep(TEMPS_1M30);               //Délai de 90 secondes pour laisser le temps au modem d'avoir un signal LTE
                        //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
                        
                        //Bloc qui sert à faire un test de connection avant d'envoyer des données     	//<- DÉBUT DU BLOC
                        String s4 = "sudo hologram network connect";    			    	            //Commande bash a etre executee
                        String[] sCmd4 = {"/bin/bash", "-c", s4};             			                //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

                        System.out.println(sCmd4[0] + " " + sCmd4[1] + " " + sCmd4[2]);                 //Affiche la commande a executer dans la console Java
                        Process p4 = Runtime.getRuntime().exec(sCmd4);        			                //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

                        if (p4.getErrorStream().available() > 0)        					            //Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
                        {
                            //Affiche l'erreur survenue
                            BufferedReader brCommand4 = new BufferedReader(new InputStreamReader(p4.getErrorStream()));
                            System.out.println(brCommand4.readLine());
                            brCommand4.close();
                        }                                                                               //<- FIN DU BLOC
                    }                                                                       
                }

                else if (m_Parent.gpioReadBit(m_Parent.NAME_GPIO) == 0)     //Mode normal (accumule les données pendant un certain nombre de temps avant de les envoyer)
                {
                    if (m_Parent.ModeDebug != 0)                            //Pour que ça éxécute le code seulement une fois
                    {
                        m_Parent.ModeDebug = 0;                             //Désactive le mode debug
                        System.out.println("Mode debug: off");

                        //Ce bloc permet de de désactiver l'alimentation sur les ports USB                  //<- DÉBUT DU BLOC
                        String s3 = "echo '1-1' |sudo tee /sys/bus/usb/drivers/usb/unbind";    			    //Commande bash a etre executee
                        String[] sCmd3 = {"/bin/bash", "-c", s3};             			                    //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

                        System.out.println(sCmd3[0] + " " + sCmd3[1] + " " + sCmd3[2]);                     //Affiche la commande a executer dans la console Java
                        Process p3 = Runtime.getRuntime().exec(sCmd3);        			                    //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

                        if (p3.getErrorStream().available() > 0)        					                //Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
                        {
                            //Affiche l'erreur survenue
                            BufferedReader brCommand3 = new BufferedReader(new InputStreamReader(p3.getErrorStream()));
                            System.out.println(brCommand3.readLine());
                            brCommand3.close();
                        }                                                                                   //<- FIN DU BLOC
                    }
                }

                //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
                Thread.sleep(TEMPS_5S);                                                                     //Lecture du GPIO à chaque 5 secondes
                //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
            }
        }

        catch (Exception e)
        {
            System.out.println(e.toString());
        }
    }
}

class EnvoieInformations implements Runnable
{
    private static final long TEMPS_5S = 5000;
    private static final long TEMPS_30S = 30000;
    private static final long TEMPS_1M = 60000;
    private static final long TEMPS_1M30 = 90000;
    private static final long TEMPS_2M = 120000;
    private static final long TEMPS_5M = 300000;
    private static final long TEMPS_10M = 600000;
    private static final long TEMPS_20M = 1200000;
    private static final long TEMPS_30M = 1800000;
    private static final long TEMPS_45M = 2700000;

    private static final long TEMPS_1H = 3600000;
    private static final long TEMPS_2H = 7200000;
    private static final long TEMPS_4H = 14400000;
    private static final long TEMPS_6H = 21600000;
    private static final long TEMPS_8H = 28800000;
    private static final long TEMPS_12H = 43200000;
    private static final long TEMPS_24H = 86400000;

    Thread m_Thread;
    private Serveur m_Parent;
    String Donnee = "";
    boolean AfficheMessage = true;

    public EnvoieInformations(Serveur Parent)
    {
        try
        {
            m_Parent = Parent;

            m_Thread = new Thread(this);
            m_Thread.start();
        }

        catch (Exception e)
        {
            System.out.println(e.toString());
        }
    }

    public void run()
    {
        File file = new File("/home/pi/ProjetNepal/Data.txt");      //Fichier dans lequel les trames seront enregistrées
        String retour4 = "";        //Pour le retour de la commande/process 4 (Connection au réseau 2G/3G)
        String retour2 = "";        //Pour le retour de la commande/process 2 (Envoi d'une donnée)

        try
        {
            while (m_Parent.TimeUpdated == false)
            {
                Thread.sleep(100);
            }

            //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
            Thread.sleep(TEMPS_1M);                                                                //<- Avant d'envoyer le premier bloc de données
            //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            while (true)
            {
                if (m_Parent.ModeDebug == 0 && file.length() != 0)                                  //Les données accumulées sont seulement envoyées si on est pas en mode debug
                {
                    AfficheMessage = true;

                    System.out.println("Début de l'envoi du bloc de données");

                    //Ce bloc permet de d'activer l'alimentation sur les ports USB                  //<- DÉBUT DU BLOC
                    String s1 = "echo '1-1' |sudo tee /sys/bus/usb/drivers/usb/bind";    			//Commande bash a etre executee
                    String[] sCmd1 = {"/bin/bash", "-c", s1};             			                //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

                    System.out.println(sCmd1[0] + " " + sCmd1[1] + " " + sCmd1[2]);                 //Affiche la commande a executer dans la console Java
                    Process p1 = Runtime.getRuntime().exec(sCmd1);        			                //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

                    p1.waitFor();                                                                   //Attend que la commande soit éxécutée soit terminée            

                    if (p1.getErrorStream().available() > 0)        					            //Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
                    {
                        //Affiche l'erreur survenue
                        BufferedReader brCommand1 = new BufferedReader(new InputStreamReader(p1.getErrorStream()));
                        System.out.println(brCommand1.readLine());
                        brCommand1.close();
                    }                                                                               //<-FIN DU BLOC
                                                                                                    
                    //Bloc qui sert à faire un test de connection avant d'envoyer des données     	    //<- DÉBUT DU BLOC
                    while (retour4.contains("PPP session started") == false)
                    {
                        //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
                        Thread.sleep(TEMPS_30S);        //Réessaie à chaque 30 secondes
                        //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

                        String s4 = "sudo hologram network connect";    			    	            //Commande bash a etre executee
                        String[] sCmd4 = {"/bin/bash", "-c", s4};             			                //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

                        System.out.println(sCmd4[0] + " " + sCmd4[1] + " " + sCmd4[2]);                 //Affiche la commande a executer dans la console Java
                        Process p4 = Runtime.getRuntime().exec(sCmd4);        			                //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

                        p4.waitFor();                                                                   //Attend que la commande soit éxécutée soit terminée                                                                   

                        BufferedReader reader2 = new BufferedReader(new InputStreamReader(p4.getInputStream()));        //Objet pour la lecture du retour
                    
                        retour4 = reader2.readLine();                                                   //Lis ce que la commande retourne dans le terminal
                        
                        if (retour4 == null)
                        {
                            retour4 = "";
                        }

                        else
                        {
                            System.out.println("Ligne trouvée: " + retour4);                                //<- FIN DU BLOC
                        }
                    }

                    System.out.println("Connection au réseau 2G/3G réussie, début de l'envoi du bloc de données");

                    //Ce bloc éxécute la commande qui envoie les informations à Hologram            //<- DÉBUT DU BLOC
                    
                    BufferedReader br = new BufferedReader(new FileReader(file));                   //Fichier à partir duquel on lit les informations

                    while ((Donnee = br.readLine()) != null) 
                    {
                        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
                        System.out.println(Donnee + " -> sera envoyé à Hologram");

                        while (retour2.contains("Message sent successfully") == false)
                        {
                            //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
                            Thread.sleep(TEMPS_30S);        //Réessaie à chaque 30 secondes
                            //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

                            String s2 = "sudo hologram send " + Donnee;    			                    //Commande bash a etre executee
                            String[] sCmd2 = {"/bin/bash", "-c", s2};             			            //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

                            System.out.println(sCmd2[0] + " " + sCmd2[1] + " " + sCmd2[2]);             //Affiche la commande a executer dans la console Java
                            Process p2 = Runtime.getRuntime().exec(sCmd2);        			            //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

                            p2.waitFor();                                                               //Attend que la commande soit éxécutée soit terminée
                        
                            BufferedReader reader3 = new BufferedReader(new InputStreamReader(p2.getInputStream()));        //Objet pour la lecture du retour

                            retour2 = reader3.readLine();                                               //Lis ce que la commande retourne dans le terminal
                            
                            if (retour2 == null)
                            {
                                retour2 = "";
                            }

                            else
                            {
                                System.out.println("Ligne trouvée: " + retour2);
                            }
                        }

                        retour2 = "";
                        System.out.println(Donnee + " -> à été envoyé à Hologram");
                        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
                    }

                    br.close();                                                                         //<- FIN DU BLOC	

                    //Ce bloc permet de de désactiver l'alimentation sur les ports USB                  //<- DÉBUT DU BLOC
                    String s6 = "echo '1-1' |sudo tee /sys/bus/usb/drivers/usb/unbind";    			    //Commande bash a etre executee
                    String[] sCmd6 = {"/bin/bash", "-c", s6};             			                    //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

                    System.out.println(sCmd6[0] + " " + sCmd6[1] + " " + sCmd6[2]);                     //Affiche la commande a executer dans la console Java
                    Process p6 = Runtime.getRuntime().exec(sCmd6);        			                    //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

                    p6.waitFor();                                                                       //Attend que la commande soit éxécutée soit terminée

                    if (p6.getErrorStream().available() > 0)        					                //Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
                    {
                        //Affiche l'erreur survenue
                        BufferedReader brCommand6 = new BufferedReader(new InputStreamReader(p6.getErrorStream()));
                        System.out.println(brCommand6.readLine());
                        brCommand6.close();
                    }                                                                                   //<- FIN DU BLOC		                     

                    //Ce bloc permet de supprimer les données après qu'elles ont été envoyées           //<- DÉBUT DU BLOC
                    String s5 = "sudo rm /home/pi/ProjetNepal/Data.txt";    			                //Commande bash a etre executee
                    String[] sCmd5 = {"/bin/bash", "-c", s5};             			                    //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

                    System.out.println(sCmd5[0] + " " + sCmd5[1] + " " + sCmd5[2]);                     //Affiche la commande a executer dans la console Java
                    Process p5 = Runtime.getRuntime().exec(sCmd5);        			                    //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

                    p5.waitFor();                                                                       //Attend que la commande soit éxécutée soit terminée

                    if (p5.getErrorStream().available() > 0)        					                //Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
                    {
                        //Affiche l'erreur survenue
                        BufferedReader brCommand5 = new BufferedReader(new InputStreamReader(p5.getErrorStream()));
                        System.out.println(brCommand5.readLine());
                        brCommand5.close();
                    }                                                                                   //<- FIN DU BLOC

                    //Ce bloc permet de supprimer les données après qu'elles ont été envoyées           //<- DÉBUT DU BLOC
                    String s3 = "sudo touch /home/pi/ProjetNepal/Data.txt";    			                //Commande bash a etre executee
                    String[] sCmd3 = {"/bin/bash", "-c", s3};             			                    //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

                    System.out.println(sCmd3[0] + " " + sCmd3[1] + " " + sCmd3[2]);                     //Affiche la commande a executer dans la console Java
                    Process p3 = Runtime.getRuntime().exec(sCmd3);        			                    //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

                    p3.waitFor();                                                                       //Attend que la commande soit éxécutée soit terminée

                    if (p3.getErrorStream().available() > 0)        					                //Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
                    {
                        //Affiche l'erreur survenue
                        BufferedReader brCommand3 = new BufferedReader(new InputStreamReader(p3.getErrorStream()));
                        System.out.println(brCommand3.readLine());
                        brCommand3.close();
                    }                                                                                   //<- FIN DU BLOC

                    System.out.println("Fin de l'envoi du bloc de données");

                    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
                    Thread.sleep(TEMPS_6H);                                                             //<- DÉLAI ENTRE CHAQUE ENVOI DE DONNÉES
                    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
                }

                else if (m_Parent.ModeDebug == 1 && AfficheMessage == true)
                {
                    System.out.println("Envoi de données annulé puisque le mode debug est activé");
                    AfficheMessage = false;
                }
            }
        }

        catch (Exception e)
        {
            System.out.println(e.toString());
        }
    }
}