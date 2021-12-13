package ReactiveMonix

import monix.execution.Ack.Continue
import monix.reactive.observables.ConnectableObservable

import scala.concurrent.Future
import scala.concurrent.duration.{DurationDouble, DurationInt}

class HotObservableSpec extends BaseSpec {

  import ObservableFactory._

  "An hot stream " can {

    val begin = 0
    val end = 10_000

    val testList = (begin until end).toList

    "start to emit data without waiting for subscriber" in {

      val throttleTime = 0.1.seconds
      val delayTime = 1.seconds

      val nElementsToTake = 10
      val index = ((delayTime / throttleTime) - 1).toInt

      val source = getObservableFromIterable(testList).throttle(throttleTime, 1).publish
      source.connect()

      val result = getFirstElem(source.take(nElementsToTake).delayExecution(delayTime))

      checkResults(testList(index), result)

    }

    val source = getRangeObservable(begin, end).publish

    "be consumed synchronously" in {

      val result = getElementsFromSource(source)
      source.connect()

      checkResults(testList, result)

    }
  }
}
