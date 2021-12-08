package ReactiveGameOfLife
import java.awt.event.ActionEvent

import javax.swing.event.{DocumentEvent, DocumentListener}
import javax.swing.{JButton, JTextField, SwingUtilities}
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.cancelables.SingleAssignCancelable
import monix.reactive.{Observable, OverflowStrategy}

import scala.concurrent.ExecutionContext

package object Utilities {

  private class SwingContext extends ExecutionContext {
    override def execute(runnable: Runnable): Unit = SwingUtilities.invokeAndWait(runnable)
    override def reportFailure(cause: Throwable): Unit = {}
  }

  val swingScheduler: Scheduler = Scheduler(new SwingContext())

  object Implicits {

    implicit class ObjectLiftable[O](obj: O) {
      def liftToObservable: Observable[O] = Observable(obj)
      def liftToTask: Task[O] = Task(obj)
    }

    val bufferSize: Int = 100

    implicit class RichButton(button: JButton) {
      val ActionEventAsObservable: Observable[ActionEvent] =
        Observable.create(OverflowStrategy.Fail(bufferSize)) { source =>
          button.addActionListener(e => source.onNext(e))
          SingleAssignCancelable()
        }
    }

    type Text = String

    implicit class RichTextField(textField: JTextField) {
      val onTextChangeAsObservable: Observable[Text] = {
        Observable.create(OverflowStrategy.Unbounded) { source =>
          textField.getDocument.addDocumentListener(new DocumentListener {
            override def insertUpdate(e: DocumentEvent): Unit = source.onNext(textField.getText)
            override def removeUpdate(e: DocumentEvent): Unit = {}
            override def changedUpdate(e: DocumentEvent): Unit = {}
          })
          SingleAssignCancelable()
        }
      }
    }

    implicit class RichObservable[A](observable: Observable[A]) {
      def debug(f: A => Unit): Observable[A] = {
        observable.map(value => {f(value); value})
      }
    }

  }

}