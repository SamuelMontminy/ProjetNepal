/**
 * @file   ClientEntrepot.java
 * @author Samuel Montminy
 * @date   Janvier 2019
 * @brief  Code qui permet de lire le capteur BME280 par I2C et puis et envoyer les données au serveur par socket tcp/ip
 *		   Le code doit être compilé avec /pi4j -c ClientEntrepot.java et doit être lancé avec /pi4j -r ClientEntrepot 192.168.4.1 (Adresse IP du serveur) 2228 (Port de communication avec le serveur)
 *
 * @version 1.0 : Première version
 * @version 1.1 : Distinction entre les codes clients. Ce code sera seulement utilisé par l'entrepot (BME280)
 * Environnement de développement: GitKraken
 * Compilateur: javac (Java version 1.8)
 * Matériel: Raspberry Pi Zero W
 */

import java.net.*;										//Importation du package io pour les accès aux fichiers
import java.io.*;
import java.io.IOException;

//Librairies pour la lecture du capteur en I2C avec Pi4J
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

public class ClientEntrepot
{
    Socket m_sClient;           						//Référence de l'objet Socket
	
	public LectureCapteur m_objCapteur;					//Objet pour la classe pour la lecture du capteur
	
	String m_IP;										//Adresse du serveur
	int m_Port;											//Port de communication avec le serveur
    
    public ClientEntrepot()
    {
    }
  
