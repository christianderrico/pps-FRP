import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.duration.DurationDouble

class ObservableOperatorsSpec extends BaseSpec {

  import ObservableFactory._
  private def provide = afterWord("provide")

  "Monix Observable abstraction " should provide {

    "operators " which {

      "manipulate data flows " in {

        val obs = getRangeObservable(from = 0, to = 10).dump("1)")
          .filter(_ % 2 == 0).dump("2)")
          .map(_ * 2).dump("3)")
          .scan(0L)((acc, next) => acc + next).dump("4)")
          .bufferTumbling(5).dump("5)")

        val result = getFirstElem(obs)

        checkResults(expected = List(0, 4, 12, 24, 40), obtained = result)

      }

      val begin = 0
      val end = 50

      val incrementOfOneUnit: Long => Task[Long] = x => parallelEval[Long, Long](x, value => value + 1)
      val parallelism = 4
      val expectedResult = ((begin+1) until (end+1)).toList
      val rangeObs = getRangeObservable(begin, end)

      "asynchronously transform data in parallel " in {

        val parallelOrderedObs = rangeObs.mapParallelOrdered(parallelism)(incrementOfOneUnit)
                                         .dump("O")

        val result = getElementsFromSource(parallelOrderedObs)

        checkResults(expected = expectedResult, obtained = result)

      }

      "synchronously transform data in parallel " in {

        val parallelOrderedObs = rangeObs.mapParallelUnordered(parallelism)(incrementOfOneUnit)
                                         .dump("O")

        val result = getElementsFromSource(parallelOrderedObs)

        checkResults(expected = expectedResult, obtained = result)

      }

/*
      val sourceA = getStrictTimedSource(List(0, 1, 2, 3), 0.3.seconds)
      val sourceB = getStrictTimedSource(List(5, 6, 7), 0.8.seconds)

      def differentSources = "different sources"

      s"neatly concat $differentSources with buffering" in {

        val concatObservable = sourceA.concatMap(v => sourceB.map(elem => (v, elem)))
        val result = getElementsFromSource(concatObservable)
        val expectedResult = List((0,5), (0,6), (0,7),
                                  (1,5), (1,6), (1,7),
                                  (2,5), (2,6), (2,7),
                                  (3,5), (3,6), (3,7))

        checkResults(expectedResult, result)
      }

      s"concat $differentSources without buffering and privileging most-recently-emitted" in {

        val switchedObservable = sourceA.switchMap(v => sourceB.map(elem => (v, elem)))
        val result = getElementsFromSource(switchedObservable)
        val expectedResult = List((0,5), (1,5), (2,5), (3,5), (3,6), (3,7))

        checkResults(expectedResult, result)

      }

      s"messily merge $differentSources" in {

        val mergedObservable = sourceA.mergeMap(v => sourceB.map(elem => (v, elem)))
        val result = getElementsFromSource(mergedObservable)
        val expectedResult = List((0, 5), (1, 5), (2, 5),
          (0, 6), (3, 5), (1, 6),
          (2, 6), (0, 7), (3, 6),
          (1, 7), (2, 7), (3, 7))

        checkResults(expectedResult, result)
      }
*/

    }
  }

}
