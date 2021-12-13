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

  /**
  * define an Execution context for the scheduler that performs task to update the view
  */
  private class SwingContext extends ExecutionContext {
    override def execute(runnable: Runnable): Unit = SwingUtilities.invokeAndWait(runnable)
    override def reportFailure(cause: Throwable): Unit = {}
  }

  /**
   * this is the scheduler instance
   */
  val swingScheduler: Scheduler = Scheduler(new SwingContext())

  /**
   * TYPE ENRICHMENT (application of pimp my library pattern)
   */
  object Implicits {

    /**
     * Enriched data type with the possibility of to be lifted to Observable or Task types
     * @param obj enriched object
     * @tparam O generic type
     */
    implicit class Liftable[O](obj: O) {
      def liftToObservable: Observable[O] = Observable(obj)
      def liftToTask: Task[O] = Task(obj)
    }

    val bufferSize: Int = 100

    /**
     * Enriched JButton with an observable action listener that emits data after on click events
     * @param button enriched button
     */
    implicit class RichButton(button: JButton) {
      val ActionEventAsObservable: Observable[ActionEvent] =
        Observable.create(OverflowStrategy.Fail(bufferSize)) { source =>
          button.addActionListener(e => source.onNext(e))
          SingleAssignCancelable()
        }
    }

    type Text = String

    /**
     * Enriched JTextfield with an observable action listener that emits data after on text change events
     * @param textField enriched textfield
     */
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

    /**
     * Enriched Observable with debug operator for debug operation
     * @param observable enriched observable
     * @tparam A type of value lifted to Observable
     */
    implicit class RichObservable[A](observable: Observable[A]) {
      def debug(f: A => Unit): Observable[A] = {
        observable.map(value => {f(value); value})
      }
    }

  }

}