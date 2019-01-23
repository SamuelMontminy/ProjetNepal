import com.pi4j.component.temperature.TemperatureSensor;
import com.pi4j.io.w1.W1Master;
import com.pi4j.temperature.TemperatureScale;

public class DS18B20 
{

	public static void main(String[] args) throws InterruptedException
	{
		// TODO Auto-generated method stub
		
		W1Master w1Master = new W1Master();
		//System.out.println(w1Master);
		
		double temp = 0;
		while(true)
		{
			
			for (TemperatureSensor device : w1Master.getDevices(TemperatureSensor.class)) 
			{
            //System.out.printf("%-20s %3.1f°C %3.1f°F\n", device.getName(), device.getTemperature(),
                    //device.getTemperature(TemperatureScale.CELSIUS));
        	temp = device.getTemperature();
       		}
			if(temp!=0)
			{
				System.out.println("Temperature is: "+ temp+" °C");
			}
			else
			{
				System.out.println("Sensor not found ");
			}
			Thread.sleep(2*1000);
		}	
	}
}


