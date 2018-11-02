import java.io.*;
import java.util.*;

//Classe principale de l'application
public class GPIO {
 
    public static final String GPIO_OUT = "out";        //Pour configurer la direction de la broche GPIO   
    public static final String GPIO_ON = "1";           //Pour l'état haut de la broche GPIO
    public static final String GPIO_OFF = "0";          //Pour l'état bas de la broche GPIO
    public static final String NUMBER_GPIO = "3";   	//ID du GPIO de le Raspberry Pi
    public static final String NAME_GPIO = "gpio3";     //Nom du GPIO pour le Raspberry Pi
 
 
    //Point d'entrée du programme
    public static void main(String[] args)
    {
        new GPIO(); 									//Appel du constructeur
    }
 
    //Affiche un message lorsque le bouton est appuyé
    public GPIO()
    {
		
		try     
		{
			gpioUnexport(NUMBER_GPIO);          		//Désaffectation du GPIO #3 (au cas ou ce GPIO est déjà défini par un autre programme)
			gpioExport(NUMBER_GPIO);            		//Affectation du GPIO #3
			gpioSetdir(NAME_GPIO, GPIO_IN);   			//Place GPIO #3 en entrée
			
			while (1)           						//Boucle infinie
			{

			}
		}
		
		catch (Exception exception)
		{
			exception.printStackTrace();    			//Affiche l'erreur qui est survenue
		}
	}
 
    //Pour désaffecter le GPIO par kernel
    public boolean gpioUnexport(String gpioid)   
    {  
        boolean bError = true;  						//Pour gestion des erreurs
        try
        {
            String sCommande = "echo \"" + gpioid + "\">/sys/class/gpio/unexport";  //Commande bash à être exécutée
            String[] sCmd = {"/bin/bash", "-c", sCommande};                       	//Spécifie que l'interpréteur de commandes est BASH. Le "-c" indique que la commande à exécuter suit
                                                                                    
            System.out.println(sCmd[0] + " " + sCmd[1] + " " + sCmd[2]);            //Affiche la commande à exécuter dans la console Java
            Process p = Runtime.getRuntime().exec(sCmd);                            //Exécute la commande par le système Linux (le programme Java
                                                                                    //doit être démarré par le root pour les accès aux GPIO)
 
            if(p.getErrorStream().available() > 0)                                  //Vérification s'il y a une erreur d'exécution par l'interpréteur de commandes BASH
            {
                // Affiche l'erreur survenue
                bError = false;
                BufferedReader brCommand = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                System.out.println(brCommand.readLine());
                brCommand.close();
			}
			
			Thread.sleep(20);   						//Délai pour laisser le temps au kernel d'agir
        }
		
        catch(Exception e)      						//Traitement de l'erreur par la VM Java (différent de l'erreur par l'interpréteur BASH)
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
            String[] sCmd = {"/bin/bash", "-c", s};                            	//Spécifie que l'interpréteur de commandes est BASH. Le "-c"
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
		 
		catch(Exception e)         												//Traitement de l'erreur par la VM Java (différent de l'erreur par l'interpréteur BASH)
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
            String[] sCmd = { "/bin/bash", "-c", sCommande };                                           //Spécifie que l'interpréteur de commandes est BASH. Le "-c"
                                                                                                        //Indique que la commande à exécuter suit
                                                                                    
            System.out.println(sCmd[0] + " " + sCmd[1] + " " + sCmd[2]);    	//Affiche la commande à exécuter dans la console Java
            Process p = Runtime.getRuntime().exec(sCmd);                    	//Exécute la commande par le système Linux (le programme Java doit 
																				//Être démarré par le root pour les accès aux GPIO)
     
            if(p.getErrorStream().available() > 0)        						//Vérification s'il y a une erreur d'exécution par l'interpréteur de commandes BASH
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
 
    //  Change l'état du GPIO
    //
    //name_gpio : nom associé au répertoire créé par le kernel (gpio +  no i/o du port : Ex: GPIO 2 ==> pgio2)
    // value : État à placer sur la ligne
    public Integer gpioSetBit(String name_gpio, String value)   
    {       
        try
        {
            FileOutputStream fos = new FileOutputStream("/sys/class/gpio/" + name_gpio + "/value");         //Sélection de la destination du flux de
                                                                                                            //données (sélection du fichier de sortie)
                                                                                                            
            DataOutputStream dos = new DataOutputStream(fos);                                               //Canal vers le fichier (sortie en "streaming")
            dos.write(value.getBytes(), 0, 1);                                                              //Écriture dans le fichier
                                                                                                            //(changera l'état du GPIO: 0 ==> niveau bas et différent de 0 niveau haut)
                                                                                                            
            System.out.println("/sys/class/gpio/" + name_gpio + "/value = " + value);                       //Affiche l'action réalisée dans la console Java
            dos.close();                                                                                    //Fermeture du canal
            fos.close();                                                                                    //Fermeture du flux de données
        }
		
        catch(Exception e)
        {
            // Affiche l'erreur survenue en Java
            value = "-1";
            System.out.println("Error on gpio setbits" + name_gpio + " :");
            System.out.println(e.toString());
        }
		
        return new Integer(value);  																		//Retourne l'état "supposé" de la sortie
	}
}







