/**
 * @file   ServeurSocket.java
 * @author Pierre Bergeron (Modifié par Samuel Montminy)
 * @date   9 nov 2018
 * @brief  Code qui permet de recevoir des données envoyées par le client tcp/ip et les afficher dans la fenêtre de commandes
 *
 * @version 1.0 : Première version
 * Environnement de développement: GitKraken
 * Compilateur: javac (Java version 1.8)
 * Matériel: Raspberry Pi Zero W
 */

import java.net.*;
import java.io.*;

public class ServeurSocket implements Runnable
{
    final static int NB_OCTETS = 1000;                              //Constante pour le nombre d'octets du tampon mémoire du mini­serveur
    int m_nPort = 2228;                                             //Numéro du port utilisé par le mini­serveur
    ServerSocket m_ssServeur;                                       //Référence vers l'objet ServerSocket
    Thread m_tService;                                              //Référence vers l'objet Thread
    
    public ServeurSocket()
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
        while(m_tService != null)
        {
            try
            {
                System.out.println("Attente d'une connexion au serveur...");    //Le mini­serveur attend une connexion réseau... -> BLOQUANT! <-
                Socket sConnexion = m_ssServeur.accept();

                System.out.println("Connexion au serveur établie!");            //Ce message est affiché si une connexion est établie
                
                InputStream isIn = sConnexion.getInputStream();                 //Objet pour la réception des données
                ObjectInputStream oisIn = new ObjectInputStream(isIn);          //Reçoit les données envoyés par le client
                String Info = (String)oisIn.readObject();                       //Lit le contenu des données reçues
                System.out.println(Info);
               
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
        }
    }
    
    public static void main(String[] args)
    {
        new ServeurSocket();                                                    //Appelle le constructeur de la classe ServeurSocket (pas d'arguments)
    }
}
