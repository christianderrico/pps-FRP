package ReactiveGameOfLife

import ReactiveGameOfLife.Controller.{Signal, StartSignal, StopSignal}
import monix.reactive.Observable
import ReactiveGameOfLife.GameOfLife.{Alive, Dead, Generation, GridDimensions, Position, Status}

import scala.concurrent.duration.DurationInt

object UpdateGameState {

  private def identity: Observable[GameOfLife] = Observable.empty[GameOfLife]

  import ReactiveGameOfLife.UpdateOps._

  private def update(previousGeneration: GameOfLife): Observable[GameOfLife] = {
    def getStatus(position: Position): Status = previousGeneration.cells(position)

    val rows = GameOfLife.gridDimensions.rows
    val columns = GameOfLife.gridDimensions.columns

    Observable.fromIterable(previousGeneration.cells)
      .mergeMap {
        case position -> status =>
          Observable.fromIterable(getNeighboursPositions(position)(GameOfLife.gridDimensions))
            .foldLeft(0)((nOfAliveNeighbours, neighbourPosition) =>
              nOfAliveNeighbours + (if (getStatus(neighbourPosition) == Alive) 1 else 0))
            .map(nAliveNeighbours => applyGameOfLifeRulesBy(nAliveNeighbours, status))
      }
      .bufferTumbling(rows * columns)
      .map(getNextGeneration(previousGeneration))
      .sample(300.millis)
  }

  def apply(tickUpdate: (Signal, GameOfLife)): Observable[GameOfLife] = {
    tickUpdate match {
      case (StartSignal, iteration) => update(iteration)
      case (StopSignal, _) => identity
    }
  }

}

object UpdateOps {

  def getNeighboursPositions(referenceCell: Position)(gridDimensions: GridDimensions): Seq[Position] = {
    def areCoordinatesLegal(row: Int, column: Int): Boolean =
      (row >= 0 && row < gridDimensions.rows) && (column >= 0 && column < gridDimensions.columns)

    def isNotCellItself(offX: Int, offY: Int): Boolean = !(offX == 0 && offY == 0)

    for (
      offsetX: Int <- -1 to 1;
      offsetY: Int <- -1 to 1;
      row = offsetX + referenceCell.row;
      column = offsetY + referenceCell.column
      if isNotCellItself(offsetX, offsetY) && areCoordinatesLegal(row, column)
    ) yield Position(row, column)
  }

  val MINIMUM_OF_ALIVE_NEIGHBOURS = 2
  val MAXIMUM_OF_ALIVE_NEIGHBOURS = 3

  def applyGameOfLifeRulesBy(nOfAliveNeighbours: Int, referenceCellStatus: Status): Status =
    (nOfAliveNeighbours, referenceCellStatus) match {
      case (n, _) if n < MINIMUM_OF_ALIVE_NEIGHBOURS || n > MAXIMUM_OF_ALIVE_NEIGHBOURS => Dead
      case (n, status) if n == MAXIMUM_OF_ALIVE_NEIGHBOURS || status == Alive => Alive
      case _ => Dead
    }

  def getNextGeneration(previous: GameOfLife)(newCellsStatus: Seq[Status]): Generation =
    Generation(
      previous.generationNumber + 1,
      previous.cells.zip(newCellsStatus).map {
        case ((previousPosition, _), newStatus) => previousPosition -> newStatus
      }.toMap
    )
}