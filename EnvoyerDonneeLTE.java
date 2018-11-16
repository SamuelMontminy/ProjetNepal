/**
 * @file   EnvoyerDonneeLTE.java
 * @author Samuel Montminy
 * @date   Novembre 2018
 * @brief  Code qui permet d'envoyer une donnée sur le dashboard Hologram grâce au module LTE
 *
 * @version 1.0 : Première version
 * Environnement de développement: GitKraken
 * Compilateur: javac (Java version 1.8)
 * Matériel: Raspberry Pi Zero W, Module LTE Hologram (+ carte SIM)
 */

import java.io.*;
import java.util.*;

public class EnvoyerDonneeLTE
{
    //Point d'entrée du programme
    public static void main(String[] args)
    {
        new EnvoyerDonneeLTE(); 							//Appel du constructeur
    }

    public EnvoyerDonneeLTE()
    {
        boolean bError = true;  							//Pour gestion des erreurs
        String donneesLTE = "bONsOiR2";

        try
        {
            //Message à éxécuter en ligne de commmande: sudo hologram send [string]

            String s = "sudo hologram send " + donneesLTE;       //Commande bash à être exécutée
            String[] sCmd = {"/bin/bash", "-c", s};             //Spécifie que l'interpréteur de commandes est BASH. Le "-c"
                                                                //indique que la commande à exécuter suit

            System.out.println(sCmd[0] + " " + sCmd[1] + " " + sCmd[2]);        //Affiche la commande à exécuter dans la console Java
            Process p = Runtime.getRuntime().exec(sCmd);        //Exécute la commande par le système Linux (le programme Java 
                                                                //doit être démarré par le root pour les accès aux GPIO)

            if (p.getErrorStream().available() > 0)        		//Vérification s'il y a une erreur d'exécution par l'interpréteur de commandes BASH
            {
                //Affiche l'erreur survenue
                bError = false;
                BufferedReader brCommand = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                System.out.println(brCommand.readLine());
                brCommand.close();
            }

            Thread.sleep(100);      							//Délai pour laisser le temps au kernel d'agir
        }

        catch (Exception e)
        {
            //Affiche l'erreur survenue en Java
			bError = false;
			System.out.println(e.toString());
        }
    }
}