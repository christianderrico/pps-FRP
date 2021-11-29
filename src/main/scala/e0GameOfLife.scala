import ReactiveGameOfLife.GameOfLife.{Alive, Cell, Cells, Dead, GridDimension, Iteration, Position, Status}
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

import scala.concurrent.duration.DurationInt

object e0GameOfLife extends App {

  implicit val gridDimension: GridDimension = GridDimension()

  val initialBoard = ".........." +
                     ".........." +
                     ".........." +
                     ".........." +
                     "....###..." +
                     "...###...." +
                     ".........." +
                     ".........." +
                     ".........." +
                     ".........."

  val initialState: Cells = charToCell

  private def charToCell: Seq[Cell] = {
    import ReactiveGameOfLife.Utilities.Implicits._

    def mapCellPositionToSingleValue(cell: Cell)(implicit gridDimension: GridDimension): Int =
      cell.position.column + cell.position.row * gridDimension.columns

    val dead = initialBoard.getAllIndicesOf('.').map((_, Dead))
    val alive = initialBoard.getAllIndicesOf('#').map((_, Alive))
    (dead ++ alive).map(v => Cell(Position(v._1 / gridDimension.rows, v._1 % gridDimension.columns), v._2))
                   .sortWith((a, b) => mapCellPositionToSingleValue(b) - mapCellPositionToSingleValue(a) > 0)

  }

  val gameLoop = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val loop = Source.repeat().throttle(1, 1.seconds)

    val printer = Sink.foreach[Iteration](i => {
      println("GENERATION: " + i.generation)
      Source(i.cells).map(cell => if(cell.status == Alive) "#" else ".")
                     .grouped(gridDimension.rows)
                     .map(s => "".concat(s))
                     .runForeach(println(_))
    })

    val gateRight = b.add(ZipWith((_:Unit, right: Iteration) => right))
    val outputPorts: Int = 2
    val broadcast = b.add(Broadcast[Iteration](outputPorts))
    val concat = b.add(Concat[Iteration]())
    val firstGenInjector = Source.single(Iteration(0, initialState))

    import GameUtilities._

    val doGeneration =
      Flow[Iteration].flatMapConcat(iteration =>
        Source(iteration.cells).flatMapConcat(referenceCell => //for every cell
          Source(computeNeighboursPositionOf(referenceCell.position)) //get all possible neighbours
            .filter(excludeNeighboursPositionsOutOfTheGrid) //exclude neighbours out of the grid
            .filter(exclude(referenceCell.position)) //exclude cell itself
            .fold(0)((nOfAliveNeigh, neighPos) => nOfAliveNeigh + countAliveNeighbourIteration(iteration, neighPos))
            .map(nOfAliveNeighbours => applyGameOfLifeRulesBy(nOfAliveNeighbours, referenceCell))
        ).grouped(gridDimension.rows * gridDimension.columns)
         .map(newCellsStatus =>
           Iteration(
             iteration.generation + 1,
             iteration.cells.zip(newCellsStatus).map {
               case (previousCell, newStatus) => Cell(previousCell.position, newStatus)
             }
           )
         )
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

object GameUtilities {

  val MINIMUM_OF_ALIVE_NEIGHBOURS = 2
  val MAXIMUM_OF_ALIVE_NEIGHBOURS = 3

  def excludeNeighboursPositionsOutOfTheGrid(position: Position)(implicit gridDimension: GridDimension): Boolean =
    (position.row >= 0 && position.row < gridDimension.rows) && (position.column >= 0 && position.column < gridDimension.columns)

  def computeNeighboursPositionOf(referenceCell: Position): Seq[Position] = {
    for(
      offsetX: Int <- -1 to 1;
      offsetY: Int <- -1 to 1
    ) yield Position(referenceCell.row + offsetX, referenceCell.column + offsetY)
  }

  def exclude(referenceCell: Position)(neighbour: Position): Boolean = referenceCell != neighbour

  def countAliveNeighbourIteration(iteration: Iteration, neighbourPosition: Position): Int =
    iteration.cells.find(_.position == neighbourPosition) match {
      case Some(existentNeighbour) if existentNeighbour.status == Alive => 1
      case _ => 0
    }

  def applyGameOfLifeRulesBy(nOfAliveNeighbours: Int, referenceCell: Cell): Status =
    (nOfAliveNeighbours, referenceCell) match {
      case (n, _) if n < MINIMUM_OF_ALIVE_NEIGHBOURS || n > MAXIMUM_OF_ALIVE_NEIGHBOURS => Dead
      case (n, cell) if n == MAXIMUM_OF_ALIVE_NEIGHBOURS || cell.status == Alive => Alive
      case _ => Dead
  }

}