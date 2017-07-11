package de.ax.uwt

import com.pi4j.io.gpio._
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import de.ax.uwt.DryRun.{doSchedule, doWater}
import io.circe.parser.decode
import io.circe.generic.auto._
import io.circe.parser._

import scala.io.Source

/**
  * Created by nyxos on 20.06.17.
  */
object PiRun extends App with UWT {


  case class IntOutputPin(i: Int) extends OutputPin {
    private val pin = RaspiPin.getPinByName(s"GPIO $i")
    val digitalOutput: GpioPinDigitalOutput = GpioFactory.getInstance.provisionDigitalOutputPin(pin, pin.getName, PinState.LOW)
    off

    override def off: Unit = digitalOutput.low()

    override def on: Unit = digitalOutput.high()

    override def shutdown: Unit = {
      GpioFactory.getInstance.unprovisionPin(digitalOutput)
    }

    override def identifier: Any = i
  }

  case class IntInputPin(i: Int) extends InputPin {
    private val pin = RaspiPin.getPinByName(s"GPIO $i")
    val digitalInput: GpioPinDigitalInput = GpioFactory.getInstance.provisionDigitalInputPin(pin, pin.getName, PinPullResistance.PULL_DOWN)

    digitalInput.addListener(new GpioPinListenerDigital {
      override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
        if (event.getState.isHigh) {
          val millis = curMs
          handlers.foreach(h => h(millis))
        }
      }
    })

    override def shutdown: Unit = {
      GpioFactory.getInstance.unprovisionPin(digitalInput)
    }

    override def identifier: Any = i
  }

  implicit def i2p(i: Int): OutputPin = {
    IntOutputPin(i)
  }

  implicit def i2p2(i: Int): InputPin = {
    IntInputPin(i)
  }

  def setup: Net = {
    RealNet.net(this)
  }


  override def getWeatherData: WeatherDataSet = {
    val res = decode[WeatherDataSet](Source.fromFile("testdata/meisenbach.json").mkString)
    val option = res.toOption
    option.get
  }

  override def doWait(waitMs: Long): Unit = Thread.sleep(waitMs)

  override def shouldStop: Boolean = false


  doSchedule(0.002777778)

  override def curMs: Long = System.currentTimeMillis()
}
