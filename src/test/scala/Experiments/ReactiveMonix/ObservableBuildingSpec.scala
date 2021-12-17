package Experiments.ReactiveMonix

import Experiments.ReactiveMonix.ObservableFactory.getStrictTimedSource
import monix.eval.Task
import monix.execution.Cancelable
import monix.reactive.{Observable, OverflowStrategy}

import scala.concurrent.duration.{DurationDouble, DurationInt}

class ObservableBuildingSpec extends BaseSpec {

  val testString = "test"

  "An Observable" should {

    val beBuiltFrom = "be built from"
    val beBuiltAs = "be built as"

    s"$beBuiltFrom simple object" in {
      val obsFromString = Observable(testString)
      val result = getFirstElem(obsFromString)

      checkResults(expected = testString, obtained = result)
    }

    s"$beBuiltFrom Iterable" in {
      val obsFromString = Observable.fromIterable(testString)
      val result = getFirstElem(obsFromString)

      checkResults(expected = testString.charAt(0), obtained = result)
    }

    "be created from subscriber" in {
      val elemNumber = 10
      val obs = Observable.create[Int](OverflowStrategy.Unbounded){ source =>
        for(i <- 0 until elemNumber)
          source.onNext(i)

        source.onComplete()

        Cancelable()
      }

      val result = getElementsFromSource(obs)

      checkResults(expected = (0 until elemNumber).toList, obtained = result)

    }

    s"$beBuiltFrom two sources" in {

      val obsFromZippedSources = Observable.zipMap2(
        Observable.fromIterable(List(1,2,3,4)),
        Observable.fromIterable(List(1,2,3,4))
      )(_+_)

      val result = getElementsFromSource(obsFromZippedSources)

      checkResults(expected = List(2,4,6,8), obtained = result)

    }

    val secondInMilliseconds = 1000
    def getCurrentTimeInSeconds: Long = System.currentTimeMillis() / secondInMilliseconds

    s"$beBuiltAs a strict timed sequence" in {
      val timedObs = Observable.zipMap2(
        Observable.fromIterable(List(0,1,2,3)),
        Observable.interval(1.seconds)
      )((_, _) => getCurrentTimeInSeconds)

      val result = getElementsFromSource(timedObs)

      checkCondition[List[Long]](l => l.last - l.head == l.indices.last, result)

    }

    s"$beBuiltAs as a relaxed timed sequence" in {

      val timedObs1 = getStrictTimedSource(List(1,5), 1.seconds)
      val timedObs2 = getStrictTimedSource(List(2,3,4,6,7,8), 0.3.seconds)

      val combinedObs = Observable.combineLatest2(timedObs1, timedObs2)

      val result = getElementsFromSource(combinedObs)

      checkResults(expected = List((1,2),(1,3),(1,4),(1,6),(5,6),(5,7),(5,8)), obtained = result)

    }

    s"$beBuiltFrom various prioritized streams of data" in {
      val high = 3
      val medium = 2
      val low = 1

      val listOfObservables = List(
        (high, Observable.fromIterable(List(1,5,8,11))),
        (medium, Observable.fromIterable(List(2,6,9,12,13))),
        (low, Observable.fromIterable(List(3,7,10))),
        (low, Observable(4))
      )

      val obsFromList = Observable.mergePrioritizedList(listOfObservables:_*)

      val result = getElementsFromSource(obsFromList)

      checkResults(expected = List.from(1 to 13), obtained = result)
    }

    s"$beBuiltFrom async state function" in {
      val obsFromStateFunc = Observable.fromAsyncStateAction {
        counter: Int =>
          val previous = counter
          val next = counter + 1
          Task((previous, next))
      }(0).take(3)

      val result = getElementsFromSource(obsFromStateFunc)

      checkResults(expected = List(0, 1, 2), obtained = result)
    }

  }
}
