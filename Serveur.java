/**
 * @file   Serveur.java
 * @author Samuel Montminy
 * @date   Novembre 2018
 * @brief  Code qui permet de recevoir des donn�es envoy�es par le client tcp/ip pour ensuite les envoyer sur le dashboard Hologram par LTE
 *
 * @version 1.0 : Premi�re version
 * Environnement de d�veloppement: GitKraken / Notepad++
 * Compilateur: javac (Java version 1.8)
 * Mat�riel: Raspberry Pi Zero W, Module LTE Hologram (+ carte SIM)
 */

import java.net.*;
import java.io.*;

public class Serveur implements Runnable
{
    final static int NB_OCTETS = 1000;                              //Constante pour le nombre d'octets du tampon m�moire du mini�serveur
    int m_nPort = 2228;                                             //Num�ro du port utilis� par le mini�serveur
    ServerSocket m_ssServeur;                                       //R�f�rence vers l'objet ServerSocket
    Thread m_tService;                                              //R�f�rence vers l'objet Thread
    
    public Serveur()
    {		
        try
        {
            m_ssServeur = new ServerSocket(m_nPort, NB_OCTETS);             //Cr�ation du mini�serveur au port sp�cifi� (m_nPort = 2228)
                                                                            //Le miniserveur a un tampon m�moire de 1000 octets (NB_OCTETS)
            m_tService = new Thread(this);                                  //Cr�ation et d�marrage de la t�che d'�coute du miniserveur sur le port 2228
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
    
    // T�che d'�coute sur le port 2228
    public void run()
    {   
		boolean bError = true;  									//Pour gestion des erreurs
		String Informations = "Erreur de lecture du client";
				
        while(m_tService != null)
        {
            try
            {
				Informations = "Erreur de lecture du client";
                System.out.println("Attente d'une connexion au serveur...");    //Le mini�serveur attend une connexion r�seau... -> BLOQUANT! <-
                Socket sConnexion = m_ssServeur.accept();

                System.out.println("Connexion au serveur �tablie!");            //Ce message est affich� si une connexion est �tablie
                
                InputStream isIn = sConnexion.getInputStream();                 //Objet pour la r�ception des donn�es
                ObjectInputStream oisIn = new ObjectInputStream(isIn);          //Re�oit les donn�es envoy�s par le client
                Informations = (String)oisIn.readObject();               		//Lit le contenu des donn�es re�ues
                System.out.println(Informations);
               
                oisIn.close();                                                  //Fermeture des objets de flux de donn�es
                isIn.close();
            }
            
            catch(IOException e)
            {
                System.out.println(e.toString());                               //Probl�me de communication r�seau
            }
            catch(ClassNotFoundException e)
            {
                System.out.println(e.toString());                               //Objet ind�finie pour la s�rialisation...
            }
            catch(Exception e)
            {
                System.out.println(e.toString());
            }
			
			try
			{
				String s = "sudo hologram send " + Informations;    			//Commande bash � �tre ex�cut�e
				String[] sCmd = {"/bin/bash", "-c", s};             			//Sp�cifie que l'interpr�teur de commandes est BASH. Le "-c"
																				//indique que la commande � ex�cuter suit

				System.out.println(sCmd[0] + " " + sCmd[1] + " " + sCmd[2]);    //Affiche la commande � ex�cuter dans la console Java
				Process p = Runtime.getRuntime().exec(sCmd);        			//Ex�cute la commande par le syst�me Linux (le programme Java 
																				//doit �tre d�marr� par le root pour les acc�s aux GPIO)

				if (p.getErrorStream().available() > 0)        					//V�rification s'il y a une erreur d'ex�cution par l'interpr�teur de commandes BASH
				{
					//Affiche l'erreur survenue
					bError = false;
					BufferedReader brCommand = new BufferedReader(new InputStreamReader(p.getErrorStream()));
					System.out.println(brCommand.readLine());
					brCommand.close();
				}

				Thread.sleep(100);      										//D�lai pour laisser le temps au kernel d'agir
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