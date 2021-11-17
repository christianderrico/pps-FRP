import javax.swing.SwingUtilities

import scala.concurrent.ExecutionContext

package object Utilities {

  val swingScheduler = new ExecutionContext {
    override def execute(runnable: Runnable): Unit = SwingUtilities.invokeAndWait(runnable)
    override def reportFailure(cause: Throwable): Unit = {}
  }

}
