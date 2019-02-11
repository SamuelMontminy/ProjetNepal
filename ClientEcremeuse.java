/**
 * @file   ClientEcremeuse.java
 * @author Samuel Montminy (Fonctions de IO faites par Pierre Bergeron)
 * @date   Janvier 2019
 * @brief  Code qui permet de déterminer la vitesse de rotation de l'écrémeuse en détectant les fronts montants sur un gpio.
 *         Le temps entre chaque front montant est ensuite converti en RPM puis est envoyé au serveur par socket tcp/ip.
 *		   Ce code incorpore aussi un thread qui permet de lire une sonde DS18B20 par protocole OneWire pour avoir la température du lait dans l'écrémeuse.
 *		   De plus, il y a un thread qui permet d'éteindre le Pi pour économiser de l'énergie lorsque aucun front montant n'est détecté pendant un certain nombre de temps.
 *		   Le code doit être compilé avec /pi4j -c ClientEcremeuse.java et doit être lancé avec /pi4j -r ClientEcremeuse 192.168.4.1 (Adresse IP du serveur) 2228 (Port de communication avec le serveur)
 *
 * @version 1.0 : Première version
 * @version 1.1 : Distinction entre les codes clients. Ce code sera seulement utilisé par l'écrémeuse (RPM & DS18B20)
 * Environnement de développement: GitKraken
 * Compilateur: javac (Java version 1.8)
 * Matériel: Raspberry Pi Zero W
 */
 
import java.time.Duration;								//Pour calculer le temps entre chaque fronts montants
import java.time.Instant;
import java.net.*;              						//Importation du package io pour les accès aux fichiers
import java.io.*;

//Librairies pour la lecture du capteur en I2C avec Pi4J
import com.pi4j.component.temperature.TemperatureSensor;
import com.pi4j.io.w1.W1Master;
import com.pi4j.temperature.TemperatureScale;

public class ClientEcremeuse
{
    Socket m_sClient;           						//Référence de l'objet Socket
	
	public Shutdown m_objShutdown;						//Objet pour la classe qui éteint le Pi après un délai d'inactivité
	public CalculeRPM m_objCalculeRPM;					//Objet pour la classe qui calcule le RPM avec la reed switch branchée sur GPIO 3 et GND
	public LectureCapteur m_objLectureCapteur;			//Objet pour la classe de lecture du capteur de température (DS18B20)
	
	public static final String GPIO_IN = "in";        	//Pour configurer la direction de la broche GPIO   
	public static final String NUMBER_GPIO = "3";   	//ID du GPIO de le Raspberry Pi avec le capteur Reed switch, gpio 3 parce que c'est la pin "reset" du Pi
														//qui lui permet de sortir du mode veille lorsqu'elle devient à un niveau haut
	public static final String NAME_GPIO = "gpio3";     //Nom du GPIO pour le kernel Raspbian
	
	String m_IP;										//Adresse du serveur
	int m_Port;											//Port de communication avec le serveur
    
    public ClientEcremeuse()
    {
    }
  
    //Constructeur de la classe, reçoit l'adresse ip et le port de la fonction main
    public ClientEcremeuse(String sIP, int nPort)
    {   
		String Message = "";
		
		try
		{
			gpioUnexport(NUMBER_GPIO);          				//Déffectation du GPIO #3 (au cas ou ce GPIO est déjè défini par un autre programme)
			gpioExport(NUMBER_GPIO);            				//Affectation du GPIO #3
			gpioSetdir(NAME_GPIO, GPIO_IN);   					//Place GPIO #3 en entrée
			
			m_objShutdown = new Shutdown(this);					//Instancie l'objet de la classe Shutdown avec une référence vers la classe principale (ClientEcremeuse)
			m_objCalculeRPM = new CalculeRPM(this);				//Instancie l'objet de la classe CalculeRPM avec une référence vers la classe principale (ClientEcremeuse)
			m_objLectureCapteur = new LectureCapteur(this);		//Instancie l'objet de la classe LectureCapteur avec une référence vers la classe principale (ClientEcremeuse)
			
			m_IP = sIP;											//Pour que les variables soient accessibles partout dans la classe
			m_Port = nPort;
			
			while (true)										//Rien dans la boucle infinie du main puisque le code de lecture du capteur roule dans un thread appart
			{
				
			}
		}
		
		catch (Exception e)
		{
			System.out.println(e.toString());
		}
    }
	
