import monix.eval.Task
import monix.execution.Cancelable
import monix.reactive.{Observable, OverflowStrategy}

import scala.concurrent.duration.{DurationDouble, DurationInt}

class ObservableSpec extends BaseSpec {

  val testString = "test"

  "An Observable" should {

    "be built from simple object" in {
      val obsFromString = Observable(testString)
      val textFuture = getFirstElem(obsFromString)

      textFuture map {value => value shouldEqual testString}
    }

    "be built from Iterable" in {
      val obsFromString = Observable.fromIterable(testString)
      val textFuture = getFirstElem(obsFromString)

      textFuture map {value => value shouldEqual testString.charAt(0)}
    }

    "be created from subscriber" in {
      val elemNumber = 10
      val obs = Observable.create[Int](OverflowStrategy.Unbounded){ source =>
        for(i <- 0 until elemNumber)
          source.onNext(i)

        source.onComplete()
        Cancelable()
      }

      val res = getElementsFromSource(obs)

      res map {r => r shouldBe (0 until elemNumber).toList}

    }

    "be built from two sources" in {

      val obsFromZippedSources = Observable.zipMap2(
        Observable.fromIterable(List(1,2,3,4)),
        Observable.fromIterable(List(1,2,3,4))
      )(_+_)

      val res = getElementsFromSource(obsFromZippedSources)

      res map {seq => seq shouldBe List(2,4,6,8)}

    }

    "be built as a strict timed sequence" in {
      val timedObs = Observable.zipMap2(
        Observable.fromIterable(List(0,1,2,3)),
        Observable.interval(1.seconds)
      )((_, _) => getCurrentTimeInSeconds())

      val res = getElementsFromSource(timedObs)

      res map {list => list.last - list.head shouldBe list.size - 1}

    }

    "be built as a relaxed timed sequence" in {

      val timedObs1 = getStrictTimedSource(List(1,5), 1.seconds)
      val timedObs2 = getStrictTimedSource(List(2,3,4,6,7,8), 0.3.seconds)

      val combinedObs = Observable.combineLatest2(timedObs1, timedObs2)

      val res = getElementsFromSource(combinedObs)

      res map {value => value shouldBe List((1,2),(1,3),(1,4),(1,6),(5,6),(5,7),(5,8))}

    }

    "be built from various prioritized streams of data" in {
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

      val res = getElementsFromSource(obsFromList)

      res map {elements => elements shouldBe List.from(1 to 13)}
    }

    "be built from async state function" in {
      val obsFromStateFunc = Observable.fromAsyncStateAction {
        counter: Int =>
          val previous = counter
          val next = counter + 1
          Task((previous, next))
      }(0).take(3)

      val res = getElementsFromSource(obsFromStateFunc)

      res map {seq => seq shouldBe List(0, 1, 2)}
    }

  }
}
