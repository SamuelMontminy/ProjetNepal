/**
 * @file   Serveur.java
 * @author Samuel Montminy
 * @date   Novembre 2018
 * @brief  Code qui permet de recevoir des données envoyées par le client tcp/ip pour ensuite les envoyer sur le dashboard Hologram par LTE
 *
 * @version 1.0 : Première version
 * Environnement de développement: GitKraken / Notepad++
 * Compilateur: javac (Java version 1.8)
 * Matériel: Raspberry Pi Zero W, Module LTE Hologram (+ carte SIM)
 */

import java.net.*;
import java.io.*;

public class Serveur implements Runnable
{
    final static int NB_OCTETS = 1000;                              //Constante pour le nombre d'octets du tampon mémoire du mini­serveur
    int m_nPort = 2228;                                             //Numéro du port utilisé par le mini­serveur
    ServerSocket m_ssServeur;                                       //Référence vers l'objet ServerSocket
    Thread m_tService;                                              //Référence vers l'objet Thread
    
    public Serveur()
    {		
        try
        {
            m_ssServeur = new ServerSocket(m_nPort, NB_OCTETS);             //Création du mini­serveur au port spécifié (m_nPort = 2228)
                                                                            //Le miniserveur a un tampon mémoire de 1000 octets (NB_OCTETS)
            m_tService = new Thread(this);                                  //Création et démarrage de la tâche d'écoute du miniserveur sur le port 2228
            m_tService.start();
        }
        
        catch(IOException e)
        {
            System.out.println(e.toString());
        }
        catch(Exception e)
        {
            System.out.println(e.toString());
        }
    }
    
    // Tâche d'écoute sur le port 2228
    public void run()
    {   
		boolean bError = true;  									//Pour gestion des erreurs
		String Informations = "Erreur de lecture du client";
				
        while(m_tService != null)
        {
            try
            {
				Informations = "Erreur de lecture du client";
                System.out.println("Attente d'une connexion au serveur...");    //Le mini­serveur attend une connexion réseau... -> BLOQUANT! <-
                Socket sConnexion = m_ssServeur.accept();

                System.out.println("Connexion au serveur établie!");            //Ce message est affiché si une connexion est établie
                
                InputStream isIn = sConnexion.getInputStream();                 //Objet pour la réception des données
                ObjectInputStream oisIn = new ObjectInputStream(isIn);          //Reçoit les données envoyés par le client
                Informations = (String)oisIn.readObject();               		//Lit le contenu des données reçues
                System.out.println(Informations);
               
                oisIn.close();                                                  //Fermeture des objets de flux de données
                isIn.close();
            }
            
            catch(IOException e)
            {
                System.out.println(e.toString());                               //Problème de communication réseau
            }
            catch(ClassNotFoundException e)
            {
                System.out.println(e.toString());                               //Objet indéfinie pour la sérialisation...
            }
            catch(Exception e)
            {
                System.out.println(e.toString());
            }
			
			try
			{
				String s = "sudo hologram send " + Informations;    			//Commande bash à être exécutée
				String[] sCmd = {"/bin/bash", "-c", s};             			//Spécifie que l'interpréteur de commandes est BASH. Le "-c"
																				//indique que la commande à exécuter suit

				System.out.println(sCmd[0] + " " + sCmd[1] + " " + sCmd[2]);    //Affiche la commande à exécuter dans la console Java
				Process p = Runtime.getRuntime().exec(sCmd);        			//Exécute la commande par le système Linux (le programme Java 
																				//doit être démarré par le root pour les accès aux GPIO)

				if (p.getErrorStream().available() > 0)        					//Vérification s'il y a une erreur d'exécution par l'interpréteur de commandes BASH
				{
					//Affiche l'erreur survenue
					bError = false;
					BufferedReader brCommand = new BufferedReader(new InputStreamReader(p.getErrorStream()));
					System.out.println(brCommand.readLine());
					brCommand.close();
				}

				Thread.sleep(100);      										//Délai pour laisser le temps au kernel d'agir
			}

			catch (Exception e)
			{
				//Affiche l'erreur survenue en Java
				bError = false;
				System.out.println(e.toString());
			}
        }
    }
    
    public static void main(String[] args)
    {
        new Serveur();                                                    		//Appelle le constructeur de la classe ServeurSocket (pas d'arguments)
    }
}