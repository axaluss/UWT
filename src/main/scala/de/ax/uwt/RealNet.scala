package de.ax.uwt

/**
  * Created by nyxos on 20.06.17.
  */
object RealNet {


  def net(parent: UWT) = {
    import parent._
    val net = Net()
    import net.{flow, pump, valve, moistureSensor => mkMoistureSensor, switch, flowMeter}

    val switch12V = switch("switch12V", 0)
    net.switch12v(switch12V)
    val switchMoistureSensor = switch("switchMoistureSensor", 4)
    val moistureSensor = mkMoistureSensor("moistureSensor", 15, switchMoistureSensor, activated = false)
    val flowMeterMeter = flowMeter("FlowMeter", 3)
    val pump1 = pump("pump", 2, flowMeterMeter)
    val valve1 = valve("valve1", 10)
    val valve2 = valve("valve2", 5)
    val valve3 = valve("valve3", 9)
    val valve4 = valve("valve4", 8)
    val valve5 = valve("valve5", 6)
    val valve6 = valve("valve6", 7)

    val fpHort = FlowPlan("hortensien", 1, 5)
    val fpRoseKlein = FlowPlan("rosen klein", 0.5, 2)
    val fpRoseGroß = FlowPlan("rosen groß", 0.5, 2)
    val fpGemüse = FlowPlan("Gemüse", 0.5, 2)

    flow("Hortensie links", pump1, valve1, fpHort, moistureSensor)
    flow("Rose klein", pump1, valve2, fpRoseKlein, moistureSensor)
    flow("Hortensie mitte", pump1, valve3, fpHort, moistureSensor)
    flow("Gemüse", pump1, valve4, fpGemüse, moistureSensor)
    flow("Hortensie rechts", pump1, valve5, fpHort, moistureSensor)
    flow("Rose Groß", pump1, valve6, fpRoseGroß, moistureSensor)
    net
  }
}
