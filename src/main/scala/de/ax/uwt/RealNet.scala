package de.ax.uwt

/**
  * Created by nyxos on 20.06.17.
  */
object RealNet {


  def net(parent: UWT) = {
    import parent._
    val net = Net()
    import net.{flow, pump, valve, moistureSensor, switch,flowMeter}

    val switch12V = switch("switch12V", 0)
    net.switch12v(switch12V)
    val switchHygro = switch("switchHygro", 5)
    val mSensor = moistureSensor("moistureSensor", 6, switchHygro)
    val flowMeterMeter = flowMeter("FlowMeter", 27)
    val pump1 = pump("pump", 2, flowMeterMeter)
    val valve1 = valve("valve1", 3)
    val valve2 = valve("valve2", 4)
    val valve3 = valve("valve3", 14)

    val fp_hort = FlowPlan("hortensien", 1, 5)
    val fp_rose = FlowPlan("rosen", 0.5, 2)

    flow("Hortensie1", pump1, valve1, fp_hort, mSensor)
    flow("Rose1", pump1, valve2, fp_rose, mSensor)
    flow("Hortensie2", pump1, valve3, fp_hort, mSensor)
    net
  }
}
