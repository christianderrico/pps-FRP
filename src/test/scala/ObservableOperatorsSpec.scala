import monix.reactive.Observable

class ObservableOperatorsSpec extends BaseSpec {

  "Observable operators" should {

    "allow to transform data" in {

      val obs = Observable.range(0, 10).dump("1)")
                          .filter(_ % 2 == 0).dump("2)")
                          .map(_ * 2).dump("3)")
                          .scan(0L)((acc, next) => acc + next).dump("4)")
                          .bufferTumbling(5).dump("5)")

      val v = getFirstElem(obs)

      v map {v => v should equal(List(0,4,12,24,40))}

    }

  }

}
