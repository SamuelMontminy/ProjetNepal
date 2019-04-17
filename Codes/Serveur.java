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
    final static int NB_OCTETS = 1000;                              //Constante pour le nombre d'octets du tampon memoire du miniserveur
    int m_nPort = 2228;                                             //Numéro du port utilise par le miniserveur (doit être entré comme argument lorsque les codes clients sont lancés)
    ServerSocket m_ssServeur;                                       //Reference vers l'objet ServerSocket
    Thread m_tService;                                              //Reference vers l'objet Thread

    //"Pattern" en Regex qui sert à vérifier si la trame reçue correspond à ce qu'on attend
    String pattern = "^(\\w{2}),(\\d+\\.?(?:\\d+)?),(\\d+\\.?(?:\\d+)?),(\\d+\\.?(?:\\d+)?),(\\d+\\.?(?:\\d+)?)$";

    public static final String NAME_GPIO = "gpio21";                //Nom du GPIO pour le kernel Raspbian

    public EnvoieInformations m_objInformations;                    //Référence du thread qui sert à envoyer les informations
    public LectureCavalier m_objCavalier;                           //Référence du thread qui sert si le cavalier est en mode debug

    int ModeDebug = 2;                                              //Mode debug = 1, Mode normal = 0, on le mets à 2 au début pour forcer la lecture du cavalier au démarrage                            
    
    public Serveur()
    {		
        try
        {
            m_ssServeur = new ServerSocket(m_nPort, NB_OCTETS);             //Création du miniserveur au port specifie (m_nPort = 2228)
                                                                            //Le miniserveur a un tampon mémoire de 1000 octets (NB_OCTETS)
            m_tService = new Thread(this);                                  //Creation et démarrage de la tâche d'écoute du miniserveur sur le port 2228
            m_tService.start();

            m_objInformations = new EnvoieInformations(this);               //Démarre le thread qui sert à envoyer les informations
            m_objCavalier = new LectureCavalier(this);                      //Démarre le thread qui fait la lecture de la position du cavalier

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
		String Informations = "";                                               //Les données vont être reçues en string dans cette variable
        String json = "";

        try
        {
            while(m_tService != null)
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
            
                Pattern r = Pattern.compile(pattern);           //Compile le "pattern" en Regex déclaré plus tôt
                Matcher m = r.matcher(Informations);            //Crée un objet de type matcher, qui va permettre de comparer la trame que l'on reçoit avec le pattern

                if (m.find( ))                                  //Regarde si la trame reçue correspond au "pattern"       
                {
                    //Affiche les valeurs trouvés dans les groupes du pattern regex. Chaque groupe correspond à la valeur d'un capteur (ID, T, P, H, R)
                    System.out.println("ID: "          + m.group(1));
                    System.out.println("Température: " + m.group(2));
                    System.out.println("Pression: "    + m.group(3));
                    System.out.println("Humidité: "    + m.group(4));
                    System.out.println("RPM: "         + m.group(5));

                    //Ajoute la date comme sixième argument de la trame json
                    json = "{ \\\"ID\\\":\\\"" + m.group(1) + "\\\", \\\"T\\\":\\\"" + m.group(2) + "\\\", \\\"P\\\":\\\"" + m.group(3) + "\\\", \\\"H\\\":\\\"" + m.group(4) + "\\\", \\\"R\\\":\\\"" + m.group(5) + "\\\", \\\"D\\\":\\\"" + java.time.LocalDateTime.now() + "\\\" }";
                    System.out.println("Trame json crée: " + json);

                    json = "\"" + json + "\"";
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
    Thread m_Thread;
    private Serveur m_Parent;
    int Memoire = 2;

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
            Thread.sleep(5000);                     //Pour laisser le temps au code d'ouvrir
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
                        Thread.sleep(90000);               //Délai de 90 secondes pour laisser le temps au modem d'avoir un signal LTE
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

                Thread.sleep(5000);
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
        File file = new File("/home/pi/ProjetNepal/Data.txt");

        try
        {
            //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
            Thread.sleep(TEMPS_2M);                                                             //<- Avant d'envoyer le premier bloc de données
            //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

            while (true)
            {
                if (m_Parent.ModeDebug == 0 && file.length() != 0)                                  //Les données accumulées sont seulement envoyées si on est pas en mode debug
                {
                    System.out.println("Début de l'envoi du bloc de données");

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
                    Thread.sleep(TEMPS_1M30);              //Délai de 90 secondes pour laisser le temps au modem d'avoir un signal LTE
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

                    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
                    Thread.sleep(TEMPS_1M);               //Délai de 60 secondes pour laisser le temps au modem de se connecter au réseau
                    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

                    //Ce bloc éxécute la commande qui envoie les informations à Hologram            //<- DÉBUT DU BLOC
                    
                    BufferedReader br = new BufferedReader(new FileReader(file));                   //Fichier à partir duquel on lit les informations

                    while ((Donnee = br.readLine()) != null) 
                    {
                        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
                        System.out.println(Donnee + " -> sera envoyé à Hologram");

                        String s2 = "sudo hologram send " + Donnee;    			                    //Commande bash a etre executee
                        String[] sCmd2 = {"/bin/bash", "-c", s2};             			            //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

                        System.out.println(sCmd2[0] + " " + sCmd2[1] + " " + sCmd2[2]);             //Affiche la commande a executer dans la console Java
                        Process p2 = Runtime.getRuntime().exec(sCmd2);        			            //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

                        if (p2.getErrorStream().available() > 0)        					        //Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
                        {
                            //Affiche l'erreur survenue
                            BufferedReader brCommand2 = new BufferedReader(new InputStreamReader(p2.getErrorStream()));
                            System.out.println(brCommand2.readLine());
                            brCommand2.close();
                        }

                        System.out.println(Donnee + " -> à été envoyé à Hologram");
                        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");

                        Thread.sleep(TEMPS_1M);                                                     //1 minute entre chaque donnée
                    }

                    br.close();                                                                     //<- FIN DU BLOC	

                    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-								
                    Thread.sleep(TEMPS_30S);               //Délai de 30 secondes pour laisser le temps d'envoyer la dernière donnée
                    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

                    //Ce bloc permet de de désactiver l'alimentation sur les ports USB                  //<- DÉBUT DU BLOC
                    String s6 = "echo '1-1' |sudo tee /sys/bus/usb/drivers/usb/unbind";    			    //Commande bash a etre executee
                    String[] sCmd6 = {"/bin/bash", "-c", s6};             			                    //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

                    System.out.println(sCmd6[0] + " " + sCmd6[1] + " " + sCmd6[2]);                     //Affiche la commande a executer dans la console Java
                    Process p6 = Runtime.getRuntime().exec(sCmd6);        			                    //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

                    if (p6.getErrorStream().available() > 0)        					                //Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
                    {
                        //Affiche l'erreur survenue
                        BufferedReader brCommand6 = new BufferedReader(new InputStreamReader(p6.getErrorStream()));
                        System.out.println(brCommand6.readLine());
                        brCommand6.close();
                    }                                                                                   //<- FIN DU BLOC		                     

                    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-								
                    Thread.sleep(TEMPS_5S);               //Délai de 5 secondes avant de supprimer les données
                    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

                    //Ce bloc permet de supprimer les données après qu'elles ont été envoyées           //<- DÉBUT DU BLOC
                    String s5 = "sudo rm /home/pi/ProjetNepal/Data.txt";    			                //Commande bash a etre executee
                    String[] sCmd5 = {"/bin/bash", "-c", s5};             			                    //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

                    System.out.println(sCmd5[0] + " " + sCmd5[1] + " " + sCmd5[2]);                     //Affiche la commande a executer dans la console Java
                    Process p5 = Runtime.getRuntime().exec(sCmd5);        			                    //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

                    if (p5.getErrorStream().available() > 0)        					                //Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
                    {
                        //Affiche l'erreur survenue
                        BufferedReader brCommand5 = new BufferedReader(new InputStreamReader(p5.getErrorStream()));
                        System.out.println(brCommand5.readLine());
                        brCommand5.close();
                    }                                                                                   //<- FIN DU BLOC

                    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-								
                    Thread.sleep(TEMPS_5S);               //Délai de 5 secondes avant de supprimer les données
                    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

                    //Ce bloc permet de supprimer les données après qu'elles ont été envoyées           //<- DÉBUT DU BLOC
                    String s3 = "sudo touch /home/pi/ProjetNepal/Data.txt";    			                //Commande bash a etre executee
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

                    System.out.println("Fin de l'envoi du bloc de données");

                     //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
                    Thread.sleep(TEMPS_6H);                                                             //<- DÉLAI ENTRE CHAQUE ENVOI DE DONNÉES
                    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
                }

                else if (m_Parent.ModeDebug == 1)
                {
                    System.out.println("Envoi de données annulé puisque le mode debug est activé");
                }
            }
        }

        catch (Exception e)
        {
            System.out.println(e.toString());
        }
    }
}