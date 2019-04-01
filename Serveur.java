/**
 * @file   Serveur.java
 * @author Samuel Montminy & Harri Laliberté
 * @date   Février 2019
 * @brief  Code qui permet de recevoir des données envoyées par le client tcp/ip pour ensuite les envoyer sur le dashboard Hologram par LTE
 *         Le code doit être compilé avec /javac Serveur.java et doit être lancé avec /java Serveur
 *
 * @version 1.0 : Première version
 * @version 1.1 : Deuxième version du fichier, enregistre les fichiers dans le système avant de les envoyer une fois par jour pour sauver du courant.
 * Environnement de developpement: GitKraken / Notepad++
 * Compilateur: javac (Java version 1.8)
 * Matériel: Raspberry Pi Zero W, Module LTE Hologram (+ carte SIM)
 */

import java.net.*;
import java.io.*;
import java.util.regex.*;

public class Serveur implements Runnable
{
    final static int NB_OCTETS = 1000;                              //Constante pour le nombre d'octets du tampon memoire du miniserveur
    int m_nPort = 2228;                                             //Numéro du port utilise par le miniserveur (doit être entré comme argument lorsque les codes clients sont lancés)
    ServerSocket m_ssServeur;                                       //Reference vers l'objet ServerSocket
    Thread m_tService;                                              //Reference vers l'objet Thread

    //"Pattern" en Regex qui sert à vérifier si la trame reçue correspond à ce qu'on attend
    String pattern = "^(\\w{2}),(\\d+\\.?(?:\\d+)?),(\\d+\\.?(?:\\d+)?),(\\d+\\.?(?:\\d+)?),(\\d+\\.?(?:\\d+)?)$";

    public EnvoieInformations m_objInformations;                    //Référence du thread qui sert à envoyer les informations
    
    public Serveur()
    {		
        try
        {
            m_ssServeur = new ServerSocket(m_nPort, NB_OCTETS);             //Création du miniserveur au port specifie (m_nPort = 2228)
                                                                            //Le miniserveur a un tampon mémoire de 1000 octets (NB_OCTETS)
            m_tService = new Thread(this);                                  //Creation et démarrage de la tâche d'écoute du miniserveur sur le port 2228
            m_tService.start();

            m_objInformations = new EnvoieInformations(this);               //Démarre le thread qui sert à envoyer les informations
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
				
        while(m_tService != null)
        {
            try
            {
				Informations = "Erreur de lecture du client";                   //Permet de savoir si il y a eu une erreur de lecture du message (la variable va rester inchangée)
                System.out.println("Attente d'une connexion au serveur...");    //Le miniserveur attend une connexion réseau... -> BLOQUANT! <-
                Socket sConnexion = m_ssServeur.accept();

                System.out.println("Connexion au serveur etablie!");            //Ce message est affiché si une connexion avec un client est établie
                
                InputStream isIn = sConnexion.getInputStream();                 //Objet pour la reception des données
                ObjectInputStream oisIn = new ObjectInputStream(isIn);          //Reçoit les données envoyés par le client
                Informations = (String)oisIn.readObject();               		//Lit le contenu des données reçues et les mets dans Informations
                //System.out.println(Informations);
               
                oisIn.close();                                                  //Fermeture des objets de flux de données
                isIn.close();
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
                System.out.println(e.toString());
            }
			
			try
			{
				System.out.println(Informations + " -> à été reçu d'un client");
				
                Pattern r = Pattern.compile(pattern);           //Compile le "pattern" en Regex déclaré plus tôt
                Matcher m = r.matcher(Informations);            //Crée un objet de type matcher, qui va permettre de comparer la trame que l'on reçoit avec le pattern

                if (m.find( ))                  
                {
                    //Affiche les valeurs trouvés dans les groupes du pattern regex. Chaque groupe correspond à la valeur d'un capteur (ID, T, P, H, R)
                    System.out.println("Found value: " + m.group(1));
                    System.out.println("Found value: " + m.group(2));
                    System.out.println("Found value: " + m.group(3));
                    System.out.println("Found value: " + m.group(4));
                    System.out.println("Found value: " + m.group(5));

                    Informations = Informations + "," + java.time.LocalDateTime.now();
                    System.out.println(Informations);
                }
                
                else
                {
                    System.out.println("NO MATCH");
                }

                //MODIFIER INFORMATIONS POUR METTRE LA DATE/HEURE DEDANS
                //ENREGISTRER INFORMATIONS DANS UN FICHIER .TXT
			}

			catch (Exception e)
			{
				System.out.println(e.toString());                               //Affiche l'erreur survenue en Java
			}
        }
    }
    
    public static void main(String[] args)
    {
        new Serveur();                                                    		//Appelle le constructeur de la classe ServeurSocket (pas d'arguments)
    }
}

class EnvoieInformations implements Runnable
{
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
        while (true)
        {
            try
            {
                /*//Ce bloc permet de d'activer l'alimentation sur les ports USB                  //<- DÉBUT DU BLOC
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
				}

				Thread.sleep(100);      										                //Delai pour laisser le temps au kernel d'agir
                                                                                                //<-FIN DU BLOC
																								
                //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
				Thread.sleep(60000);               //Délai de 30 secondes pour laisser le temps au modem d'avoir un signal LTE
                //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
				
				//Bloc qui sert à faire un test de connection avant d'envoyer des données     	//<- DÉBUT DU BLOC
				System.out.println(Donnee + " -> sera envoyé à Hologram");
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
				}

				Thread.sleep(100);      										                //Delai pour laisser le temps au kernel d'agir
				                                                                                //<- FIN DU BLOC

                //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
                Thread.sleep(30000);               //Délai de 60 secondes pour laisser le temps au modem d'avoir un signal LTE
                //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

				//Ce bloc éxécute la commande qui envoie les informations à Hologram            //<- DÉBUT DU BLOC
				System.out.println(Donnee + " -> sera envoyé à Hologram");
				String s2 = "sudo hologram send " + Donnee;    			                //Commande bash a etre executee
				String[] sCmd2 = {"/bin/bash", "-c", s2};             			                //Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

				System.out.println(sCmd2[0] + " " + sCmd2[1] + " " + sCmd2[2]);                 //Affiche la commande a executer dans la console Java
				Process p2 = Runtime.getRuntime().exec(sCmd2);        			                //Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

				if (p2.getErrorStream().available() > 0)        					            //Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
				{
					//Affiche l'erreur survenue
					BufferedReader brCommand2 = new BufferedReader(new InputStreamReader(p2.getErrorStream()));
					System.out.println(brCommand2.readLine());
					brCommand2.close();
				}

				Thread.sleep(100);      										                //Delai pour laisser le temps au kernel d'agir
				System.out.println(Donnee + " -> à été envoyé à Hologram");               //<- FIN DU BLOC   

                //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-								
				Thread.sleep(30000);               //Délai de 30 secondes pour laisser le temps d'envoyer la donnée
                //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

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
				}

				Thread.sleep(100);      										                    //Delai pour laisser le temps au kernel d'agir
                                                                                                    //<- FIN DU BLOC*/
            }

            catch (Exception e)
            {
                System.out.println(e.toString());
            }
        }
    }
}