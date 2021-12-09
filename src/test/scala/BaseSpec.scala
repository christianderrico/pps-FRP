import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.{Consumer, Observable}
import org.scalatest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}


trait BaseSpec extends AsyncWordSpec with Matchers {

  implicit val scheduler = Scheduler(super.executionContext)

  def headSink[A]: Consumer.Sync[A, A] = Consumer.head[A]

  def getFirstElem[A](source: Observable[A]): Future[A] = source.consumeWith(headSink).runToFuture

  def getElementsFromSource[A](source: Observable[A]): Future[List[A]] = source.toListL.runToFuture

  val secondInMilliseconds = 1000
  def getCurrentTimeInSeconds(): Long = System.currentTimeMillis() / secondInMilliseconds

  def parallelEval[A, B](value: A, doOnEval: A => B): Task[B] = {
    Task(println("Value is: " + value)) *> Task(doOnEval(value))
  }

  def checkResults[A](expected: A, obtained: Future[A]): Future[scalatest.Assertion] =
    obtained map(value => value should equal(expected))

}

object ObservableFactory {

  def getStrictTimedSource[A](iterable: Iterable[A], interval: FiniteDuration): Observable[A] = {
    Observable.zipMap2(
      Observable.fromIterable(iterable),
      Observable.interval(interval)
    )((first, _) => first)
  }

  def getRangeObservable(from: Long, to: Long): Observable[Long] = Observable.range(from, to)

}
