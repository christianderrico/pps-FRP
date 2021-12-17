package Experiments.ReactiveMonix

import monix.execution.Ack.Continue
import monix.execution.Cancelable
import monix.execution.exceptions.BufferOverflowException
import monix.reactive.OverflowStrategy
import monix.reactive.observers.Subscriber

import scala.concurrent.Future

class BackPressureSpec extends BaseSpec {

  import ObservableFactory._

  "Too fast producer" can {

    "produce for a too slow consumer" which {

      val bufferCapacity = 100
      val rangeLimit = 1000
      val highVelocityEmitter = getRangeObservable(from = 0, to = rangeLimit)

      val emitFunction: Subscriber.Sync[Long] => Cancelable = producer => {
        highVelocityEmitter.map(e => producer.onNext(e.toInt))
          .toListL
          .runToFuture.onComplete(_ => producer.onComplete())
        Cancelable()
      }

      val sourceWithEmitFunction = createSourceWithBackPressurePolicy[Long](emitFunction)(_)

      "stops streaming " in {

        val obsOfFail = sourceWithEmitFunction(OverflowStrategy.Fail(bufferCapacity))

        val errorDetector = createSourceWithBackPressurePolicy[Option[Throwable]] { producer =>
          obsOfFail.subscribe(
            _ => Future(Continue),
            errorFn = e => {
              producer.onNext(Some(e))
              producer.onComplete()
            }
          )
        }(OverflowStrategy.Unbounded)

        errorDetector.headL.runToFuture.map(e => assertThrows[BufferOverflowException](throw e.get))
      }

      "discards excess elements" in {

        val obsOfClearBuffer = sourceWithEmitFunction(OverflowStrategy.ClearBuffer(bufferCapacity))
        val result = getElementsFromSource(obsOfClearBuffer)

        checkCondition[List[Long]](list => list.size < rangeLimit, result)

      }

      "process all elements" in {

        val obsOfUnbounded = sourceWithEmitFunction(OverflowStrategy.Unbounded)
        val result = getElementsFromSource(obsOfUnbounded)

        checkCondition[List[Long]](list => list.size == rangeLimit, result)

      }

    }
  }
}
