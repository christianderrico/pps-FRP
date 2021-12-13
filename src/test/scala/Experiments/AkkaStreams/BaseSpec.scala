package Experiments.AkkaStreams

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class BaseSpec extends AnyWordSpec with Matchers {

  implicit val testKit: ActorSystem[Nothing] = ActorTestKit().system

  protected def getElementsFromSource[A, Seq[_]](source: Source[A, _]): Future[scala.Seq[A]] = source.runWith(Sink.seq[A])

  protected def getFirstElementFromSource[A](source: Source[A, _]): Future[A] = source.runWith(Sink.head[A])

  protected def awaitForResult[A](result: Future[A], timeout: FiniteDuration = 1.seconds): A = Await.result(result, timeout)

}
