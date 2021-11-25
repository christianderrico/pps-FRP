package ReactiveGameOfLife

import akka.stream.{ClosedShape, Graph}
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Source, ZipWith}

import scala.concurrent.duration.{DurationInt, FiniteDuration, MILLISECONDS, TimeUnit}

object Controller {

  case class Config(sleepTime: FiniteDuration = 16.millis)

  def apply(config: Config): Unit = {
    RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      import Utilities.Implicits._

      /*val timeSleepCalculator = b.add(ZipWith(
        (t1: FiniteDuration, t2: FiniteDuration) => FiniteDuration(t1.time - t2.time, MILLISECONDS))
      )*/

      def waitForNextElement(standardTime: FiniteDuration, deltaTime: FiniteDuration) =
        if(deltaTime.time > standardTime.time)
          standardTime
        else
          standardTime - deltaTime


      /*val engine = (Source.repeat()
                           >>= (_ => println("Process Input"))
                           >>= (_ => getClock)
                           >>= (prevTime => { println("update model"); prevTime })
                           >>= (prevTime => (prevTime, getClock))
                           >>= (t => t._1 - t._2)
        )*/

      //engine ~> timeSleepCalculator

      ClosedShape
    })
  }

  private val getClock = FiniteDuration(System.currentTimeMillis(), MILLISECONDS)

}
