package ReactiveGameOfLife

import ReactiveGameOfLife.View.View
import monix.execution.Scheduler

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {

  def main(args: Array[String]): Unit = {

    implicit val scheduler: Scheduler = monix.execution.Scheduler.global

    val gridDimension = GameOfLife.gridDimensions

    val view = View(gridDimension)
    Await.result(Controller(view).start.runToFuture, Duration.Inf)
  }

}
