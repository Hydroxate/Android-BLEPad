#include "BLEPad_UART.h"
#include <Adafruit_NeoPixel.h>

#include "Adafruit_DRV2605.h"
 
Adafruit_DRV2605 drv;

int forceValue = 0;
int prevForceValue = 0;

int thermValue = 0;
int prevThermValue = 0;

int bendValue = 0;
int prevBendValue = 0;

int buttonValue = 0;
int prevButtonValue = 0;

// Which pin on the Arduino is connected to the NeoPixels?
#define PIN            6

// How many NeoPixels are attached to the Arduino?
#define NUMPIXELS      6

Adafruit_NeoPixel pixels = Adafruit_NeoPixel(NUMPIXELS, PIN, NEO_GRBW + NEO_KHZ800);

char outType;  //N for neopixel, V for vibrate, D or digitalOutput
int neoIndex;
int red;
int green;
int blue;

String tmp;
int index = 0;

// Buffer for the incoming data
char inData[100];
// Buffer for the parsed data chunks
char *inParse[100];
char *labelPtr;  //define a pointer

int myDataArray[50];

BLEPad_UART ble(Serial1);
char sendBendBuffer[6];
char sendThermBuffer[6];
char sendButBuffer[6];
char sendForceBuffer[6];

char data;

void setup() {
  Serial.begin(9600);
  ble.begin(9600);

  drv.begin();
  drv.setMode(DRV2605_MODE_INTTRIG); 
  drv.selectLibrary(1);
  
  // I2C trigger by sending 'go' command 
  // default, internal trigger when sending GO command
  
  pinMode(A1,INPUT);
  pinMode(A3,INPUT);
  pinMode(A2,INPUT);
  pinMode(A0,INPUT);

  pinMode(3,OUTPUT);
  pinMode(6,OUTPUT);
  pinMode(9, OUTPUT);
  pinMode(13,OUTPUT);
  
pixels.begin(); // This initializes the NeoPixel library.
  //while (!Serial);
  //Serial.println("hello!");
  
  ble.setConfigMode(0);
  static int countWait = 0;
  int x = 0;
  //wait for android to say something
  while (!ble.available())
  {
      //Serial.println("wait android");
      if(countWait > 50)
      {
        x =!x;
        digitalWrite(13, x);
        countWait = 0;
      }
      delay(10);
      countWait++;
  }

  while (ble.available() > 0)  {
    tmp += char(ble.read());
    delay(2);
  }
    
  if(tmp.length() > 0) {
    //Serial.println(tmp);
    tmp = "";
  }

  
  
}

void loop() {

  while (ble.available() > 0)  {
    data = char(ble.read());
      if (data != ';') //add it to the string
      { //Serial.print(data);
       inData[index] = data;
       index++;
       delay(1);
      }
      else
      { 
       //Serial.println(" ");  
      inData[index] = "  "; //put a gap in for strtok
      ParseData();         
      index=0;
      }
    } 

    readSensors();
 }    



 void ParseData()
{
      // The data to be parsed
      char *p = inData;
      outType = *p;
      p++;
      
      char *str;    // Temp store for each data chunk    
      int count = 0;  // Id ref for each chunk
      // Loop through the data and seperate it into
      // chunks at each "," delimeter
      while ((str = strtok_r(p, ",", &p)) != NULL)
      {
      // Add chunk to array
      int val = atoi(str);
      myDataArray[count] = val;
      count++;
      }
       
      switch (outType)
      {
        case 'N':
        //Serial.println("its an n");
        pixels.setPixelColor(myDataArray[0], pixels.Color(myDataArray[1],myDataArray[2],myDataArray[3]));
       // Serial.println(myDataArray[0]); Serial.println(myDataArray[1]); Serial.println(myDataArray[2]); Serial.println(myDataArray[3]);
        pixels.show();

  
        break;

        case 'D':
       
        analogWrite(myDataArray[0], myDataArray[1]);
        
        break;

        case 'V':

         drv.setWaveform(myDataArray[0], myDataArray[1]);  // 0, effect no
                // end waveform 1, 0
                drv.setWaveform(1, 0);       // end waveform
 
          // play the effect!
          drv.go();
          delay(100);
        break;
        
      }
}




void readSensors()
{ 
  static int keepalive =0;
  static long previousTime=0;
  long currentTime = millis();
  long timeGap = currentTime - previousTime;
  
  
  buttonValue = digitalRead(A3);
  bendValue = analogRead(A2);
  forceValue = analogRead(A1);
  thermValue = analogRead(A0);

  if(timeGap > 25)
  {

        if(bendValue!=prevBendValue || timeGap > 250)
        {   
          sprintf(sendBendBuffer, "C%d", bendValue);
          ble.write(sendBendBuffer); 
          previousTime=millis();
        }
      
      
        if(thermValue!=prevThermValue )
        {   
          sprintf(sendThermBuffer, "A%d", thermValue);
          ble.write(sendThermBuffer);   
          previousTime=millis();
        }
      
         if(forceValue!=prevForceValue )
        {   
          sprintf(sendForceBuffer, "B%d", forceValue);
          ble.write(sendForceBuffer);  
          previousTime=millis();   
        }
      
         if(buttonValue!=prevButtonValue )
        {   
          sprintf(sendButBuffer, "D%d", buttonValue);
          ble.write(sendButBuffer);  
          previousTime=millis();  
        }
      
        prevButtonValue = buttonValue;
        prevBendValue = bendValue;
        prevForceValue = forceValue;
        prevThermValue = thermValue;
  }
}

