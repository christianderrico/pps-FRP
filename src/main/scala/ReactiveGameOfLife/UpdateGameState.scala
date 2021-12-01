package ReactiveGameOfLife

import ReactiveGameOfLife.Controller.{Signal, StartSignal, StopSignal}
import monix.reactive.Observable
import ReactiveGameOfLife.GameOfLife.{Alive, Dead, GridDimensions, Position, Status}

import scala.concurrent.duration.DurationInt

object Model {

  private def identity(init: Int): Observable[Int] =
    Observable(init)

  private def update(init: Int): Observable[Int] =
    Observable(init).map(_ + 1)

  def apply(tick: (Signal, Int)): Observable[Int] = {
    println(tick._1)
    tick match {
      case (StartSignal, value) => update(value)
      case (StopSignal, value) => identity(value)
    }
  }

}

object ModelOps {

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

}