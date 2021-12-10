package AkkaStreamsExamples

import akka.actor.ActorSystem
import akka.stream.KillSwitches
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.reactivestreams.{Publisher, Subscriber}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

object test extends App {

  implicit val ec = ExecutionContext.global
  implicit val mat = ActorSystem("Materializer")

  /*
  Primo esempio
   */
  /*val s1 = Source.fromPublisher((s: Subscriber[_ >: Int]) => {
    for (i <- 0 to 10) {
      s.onNext(i)
    }
    s.onComplete()
  }).throttle(1, 100.millis)

  val result = s1.runWith(Sink.seq[Int])

  val seq = Await.result(result, 4.seconds)

  result.onComplete(_ => mat.terminate())

  println(seq)*/

  /*
  Secondo esempio
   */
  /*
  val result = Source(1 to 10).runWith(Sink.seq[Int])

  val seq = Await.result(result, 1.seconds)

  result.onComplete(_ => mat.terminate())

  println(seq)
   */

  /*
  Terzo esempio
   */

  /*
  val result = Source.single(0).runWith(Sink.head)

  val elem = Await.result(result, 1.seconds)

  result.onComplete(_ => mat.terminate())

  println(elem)

   */

  val source = Source[Int]((1 to 200).toList)

  val pipe = Flow[Int]
              .map(_*2)
              .filter {println("B: " + Thread.currentThread()); _%10 == 0}
              .map(v => {println("A: " + Thread.currentThread()); v})
              .viaMat(KillSwitches.single)(Keep.right)

  val res = source.viaMat(pipe)(Keep.right).toMat(Sink.foreach(println))(Keep.both)

  val (graph, done) = res.run()

  done.onComplete(_ => {
    graph.shutdown()
    mat.terminate()
  })

  Await.result(done, 1.seconds)


}
