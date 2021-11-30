package ReactiveGameOfLife

import ReactiveGameOfLife.GameOfLife.{Alive, Dead, GridDimensions, Position, Status}

object GameOfLifeLogic {

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
