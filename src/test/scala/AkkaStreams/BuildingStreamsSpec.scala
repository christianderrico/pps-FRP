package AkkaStreams

import akka.Done
import akka.stream.{CompletionStrategy, KillSwitches, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.reactivestreams.Subscriber

import scala.concurrent.Await
import scala.concurrent.duration.{DurationDouble, DurationInt}

class BuildingStreamsSpec extends BaseSpec {

  "An Akka Stream " should {

    val beComposed = "be composed "
    val canBeBuiltAndMaterialized = afterWord("can be built and materialized")

    s"$beComposed by a couple Source-Sink " which canBeBuiltAndMaterialized {

      "from Iterable" in {
        val rangeStart = 1
        val rangeLimit = 10_000
        val source = Source(rangeStart to rangeLimit)

        Await.result(source.runWith(Sink.seq), 1.seconds) shouldBe (rangeStart to rangeLimit)
      }

      "from a single value" in {

        val source = Source.single(0)

        Await.result(source.runWith(Sink.head), 1.seconds) shouldBe 0
      }

      val rangeStart = 0
      val rangeLimit = 10

      val sourceFromPublisher = Source.fromPublisher((s: Subscriber[_ >: Int]) => {
        for (i <- rangeStart until rangeLimit) {
          s.onNext(i)
        }
        s.onComplete()
      })

      "from a Publisher " in {

        //publisher doesn't backpressure source
        val result1 = getElementsFromSource(sourceFromPublisher)
        assertThrows[IllegalStateException](Await.result(result1, 1.seconds))

        //timed-based flow control
        val result2 = getElementsFromSource(sourceFromPublisher.throttle(1, 100.millis))
        val seq = Await.result(result2, 1.seconds)

        seq shouldBe (rangeStart until rangeLimit)
      }

    }

    val can = afterWord("can")

    s"$beComposed by 3-element tuple of Source-Sink-Flow " which can {

      val startRange = 1
      val endRange = 200

      val source = Source[Int](startRange to endRange)

      "be plugged together to build a Graph Stream " in {

        val squarePow = 2

        val square = Flow[Int].map(v => Math.pow(v, squarePow).toInt)
        val sink = Sink.last[Int]

        val result = source.via(square).toMat(sink)(Keep.right).run()

        val lastSquare = Await.result(result, 1.seconds)

        lastSquare shouldBe Math.pow(endRange, squarePow)
      }

      "be manipulated controlling materialized values " in {

        val timedSource = Source.tick(0.seconds, 0.1.seconds, empty)
        val sum = Flow[Int].fold(0)((acc, value) => acc+value).zipWith(timedSource)(Keep.left)
        val sink = Sink.head[Int]

        val result = source.viaMat(sum)(Keep.right).toMat(sink)(Keep.right).run()

        Await.result(result, 1.seconds) shouldBe (endRange * (endRange + 1) / 2)
      }

    }

  }



}
