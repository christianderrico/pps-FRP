package AkkaStreamsExamples

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Source}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

object test extends App {

  implicit val ec = ExecutionContext.global
  implicit val mat = ActorSystem.create("Materializer")

  /*Source.future(Future {Thread.sleep(3000); 10})
        .via(Flow[Int]
        .flatMapConcat(v =>
          Source(List(1,2,3,4,5,6))
            .map(e => v+e))).runForeach(println)*/

  val s = Source.unfoldResource[String, Iterator[String]](
    () => Iterator.continually(StdIn.readLine),
    iterator => Some(iterator.next()),
    _ => ()
  )
  s.runForeach(println)
}
