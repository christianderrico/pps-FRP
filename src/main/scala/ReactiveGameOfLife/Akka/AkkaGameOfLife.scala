package ReactiveGameOfLife.Akka

import java.util.regex.Pattern

import ReactiveGameOfLife.ReactiveMonix.Model.GameOfLife.{Board, Dead, Generation, GridDimensions, Live, Position}
import ReactiveGameOfLife.ReactiveMonix.Model.UpdateOps.{applyGameOfLifeRulesBy, getNeighboursPositions, getNextGeneration}
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.util.Random

object AkkaGameOfLife extends App {

  val gridDimension: GridDimensions = GridDimensions()

  val initialBoard = randomBoard(gridDimension.rows * gridDimension.columns)("")

  @tailrec
  private def randomBoard(count: Int)(res: String): String = {
    val isNextCellAlive:Boolean = Random.nextBoolean()
    count match {
      case count if count == 0 => res
      case _ => randomBoard(count-1)(res + (if (isNextCellAlive) "#" else "."))
    }
  }

  val initialState: Board = initialBoardStringToBoard()

  private def initialBoardStringToBoard(): Board = {
    import Implicits._

    val dead = initialBoard.getAllIndicesOf('.').map((_, Dead))
    val alive = initialBoard.getAllIndicesOf('#').map((_, Live))
    (dead ++ alive).map(v => (Position(v._1 / gridDimension.rows, v._1 % gridDimension.columns), v._2))
      .map(cell => cell._1 -> cell._2)
      .toMap

  }

  val gameLoop = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val loopEngine = Source.repeat().throttle(1, 1.seconds)

    val printer = Sink.foreach[Generation](i => {
      def compareTwoPositions(pos1: Position, pos2: Position): Boolean = {
        def map2DPositionTo1DValue(position: Position): Int =
          position.row * gridDimension.rows + position.column

        map2DPositionTo1DValue(pos1) < map2DPositionTo1DValue(pos2)
      }

      println("GENERATION: " + i.generationNumber)

      Source(i.world.toList.sortWith((first, second) => (first, second) match {
        case ((pos1, _), (pos2, _)) => compareTwoPositions(pos1, pos2)
      })).map {
        case (_, status) if status == Live => "#"
        case _ => "."
      }.grouped(gridDimension.rows)
        .map(s => "".concat(s))
        .runForeach(println)
    })

    val zip = b.add(ZipWith((_: Unit, right: Generation) => right))
    val outputPorts: Int = 2
    val broadcast = b.add(Broadcast[Generation](outputPorts))
    val concat = b.add(Concat[Generation]())
    val firstGenInjector = Source.single(Generation(0, initialState))

    val doGeneration =
      Flow[Generation].flatMapConcat(previousGeneration =>
        Source(previousGeneration.world).flatMapConcat {
          case (cellPosition, cellStatus) =>
            Source(getNeighboursPositions(cellPosition)(gridDimension)) //get all possible neighbours
              .fold(0)((nOfAliveNeigh, neighbourPosition) =>
                nOfAliveNeigh + (if (previousGeneration.world(neighbourPosition) == Live) 1 else 0))
              .map(nOfAliveNeighbours => applyGameOfLifeRulesBy(nOfAliveNeighbours, cellStatus))
        }
          .grouped(gridDimension.rows * gridDimension.columns)
          .map(getNextGeneration(previousGeneration)))

    loopEngine ~> zip.in0
                  zip.out ~> broadcast ~> printer
                  zip.in1 <~ concat <~ firstGenInjector
                             concat <~ doGeneration <~ broadcast

    ClosedShape
  })

  implicit val system: ActorSystem = ActorSystem("Materializer")
  gameLoop.run()

}

object Implicits {
  implicit class RichString(string: String) {
    def getAllIndicesOf(char: Char): Seq[Int] = {
      @tailrec
      def getIndexes(char: Char, string: String)(indexes: List[Int]): Seq[Int] = string match {
        case seq if seq.indexOf(char) < 0 => indexes
        case _ => getIndexes(char, string replaceFirst(Pattern.quote(s"$char"), s"${0.toChar}"))(indexes appended string.indexOf(char))
      }

      getIndexes(char, string)(List.empty)
    }
  }
}
