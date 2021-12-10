package AkkaStreams

import akka.stream.scaladsl.Source
import org.reactivestreams.Subscriber

import scala.concurrent.Await
import scala.concurrent.duration.{DurationDouble, DurationInt}

class BuildingStreamsSpec extends BaseSpec {

  "An Akka Stream " should {

    val canBeBuilt = afterWord("can be built")

    "be composed by a Source " which canBeBuilt {

      "from Iterable" in {

        val source = Source(1 to 10_000)

        Await.result(getElementsFromSource(source), 1.seconds) shouldBe (1 to 10_000)
      }

      "from a single value" in {

        val source = Source.single(0)

        Await.result(getFirstElementFromSource(source), 1.seconds) shouldBe 0
      }

      val sourceFromPublisher = Source.fromPublisher((s: Subscriber[_ >: Int]) => {
        for (i <- 0 until 10) {
          s.onNext(i)
        }
        s.onComplete()
      })

      "from a Publisher " in {

        //source is not backpressuring upstream
        val result1 = getElementsFromSource(sourceFromPublisher)
        assertThrows[IllegalStateException](Await.result(result1, 1.seconds))

        val result2 = getElementsFromSource(sourceFromPublisher.throttle(1, 100.millis))
        val seq = Await.result(result2, 1.seconds)

        seq shouldBe (0 until 10)
      }

    }



  }



}
