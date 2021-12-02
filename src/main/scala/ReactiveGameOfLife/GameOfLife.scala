package ReactiveGameOfLife

import ReactiveGameOfLife.Generation.Board

sealed trait Generation {
  val cells: Board
  val generation: Int
}

object Generation {

  case class Generation(generation: Int, cells: Board) extends Generation

  type Board = Map[Position, Status]

  trait Status
  case object Alive extends Status
  case object Dead extends Status

  case class Position(row: Int, column: Int)

  val defaultRows: Int = 10
  val defaultColumns: Int = 10

  case class GridDimensions(rows: Int = defaultRows, columns: Int = defaultColumns)

}

