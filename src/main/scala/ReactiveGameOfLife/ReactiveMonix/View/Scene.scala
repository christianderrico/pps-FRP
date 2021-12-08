package ReactiveGameOfLife.View

import monix.eval.Task
import monix.reactive.Observable

trait Scene[-Input, +Output] {

  def emit: Observable[Output]

  def render(input: Input): Task[Unit]

  def display: Task[Unit]

}
