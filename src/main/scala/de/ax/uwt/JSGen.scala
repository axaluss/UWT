package de.ax.uwt

import java.util.Locale

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.io.Source
import com.github.nscala_time.time.Imports._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Json

import scala.collection.immutable

/**
  * Created by nyxos on 13.07.17.
  */
object JSGen  extends LazyLogging{

  /*
{
               labels: ["Red", "Blue", "Yellow", "Green", "Purple", "Orange"],
               datasets: [{
                   label: '# of Votes',
                   data: [12, 19, 3, 5, 2, 3],
                   backgroundColor: [
                       'rgba(255, 99, 132, 0.2)',
                       'rgba(54, 162, 235, 0.2)',
                       'rgba(255, 206, 86, 0.2)',
                       'rgba(75, 192, 192, 0.2)',
                       'rgba(153, 102, 255, 0.2)',
                       'rgba(255, 159, 64, 0.2)'
                   ],
                   borderColor: [
                       'rgba(255,99,132,1)',
                       'rgba(54, 162, 235, 1)',
                       'rgba(255, 206, 86, 1)',
                       'rgba(75, 192, 192, 1)',
                       'rgba(153, 102, 255, 1)',
                       'rgba(255, 159, 64, 1)'
                   ],
                   borderWidth: 1
               }]
           }
 */

  def genJson(pi: HasHistory): String = {
    val times = pi.flowHistory.map(_.time)
    if (times.nonEmpty) {
      val min = times.min
      val max = times.max
      val range = max - min
      val stepCount = 56
      val step = range / stepCount
      //    val stepsToTimes = times.groupBy(t => ((t - min) / step))
      val steps = 0.to(stepCount).toList.map(i => min + (i * step))
      val labels = steps
      val flowNameToFlowHistoryEntries = pi.flowHistory.groupBy(_.f.name)


      val flowsToValues: Map[String, Seq[Double]] = flowNameToFlowHistoryEntries.map {
        case (flowName, entries) =>
          val stepsToEntries = entries.groupBy(e => min + (((e.time - min) / step) * step))
          logger.info(s"sorted entries: ${stepsToEntries.keys.toList.sorted}")
          logger.info(s"steps: $steps")
          val list: Seq[Double] = steps.map(s =>
            stepsToEntries.get(s).map(_.map(_.actualLiters).sum)
              .getOrElse(0.0))
          (flowName, list)
      }
      val flowsToDataSets: Map[String, Json] = flowsToValues.map {
        case (flowName, values) =>
          (flowName, Map("labels" -> steps.map(s => DateTimeFormat.forStyle("SS").withLocale(Locale.GERMANY).print(s)).asJson,
            "dataSets" -> List(Map("data" -> values.map(v => Math.round(v * 100.0) / 100.0).asJson)).asJson).asJson)
      }

      Map("data" -> flowsToDataSets.asJson).asJson.toString()
    } else {
      "{}"
    }
  }

  object TestJS extends App with LazyLogging {
    val weekInMs: Long = 1000L * 60L * 60L * 24L * 7L
    val step: Long = weekInMs / 10L
    private val history = new HasHistory {
      var times = 1.to(100).map(i => System.currentTimeMillis + (step * i))

      override def curMs: Long = {
        val h = times.head
        times = times.tail
        h
      }

      1.to(99).foreach(i => {
        flowHistory = FlowHistoryEntry(new FlowLike {
          override def name: String = "myFlow"
        }, i, 10) +: flowHistory
      })

      logger.info(s"history: $flowHistory")
    }

    logger.info(JSGen.genJson(history))
  }

}