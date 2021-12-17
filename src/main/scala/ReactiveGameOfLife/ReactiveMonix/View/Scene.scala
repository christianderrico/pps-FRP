package ReactiveGameOfLife.ReactiveMonix.View

import monix.eval.Task
import monix.reactive.Observable

/**
 * It models the view part of the application. In a Functional Reactive Programming style, based on Reactive Monix,
 * view emits output (user inputs) to controller as an [[Observable]] stream of datas and performs side-effects
 * using [[Task]] abstraction, perfectly integrated with Monix ecosystem.
 *
 * @tparam Input view's input coming from controller
 * @tparam Output view's output directed to controller
 */
trait Scene[-Input, +Output] {

  /**
   * Emits output as [[Observable]] instance
   * @return output as [[Observable]]
   */
  def emit: Observable[Output]

  /**
   * Gets controller's output and draws it
   * @param input view's input to be rendered
   * @return a Task wrapping IO computation
   */
  def render(input: Input): Task[Unit]

  /**
   * Shows view on application start
   * @return a Task wrapping IO computation
   */
  def display: Task[Unit]

}
