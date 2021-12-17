package ReactiveGameOfLife.ReactiveMonix.Model

import ReactiveGameOfLife.ReactiveMonix.Model.GameOfLife.{Dead, Generation, GridDimensions, Live, Position, State}
import monix.reactive.Observable

/**
 * A module used to apply game logic on request coming from Controller part of the application.
 */
object UpdateGameState {

  trait ModelInput
  case class UpdateRequest(currentState: GameOfLife) extends ModelInput
  case object StopRequest extends ModelInput

  import UpdateOps._

  /*
  *  Doesn't apply any transformation on current generation to compute next
  */
  private def identity: Observable[GameOfLife] = Observable.empty[GameOfLife]

  /*
   * Computes next generation from the previous one according Game of Life's logics
   */
  private def update(currentGeneration: GameOfLife): Observable[GameOfLife] = {
    def countIfAlive(position: Position): Int = currentGeneration.world(position) match {
      case Live => 1
      case _ => 0
    }

    val rows = GameOfLife.gridDimensions.rows
    val columns = GameOfLife.gridDimensions.columns

    Observable.fromIterable(currentGeneration.world)
      .mergeMap {
        case (position, status) =>
          Observable.fromIterable(getNeighboursPositions(position)(GameOfLife.gridDimensions))
            .foldLeft(0)((nAlive, neighbourPosition) => nAlive + countIfAlive(neighbourPosition))
            .map(nAliveNeighbours => applyGameOfLifeRulesBy(nAliveNeighbours, status))
      }
      .bufferTumbling(rows * columns)
      .map(newStatus => getNextGeneration(currentGeneration)(newStatus))
      .sample(GameOfLife.INTERVAL_BETWEEN_GENERATION)
  }

  /***
   * According to the [[UpdateRequest]], computes the next [[Generation]]
   * @param updateRequest incoming request
   * @return an [[Observable]] of next Generation
   */
  def apply(updateRequest: ModelInput): Observable[GameOfLife] = updateRequest match {
    case req: UpdateRequest => update(req.currentState)
    case StopRequest => identity
  }

}

/**
 * Game Logic utility functions.
 */
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

  def applyGameOfLifeRulesBy(nOfAliveNeighbours: Int, referenceCellStatus: State): State =
    (nOfAliveNeighbours, referenceCellStatus) match {
      case (n, _) if n < MINIMUM_OF_ALIVE_NEIGHBOURS || n > MAXIMUM_OF_ALIVE_NEIGHBOURS => Dead
      case (n, status) if n == MAXIMUM_OF_ALIVE_NEIGHBOURS || status == Live => Live
      case _ => Dead
    }

  def getNextGeneration(previous: GameOfLife)(newCellsStatus: Seq[State]): Generation =
    Generation(
      previous.generationNumber + 1,
      previous.world.zip(newCellsStatus).map {
        case ((previousPosition, _), newStatus) => previousPosition -> newStatus
      }.toMap
    )
}