import ReactiveGameOfLife.GameOfLife
import ReactiveGameOfLife.GameOfLife.{Cells, Iteration, Position, defaultHeight}
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}
import akka.util.Index

import scala.concurrent.duration.{DurationInt, FiniteDuration, SECONDS}

object e0GameOfLife extends App {

  val width = GameOfLife.defaultWidth
  val height = GameOfLife.defaultHeight

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
  val initialState: Cells = initial.flatMap(v => Seq(v)).map(isAlive)

  val NUMBER_OF_NEIGHBOURS = 9
  val NEIGHBOURS_SQUARE_GRID_SIDE_LENGTH = 3
  val MINIMUM_OF_ALIVE_NEIGHBOURS = 2
  val MAXIMUM_OF_ALIVE_NEIGHBOURS = 3
  val ALIVE = 1
  val DEAD = 0

  val gameLoop = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val loop = Source.repeat().throttle(1, 1.seconds)

    val printer = Sink.foreach[Iteration](i => {
      println("GENERATION: " + i.generation)
      Source(i.cells).map(c => if (c > 0) "#" else ".")
                     .grouped(width)
                     .map(s => "".concat(s))
                     .runForeach(println(_))
    })

    val gateRight = b.add(ZipWith((_:Unit, right: Iteration) => right))
    val broadcast = b.add(Broadcast[Iteration](2))
    val concat = b.add(Concat[Iteration]())
    val firstGenInjector = Source.single(Iteration(0, initialState))

    import Utilities._

    val doGeneration = Flow[Iteration]
      .flatMapConcat(iteration =>
        Source(iteration.cells.indices).flatMapConcat(referenceCellId => //for every cell
          Source(0 until NUMBER_OF_NEIGHBOURS)
            .map(computeNeighboursPositionsOf(referenceCellId)) //get neighbours
            .filter(excludeNeighboursOutOfTheGrid) //exclude neighbours out of the grid
            .filter(exclude(referenceCellId)) //exclude cell itself
            .fold(0)((numberOfAliveNeighbours, neighbour) =>
              numberOfAliveNeighbours + iteration.cells(neighbour.toCellsIndex))//compute number of alive neighbours
            .map {
              case n if n < MINIMUM_OF_ALIVE_NEIGHBOURS || n > MAXIMUM_OF_ALIVE_NEIGHBOURS => DEAD
              case n if n == MAXIMUM_OF_ALIVE_NEIGHBOURS || iteration.cells(referenceCellId) == ALIVE => ALIVE
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

object Utilities {
  import e0GameOfLife._

  def excludeNeighboursOutOfTheGrid(cell: Position): Boolean =
    (cell.row >= 0 && cell.row < width) && (cell.column >= 0 && cell.column < height)

  def exclude(referenceCellId: Int)(neighbour: Position): Boolean = neighbour.toCellsIndex != referenceCellId

  def computeNeighboursPositionsOf(cellIndex: Int)(neighbourId: Int): Position =
    Position(
      (cellIndex % width) + (neighbourId % NEIGHBOURS_SQUARE_GRID_SIDE_LENGTH) - 1,
      (cellIndex / width) + (neighbourId / NEIGHBOURS_SQUARE_GRID_SIDE_LENGTH) - 1
    )

}