	//Envoie le RPM au serveur (Pi 3b)
	//Fonction faite par Pierre Bergeron (Modifiée par Samuel Montminy)
	public void EnvoyerAuServeur(String sIP, int nPort, String Message)
	{   
        try
        {
			System.out.println(Message + " -> à été reçu par la fonction");
						
			///*		Mettre en commentaire le bloc pour ne pas envoyer au serveur<- DÉBUT DU BLOC
			System.out.println(Message + " -> sera envoyé au serveur");
            m_sClient = new Socket(sIP, nPort);                                     //Objet Socket pour établir la connexion au miniserveur
            
            OutputStream osOut = m_sClient.getOutputStream();                       //Requête vers le serveur... (flux de données)
            ObjectOutputStream oosOut = new ObjectOutputStream(osOut);
            oosOut.writeObject(Message);

            //Fermeture des objets de flux de données
            oosOut.close();
            osOut.close();
			System.out.println(Message + " -> à été envoyé au serveur");
			//*/																	//<- FIN DU BLOC
        }
        
        catch (UnknownHostException e)
        {
            System.out.println(e.toString());                                       //Nom ou adresse du miniserveur inexistant
        }
        catch (IOException e)
        {
            System.out.println(e.toString());                                       //Problème de communication réseau
        }
        catch (SecurityException e)
        {
            System.out.println(e.toString());                                       //Problème de sécurité (si cela est géré...)
        }
        catch (Exception e)                          
        {
            System.out.println(e.toString());                                       //Autre erreur...
        }
	}

    public static void main(String[] args)
    {
        int argc = 0;																//Variable pour le compte du nombre d'arguments lors de l'appel du code
        
        for (String argument : args)                                                //Compte le nombre d'arguments dans la ligne de commande
        {
            argc++;
        }
        
        if (argc == 2)                                                              //L'utilisateur doit avoir entré deux arguments (IP + Port)
        {
            try
            {
                Integer iArgs = new Integer(args[1]);                               //Conversion du 2e paramètre (port) en entier
                
                ClientEcremeuse obj = new ClientEcremeuse(args[0], iArgs.intValue());     			//Connexion au serveur s'il existe...
            }
            
            catch (NumberFormatException e)
            {
                System.out.println(e.toString());
            }
        }
        
        else
        {
            System.out.println("Nombre d'arguments incorrect (IP + Port)");
        }
    }
	
	public void ResetCountdown()													//Remet le compteur d'inactivité à sa valeur par défaut (120 secondes)
	{
		m_objShutdown.ResetCountdown();
	}
	
	//Pour lire l'état du GPIO
	//Fonction faite par Pierre Bergeron (Modifiée par Samuel Montminy)
    public Integer gpioReadBit(String name_gpio)
    {
        String sLecture;

        try
        {
            FileInputStream fis = new FileInputStream("/sys/class/gpio/" + name_gpio + "/value");           //Sélection de la destination du flux de
                                                                                                            //données (sélection du fichier d'entrée)
                                                                                                            
            DataInputStream dis = new DataInputStream(fis);                                                 //Canal vers le fichier (entrée en "streaming")
            sLecture = dis.readLine();                                                                      //Lecture du fichier                
                                                                                                            
            //System.out.println("/sys/class/gpio/" + name_gpio + "/value = " + sLecture);                  //Affiche l'action réalisée dans la console Java
            dis.close();                                                                                    //Fermeture du canal
            fis.close();                                                                                    //Fermeture du flux de données
        }
		
        catch (Exception e)
        {
            // Affiche l'erreur survenue en Java
            sLecture = "-1";
            System.out.println("Error on gpio readbits" + name_gpio + " :");
            System.out.println(e.toString());
        }
		
        return new Integer(sLecture);  												//Retourne l'état "supposé" de la sortie
    }
	
