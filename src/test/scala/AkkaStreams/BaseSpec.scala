package AkkaStreams

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future

class BaseSpec extends AnyWordSpec with Matchers {

  implicit val testKit = ActorTestKit().system

  def getElementsFromSource[A, Seq[A]](source: Source[A, _]): Future[scala.Seq[A]] = source.runWith(Sink.seq[A])

  def getFirstElementFromSource[A](source: Source[A, _]): Future[A] = source.runWith(Sink.head[A])

}
