/**
 * @file   MQTT.ino
 * @author Branche originale: Arduino, modifiée par: Daz, Samuel Montminy & Harri Laliberté
 * @date   Mars 2019
 * @brief  Ce code permet d'envoyer des données sur AWS IoT à partir du module Arduino MKR GSM 1400
 * @version 1.0 : Première version
 * Environnement de développement: Notepad++
 * Compilateur: Arduino
 * Matériel: Raspberry Pi 3b, Arduino MKR GSM 1400
 */

#include <MKRGSM.h>
#include <PubSubClient.h>
#include "arduino_secrets.h" 

// PIN Number
const char PINNUMBER[]     = SECRET_PINNUMBER;
// APN data
const char GPRS_APN[]      = SECRET_GPRS_APN;
const char GPRS_LOGIN[]    = SECRET_GPRS_LOGIN;
const char GPRS_PASSWORD[] = SECRET_GPRS_PASSWORD;

const char server[] = "a2f4d7lmuqybqc-ats.iot.us-east-1.amazonaws.com";
const char topic[] = "hologram-projetnepal/to";
const char publishTopic[] = "hologram-projetnepal/from";
const char clientId[] = "Arduino_MKR_GSM_1400";

GPRS gprs;
GSM gsmAccess;
GSMClient gsmClient;
GSMSecurity profile;

MqttClient mqttClient(gsmClient);
PubSubClient mqttClient(server, 8883, gsmClient);

void setup() 
{
  //Initialize serial and wait for port to open:
  Serial.begin(9600);
  while (!Serial) 
  {
    ; //wait for serial port to connect. Needed for native USB port only
  }

  //attempt to connect to GSM and GPRS:
  Serial.print("Attempting to connect to GSM and GPRS");
  //connection state
  bool connected = false;

  //After starting the modem with GSM.begin()
  //attach the shield to the GPRS network with the APN, login and password
  while (!connected) 
  {
    if ((gsmAccess.begin(PINNUMBER) == GSM_READY) && (gprs.attachGPRS(GPRS_APN, GPRS_LOGIN, GPRS_PASSWORD) == GPRS_READY)) 
	{
      connected = true;
    }
	
	else 
	{
      Serial.println("Not connected");
      delay(1000);
    }
  }

  Serial.println("You're connected to the network");
  Serial.println("Importing certificates...");
  
  profile.setRootCertificate(SECRET_ROOT_CERT);
  profile.setClientCertificate(SECRET_CLIENT_CERT);
  profile.setPrivateKey(SECRET_PRIVATE_KEY);
  profile.setValidation(SSL_VALIDATION_ROOT_CERT);
  profile.setVersion(SSL_VERSION_TLS_1_2);
  profile.setCipher(SSL_CIPHER_AUTO);
  gsmClient.setSecurityProfile(profile);
  
  Serial.println("Connecting...");
  
  //You can provide a unique client ID, if not set the library uses Arduino-millis()
  //Each client must have a unique client ID
  //mqttClient.setId("clientId");
  while (!mqttClient.connect(clientId)) 
  {
	Serial.print(".");
	delay(500);
  }
}

unsigned long prevNow = millis();

void loop() 
{
  unsigned long now = millis();
  
  if (now - prevNow >= 30000) 
  {
    prevNow = now;
	
    if (mqttClient.publish(topic, "{d: { status: \"connected!\"}")) 
	{
      Serial.println("Publish ok");
    } 
	
	else 
	{
      Serial.println("Publish failed");
    }
  }

  mqttClient.loop();
}
