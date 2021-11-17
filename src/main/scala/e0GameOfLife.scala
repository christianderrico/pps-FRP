import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

import scala.concurrent.duration.{FiniteDuration, SECONDS}

object e0GameOfLife extends App {

  case class Iteration(generation: Int, cells: Seq[Int])
  case class CellCoordinates(row: Int, column: Int)

  val width: Int = 10
  val height: Int = 10

  val initial = ".........." +
                ".........." +
                ".........." +
                ".........." +
                "....###..." +
                "...###...." +
                ".........." +
                ".........." +
                ".........." +
                "..........";

  val isAlive: Char => Int = c => if (c == '#') 1 else 0
  val initialState: Seq[Int] = initial.flatMap(v => Seq(v)).map(isAlive)

  val printer = Sink.foreach[Iteration](i => {
    println("GENERATION: " + i.generation)
    Source(i.cells).map(c => if (c > 0) "#" else ".").grouped(width).map(s => "".concat(s)).runForeach(println(_))
  })

  val loop = Source.repeat(Iteration(0, List.empty)).throttle(1, FiniteDuration(1, SECONDS))

  val NUMBER_OF_NEIGHBOURS = 8
  val NEIGHBOURS_SQUARE_GRID_SIDE_LENGHT = 3
  val MINIMUM_OF_ALIVE_NEIGHBOURS = 2
  val MAXIMUM_OF_ALIVE_NEIGHBOURS = 3
  val ALIVE = 1
  val DEAD = 0

  def excludeNeighboursOutOfTheGrid(cell: CellCoordinates): Boolean =
    (cell.row >= 0 && cell.row < width) && (cell.column >= 0 && cell.column < height)

  def getCellIndex(cell: CellCoordinates): Int =
    cell.row + cell.column * width

  def computeNeighbourCoordinates(cellIndex: Int, neighbourId: Int): CellCoordinates =
    CellCoordinates(
      (cellIndex % width) + (neighbourId % NEIGHBOURS_SQUARE_GRID_SIDE_LENGHT) - 1,
      (cellIndex / width) + (neighbourId / NEIGHBOURS_SQUARE_GRID_SIDE_LENGHT) - 1
    )

  val gameLoop = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val gateRight = b.add(ZipWith((_: Iteration, right: Iteration) => right))
    val broadcast = b.add(Broadcast[Iteration](2))
    val concat = b.add(Concat[Iteration]())
    val firstGenInjector = Source.single(Iteration(0, initialState))

    val doGeneration = Flow[Iteration]
      .flatMapConcat(iteration =>
        Source(iteration.cells.indices).flatMapConcat(cellIndex => //for every cell
          Source(0 to NUMBER_OF_NEIGHBOURS)
            .map(computeNeighbourCoordinates(cellIndex, _)) //get neighbours
            .filter(excludeNeighboursOutOfTheGrid) //exclude neighbours out of the grid
            .filter(neighbour => getCellIndex(neighbour) != cellIndex) //exclude cell itself
            .fold(0)((numberOfAliveNeighbours, cell) =>
              numberOfAliveNeighbours + iteration.cells(getCellIndex(cell))) //compute number of alive neighbours
            .map {
              case n if n < MINIMUM_OF_ALIVE_NEIGHBOURS || n > MAXIMUM_OF_ALIVE_NEIGHBOURS => DEAD
              case n if n == MAXIMUM_OF_ALIVE_NEIGHBOURS || iteration.cells(cellIndex) == ALIVE => ALIVE
              case _ => DEAD
            } //apply Game Of Life's Rules
        ).grouped(width * height)
          .map(cells => Iteration(iteration.generation + 1, cells))
      )

    loop ~> gateRight.in0
    gateRight.out ~> broadcast ~> printer
    gateRight.in1 <~ concat <~ firstGenInjector
                     concat <~ doGeneration <~ broadcast

    ClosedShape
  })

  implicit val system: ActorSystem = ActorSystem("QuickStart")
  gameLoop.run()

}
