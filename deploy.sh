#!/bin/bash

sbt clean assembly

scp target/scala-2.12/UWT-assembly-1.0.jar pi@192.168.1.128:/home/pi

scp -r src pi@192.168.1.128:/home/pi

