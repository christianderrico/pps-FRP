package MonixReactiveExamples

import monix.reactive.Observable

object MeaningfulObservableFactory {

  def pureObservable[A](single: A): Observable[A] = Observable(single)

  def fromIterable[A](single: Iterable[A]): Observable[A] = Observable.fromIterable(single)

  def fromPrioritizedList[A](list: List[(Int, Observable[A])]): Observable[A] = Observable.mergePrioritizedList(list:_*)

}