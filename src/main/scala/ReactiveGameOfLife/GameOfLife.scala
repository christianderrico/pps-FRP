package ReactiveGameOfLife

object GameOfLife {

  type Cells = Seq[Int]

  val defaultWidth: Int = 10
  val defaultHeight: Int = 10

  case class Iteration(generation: Int, cells: Cells)
  case class Position(row: Int, column: Int) {
    private def convertToIndex(width: Int = defaultWidth): Int = row + column * width
    val toCellsIndex = convertToIndex()
  }

}

