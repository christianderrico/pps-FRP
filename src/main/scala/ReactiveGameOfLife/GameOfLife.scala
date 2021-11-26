package ReactiveGameOfLife

object Model {

  type Cells = Seq[Int]

  case class Iteration(generation: Int, cells: Cells)
  case class CellCoordinates(row: Int, column: Int)

}

