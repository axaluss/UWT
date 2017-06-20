import Main.setup
import com.pi4j.io.gpio.{GpioFactory, GpioPinDigitalOutput, PinState, RaspiPin}
//import com.pi4j.io.gpio.{GpioFactory, GpioPinDigitalOutput, Pin, PinState, RaspiPin => Pins}

/**
  * Created by nyxos on 14.06.17.
  */

trait UWT {

  trait Pin {


    def off

    def on

  }


  abstract class Relais {
    val name: String
    val pin: Pin
    var isOn = false
    off

    def isOff = !isOn

    def off: Unit = {
      pin.off
      isOn = false
    }

    def on: Unit = {
      pin.on
      isOn = true
    }

  }


  case class Net(var elms: List[Relais] = List.empty,
                 var flows: List[Flow] = List.empty) {
    def valve(name: String, pin: Pin): Valve = {
      val out1 = Valve(name, pin)

      addElm(out1)
      out1
    }

    def pump(name: String, pin: Pin): Pump = {
      val out1 = Pump(name, pin)
      addElm(out1)
      out1
    }

    private def addElm(out1: Relais): Unit = {
      assert(!elms.exists(_.pin == out1.pin), s"pin ${out1.pin} already in use")
      assert(!elms.exists(_.name == out1.name), s"name '${out1.name}' for valve already in use")
      elms = elms :+ out1
    }

    def flow(name: String, pump: Pump, valve: Valve, flowPlan: FlowPlan): Flow = {
      val flow1 = Flow(name: String, pump: Pump, valve: Valve, flowPlan)
      assert(!flows.exists(f => f.valve == valve), s"valve $valve for flow $flow1 already in use")
      assert(!flows.exists(f => f.name == name), s"name '$name' for flow $flow1 already in use")
      flows = flows :+ flow1
      flow1
    }
  }


  case class Valve(name: String, pin: Pin) extends Relais

  case class Pump(name: String, pin: Pin) extends Relais

  case class Flow(name: String, pump: Pump, valve: Valve, flowPlan: FlowPlan)

  case class FlowPlan(name: String, minFlow: Double, maxFlow: Double)

  def setup: Net

  def calcLitersFromFlowPlan(flowPlan: FlowPlan) = {
    (flowPlan.maxFlow + flowPlan.minFlow) * 0.5
  }

  def doWater: Unit = {
    val net = setup
    checkAllOff(net)
    net.flows.foreach { case f@Flow(name, pump, valve, flowPlan) =>
      val liters = calcLitersFromFlowPlan(flowPlan)
      val waitMs = calcFlowTimeFromLiters(liters)
      println(s"watering $liters l in $waitMs ms for flow $f")
      try {
        pump.on
        valve.on

        Thread.sleep(waitMs)
      } finally {
        pump.off
        valve.off
      }
    }
  }

  private def calcFlowTimeFromLiters(liters: Double) = {
    ((liters / 15.0) * 60 * 1000).toInt
  }

  private def checkAllOff(net: Net) = {
    val relaises = net.elms.filter(_.isOn)
    assert(relaises.isEmpty, s"there were enabled elements: $relaises")
  }
}

object Main extends App with UWT {

  /*
  RaspiExample
   val Pins = RaspiPin

   implicit def r2p(p: com.pi4j.io.gpio.Pin) = new Pin {
     val digitalOutput: GpioPinDigitalOutput = GpioFactory.getInstance.provisionDigitalOutputPin(p, name, PinState.HIGH)
     off

     override def off: Unit = digitalOutput.low()

     override def on: Unit = digitalOutput.high()
   }
    */
  case class IntPin(i: Int) extends Pin {
    override def off: Unit = println(s"pin $i off")

    override def on: Unit = println(s"pin $i on")
  }

  implicit def i2p(i: Int): Pin = {
    IntPin(i)
  }

  def setup: Net = {
    val net = this.Net()

    import net.{valve, pump, flow}

    val pump1 = pump("pump", 2)
    val valve1 = valve("valve1", 3)
    val valve2 = valve("valve2", 4)
    val valve3 = valve("valve3", 14)
    val fp_hort = FlowPlan("hortensien", 1, 10)
    val fp_rose = FlowPlan("rosen", 0.5, 1)
    flow("Hortensie1", pump1, valve1, fp_hort)
    flow("Rose1", pump1, valve2, fp_rose)
    flow("Hortensie2", pump1, valve3, fp_hort)
    net
  }


  doWater

}