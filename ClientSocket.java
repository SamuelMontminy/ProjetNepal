import java.net.*;
import java.io.*;

public class ClientSocket
{
    Socket m_sClient;
    
    public ClientSocket()
    {
    }
  
    public ClientSocket(String sIP, int nPort)
    {   
        String sLectureClavier = "";
        boolean Modification = false;
        BufferedReader brLectureClavier;
 
        brLectureClavier = new BufferedReader(new InputStreamReader(System.in));
        
        try
        {
            m_sClient = new Socket(sIP, nPort);
			
			System.out.println("Entrez un numéro");
			sLectureClavier = brLectureClavier.readLine();
            
            OutputStream osOut = m_sClient.getOutputStream();
            ObjectOutputStream oosOut = new ObjectOutputStream(osOut);
            oosOut.writeObject(sLectureClavier);

            oosOut.close();
            osOut.close();
        }
        
        catch(UnknownHostException e)
        {
            System.out.println(e.toString());
        }
        catch(IOException e)
        {
            System.out.println(e.toString());
        }
        catch(SecurityException e)
        {
            System.out.println(e.toString());
        }
        catch(Exception e)
        {
            System.out.println(e.toString());
        }
    }

    public static void main(String[] args)
    {
        int argc = 0;
        
        for (String argument : args)
        {
            argc++;
        }
        
        if (argc == 2)
        {
            try
            {
                Integer iArgs = new Integer(args[1]);
                
                ClientSocket obj = new ClientSocket(args[0], iArgs.intValue());
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
