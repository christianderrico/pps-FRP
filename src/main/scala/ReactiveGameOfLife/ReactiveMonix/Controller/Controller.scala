package ReactiveGameOfLife.Controller

import java.awt.Color

import ReactiveGameOfLife.Model.GameOfLife.{Live, Board, Dead, Position, State}
import ReactiveGameOfLife.Model.{GameOfLife, UpdateGameState}
import ReactiveGameOfLife.Model.UpdateGameState.{ModelInput, StopRequest, UpdateRequest}
import ReactiveGameOfLife.Utilities.Implicits.RichObservable
import ReactiveGameOfLife.View.View
import ReactiveGameOfLife.View.View._
import javax.swing.JButton
import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.duration.DurationInt

/*
 * In details, Controller is thought as a reactive game loop, which collects input coming from view, adapts it
 * to be elaborated according to the Game Logic, and then, re-elaborated to be draw by the view.
 */
private class Controller(view: View){

  import Controller.ImplicitConversions._
  import Controller._

  private val gameLoopEngine = Observable.interval(TIME_INTERVAL).doOnStart(_ => view.display)

  private def processInput: Observable[ModelInput] =
    view.emit.collect {
      case request: CycleComputationRequest => request.cmd match {
        case View.START => UpdateRequest(GameOfLife.Generation(request.genNumber, request.tiles))
        case _ => StopRequest
      }
    }

  private def updateModel(): Observable[GameOfLife] = processInput.switchMap(UpdateGameState(_))

 /*
  * Proactive version of the game loop: game loop is not waiting for user's input
  */
  private val proactiveLoop: Observable[GameOfLife] =
    gameLoopEngine
      .combineLatestMap(updateModel())((_, updatedModel) => updatedModel)
      .debug(println)
      .distinctUntilChanged

  /*
  * Reactive version of the game loop: game loop runs only after user's request
  */
  private val reactiveLoop: Observable[GameOfLife] =
    gameLoopEngine
      .zipMap(updateModel())((_, updatedModel) => updatedModel)
      .debug(println)

  def start: Task[Unit] = proactiveLoop.doOnNext(model => view.render(model)).completedL

}

/**
 * It models the controller part of the application.
 */
object Controller {

  private val FRAMES_PER_SECONDS = 60
  private val TIME_INTERVAL = 1.seconds / FRAMES_PER_SECONDS

  def apply(view: View): Task[Unit] = new Controller(view).start

  /**
   * According to a Model-View-Adapter interpretation of MVC, Controller knows protocols used by Model and View, and it's
   * man-in-the-middle for the communication between them, which don't speak directly between them
   */
  private object ImplicitConversions {

    //view sequence of colored buttons is map to a logical board
    implicit def viewOutputToGameState(tiles: Seq[Tile]): Board = {
      def mapColorToStatus(button: JButton): State = button.getBackground match {
        case TURNED_ON_CELLS_COLOR => Live
        case _ => Dead
      }
      tiles.map(tile => Position(tile.row, tile.column) -> mapColorToStatus(tile.button)) toMap
    }

    //Colors are the visual representation of cell state
    implicit def statusToColor(status: State): Color = status match {
      case Live => TURNED_ON_CELLS_COLOR
      case _ => TURNED_OFF_CELLS_COLOR
    }

    //Game of life gen is "adapted" for the view in order to be drawn
    implicit def modelToModelOnView(model: GameOfLife): ViewInput = new ViewInput {
      override val generationCount: Int = model.generationNumber
      override val tiles: BoardToDraw = model.cells.map {
        case (position: Position, status) => ((position.row, position.column), status: Color)
      }
    }
  }

}