    //Constructeur de la classe, reçoit l'adresse ip et le port de la fonction main
    public ClientEntrepot(String sIP, int nPort)
    {   
		String Message = "";
		
		try
		{	
			m_objCapteur = new LectureCapteur(this);	//Instancie l'objet de la classe LectureCapteur avec une référence vers la classe principale (ClientEntrepot)
			
			m_IP = sIP;									//Pour que les variables soient accessibles partout dans la classe
			m_Port = nPort;
			
			while (true)								//Rien dans la boucle infinie du main puisque le code de lecture du capteur roule dans un thread appart
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
            oosOut.writeObject(Message);											//Envoie les données (qui sont dans la variable message)

            oosOut.close();															//Fermeture des objets de flux de données
            osOut.close();
			System.out.println(Message + " -> à été envoyé au serveur");
			//*/																	//- FIN DU BLOC
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
                
                ClientEntrepot obj = new ClientEntrepot(args[0], iArgs.intValue()); //Connexion au serveur s'il existe...
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
class LectureCapteur implements Runnable				//Runnable puisque la classe contient un thread
{
	String Message = "";								//Les données vont être dans cette variable pour les envoyer au serveur
	Thread m_Thread;									
    private ClientEntrepot m_Parent;					//Référence vers la classe principale (ClientEntrepot)
		
	public LectureCapteur(ClientEntrepot Parent)		//Constructeur
	{
		try
		{
			m_Parent = Parent;							//Référence vers la classe principale (ClientEntrepot)
			
			m_Thread = new Thread(this);				//Crée le thread dans cette classe
			m_Thread.start();							//Démarre le thread
		}
		
		catch(Exception e)
		{
			System.out.println(e.toString());
		}
	}
	
	public void run()									//Thread qui roule en parallèle de la classe principale, fonction appelée automatiquement après le constructeur de la classe
	{
		while (true)									//Boucle infinie sinon le thread se termine
		{
			try
			{
				Message = "Erreur Lecture Capteur";		//Permet de savoir si il y a eu une erreur de lecture de capteur (la variable va rester inchangée)

				//CODE TROUVÉ SUR INTERNET <--- Projet github: https://github.com/ControlEverythingCommunity/BME280/blob/master/Java/BME280.java
				//Create I2C bus
				I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_1);
				//Get I2C device, BME280 I2C address is 0x77(108)
				I2CDevice device = bus.getDevice(0x77);
				
				//Read 24 bytes of data from address 0x88(136)
				byte[] b1 = new byte[24];
				device.read(0x88, b1, 0, 24);
				
				//Convert the data
				//temp coefficients
				int dig_T1 = (b1[0] & 0xFF) + ((b1[1] & 0xFF) * 256);
				int dig_T2 = (b1[2] & 0xFF) + ((b1[3] & 0xFF) * 256);
				if(dig_T2 > 32767)
				{
					dig_T2 -= 65536;
				}
				int dig_T3 = (b1[4] & 0xFF) + ((b1[5] & 0xFF) * 256);
				if(dig_T3 > 32767)
				{
					dig_T3 -= 65536;
				}
				
				//pressure coefficients
				int dig_P1 = (b1[6] & 0xFF) + ((b1[7] & 0xFF) * 256);
				int dig_P2 = (b1[8] & 0xFF) + ((b1[9] & 0xFF) * 256);
				if(dig_P2 > 32767)
				{
					dig_P2 -= 65536;
				}
				int dig_P3 = (b1[10] & 0xFF) + ((b1[11] & 0xFF) * 256);
				if(dig_P3 > 32767)
				{
					dig_P3 -= 65536;
				}
				int dig_P4 = (b1[12] & 0xFF) + ((b1[13] & 0xFF) * 256);
				if(dig_P4 > 32767)
				{
					dig_P4 -= 65536;
				}
				int dig_P5 = (b1[14] & 0xFF) + ((b1[15] & 0xFF) * 256);
				if(dig_P5 > 32767)
				{
					dig_P5 -= 65536;
				}
				int dig_P6 = (b1[16] & 0xFF) + ((b1[17] & 0xFF) * 256);
				if(dig_P6 > 32767)
				{
					dig_P6 -= 65536;
				}
				int dig_P7 = (b1[18] & 0xFF) + ((b1[19] & 0xFF) * 256);
				if(dig_P7 > 32767)
				{
					dig_P7 -= 65536;
				}
				int dig_P8 = (b1[20] & 0xFF) + ((b1[21] & 0xFF) * 256);
				if(dig_P8 > 32767)
				{
					dig_P8 -= 65536;
				}
				int dig_P9 = (b1[22] & 0xFF) + ((b1[23] & 0xFF) * 256);
				if(dig_P9 > 32767)
				{
					dig_P9 -= 65536;
				}
				
				//Read 1 byte of data from address 0xA1(161)
				int dig_H1 = ((byte)device.read(0xA1) & 0xFF);
				
				//Read 7 bytes of data from address 0xE1(225)
				device.read(0xE1, b1, 0, 7);
				
				//Convert the data
				//humidity coefficients
				int dig_H2 = (b1[0] & 0xFF) + (b1[1] * 256);
				if(dig_H2 > 32767)
				{
					dig_H2 -= 65536;
				}
				int dig_H3 = b1[2] & 0xFF ;
				int dig_H4 = ((b1[3] & 0xFF) * 16) + (b1[4] & 0xF);
				if(dig_H4 > 32767)
				{
					dig_H4 -= 65536;
				}
				int dig_H5 = ((b1[4] & 0xFF) / 16) + ((b1[5] & 0xFF) * 16);
				if(dig_H5 > 32767)
				{
					dig_H5 -= 65536;
				}
				int dig_H6 = b1[6] & 0xFF;
				if(dig_H6 > 127)
				{
					dig_H6 -= 256;
				}
				
				//Select control humidity register
				//Humidity over sampling rate = 1
				device.write(0xF2 , (byte)0x01);
				//Select control measurement register
				//Normal mode, temp and pressure over sampling rate = 1
				device.write(0xF4 , (byte)0x27);
				//Select config register
				//Stand_by time = 1000 ms
				device.write(0xF5 , (byte)0xA0);
				
				//Read 8 bytes of data from address 0xF7(247)
				//pressure msb1, pressure msb, pressure lsb, temp msb1, temp msb, temp lsb, humidity lsb, humidity msb
				byte[] data = new byte[8];
				device.read(0xF7, data, 0, 8);
				
				//Convert pressure and temperature data to 19-bits
				long adc_p = (((long)(data[0] & 0xFF) * 65536) + ((long)(data[1] & 0xFF) * 256) + (long)(data[2] & 0xF0)) / 16;
				long adc_t = (((long)(data[3] & 0xFF) * 65536) + ((long)(data[4] & 0xFF) * 256) + (long)(data[5] & 0xF0)) / 16;
				// Convert the humidity data
				long adc_h = ((long)(data[6] & 0xFF) * 256 + (long)(data[7] & 0xFF));
				
				//Temperature offset calculations
				double var1 = (((double)adc_t) / 16384.0 - ((double)dig_T1) / 1024.0) * ((double)dig_T2);
				double var2 = ((((double)adc_t) / 131072.0 - ((double)dig_T1) / 8192.0) *
							   (((double)adc_t)/131072.0 - ((double)dig_T1)/8192.0)) * ((double)dig_T3);
				double t_fine = (long)(var1 + var2);
				double cTemp = (var1 + var2) / 5120.0;
				double fTemp = cTemp * 1.8 + 32;
				
				//Pressure offset calculations
				var1 = ((double)t_fine / 2.0) - 64000.0;
				var2 = var1 * var1 * ((double)dig_P6) / 32768.0;
				var2 = var2 + var1 * ((double)dig_P5) * 2.0;
				var2 = (var2 / 4.0) + (((double)dig_P4) * 65536.0);
				var1 = (((double) dig_P3) * var1 * var1 / 524288.0 + ((double) dig_P2) * var1) / 524288.0;
				var1 = (1.0 + var1 / 32768.0) * ((double)dig_P1);
				double p = 1048576.0 - (double)adc_p;
				p = (p - (var2 / 4096.0)) * 6250.0 / var1;
				var1 = ((double) dig_P9) * p * p / 2147483648.0;
				var2 = p * ((double) dig_P8) / 32768.0;
				double pressure = (p + (var1 + var2 + ((double)dig_P7)) / 16.0) / 100;
				
				//Humidity offset calculations
				double var_H = (((double)t_fine) - 76800.0);
				var_H = (adc_h - (dig_H4 * 64.0 + dig_H5 / 16384.0 * var_H)) * (dig_H2 / 65536.0 * (1.0 + dig_H6 / 67108864.0 * var_H * (1.0 + dig_H3 / 67108864.0 * var_H)));
				double humidity = var_H * (1.0 -  dig_H1 * var_H / 524288.0);
				if(humidity > 100.0)
				{
					humidity = 100.0;
				}else
					if(humidity < 0.0) 
					{
						humidity = 0.0;
					}
				
				/*
				System.out.printf("Temperature in Celsius : %.2f C %n", cTemp);
				System.out.printf("Temperature in Fahrenheit : %.2f F %n", fTemp);
				System.out.printf("Pressure : %.2f hPa %n", pressure);
				System.out.printf("Relative Humidity : %.2f %% RH %n", humidity);
				*/
				//FIN DU CODE TROUVÉ SUR INTERNET <---
		
				pressure = pressure / 10;								//Pour avoir la pression en bars au lieu de deci-bars
				
				String Temperature = String.format("%1$.3f", cTemp);
				String Pression = String.format("%1$.3f", pressure);
				String Humidite = String.format("%1$.3f", humidity);
				
				//À CHANGER
				m_Parent.EnvoyerAuServeur(m_Parent.m_IP, m_Parent.m_Port, String.valueOf("Entrepot/T:" + Temperature + "/P:" + Pression + "/H:" + Humidite));	//Envoie l'information (RPM) à la fonction qui va l'envoyer au serveur
				Thread.sleep(10000);
			}
			
			catch(Exception e)
			{
				System.out.println(e.toString());
			}
		}
	}
}