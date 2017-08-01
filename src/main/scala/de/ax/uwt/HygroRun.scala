package de.ax.uwt

import com.pi4j.io.gpio._
import com.pi4j.io.gpio.event.{GpioPinAnalogValueChangeEvent, GpioPinDigitalStateChangeEvent, GpioPinListenerAnalog, GpioPinListenerDigital}
import com.typesafe.scalalogging.LazyLogging

/**
  * Created by nyxos on 12.07.17.
  */
object HygroRun extends App with LazyLogging{
  logger.info("HygroRun")
  val pin=RaspiPin.GPIO_00

  val digitalInput: GpioPinDigitalInput = GpioFactory.getInstance.provisionDigitalInputPin(pin, pin.getName, PinPullResistance.PULL_DOWN)

  digitalInput.addListener(new GpioPinListenerDigital {
    override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
      logger.info(s"event state is ${event.getState.getName}")
    }
  })
  System.in.read()
}
