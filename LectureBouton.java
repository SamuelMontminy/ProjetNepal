import java.io.*;
import java.util.*;

// Classe principale de l'application
public class GPIO {
 
    public static final String GPIO_OUT = "out";        // Pour configurer la direction de la broche GPIO   
    public static final String GPIO_ON = "1";           // Pour l'�tat haut de la broche GPIO
    public static final String GPIO_OFF = "0";          // Pour l'�tat bas de la broche GPIO
    public static final String NUMBER_GPIO = "2";       // ID du GPIO de le Raspberry Pi
    public static final String NAME_GPIO="gpio2";       // Nom du GPIO pour le Raspberry Pi
 
 
    // Point d'entr�e du programme
    public static void main(String[] args)
    {
        new GPIO(); // Appel du constructeur
    }
 
    // Fera changer l'�tat du GPIO #2 fois
    public GPIO()
    {
		int i = 0;  // Compteur
		
		try     
		{
			gpioUnexport(NUMBER_GPIO);          // D�saffectation du GPIO #2 (au cas ou ce GPIO est d�j� d�fini par un autre programme)
			gpioExport(NUMBER_GPIO);            // Affectation du GPIO #2
			gpioSetdir(NAME_GPIO, GPIO_OUT);    // Place GPIO #2 en sortie
			
			while (i < 5)           // Boucle 5 fois
			{
				gpioSetBit(NAME_GPIO, GPIO_ON);     //GPIO #2 � un niveau haut
				Thread.sleep(1000);                 // D�lai de 1 seconde
				gpioSetBit(NAME_GPIO, GPIO_OFF);    //GPIO #2 � un niveau bas
				Thread.sleep(1000);                 // D�lai de 1 seconde
				i++;                                // Incr�mente le compteur de 1
			}
		}
		
		catch (Exception exception)
		{
			exception.printStackTrace();    // Affiche l'erreur qui est survenue
		}
	}
 
    // Pour d�saffecter le GPIO par kernel
    public boolean gpioUnexport(String gpioid)   
    {  
        boolean bError = true;  // Pour gestion des erreurs
        try
        {
            String sCommande = "echo \"" + gpioid + "\">/sys/class/gpio/unexport";  // Commande bash � �tre ex�cut�e
            String[] sCmd = { "/bin/bash", "-c", sCommande };                       // Sp�cifie que l'interpr�teur de commandes est BASH. Le "-c" indique que la commande � ex�cuter suit
                                                                                    
            System.out.println(sCmd[0] + " " + sCmd[1] + " " + sCmd[2]);            // Affiche la commande � ex�cuter dans la console Java
            Process p = Runtime.getRuntime().exec(sCmd);                            // Ex�cute la commande par le syst�me Linux (le programme Java
                                                                                    // doit �tre d�marr� par le root pour les acc�s aux GPIO)
 
            if(p.getErrorStream().available()>0)                                    // V�rification s'il y a une erreur d'ex�cution par l'interpr�teur de commandes BASH
            {
                // Affiche l'erreur survenue
                bError = false;
                BufferedReader brCommand = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                System.out.println(brCommand.readLine());
                brCommand.close();
			}
			
			Thread.sleep(20);   // D�lai pour laisser le temps au kernel d'agir
        }
		
        catch(Exception e)      // Traitement de l'erreur par la VM Java (diff�rent de l'erreur par l'interpr�teur BASH)
        {
            // Affiche l'erreur survenue en Java
            bError = false;
            System.out.println("Error on export GPIO :" + gpioid);
            System.out.println(e.toString());
        }
		
        return  bError;
    }
 
