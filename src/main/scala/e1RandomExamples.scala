import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source, Zip}
import org.reactivestreams.{Subscriber, Subscription}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Success

object e1RandomExamples extends App {

  implicit val system: ActorSystem = ActorSystem("StreamDispatcher")
  implicit val executionContext: ExecutionContext = ExecutionContext.global

  def awaitResult[T](f: Future[T]) = {
    Await.ready(f, Duration.Inf)
  }

  implicit class richInt(value: Int) {
    private def square(degree: Int): Int = degree match {
      case 0 => 1
      case 1 => value
      case _ => value * square(degree - 1)
    }
    def **(degree: Int) = square(degree)
  }

  val strings:List[String] = List("ciao", "Hello", "world")

  //simple launch of source with runForeach
  val f1 = Source(strings).runForeach(println(_))

  awaitResult(f1)

  //simple launch with sink
  val f2 = Source("ciao Hello world").runWith(Sink.foreach({
    println(Thread.currentThread())
    println(_)
  }))

  awaitResult(f2)

  //test with publisher and subscribe
  val publisher1 = Source(strings).runWith(Sink.asPublisher(false))

  publisher1.subscribe(new Subscriber[String] {
    override def onSubscribe(s: Subscription): Unit = s.request(Long.MaxValue) //max of elements that can be asked
    override def onNext(t: String): Unit = println("> " + t)
    override def onError(t: Throwable): Unit = println("error " + t)
    override def onComplete(): Unit = println("complete")
  })

  Source(1 to 10).toMat(Sink.seq)(Keep.right).run().andThen {
    case Success(list) => println(list)
    case _ => println("Error in future")
  }

  val publisher2 = Source(1 to 20).map(_ ** 2).filter(_ % 3 == 0).runWith(Sink.asPublisher(true))

  //subscriber with pluggable on Next logic
  case class OurSubscriber[T](name: String, doOnNext: T => Unit) extends Subscriber[T] {
    override def onNext(t: T): Unit = doOnNext(t)
    override def onSubscribe(s: Subscription): Unit = s.request(Long.MaxValue)
    override def onError(t: Throwable): Unit = t.printStackTrace()
    override def onComplete(): Unit = println(name + " completes operations")
  }

  //Companion object with factory
  object OurSubscriber {
    def apply[T](name: String, doOnNext: T => Unit): OurSubscriber[T] = new OurSubscriber(name, doOnNext)
  }

  println("First subscription")
  publisher2.subscribe(OurSubscriber[Int]("First subscriber", println))

  println("Second subscription")
  publisher2.subscribe(OurSubscriber[Int]("Second subscriber", v => println("Value: " + v)))*/

  //extended doOnNext method for debug and little DSL pimping Akka streams
  implicit class RichSource[In, M](val source: Source[In, M]) {
    def doOnNext(peek: In => Unit): Source[In, M] = source.map({ v => peek(v); v}) //print value for debug
    def >(peek: In => Unit): Source[In, M] = doOnNext(peek)
    def >>[T](mapper: In => T): Source[T, M] = source.map(mapper)
    def !!(filter: In => Boolean): Source[In, M] = source.filter(filter)
  }

  def printStep[T](n: Int)(arg: T) = println(n + ") " + arg)

  val source = (Source(1 to 10) > printStep(1)
    > printStep(2) >> (_ ** 2)
    > printStep(3) !! (_ % 3 == 0) runForeach(println))

  //simple composition
  val source1 = Source(0 until 3)
  val source2 = Source(strings)
  val zip = Zip()

  source1.zipWith(source2)((id, word) => id + ") " + word).runForeach(println)
}