	//Pour désaffecter le GPIO par kernel
	//Fonction faite par Pierre Bergeron (Modifiée par Samuel Montminy)
    public boolean gpioUnexport(String gpioid)   
    {  
        boolean bError = true;  													//Pour gestion des erreurs
		
        try
        {
            String sCommande = "echo \"" + gpioid + "\">/sys/class/gpio/unexport";  //Commande bash à être exécutée
            String[] sCmd = {"/bin/bash", "-c", sCommande};                       	//Spécifie que l'interpreteur de commandes est BASH. Le "-c" indique que la commande � ex�cuter suit
                                                                                    
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
		
        catch (Exception e)      												//Traitement de l'erreur par la VM Java (différent de l'erreur par l'interpreteur BASH)
        {
			//Affiche l'erreur survenue en Java
            bError = false;
            System.out.println("Error on export GPIO :" + gpioid);
            System.out.println(e.toString());
        }
		
        return  bError;
    }
	
	//Pour affecter le GPIO par kernel
	//Fonction faite par Pierre Bergeron (Modifiée par Samuel Montminy)
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
		 
		catch (Exception e)         											//Traitement de l'erreur par la VM Java (différent de l'erreur par l'interpreteur BASH)
		{
			//Affiche l'erreur survenue en Java
			bError = false;
			System.out.println("Error on export GPIO :" + gpioid);
			System.out.println(e.toString());
		}
		 
        return bError;
    }  
	
	//Configure la direction du GPIO
    //name_gpio : nom associé au répertoire créé par le kernel (gpio +  no i/o du port : Ex: GPIO 2 ==> pgio2)
    //sMode : Configuration de la direction du GPIO("out" ou "in")
	//Fonction faite par Pierre Bergeron (Modifiée par Samuel Montminy)
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
     
            if (p.getErrorStream().available() > 0)        						//Vérification s'il y a une erreur d'exécution par l'interpreteur de commandes BASH
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
		
	    catch (Exception e)
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
class CalculeRPM implements Runnable				//Runnable puisque la classe contient un thread
{
	long MilliSecondes;
	long RPM;
	
	Duration duree;
	Instant start;
	Instant end ;
	
	Thread m_Thread;
    private ClientEcremeuse m_Parent;				//Référence vers la classe principale (ClientEcremeuse)
		
	public CalculeRPM(ClientEcremeuse Parent)		//Constructeur
	{
		try
		{
			m_Parent = Parent;
			
			m_Thread = new Thread(this);			//Crée le thread
			m_Thread.start();						//Démarre le thread
		}
		
		catch (Exception e)
		{
			System.out.println(e.toString());
		}
	}
	
	public void run()								//Thread qui roule en parallèle de la classe principale, fonction appelée automatiquement après le constructeur de la classe
	{
		while (true)								//Boucle infinie sinon le thread se termine
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
				
				duree = Duration.between(start, end);		//La durée entre deux fronts montants (en millisecondes) est la durée entre start et end
				MilliSecondes = duree.toMillis();
				RPM = 60000 / MilliSecondes;																		//Convertit le temps en millisecondes en RPM
				System.out.println("Tour en: " + String.valueOf(MilliSecondes) + "ms, RPM: " + String.valueOf(RPM));
				
				//ID (EC) = Écrémeuse, T,P,H à 0 puisque nous nous en servons pas. C'est une structure de fichier json qui sera ensuite transformée en fichier csv par Hologram
				//Cette string sera envoyée au serveur qui l'envoiera ensuite à Hologram, qui lui va l'envoyer à S3 puis à QuickSight en fichier csv
				m_Parent.EnvoyerAuServeur(m_Parent.m_IP, m_Parent.m_Port, String.valueOf("{ \"ID\":\"EC\", \"T\":\"0\", \"P\":\"0\", \"H\":\"0\", \"R\":\"" + RPM + "\" }"));
				m_Parent.ResetCountdown();																			//Réinitialise le compteur d'inactivité
			}
			
			catch (Exception e)
			{
				System.out.println(e.toString());
			}
		}
	}
}