    // Pour affecter le GPIO par kernel
    public boolean gpioExport(String gpioid)   
    {  
        boolean bError = true;  // Pour gestion des erreurs
        
		try
        {
            String s = "echo \"" + gpioid + "\">/sys/class/gpio/export";        // Commande bash � �tre ex�cut�e
            String[] sCmd = { "/bin/bash", "-c", s};                            // Sp�cifie que l'interpr�teur de commandes est BASH. Le "-c"
                                                                                // indique que la commande � ex�cuter suit
                                                                                
            System.out.println(sCmd[0] + " " + sCmd[1] + " " + sCmd[2]);        // Affiche la commande � ex�cuter dans la console Java
            Process p = Runtime.getRuntime().exec(sCmd);                        // Ex�cute la commande par le syst�me Linux (le programme Java 
                                                                                // doit �tre d�marr� par le root pour les acc�s aux GPIO)
     
            if(p.getErrorStream().available()>0)        // V�rification s'il y a une erreur d'ex�cution par l'interpr�teur de commandes BASH
            {
                // Affiche l'erreur survenue
                bError = false;
                BufferedReader brCommand = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                System.out.println(brCommand.readLine());
                brCommand.close();
            }
            Thread.sleep(100);      // D�lai pour laisser le temps au kernel d'agir
        }
		 
		catch(Exception e)         // Traitement de l'erreur par la VM Java (diff�rent de l'erreur par l'interpr�teur BASH)
		{
			// Affiche l'erreur survenue en Java
			bError = false;
			System.out.println("Error on export GPIO :" + gpioid);
			System.out.println(e.toString());
		}
		 
        return bError;
    }  
 
    // Configure la direction du GPIO
    //
    // name_gpio : nom associ� au r�pertoire cr�� par le kernel (gpio +  no i/o du port : Ex: GPIO 2 ==> pgio2)
    // sMode : Configuration de la direction du GPIO("out" ou "in")
    public boolean gpioSetdir(String name_gpio, String sMode)   
    {  
        boolean bError = true;  // Pour gestion des erreurs
        try
        {
			String sCommande = "echo \"" + sMode + "\" >/sys/class/gpio/" + name_gpio + "/direction";   // Commande bash � �tre ex�cut�e
            String[] sCmd = { "/bin/bash", "-c", sCommande };                                           // Sp�cifie que l'interpr�teur de commandes est BASH. Le "-c"
                                                                                                        // indique que la commande � ex�cuter suit
                                                                                    
            System.out.println(sCmd[0] + " " + sCmd[1] + " " + sCmd[2]);    // Affiche la commande � ex�cuter dans la console Java
            Process p = Runtime.getRuntime().exec(sCmd);                    // Ex�cute la commande par le syst�me Linux (le programme Java doit 
                                                                            // �tre d�marr� par le root pour les acc�s aux GPIO)
     
            if(p.getErrorStream().available()>0)        // V�rification s'il y a une erreur d'ex�cution par l'interpr�teur de commandes BASH
            {
                // Affiche l'erreur survenue
                bError = false;
                BufferedReader brCommand = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                sCommande = brCommand.readLine();
                System.out.println(sCommande);
                brCommand.close();
            }
			
            Thread.sleep(100);      // D�lai pour laisser le temps au kernel d'agir
	    }
		
	    catch(Exception e)
	    {
			// Affiche l'erreur survenue en Java
			bError = false;
			System.out.println("Error on direction setup :");
			System.out.println(e.toString());
	    }
		
		return bError;
    }  
 
    //  Change l'�tat du GPIO
    //
    //name_gpio : nom associ� au r�pertoire cr�� par le kernel (gpio +  no i/o du port : Ex: GPIO 2 ==> pgio2)
    // value : �tat � placer sur la ligne
    public Integer gpioSetBit(String name_gpio, String value)   
    {       
        try
        {
            FileOutputStream fos = new FileOutputStream("/sys/class/gpio/"+name_gpio+"/value");             // S�lection de la destination du flux de
                                                                                                            // donn�es (s�lection du fichier de sortie)
                                                                                                            
            DataOutputStream dos = new DataOutputStream(fos);                                               // Canal vers le fichier (sortie en "streaming")
            dos.write(value.getBytes(), 0, 1);                                                              // �criture dans le fichier
                                                                                                            //(changera l'�tat du GPIO: 0 ==> niveau bas et diff�rent de 0 niveau haut)
                                                                                                            
            System.out.println("/sys/class/gpio/"+ name_gpio + "/value = " + value);                        // Affiche l'action r�alis�e dans la console Java
            dos.close();                                                                                    // Fermeture du canal
            fos.close();                                                                                    // Fermeture du flux de donn�es
        }
		
        catch(Exception e)
        {
            // Affiche l'erreur survenue en Java
            value = "-1";
            System.out.println("Error on gpio setbits"+name_gpio+ " :");
            System.out.println(e.toString());
        }
		
        return new Integer(value);  // Retourne l'�tat "suppos�" de la sortie
	}
}







