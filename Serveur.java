/**
 * @file   Serveur.java
 * @author Samuel Montminy
 * @date   Novembre 2018
 * @brief  Code qui permet de recevoir des donn?es envoy?es par le client tcp/ip pour ensuite les envoyer sur le dashboard Hologram par LTE
 *
 * @version 1.0 : Premiere version
 * Environnement de developpement: GitKraken / Notepad++
 * Compilateur: javac (Java version 1.8)
 * Mat?riel: Raspberry Pi Zero W, Module LTE Hologram (+ carte SIM)
 */

import java.net.*;
import java.io.*;

public class Serveur implements Runnable
{
    final static int NB_OCTETS = 1000;                              //Constante pour le nombre d'octets du tampon memoire du miniserveur
    int m_nPort = 2228;                                             //Numero du port utilise par le miniserveur
    ServerSocket m_ssServeur;                                       //Reference vers l'objet ServerSocket
    Thread m_tService;                                              //Reference vers l'objet Thread
    
    public Serveur()
    {		
        try
        {
            m_ssServeur = new ServerSocket(m_nPort, NB_OCTETS);             //Creation du miniserveur au port specifie (m_nPort = 2228)
                                                                            //Le miniserveur a un tampon memoire de 1000 octets (NB_OCTETS)
            m_tService = new Thread(this);                                  //Creation et demarrage de la tache d'ecoute du miniserveur sur le port 2228
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
    
    // Tache d'ecoute sur le port 2228
    public void run()
    {   
		boolean bError = true;  									//Pour gestion des erreurs
		String Informations = "Erreur de lecture du client";
				
        while(m_tService != null)
        {
            try
            {
				Informations = "Erreur de lecture du client";
                System.out.println("Attente d'une connexion au serveur...");    //Le miniserveur attend une connexion r?seau... -> BLOQUANT! <-
                Socket sConnexion = m_ssServeur.accept();

                System.out.println("Connexion au serveur etablie!");            //Ce message est affiche si une connexion est etablie
                
                InputStream isIn = sConnexion.getInputStream();                 //Objet pour la reception des donn?es
                ObjectInputStream oisIn = new ObjectInputStream(isIn);          //Recoit les donnees envoyes par le client
                Informations = (String)oisIn.readObject();               		//Lit le contenu des donnees recues
                //System.out.println(Informations);
               
                oisIn.close();                                                  //Fermeture des objets de flux de donnees
                isIn.close();
            }
            
            catch(IOException e)
            {
                System.out.println(e.toString());                               //Probleme de communication reseau
            }
            catch(ClassNotFoundException e)
            {
                System.out.println(e.toString());                               //Objet indefinie pour la serialisation...
            }
            catch(Exception e)
            {
                System.out.println(e.toString());
            }
			
			try
			{
				System.out.println(Informations + " -> à été reçu d'un client");
				
				/* 		//Mettre en commentaire le bloc pour ne pas envoyer à Hologram
				System.out.println(Informations + " -> sera envoyé à Hologram");
				String s = "sudo hologram send " + Informations;    			//Commande bash a etre executee
				String[] sCmd = {"/bin/bash", "-c", s};             			//Specifie que l'interpreteur de commandes est BASH. Le "-c"
																				//indique que la commande a executer suit

				System.out.println(sCmd[0] + " " + sCmd[1] + " " + sCmd[2]);    //Affiche la commande a executer dans la console Java
				Process p = Runtime.getRuntime().exec(sCmd);        			//Execute la commande par le systeme Linux (le programme Java 
																				//doit etre demarre par le root pour les acces aux GPIO)

				if (p.getErrorStream().available() > 0)        					//Verification s'il y a une erreur d'execution par l'interpreteur de commandes BASH
				{
					//Affiche l'erreur survenue
					bError = false;
					BufferedReader brCommand = new BufferedReader(new InputStreamReader(p.getErrorStream()));
					System.out.println(brCommand.readLine());
					brCommand.close();
				}

				Thread.sleep(100);      										//Delai pour laisser le temps au kernel d'agir
				System.out.println(Informations + " -> à été envoyé à Hologram");
				*/
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