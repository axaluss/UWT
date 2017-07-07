package de.ax.uwt

/**
  * Created by nyxos on 20.06.17.
  */
object RealNet {


  def net(parent: UWT) = {
    import parent._
    val net = Net()
    import net.{flow, pump, valve}

    val pump1 = pump("pump", 2, FlowMeter("FlowMeter", 4))
    val valve1 = valve("valve1", 3)
    val valve2 = valve("valve2", 4)
    val valve3 = valve("valve3", 14)
    val fp_hort = FlowPlan("hortensien", 1, 100000)
    val fp_rose = FlowPlan("rosen", 0.5, 1)
    flow("Hortensie1", pump1, valve1, fp_hort)
    flow("Rose1", pump1, valve2, fp_rose)
    flow("Hortensie2", pump1, valve3, fp_hort)
    net
  }
}
