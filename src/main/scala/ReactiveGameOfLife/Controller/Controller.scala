package ReactiveGameOfLife.Controller

import java.awt.Color

import ReactiveGameOfLife.Model.GameOfLife.{Alive, Board, Dead, Position, Status}
import ReactiveGameOfLife.Model.{GameOfLife, UpdateGameState}
import ReactiveGameOfLife.Model.UpdateGameState.{ModelInput, StopRequest, UpdateRequest}
import ReactiveGameOfLife.Utilities.Implicits.RichObservable
import ReactiveGameOfLife.View.View
import ReactiveGameOfLife.View.View._
import javax.swing.JButton
import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.duration.DurationInt

class Controller(view: View){

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

  private def updateModel(): Observable[GameOfLife] = processInput.mergeMap(UpdateGameState(_))

  def start: Task[Unit] =
    gameLoopEngine.zipMap(updateModel())((_, updatedModel) => updatedModel)
                  .distinctUntilChanged
                  .debug(println)
                  .doOnNext(model => view.render(model))
                  .completedL

}

object Controller {

  private val FRAMES_PER_SECONDS = 60
  private val TIME_INTERVAL = 1.seconds / FRAMES_PER_SECONDS

  def apply(view: View): Controller = new Controller(view)

  private object ImplicitConversions {

    implicit def inputToGameState(tiles: Seq[Tile]): Board = {
      def mapColorToStatus(button: JButton): Status = button.getBackground match {
        case TURNED_ON_CELLS_COLOR => Alive
        case _ => Dead
      }
      tiles.map(tile => Position(tile.row, tile.column) -> mapColorToStatus(tile.button)) toMap
    }

    implicit def statusToColor(status: Status): Color = status match {
      case Alive => TURNED_ON_CELLS_COLOR
      case _ => TURNED_OFF_CELLS_COLOR
    }

    implicit def modelToModelOnView(model: GameOfLife): ViewInput = new ViewInput {
      override val generationCount: Int = model.generationNumber
      override val tiles: BoardToDraw = model.cells.map {
        case (position: Position, status) => ((position.row, position.column), status: Color)
      }
    }
  }

}
