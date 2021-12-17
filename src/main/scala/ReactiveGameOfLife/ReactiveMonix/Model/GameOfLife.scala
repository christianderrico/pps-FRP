package ReactiveGameOfLife.ReactiveMonix.Model

import ReactiveGameOfLife.ReactiveMonix.Model.GameOfLife.Board
import cats.Eq

import scala.concurrent.duration.{DurationInt, FiniteDuration}

/**
 * ADT that describes John Conway's game of Life: [[https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life]]
 */
sealed trait GameOfLife {
  def world: Board
  def generationNumber: Int
}

object GameOfLife {

  /**
   * Game of life is a cellular automaton, whose evolution is determined by its initial state, requiring no further input.
   * Initial pattern constitutes the seed of the system. The first generation is created by applying game of life rules,
   * and every generation is a pure function of preceding one.
   * @param generationNumber progressive number of generation, which indicates number of evolution steps made from the first one.
   * @param world game of life universe as [[Board]]
   */
  case class Generation(generationNumber: Int, world: Board) extends GameOfLife

  /**
   * An alternative type class using to checks generations equality,
   * used with [[monix.reactive.Observable.distinctUntilChanged]] operator
   */
  implicit val eqGameOfLife: Eq[GameOfLife] =
    (x: GameOfLife, y: GameOfLife) => x.generationNumber == y.generationNumber

  /**
   * The universe of the Game of Life is a two-dimensional orthogonal grid of square cells,
   * each of which is in one admissible state
   */
  type Board = Map[Position, State]

  case class Position(row: Int, column: Int)

  /**
   * Possible states are two: a cell could be [[Live]] or alternatively [[Dead]].
   * In particularly, there are precise rules which regulate life in the universe:
   * - Any live cell with fewer than two live neighbours dies, as if by underpopulation.
   * - Any live cell with two or three live neighbours lives on to the next generation.
   * - Any live cell with more than three live neighbours dies, as if by overpopulation.
   * - Any dead cell with exactly three live neighbours becomes a live cell, as if by reproduction.
   */
  trait State
  case object Live extends State
  case object Dead extends State

  /**
   * Time that passes between two [[Generation]].
   */
  val INTERVAL_BETWEEN_GENERATION: FiniteDuration = 1.seconds

  val defaultRows: Int = 10
  val defaultColumns: Int = 10

  /**
   * Grid board dimension
   * @param rows grid number of rows
   * @param columns grid number of columns
   */
  case class GridDimensions(rows: Int = defaultRows, columns: Int = defaultColumns)

  val gridDimensions: GridDimensions = GridDimensions(rows = 30, columns = 30)

}

