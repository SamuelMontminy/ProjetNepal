/**
 * @file   Client.java
 * @author Samuel Montminy (Fonctions de IO faites par Pierre Bergeron)
 * @date   Novembre 2018
 * @brief  Code qui permet de lire la vitesse de rotation du gpio et puis de l'envoyer au serveur par socket tcp/ip
 *
 * @version 1.0 : Première version
 * Environnement de développement: GitKraken
 * Compilateur: javac (Java version 1.8)
 * Matériel: Raspberry Pi Zero W
 */
 
import java.time.Duration;
import java.time.Instant;
import java.net.*;              						//Importation du package io pour les accès aux fichiers
import java.io.*;

public class Client
{
    Socket m_sClient;           						//Référence de l'objet Socket
	
	public Shutdown m_objShutdown;
	public CalculeRPM m_objCalculeRPM;
	
	public static final String GPIO_IN = "in";        	//Pour configurer la direction de la broche GPIO   
	public static final String NUMBER_GPIO = "3";   	//ID du GPIO de le Raspberry Pi avec le capteur Reed switch
	public static final String NAME_GPIO = "gpio3";     //Nom du GPIO pour le Raspberry Pi
	
	String m_IP;
	int m_Port;
    
    public Client()
    {
    }
  
    //Constructeur de la classe, reçoit l'adresse ip et le port de la fonction main
    public Client(String sIP, int nPort)
    {   
		String Message = "Erreur client";
		
		try
		{
			gpioUnexport(NUMBER_GPIO);          		//Déffectation du GPIO #3 (au cas ou ce GPIO est déjè défini par un autre programme)
			gpioExport(NUMBER_GPIO);            		//Affectation du GPIO #3
			gpioSetdir(NAME_GPIO, GPIO_IN);   			//Place GPIO #3 en entrée
			
			m_objShutdown = new Shutdown(this);
			m_objCalculeRPM = new CalculeRPM(this);
			
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
			System.out.println(Message + " -> à été reçu par la fonction");
						
			///*		Mettre en commentaire le bloc pour ne pas envoyer au serveur
			System.out.println(Message + " -> sera envoyé au serveur");
            m_sClient = new Socket(sIP, nPort);                                     //Objet Socket pour établir la connexion au miniserveur
            
            OutputStream osOut = m_sClient.getOutputStream();                       //Requête vers le serveur... (flux de données)
            ObjectOutputStream oosOut = new ObjectOutputStream(osOut);
            oosOut.writeObject(Message);

            //Fermeture des objets de flux de données
            oosOut.close();
            osOut.close();
			System.out.println(Message + " -> à été envoyé au serveur");
			//*/
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
	
	public void ResetCountdown()													//Remet le compteur d'inactivité à sa valeur par défaut (300 secondes)
	{
		m_objShutdown.ResetCountdown();
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
			
			Thread.sleep(20);   												//Délai pour laisser le temps au kernel d'agir
        }
		
        catch(Exception e)      												//Traitement de l'erreur par la VM Java (différent de l'erreur par l'interpreteur BASH)
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

//Thread qui permet de calculer la vitesse de rotation en utilisant le temps entre chaque front montant
class CalculeRPM implements Runnable
{
	long MilliSecondes;
	long RPM;
	
	Duration duree;
	Instant start;
	Instant end ;
	
	Thread m_Thread;
    private Client m_Parent;				//Référence vers la classe principale (Client)
		
	public CalculeRPM(Client Parent)		//Constructeur
	{
		try
		{
			m_Parent = Parent;
			
			m_Thread = new Thread(this);	//Crée le thread
			m_Thread.start();				//Démarre le thread
		}
		
		catch(Exception e)
		{
			System.out.println(e.toString());
		}
	}
	
	public void run()						//Thread qui roule en parallèle de la classe principale
	{
		while (true)
		{
			try
			{
				while (m_Parent.gpioReadBit(m_Parent.NAME_GPIO) == 1)
				{
					
				}	//Détecte un front montant
				
				start = Instant.now();
				
				while (m_Parent.gpioReadBit(m_Parent.NAME_GPIO) == 0)
				{
					
				}	//Front descendant
				
				while (m_Parent.gpioReadBit(m_Parent.NAME_GPIO) == 1)
				{
					
				}	//Front montant
				
				end = Instant.now();
				
				duree = Duration.between(start, end);
				MilliSecondes = duree.toMillis();
				RPM = 60000 / MilliSecondes;													//Convertit le temps en millisecondes en RPM
				System.out.println("Tour en: " + String.valueOf(MilliSecondes) + "ms, RPM: " + String.valueOf(RPM));
				
				m_Parent.EnvoyerAuServeur(m_Parent.m_IP, m_Parent.m_Port, String.valueOf("RPM:" + RPM));	//Envoie l'information (RPM) à la fonction qui va l'envoyer au serveur
				m_Parent.ResetCountdown();														//Réinitialise le compteur d'inactivité
			}
			
			catch(Exception e)
			{
				System.out.println(e.toString());
			}
		}
	}
}

//Thread qui permet d'éteindre le Pi Zero après deux minutes d'inactivité (pas de front montants détectés) pour conserver la batterie
class Shutdown implements Runnable
{
	boolean EnVie;
	Thread m_Thread;
    private Client m_Parent;				//Référence vers la classe principale (Client)
	
	int m_Countdown;
	
	public Shutdown(Client Parent)			//Constructeur
	{
		try
		{
			m_Parent = Parent;
			
			m_Thread = new Thread(this);	//Crée le thread
			m_Thread.start();				//Démarre le thread
			
			m_Countdown = 120;				//Après trois minutes d'inactivité, le pi s'éteint
			EnVie = true;
		}
		
		catch(Exception e)
		{
			System.out.println(e.toString());
		}
	}
	
	public void ResetCountdown()			//Permet de rénitialiser la valeur du compteur d'inactivité
	{
		m_Countdown = 120;
	}
	
	public void run()						//Thread qui roule en parallèle de la classe principale
	{
		while (true)
		{
			try
			{
				if (m_Countdown <= 0 && EnVie == true)										//Si aucun front montant n'à été détecté dans les deux dernières minutes
				{
					EnVie = false;
					String sCommande = "sudo shutdown now";  								//Commande bash à être exécutée
					String[] sCmd = {"/bin/bash", "-c", sCommande};                       	//Spécifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande à exécuter suit
																							
					System.out.println(sCmd[0] + " " + sCmd[1] + " " + sCmd[2]);            //Affiche la commande à exécuter dans la console Java
					Process p = Runtime.getRuntime().exec(sCmd);                            //Exécute la commande par le système Linux (le programme Java
																							//doit être démarré par le root pour les accès aux GPIO)
		 
					if(p.getErrorStream().available() > 0)                                  //Vérification s'il y a une erreur d'exécution par l'interpreteur de commandes BASH
					{
						BufferedReader brCommand = new BufferedReader(new InputStreamReader(p.getErrorStream()));
						System.out.println(brCommand.readLine());
						brCommand.close();
					}
				}
				
				else																		//Décrémente la valeur du compteur d'inactivité
				{
					m_Countdown--;
					Thread.sleep(1000);
					System.out.println("Countdown: " + String.valueOf(m_Countdown));
				}
			}
			
			catch(Exception e)
			{
				System.out.println(e.toString());
			}
		}
	}
}
