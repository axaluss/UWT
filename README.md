# README #

# Ultimate Watering Tool #

This project aims at watering the plants in a garden with the help of a raspberry pi, some relays, valves and a pump.
To calculate the watering requirements weatherdata from https://darksky.net/dev/docs/forecast is utilized.

#usage

## run on PC without raspberry pi
* install sbt (simple build tool)
* sbt 
    * run
    * choose de.ax.uwt.Dry

## run on raspberry
* `sbt assembly`
* `scp target/scala-2.12/UWT-assembly-1.0.jar pi@192.168.1.128:/home/pi`

    