//Thread qui permet d'éteindre le Pi Zero après deux minutes d'inactivité (pas de front montants détectés) pour conserver la batterie
class Shutdown implements Runnable					//Runnable puisque la classe contient un thread
{
	Thread m_Thread;
    private ClientEcremeuse m_Parent;				//Référence vers la classe principale (ClientEcremeuse)
	
	int m_Countdown;								//Compte pour la fermeture du Pi, si il atteint 0 le Pi s'éteint
	
	public Shutdown(ClientEcremeuse Parent)			//Constructeur
	{
		try
		{
			m_Parent = Parent;
			
			m_Thread = new Thread(this);			//Crée le thread
			m_Thread.start();						//Démarre le thread
			
			m_Countdown = 120;						//Après deux minutes d'inactivité, le pi s'éteint
		}
		
		catch (Exception e)
		{
			System.out.println(e.toString());
		}
	}
	
	public void ResetCountdown()					//Permet de rénitialiser la valeur du compteur d'inactivité (quand un front montant est détecté)
	{
		m_Countdown = 120;
	}
	
	public void run()								//Thread qui roule en parallèle de la classe principale, fonction appelée automatiquement après le constructeur de la classe
	{
		while (true)								//Boucle infinie sinon le thread se termine
		{
			try
			{
				if (m_Countdown <= 0)														//Si aucun front montant n'à été détecté dans les deux dernières minutes
				{
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
				
				else																		//Décrémente la valeur du compteur d'inactivité à chaque seconde
				{
					m_Countdown--;
					Thread.sleep(1000);
					System.out.println("Countdown: " + String.valueOf(m_Countdown));
				}
			}
			
			catch (Exception e)
			{
				System.out.println(e.toString());
			}
		}
	}
}

//Thread qui permet de lire le capteur OneWire DS18B20
class LectureCapteur implements Runnable			//Runnable puisque la classe contient un thread
{
	Thread m_Thread;
		
    private ClientEcremeuse m_Parent;				//Référence vers la classe principale (ClientEcremeuse)
	
	W1Master w1Master = new W1Master();				//Besoin pour le code trouvé sur internet		
		
	double Temperature = 0;
	
	public LectureCapteur(ClientEcremeuse Parent)	//Constructeur
	{
		try
		{
			m_Parent = Parent;
			
			m_Thread = new Thread(this);			//Crée le thread
			m_Thread.start();						//Démarre le thread
		}
		
		catch (Exception e)
		{
			System.out.println(e.toString());
		}
	}
	
	public void run()								//Thread qui roule en parallèle de la classe principale, fonction appelée automatiquement après le constructeur de la classe
	{	
		while (true)								//Boucle infinie sinon le thread se termine
		{
			try
			{
				//CODE TROUVÉ SUR INTERNET <--- Projet github: https://github.com/oksbwn/IOT_Raspberry_Pi/tree/master/src/in/weargenius
				//Agis comme un for each --> met les elements de la classe température dans objet device
				for (TemperatureSensor device : w1Master.getDevices(TemperatureSensor.class)) 
				{
					Temperature = device.getTemperature();
				}
				 
				/*if (Temperature != 0)
				{
					System.out.println("Temperature:" + Temperature + " °C");
				}
				
				else
				{
					System.out.println("Erreur lecture capteur");
				}*/
				//FIN DU CODE TROUVÉ SUR INTERNET <---
				
				//ID (EC) = Écrémeuse, R,P,H à 0 puisque nous nous en servons pas. C'est une structure de fichier json qui sera ensuite transformée en fichier csv par Hologram
				//Cette string sera envoyée au serveur qui l'envoiera ensuite à Hologram, qui lui va l'envoyer à S3 puis à QuickSight en fichier csv
				m_Parent.EnvoyerAuServeur(m_Parent.m_IP, m_Parent.m_Port, String.valueOf("{ \"ID\":\"EC\", \"T\":\"" + Temperature + "\", \"P\":\"0\", \"H\":\"0\", \"R\":\"0\" }"));	
				Thread.sleep(25000);
			}
			
			catch (Exception e)
			{
				System.out.println(e.toString());
			}
		}
	}
}