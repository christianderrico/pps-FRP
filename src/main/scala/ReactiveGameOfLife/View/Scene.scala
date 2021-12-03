package ReactiveGameOfLife.View

import monix.eval.Task
import monix.reactive.Observable

trait View[-Output, +Input] {

  def emitUserInput: Observable[Input]

  def render(input: Output): Task[Unit]

  def display: Task[Unit]

}
