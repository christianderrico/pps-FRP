import java.awt.event.ActionEvent

import ReactiveGameOfLife.View.{Signal, Start, Stop}
import akka.actor.ActorSystem
import javax.swing.{JButton, SwingUtilities}
import monix.execution.Scheduler
import monix.execution.cancelables.SingleAssignCancelable
import monix.reactive.OverflowStrategy.{BackPressure, Unbounded}
import monix.reactive.{Observable, OverflowStrategy}

import scala.concurrent.{ExecutionContext, Future}

package object Utilities {

  implicit val system: ActorSystem = ActorSystem("ActorPool")

  val swingScheduler = Scheduler(new ExecutionContext {
    override def execute(runnable: Runnable): Unit = SwingUtilities.invokeAndWait(runnable)
    override def reportFailure(cause: Throwable): Unit = {}
  })

  object Implicits {

    val bufferSize: Int = 100

    implicit class richButton(button: JButton){
      val liftedActionEvent: Observable[ActionEvent] =
        Observable.create(OverflowStrategy.Fail(bufferSize)) { source =>
          button.addActionListener(e => source.onNext(e))
          SingleAssignCancelable()
      }
    }

  }

}
