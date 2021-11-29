package ReactiveGameOfLife

import ReactiveGameOfLife.View.Start
import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.duration.DurationInt

case class Controller(view: View){

  val loop = Observable.interval(33.millis)

  def getNext(accumulator: Int, next: Int): Int = accumulator match {
    case 0 => next + 1
    case _ => accumulator + 1
  }

  def gameLoop: Task[Unit] = Observable.combineLatest2(view.startAndStopInput, loop/*, view.cellsInput*/)
                                       .collect {
                                         case (Start, _) => 0
                                       }
                                       .scan(0)(getNext)
                                       .foreachL(println)
}

object Controller {

  def apply(view: View): Controller = new Controller(view)

}
