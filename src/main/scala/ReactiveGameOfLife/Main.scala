package ReactiveGameOfLife

import Utilities._
import cats.effect.unsafe.IORuntime

object Main {

  implicit val context: IORuntime = IORuntime.global

  def main(args: Array[String]): Unit = {
    val v = View().display().evalOn(swingScheduler).unsafeRunSync()
  }

}
