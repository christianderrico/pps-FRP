import ReactiveGameOfLife.GameOfLife.{Alive, Cell, Board, Dead, GridDimensions, Iteration, Position}
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

import scala.concurrent.duration.DurationInt

object e0GameOfLife extends App {

  implicit val gridDimension: GridDimensions = GridDimensions()

  val initialBoard = ".........." +
                     ".........." +
                     ".........." +
                     ".........." +
                     "....###..." +
                     "...##.#..." +
                     ".........." +
                     ".........." +
                     ".........." +
                     ".........."

  val initialState: Board = initialBoardStringToCells()

  private def initialBoardStringToCells(): Board = {
    import ReactiveGameOfLife.Utilities.Implicits._

    val dead = initialBoard.getAllIndicesOf('.').map((_, Dead))
    val alive = initialBoard.getAllIndicesOf('#').map((_, Alive))
    (dead ++ alive).map(v => Cell(Position(v._1 / gridDimension.rows, v._1 % gridDimension.columns), v._2))
                   .map(cell => cell.position -> cell.status)
                   .toMap

  }

  val gameLoop = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val loopEngine = Source.repeat().throttle(1, 1.seconds)

    val printer = Sink.foreach[Iteration](i => {
      def compareTwoPositions(pos1: Position, pos2: Position): Boolean = {
        def map2DPositionTo1DValue(position: Position): Int =
          position.row * gridDimension.rows + position.column

        map2DPositionTo1DValue(pos1) < map2DPositionTo1DValue(pos2)
      }


      println("GENERATION: " + i.generation)

      Source(i.cells.toList.sortWith( (first, second) => (first, second) match {
        case ((pos1, _), (pos2, _)) => compareTwoPositions(pos1, pos2)
      })).map {
        case (_, status) if status == Alive => "#"
        case _ => "."
      }.grouped(gridDimension.rows)
       .map(s => "".concat(s))
       .runForeach(println)
    })

    val zip = b.add(ZipWith((_:Unit, right: Iteration) => right))
    val outputPorts: Int = 2
    val broadcast = b.add(Broadcast[Iteration](outputPorts))
    val concat = b.add(Concat[Iteration]())
    val firstGenInjector = Source.single(Iteration(0, initialState))

    import ReactiveGameOfLife.GameOfLifeOperations._

    val doGeneration =
      Flow[Iteration].flatMapConcat(iteration =>
        Source(iteration.cells).flatMapConcat {
          case (cellPosition, cellStatus) =>
            Source(getNeighboursPositions(cellPosition)(gridDimension)) //get all possible neighbours
              .fold(0)((nOfAliveNeigh, neighbourPosition) =>
                nOfAliveNeigh + (if (iteration.cells(neighbourPosition) == Alive) 1 else 0 ) )
              .map(nOfAliveNeighbours => applyGameOfLifeRulesBy(nOfAliveNeighbours, cellStatus))
        }
        .grouped(gridDimension.rows * gridDimension.columns)
        .map(newCellsStatus =>
           Iteration(
             iteration.generation + 1,
             iteration.cells.zip(newCellsStatus).map {
               case (previousCell, newStatus) => previousCell._1 -> newStatus
             }.toMap
           )
         )
      )


    loopEngine ~> zip.in0
    zip.out ~> broadcast ~> printer
    zip.in1 <~ concat <~ firstGenInjector
                     concat <~ doGeneration <~ broadcast

    ClosedShape
  })

  implicit val system: ActorSystem = ActorSystem("QuickStart")
  gameLoop.run()

}