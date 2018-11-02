import java.net.*;
import java.io.*;

public class ServeurSocket implements Runnable
{
    final static int NB_OCTETS = 1000;
    int m_nPort = 2228;
    ServerSocket m_ssServeur;
    Thread m_tService;
    
    public ServeurSocket()
    {
        try
        {
            m_ssServeur = new ServerSocket(m_nPort, NB_OCTETS);
            m_tService = new Thread(this);
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
    
    public void run()
    {
        FileOutputStream fosFichier;
        DataOutputStream dosFluxDonnees;
        
        while(m_tService != null)
        {
            try
            {
                System.out.println("Attente d'une connexion au serveur...");
                Socket sConnexion = m_ssServeur.accept();

                System.out.println("Connexion au serveur établie!");
                
                InputStream isIn = sConnexion.getInputStream();
                ObjectInputStream oisIn = new ObjectInputStream(isIn);
                String Info = (String)oisIn.readObject();
                System.out.println(Info);
               
                oisIn.close();
                isIn.close();
            }
            
            catch(IOException e)
            {
                System.out.println(e.toString());
            }
            catch(ClassNotFoundException e)
            {
                System.out.println(e.toString());
            }
            catch(Exception e)
            {
                System.out.println(e.toString());
            }
        }
    }
    
    public static void main(String[] args)
    {
        new ServeurSocket();
    }
}
