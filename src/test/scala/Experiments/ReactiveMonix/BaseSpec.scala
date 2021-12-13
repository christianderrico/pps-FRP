package ReactiveMonix

import monix.execution.{Cancelable, Scheduler}
import monix.reactive.observers.Subscriber
import monix.reactive.{Observable, OverflowStrategy}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait BaseSpec extends AsyncWordSpec with Matchers {

  implicit val scheduler = Scheduler(super.executionContext)

  def getFirstElem[A](source: Observable[A]): Future[A] = source.headL.runToFuture

  def getElementsFromSource[A](source: Observable[A]): Future[List[A]] = source.toListL.runToFuture

  def checkCondition[A](expected: A => Boolean, obtained: Future[A]) =
    obtained map(value => assert(expected(value)))

  def checkResults[A](expected: A, obtained: Future[A]) =
    obtained map(value => value should equal(expected))

}

object ObservableFactory {

  def getObservableFromIterable[A](elems: Iterable[A]): Observable[A] = Observable.fromIterable(elems)

  def getStrictTimedSource[A](iterable: Iterable[A], interval: FiniteDuration): Observable[A] = {
    Observable.zipMap2(
      getObservableFromIterable(iterable),
      Observable.interval(interval)
    )((first, _) => first)
  }

  def getRangeObservable(from: Long, to: Long): Observable[Long] = Observable.range(from, to)

  def createSourceWithBackPressurePolicy[A](subFunction: Subscriber.Sync[A] => Cancelable)
                                           (overflowStrategy: OverflowStrategy.Synchronous[A]): Observable[A] =
    Observable.create(overflowStrategy)(subFunction)

}
