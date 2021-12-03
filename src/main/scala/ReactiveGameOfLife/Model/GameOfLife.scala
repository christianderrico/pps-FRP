package ReactiveGameOfLife.Model

import ReactiveGameOfLife.Model.GameOfLife.Board
import cats.Eq

import scala.concurrent.duration.{DurationInt, FiniteDuration}

sealed trait GameOfLife {
  def cells: Board
  def generationNumber: Int
}

object GameOfLife {

  case class Generation(generationNumber: Int, cells: Board) extends GameOfLife
  implicit val eqGameOfLife: Eq[GameOfLife] =
    (x: GameOfLife, y: GameOfLife) => x.generationNumber == y.generationNumber

  type Board = Map[Position, Status]

  trait Status
  case object Alive extends Status
  case object Dead extends Status

  case class Position(row: Int, column: Int)

  val INTERVAL_BETWEEN_GENERATION: FiniteDuration = 1.seconds

  val defaultRows: Int = 10
  val defaultColumns: Int = 10

  case class GridDimensions(rows: Int = defaultRows, columns: Int = defaultColumns)

  val gridDimensions: GridDimensions = GridDimensions(rows = 30, columns = 30)

}

