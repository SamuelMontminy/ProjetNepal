/**
 * @file   ClientEntrepot.java
 * @author Samuel Montminy
 * @date   Novembre 2018
 * @brief  Code qui permet de lire le capteur BME280 et puis de l'envoyer au serveur par socket tcp/ip
 *
 * @version 1.0 : Premi�re version
 * @version 1.1 : Code pour le Pi dans l'entrepot
 * Environnement de d�veloppement: GitKraken
 * Compilateur: javac (Java version 1.8)
 * Mat�riel: Raspberry Pi Zero W
 */
 
import java.net.*;              						//Importation du package io pour les acc�s aux fichiers
import java.io.*;

public class ClientEntrepot
{
    Socket m_sClient;           						//R�f�rence de l'objet Socket
	
	public LectureCapteur m_objCapteur;
	
	String m_IP;
	int m_Port;
    
    public ClientEntrepot()
    {
    }
  
    //Constructeur de la classe, re�oit l'adresse ip et le port de la fonction main
    public ClientEntrepot(String sIP, int nPort)
    {   
		String Message = "Erreur client";
		
		try
		{	
			m_objCapteur = new LectureCapteur(this);
			
			m_IP = sIP;
			m_Port = nPort;
			
			while (true)
			{
				
			}
		}
		
		catch (Exception e)
		{
			System.out.println(e.toString());
		}
    }
	
	//Envoie le RPM au serveur (Pi 3b)
	public void EnvoyerAuServeur(String sIP, int nPort, String Message)
	{   
        try
        {
			System.out.println(Message + " -> � �t� re�u par la fonction");
						
			///*		Mettre en commentaire le bloc pour ne pas envoyer au serveur
			System.out.println(Message + " -> sera envoy� au serveur");
            m_sClient = new Socket(sIP, nPort);                                     //Objet Socket pour �tablir la connexion au miniserveur
            
            OutputStream osOut = m_sClient.getOutputStream();                       //Requ�te vers le serveur... (flux de donn�es)
            ObjectOutputStream oosOut = new ObjectOutputStream(osOut);
            oosOut.writeObject(Message);

            //Fermeture des objets de flux de donn�es
            oosOut.close();
            osOut.close();
			System.out.println(Message + " -> � �t� envoy� au serveur");
			//*/
        }
        
        catch(UnknownHostException e)
        {
            System.out.println(e.toString());                                       //Nom ou adresse du miniserveur inexistant
        }
        catch(IOException e)
        {
            System.out.println(e.toString());                                       //Probl�me de communication r�seau
        }
        catch(SecurityException e)
        {
            System.out.println(e.toString());                                       //Probl�me de s�curit� (si cela est g�r�...)
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
        
        if (argc == 2)                                                              //L'utilisateur doit avoir entr� deux arguments (IP + Port)
        {
            try
            {
                Integer iArgs = new Integer(args[1]);                               //Conversion du 2e param�tre en entier
                
                Client obj = new Client(args[0], iArgs.intValue());     			//Connexion au serveur s'il existe...
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

//Thread qui permet de calculer la vitesse de rotation en utilisant le temps entre chaque front montant
class LectureCapteur implements Runnable
{
	String Message = "Erreur Lecture Capteur";
	Thread m_Thread;
    private Client m_Parent;				//R�f�rence vers la classe principale (Client)
		
	public LectureCapteur(Client Parent)		//Constructeur
	{
		try
		{
			m_Parent = Parent;
			
			m_Thread = new Thread(this);	//Cr�e le thread
			m_Thread.start();				//D�marre le thread
		}
		
		catch(Exception e)
		{
			System.out.println(e.toString());
		}
	}
	
	public void run()						//Thread qui roule en parall�le de la classe principale
	{
		while (true)
		{
			try
			{
				//LECTURE DU CAPTEUR ET METTRE LE R�SULTAT DANS MESSAGE
				m_Parent.EnvoyerAuServeur(m_Parent.m_IP, m_Parent.m_Port, Message);	//Envoie l'information (RPM) � la fonction qui va l'envoyer au serveur
			}
			
			catch(Exception e)
			{
				System.out.println(e.toString());
			}
		}
	}
}