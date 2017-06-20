package de.ax.uwt

import com.pi4j.io.gpio._
import de.ax.uwt.DryRun.doWater

/**
  * Created by nyxos on 20.06.17.
  */
object PiRun extends App with UWT {


  case class IntPin(i: Int) extends Pin {
    private val pin = RaspiPin.getPinByName(s"GPIO $i")
    val digitalOutput: GpioPinDigitalOutput = GpioFactory.getInstance.provisionDigitalOutputPin(pin, pin.getName, PinState.HIGH)
    off

    override def off: Unit = digitalOutput.low()

    override def on: Unit = digitalOutput.high()
  }

  implicit def i2p(i: Int): Pin = {
    IntPin(i)
  }

  def setup: Net = {
    RealNet.net(this)
  }


  doWater

  override def getWeatherData: WeatherDataSet = ???

  override def doWait(waitMs: Long): Unit = ???
}
