package de.ax.uwt

/**
  * Created by nyxos on 20.06.17.
  */
object QuickTestNet {



  def net(parent:UWT)={
    import parent._
    val net = Net()
    import net.{flow, pump, valve}

    val pump1 = pump("pump", 0, FlowMeter("FlowMeter",4))
    val valve1 = valve("valve1", 2)
    val valve3 = valve("valve2", 3)
    val fp_hort = FlowPlan("hortensien", 1, 5000)
    val fp_hort2 = FlowPlan("hortensien2", 1, 7000)
    flow("Hortensie1", pump1, valve1, fp_hort)
    flow("Hortensie2", pump1, valve3, fp_hort2)
    net
  }
}
