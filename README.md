# kettler-bike
My first non trivial Clojure project.
Aim is to learn Clojure and to implement a control software for my Kettler Ergometer:
- Control heart rate measured by Polar HR Belt and Polar Reciever by setting power
- Manual exercise power setting
- Time controled profiles (e.g. stepwise increase of power)
- Rest Api

## Architecture Sketch
![Alt text](./kettler.drawio.svg) 
### Kettler Ergometer KX1
ergometer Bike which has a well documented Serial Interface
### RS232 
bike is connected to a Raspberry Pi via RS232 Interface
### Mock Up 
could replace ergometer and RS232 for development purposes
### state machine
to handle the states presented by RS232 or mimicked by Mock Up component
### PID Controller
in the *hr control mode* I  want to control the heart rate by setting the appropiate power at the ergometer
### Rest API
to publish  the state of excercise and to manually set power
### M5Stack or Web Front End
to offer a User Interface (haven't yet decided)
