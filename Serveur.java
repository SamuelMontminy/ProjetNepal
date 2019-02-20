/**
 * @file   Serveur.java
 * @author Samuel Montminy
 * @date   Janvier 2019
 * @brief  Code qui permet de recevoir des données envoyées par le client tcp/ip pour ensuite les envoyer sur le dashboard Hologram par LTE
 *         Le code doit être compilé avec /javac Serveur.java et doit être lancé avec /java Serveur
 *
 * @version 1.0 : Premiere version
 * Environnement de developpement: GitKraken / Notepad++
 * Compilateur: javac (Java version 1.8)
 * Matériel: Raspberry Pi Zero W, Module LTE Hologram (+ carte SIM)
 */

import java.net.*;
import java.io.*;

public class Serveur implements Runnable
{
    final static int NB_OCTETS = 1000;                              //Constante pour le nombre d'octets du tampon memoire du miniserveur
    int m_nPort = 2228;                                             //Numéro du port utilise par le miniserveur (doit être entré comme argument lorsque les codes clients sont lancés)
    ServerSocket m_ssServeur;                                       //Reference vers l'objet ServerSocket
    Thread m_tService;                                              //Reference vers l'objet Thread
    
    public Serveur()
    {		
        try
        {
            m_ssServeur = new ServerSocket(m_nPort, NB_OCTETS);             //Création du miniserveur au port specifie (m_nPort = 2228)
                                                                            //Le miniserveur a un tampon mémoire de 1000 octets (NB_OCTETS)
            m_tService = new Thread(this);                                  //Creation et démarrage de la tâche d'écoute du miniserveur sur le port 2228
            m_tService.start();
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
				
				//Mettre en commentaire le bloc pour ne pas envoyer à Hologram      <- DÉBUT DU BLOC
				System.out.println(Informations + " -> sera envoyé à Hologram");
				String s = "sudo hologram send " + Informations;    			//Commande bash a etre executee
				String[] sCmd = {"/bin/bash", "-c", s};             			//Specifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande a executer suit

				System.out.println(sCmd[0] + " " + sCmd[1] + " " + sCmd[2]);    //Affiche la commande a executer dans la console Java
				Process p = Runtime.getRuntime().exec(sCmd);        			//Execute la commande par le systeme Linux (le programme Java doit etre demarré par le root pour les acces aux GPIO)

				if (p.getErrorStream().available() > 0)        					//Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
				{
					//Affiche l'erreur survenue
					BufferedReader brCommand = new BufferedReader(new InputStreamReader(p.getErrorStream()));
					System.out.println(brCommand.readLine());
					brCommand.close();
				}

				Thread.sleep(100);      										//Delai pour laisser le temps au kernel d'agir
				System.out.println(Informations + " -> à été envoyé à Hologram");
				//<- FIN DU BLOC
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