/**
 * @file   ClientSocket.java
 * @author Pierre Bergeron (Modifié par Samuel Montminy)
 * @date   9 nov 2018
 * @brief  Code qui permet d'envoyer une donnée quelquonque au serveur par socket tcp/ip
 *
 * @version 1.0 : Première version
 * Environnement de développement: GitKraken
 * Compilateur: javac (Java version 1.8)
 * Matériel: Raspberry Pi Zero W
 */

import java.net.*;              //Importation du package io pour les accès aux fichiers
import java.io.*;

public class ClientSocket
{
    Socket m_sClient;           //Référence de l'objet Socket
    
    public ClientSocket()
    {
    }
  
    //Constructeur de la classe, reçoit l'adresse ip et le port de la fonction main
    public ClientSocket(String sIP, int nPort)
    {   
        String sLectureClavier = "";
        boolean Modification = false;
        BufferedReader brLectureClavier;                                            //Objet pour la lecture du clavier
 
        brLectureClavier = new BufferedReader(new InputStreamReader(System.in));    //Sélection de la source de données pour la lecture du clavier
        
        try
        {
            m_sClient = new Socket(sIP, nPort);                                     //Objet Socket pour établir la connexion au miniserveur
			
			System.out.println("Entrez un numero");
			sLectureClavier = brLectureClavier.readLine();                          //Lecture du clavier
            
            OutputStream osOut = m_sClient.getOutputStream();                       //Requête vers le serveur... (flux de données)
            ObjectOutputStream oosOut = new ObjectOutputStream(osOut);
            oosOut.writeObject(sLectureClavier);

            //Fermeture des objets de flux de données
            oosOut.close();
            osOut.close();
        }
        
        catch(UnknownHostException e)
        {
            System.out.println(e.toString());                                       //Nom ou adresse du miniserveur inexistant
        }
        catch(IOException e)
        {
            System.out.println(e.toString());                                       //Problème de communication réseau
        }
        catch(SecurityException e)
        {
            System.out.println(e.toString());                                       //Problème de sécurité (si cela est géré...)
        }
        catch(Exception e)                          
        {
            System.out.println(e.toString());                                       //Autre erreur...
        }
    }

    public static void main(String[] args)
    {
        int argc = 0;
        
        for (String argument : args)                                                //Compte le nombre d'arguments dans la ligne de commande
        {
            argc++;
        }
        
        if (argc == 2)                                                              //L'utilisateur doit avoir entré deux arguments (IP + Port)
        {
            try
            {
                Integer iArgs = new Integer(args[1]);                               //Conversion du 2e paramètre en entier
                
                ClientSocket obj = new ClientSocket(args[0], iArgs.intValue());     //Connexion au serveur s'il existe...
            }
            
            catch(NumberFormatException e)
            {
                System.out.println(e.toString());
            }
        }
        
        else
        {
            System.out.println("Nombre d'arguments incorrect (IP + Port)");
        }
    }
}
