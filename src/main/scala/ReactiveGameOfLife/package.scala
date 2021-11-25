import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import javax.swing.{AbstractButton, JButton, SwingUtilities}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

package object Utilities {

  implicit val system: ActorSystem = ActorSystem("ActorPool")

  val swingScheduler = new ExecutionContext {
    override def execute(runnable: Runnable): Unit = SwingUtilities.invokeAndWait(runnable)
    override def reportFailure(cause: Throwable): Unit = {}
  }

  object Implicits {

    implicit class richSource[In, M](source: Source[In, M]){
      def doOnNext(f: In => Unit): Source[In, M] = source.map(elem => {f(elem); elem})
      def >>=[T](f: In => T): Source[T, M] = source.map(f)
    }

    implicit class richFiniteDuration(finiteTime: FiniteDuration){
      val time = finiteTime._1;
    }

  }

}
