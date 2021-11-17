import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

import scala.concurrent.duration.{FiniteDuration, SECONDS}

object e0GameOfLife extends App {

  case class Iteration(generation: Int, cells: Seq[Int])

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

  val NEIGHBOURS = 8
  val NEIGHBOURS_SQUARE_GRID_SIDE = 3
  val MINIMUM_OF_ALIVE_NEIGHBOURS = 2
  val MAXIMUM_OF_ALIVE_NEIGHBOURS = 3
  val ALIVE = 1
  val DEAD = 0

  def excludeNotAuthorizedNeighbour(cell: (Int, Int)): Boolean =
    (cell._1 >= 0 && cell._1 < width) && (cell._2 >= 0 && cell._2 < height)

  val gameLoop = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val gateRight = b.add(ZipWith((_: Iteration, right: Iteration) => right))
    val broadcast = b.add(Broadcast[Iteration](2))
    val concat = b.add(Concat[Iteration]())
    val firstGenInjector = Source.single(Iteration(0, initialState))

    val doGeneration = Flow[Iteration]
      .flatMapConcat(i =>
        Source(i.cells.indices).flatMapConcat(cellIndex =>
          Source(0 to NEIGHBOURS)
            .map(neighbourCellIndex => (
              (cellIndex % width) + (neighbourCellIndex % NEIGHBOURS_SQUARE_GRID_SIDE) - 1,
              (cellIndex / width) + (neighbourCellIndex / NEIGHBOURS_SQUARE_GRID_SIDE) - 1)
            ).filter(excludeNotAuthorizedNeighbour)
            .filter(cell => cell._1 + cell._2 * width != cellIndex)
            .fold(0)((acc, point) => acc + i.cells(point._1 + point._2 * width))
            .map {
              case n if n < MINIMUM_OF_ALIVE_NEIGHBOURS || n > MAXIMUM_OF_ALIVE_NEIGHBOURS => DEAD
              case n if n == MAXIMUM_OF_ALIVE_NEIGHBOURS || i.cells(cellIndex) == ALIVE => ALIVE
              case _ => DEAD
            }
        ).grouped(width * height)
          .map(cells => Iteration(i.generation + 1, cells))
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
