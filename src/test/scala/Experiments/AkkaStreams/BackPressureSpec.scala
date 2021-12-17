package Experiments.AkkaStreams

import akka.stream.{ClosedShape, OverflowStrategy}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}

import scala.concurrent.{Future, TimeoutException}
import scala.concurrent.duration.DurationInt

class BackPressureSpec extends BaseSpec {

  private def createLoopGraph[A](f1: Flow[A, A, _], f2: Flow[A, A, _], start: A = 0): RunnableGraph[Future[A]] = {

    val sink = Sink.head[A]

    RunnableGraph.fromGraph(GraphDSL.createGraph(f1, f2, sink)((_, _, s) => s){ implicit builder =>
      (add, gate, s) =>
        import GraphDSL.Implicits._

        val source = Source.single(start)

        val broad = builder.add(Broadcast[A](2))
        val merge = builder.add(Merge[A](2))

        source ~> merge.in(0)
                  merge.out          ~> broad.in
                                        broad.out(0) ~> gate ~> s
                  merge.in(1) <~ add <~ broad.out(1)

        ClosedShape
    })

  }

  val incrementOfUnity: Int => Int = _+1
  val threshold = 50

  private val increment = Flow[Int].map(incrementOfUnity)
  private val filter = Flow[Int].filter(_ > threshold)

  "Handling streams with asynchronous backpressure " can {

    "be dangerous because it can cause deadlock if all internal buffers get full, " +
      "stopping source with backpressure forever" in {

      val graph = createLoopGraph[Int](increment, filter)

      assertThrows[TimeoutException](awaitForResult(graph.run(), 5.seconds))

    }

  }

   "Especially, when dataflow runs in loop " + it should {

     "be handled with Buffer Overflow Strategy, to avoid deadlock" in {

       val bufferDim = 100
       val incrementWithBuffer = increment.buffer(bufferDim, OverflowStrategy.dropHead)

       val graph = createLoopGraph[Int](incrementWithBuffer, filter)
       val materializedValue = graph.run()
       val numberAfterThreshold = awaitForResult(materializedValue)

       numberAfterThreshold shouldBe threshold + 1
     }

   }

}
