# kettler-bike
## Architecture Sketch
![Alt text](./kettler.drawio.svg) 
### Kettler Ergometer KX1
Ergometer Bike which has a well documented Serial Interface
### RS232 
Connected to a Raspberry Pi via RS232 Interface
### Mock Up 
could replace ergometer and RS232 for development purposes
### state machine
To handle the states presented by RS232 or mimicked by Mock Up component
### PID Controller
In the *hr control mode* I  want to control the heart rate by setting the appropiate power at the ergometer
### Rest API
To publish  the state of excercise and to manually set power
### M5Stack or Web Front End
To offer a User Interface (haven't yet decided)
