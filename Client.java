/**
 * @file   Client.java
 * @author Samuel Montminy
 * @date   Novembre 2018
 * @brief  Code qui permet de lire la vitesse de rotation du gpio et puis de l'envoyer au serveur par socket tcp/ip
 *
 * @version 1.0 : Première version
 * Environnement de développement: GitKraken
 * Compilateur: javac (Java version 1.8)
 * Matériel: Raspberry Pi Zero W
 */

import java.net.*;              						//Importation du package io pour les accès aux fichiers
import java.io.*;

public class Client
{
    Socket m_sClient;           						//Référence de l'objet Socket
	
	public static final String GPIO_IN = "in";        	//Pour configurer la direction de la broche GPIO   
	public static final String NUMBER_GPIO = "3";   	//ID du GPIO de le Raspberry Pi avec le capteur Reed switch
	public static final String NAME_GPIO = "gpio3";     //Nom du GPIO pour le Raspberry Pi
    
    public Client()
    {
    }
  
    //Constructeur de la classe, reçoit l'adresse ip et le port de la fonction main
    public Client(String sIP, int nPort)
    {   
		String Message = "Erreur client";
		int vitesseRotation = 0;
		
		try
		{
			gpioUnexport(NUMBER_GPIO);          		//Déffectation du GPIO #3 (au cas ou ce GPIO est déjè défini par un autre programme)
			gpioExport(NUMBER_GPIO);            		//Affectation du GPIO #3
			gpioSetdir(NAME_GPIO, GPIO_IN);   			//Place GPIO #3 en entrée
			
			while (true)
			{
				vitesseRotation = LectureReed();
				Message = Integer.toString(vitesseRotation);
				EnvoyerAuServeur(sIP, nPort, Message);
			}
		}
		
		catch (Exception e)
		{
			System.out.println(e.toString());
		}
    }
	
	public int LectureReed()
	{
		int vitesseRotation = 0;
		int nbFronts = 0;
		int etatCapteur = 1;
		int etatCapteur_c = 1;
			
		try
		{	
		   for (int i = 0; i < 1000; i++)
			{
				etatCapteur = gpioReadBit(NAME_GPIO);   //Lecture du gpio

				if (etatCapteur != etatCapteur_c)       //Il y a eu un changement de front
				{
					nbFronts++;
					etatCapteur_c = etatCapteur;
				}

				Thread.sleep(10);                   	//Délai de 10ms répété 1000 fois, donc 10 secondes
			}

			vitesseRotation = (nbFronts * 6) / 2;       //Le fois 6 est parce que nbFronts correspond au nombre de fronts montants sur 10 secondes, et nous le voulons sur 1 minute. 
														//Le /2 est parce que la variable nbFronts détecte les fronts montants et descendents, alors que nous voulons uniquement les fronts montant.
		}
		
		catch (Exception e)
		{
			System.out.println(e.toString());
		}
		
		return vitesseRotation;
	}
	
	public void EnvoyerAuServeur(String sIP, int nPort, String Message)
	{
        boolean Modification = false;
        BufferedReader brLectureClavier;                                            //Objet pour la lecture du clavier
 
        brLectureClavier = new BufferedReader(new InputStreamReader(System.in));    //Sélection de la source de données pour la lecture du clavier
        
        try
        {
            m_sClient = new Socket(sIP, nPort);                                     //Objet Socket pour établir la connexion au miniserveur
            
            OutputStream osOut = m_sClient.getOutputStream();                       //Requête vers le serveur... (flux de données)
            ObjectOutputStream oosOut = new ObjectOutputStream(osOut);
            oosOut.writeObject(Message);

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
	
	//Pour lire l'état du GPIO
    public Integer gpioReadBit(String name_gpio)
    {
        String sLecture;

        try
        {
            FileInputStream fis = new FileInputStream("/sys/class/gpio/" + name_gpio + "/value");           //Sélection de la destination du flux de
                                                                                                            //données (sélection du fichier d'entrée)
                                                                                                            
            DataInputStream dis = new DataInputStream(fis);                                                 //Canal vers le fichier (entrée en "streaming")
            sLecture = dis.readLine();                                                                      //Lecture du fichier                
                                                                                                            
            //System.out.println("/sys/class/gpio/" + name_gpio + "/value = " + sLecture);                    //Affiche l'action réalisée dans la console Java
            dis.close();                                                                                    //Fermeture du canal
            fis.close();                                                                                    //Fermeture du flux de données
        }
		
        catch(Exception e)
        {
            // Affiche l'erreur survenue en Java
            sLecture = "-1";
            System.out.println("Error on gpio readbits" + name_gpio + " :");
            System.out.println(e.toString());
        }
		
        return new Integer(sLecture);  																		//Retourne l'état "supposé" de la sortie
    }
	
	//Pour désaffecter le GPIO par kernel
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
			
			Thread.sleep(20);   						//Délai pour laisser le temps au kernel d'agir
        }
		
        catch(Exception e)      						//Traitement de l'erreur par la VM Java (différent de l'erreur par l'interpreteur BASH)
        {
														//Affiche l'erreur survenue en Java
            bError = false;
            System.out.println("Error on export GPIO :" + gpioid);
            System.out.println(e.toString());
        }
		
        return  bError;
    }
	
	//Pour affecter le GPIO par kernel
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
		 
		catch(Exception e)         												//Traitement de l'erreur par la VM Java (différent de l'erreur par l'interpreteur BASH)
		{
			//Affiche l'erreur survenue en Java
			bError = false;
			System.out.println("Error on export GPIO :" + gpioid);
			System.out.println(e.toString());
		}
		 
        return bError;
    }  
	
	// Configure la direction du GPIO
    //
    // name_gpio : nom associé au répertoire créé par le kernel (gpio +  no i/o du port : Ex: GPIO 2 ==> pgio2)
    // sMode : Configuration de la direction du GPIO("out" ou "in")
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
		
	    catch(Exception e)
	    {
			//Affiche l'erreur survenue en Java
			bError = false;
			System.out.println("Error on direction setup :");
			System.out.println(e.toString());
	    }
		
		return bError;
    }  
}
