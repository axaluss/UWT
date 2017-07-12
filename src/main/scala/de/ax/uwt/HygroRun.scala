package de.ax.uwt

import com.pi4j.io.gpio._
import com.pi4j.io.gpio.event.{GpioPinAnalogValueChangeEvent, GpioPinDigitalStateChangeEvent, GpioPinListenerAnalog, GpioPinListenerDigital}

/**
  * Created by nyxos on 12.07.17.
  */
object HygroRun extends App{
  println("HygroRun")
  val pin=RaspiPin.GPIO_00

  val digitalInput: GpioPinDigitalInput = GpioFactory.getInstance.provisionDigitalInputPin(pin, pin.getName, PinPullResistance.PULL_DOWN)

  digitalInput.addListener(new GpioPinListenerDigital {
    override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
      println(s"event state is ${event.getState.getName}")
    }
  })
  System.in.read()
}
