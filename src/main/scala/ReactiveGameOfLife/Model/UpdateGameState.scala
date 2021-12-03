package ReactiveGameOfLife.Model

import ReactiveGameOfLife.Model.GameOfLife.{Alive, Dead, Generation, GridDimensions, Position, Status}
import monix.reactive.Observable

object UpdateGameState {

  trait ModelInput
  case class UpdateRequest(currentState: GameOfLife) extends ModelInput
  case object StopRequest extends ModelInput

  import UpdateOps._

  private def identity: Observable[GameOfLife] = Observable.empty[GameOfLife]

  private def update(previousGeneration: GameOfLife): Observable[GameOfLife] = {
    def countIfAlive(position: Position): Int = previousGeneration.cells(position) match {
      case Alive => 1
      case _ => 0
    }

    val rows = GameOfLife.gridDimensions.rows
    val columns = GameOfLife.gridDimensions.columns

    Observable.fromIterable(previousGeneration.cells)
      .mergeMap {
        case (position, status) =>
          Observable.fromIterable(getNeighboursPositions(position)(GameOfLife.gridDimensions))
            .foldLeft(0)((nAlive, neighbourPosition) => nAlive + countIfAlive(neighbourPosition))
            .map(nAliveNeighbours => applyGameOfLifeRulesBy(nAliveNeighbours, status))
      }
      .bufferTumbling(rows * columns)
      .map(newStatus => getNextGeneration(previousGeneration)(newStatus))
      .sample(GameOfLife.INTERVAL_BETWEEN_GENERATION)
  }

  def apply(updateRequest: ModelInput): Observable[GameOfLife] = updateRequest match {
    case req: UpdateRequest => update(req.currentState)
    case StopRequest => identity
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