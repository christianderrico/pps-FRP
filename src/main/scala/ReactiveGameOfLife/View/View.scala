package ReactiveGameOfLife

import java.awt.event.ActionEvent
import java.awt.{BorderLayout, Color, Dimension, FlowLayout, GridLayout, Toolkit}

import ReactiveGameOfLife.GameOfLife.GridDimensions
import ReactiveGameOfLife.View.{Output, Scene}
import Utilities.Implicits.{RichButton, RichObservable, RichTextField}
import Utilities.swingScheduler
import akka.stream.impl.QueueSource.Input
import javax.swing.border.Border
import javax.swing.{BorderFactory, JButton, JFrame, JLabel, JPanel, JTextField, WindowConstants}
import monix.eval.Task
import monix.reactive.Observable

object View {

  trait Output {
    type ButtonCoordinates = (Int, Int)
    type BoardToDraw = Map[ButtonCoordinates, Color]
    val generationCount: Int
    val tiles: BoardToDraw
  }

  type Input = (ActionCommand, Seq[Tile], Int)

  case class Tile(row: Int, column: Int, button: JButton)

  def apply(dimension: GridDimensions): View = new View(dimension)

  val TURNED_ON_CELLS_COLOR: Color = Color.cyan
  val TURNED_OFF_CELLS_COLOR: Null = null
  private val SCREEN_SIZE: Dimension = Toolkit.getDefaultToolkit.getScreenSize
  private val SCALE: Double = 1.5
  private val TITLE = "Game of Life"
  private val GENERATION_LABEL_TEXT = "Generation: "
  private val FIRST_GENERATION = "0"

  trait ActionCommand
  case object Start extends ActionCommand
  case object Stop extends ActionCommand

  private val START = Start.toString
  private val STOP = Stop.toString

  implicit def stringToActionCommand(cmd: String): ActionCommand = cmd match {
    case START => Start
    case _ => Stop
  }

  def getEmptyBorderFromOneSize(pxDimension: Int): Border =
    BorderFactory.createEmptyBorder(pxDimension, pxDimension, pxDimension, pxDimension)
}

case class View(dimension: GridDimensions) extends Scene[Output, View.Input] {

  import View._

  private lazy val frame: Task[JFrame] = for {
    frame <- Task(new JFrame())
    screenWidth <- Task((SCREEN_SIZE.width / SCALE).toInt)
    screenHeight <- Task((SCREEN_SIZE.height / SCALE).toInt)
    _ <- Task {
      frame.setPreferredSize(new Dimension(screenWidth, screenHeight))
      frame.setTitle(TITLE)
      frame.setResizable(true)
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    }
    mainPanel <- mainPanel
    lowerPanel <- lowerPanel
    _ <- Task {
      frame.getContentPane.add(mainPanel, BorderLayout.CENTER)
      frame.getContentPane.add(lowerPanel, BorderLayout.SOUTH)
    }
  } yield frame

  private lazy val mainPanel: Task[JPanel] = for {
    panel <- Task(new JPanel())
    _ <- Task {
      panel.setBorder(BorderFactory.createLineBorder(Color.gray))
      panel.setLayout(new GridLayout(dimension.rows, dimension.columns))
      tiles.map(_.button).foreach(panel.add)
    }
  } yield panel

  private lazy val textPanel: Task[JPanel] = for {
    panel <- Task(new JPanel())
    textField <- Task(textField)
    _ <- Task {
      val columns = 6
      textField.setColumns(columns)
      textField.setEnabled(false)
      textField.setDisabledTextColor(Color.BLACK)
    }
    genLabel <- Task(new JLabel(GENERATION_LABEL_TEXT))
    _ <- Task {
      panel.setLayout(new FlowLayout())
      panel.add(genLabel)
      panel.add(textField)
    }
  } yield panel

  private lazy val lowerPanel: Task[JPanel] = for {
    lowPanel <- Task(new JPanel())
    switch <- Task(switch)
    genPanel <- textPanel
    _ <- Task {
      lowPanel.setLayout(new BorderLayout())
      val pxSize = 5
      lowPanel.setBorder(View.getEmptyBorderFromOneSize(pxSize))
      lowPanel.add(BorderLayout.LINE_START, switch)
      lowPanel.add(BorderLayout.EAST, genPanel)
    }
  } yield lowPanel

  private lazy val tiles: Seq[Tile] = for {
    i <- 0 until dimension.rows
    j <- 0 until dimension.columns
  } yield Tile(i, j, new JButton())

  private lazy val switch: JButton = new JButton(START)

  private lazy val textField: JTextField = new JTextField(FIRST_GENERATION)

  private lazy val switchInput: Observable[ActionCommand] =
    switch.ActionEventAsObservable.doOnNext(executeOnActionCommand).map(_.getActionCommand)

  private lazy val tilesInput: Observable[Seq[Tile]] =
    Observable.combineLatestList(tiles.map(tile => Observable(tile) ++ buttonsPressedInput(tile)) : _*)

  private lazy val labelInput: Observable[Int] =
    Observable(textField.getText toInt).debug(v => println("on start " + v)) ++
      textField.onTextChangeAsObservable.map(_.toInt).debug(v => println ("on change " + v))

  private def buttonsPressedInput(tile: Tile): Observable[Tile] =
    tile.button.ActionEventAsObservable
      .doOnNext(_ => paintButton(tile.button))
      .map(_ => tile)

  private def paintButton(btn: JButton): Task[Unit] = Task {
    btn.setBackground(btn.getBackground match {
      case TURNED_ON_CELLS_COLOR => TURNED_OFF_CELLS_COLOR
      case _ => TURNED_ON_CELLS_COLOR
    })
  }

  private def disableButton(cmd: ActionCommand): Task[Unit] = Task {
    tiles.tapEach(
      _.button.setEnabled(cmd match {
        case Start => false
        case _ => true
      }))
  }

  private def changeText(button: JButton): Task[Unit] = Task {
    button.getText match {
      case START => button.setText(STOP)
      case _ => button.setText(START)
    }
  }

  private def executeOnActionCommand(event: ActionEvent): Task[Unit] = {
    implicit def sourceToButton(source: Object): JButton = source match {
      case btn: JButton => btn
    }

    for {
      _ <- changeText(event.getSource)
      _ <- disableButton(event.getActionCommand)
    } yield ()
  }

  private def updateTiles(tiles: Seq[Tile], boardToDraw: Output#BoardToDraw): Task[Unit] = Task {
    boardToDraw foreach {
      case ((row, column), color) => tiles.find(tile => tile.row == row && tile.column == column) match {
        case Some(tile) => tile.button.setBackground(color)
      }
    }
  }

  private def updateLabel(textField: JTextField, newGeneration: String): Task[Unit] = Task {
    textField.setText(newGeneration)
  }

  def render(gameState: Output): Task[Unit] = for {
    _ <- Task.shift(swingScheduler)
    tiles <- Task(tiles)
    generationTextField <- Task(textField)
    _ <- updateTiles(tiles, gameState.tiles)
    _ <- updateLabel(generationTextField, gameState.generationCount toString)
  } yield ()

  def display(): Task[Unit] = for {
    _ <- Task.shift(swingScheduler)
    container <- frame
    _ <- Task {
      container.pack()
      container.setLocationRelativeTo(null)
      container.setVisible(true)
    }
  } yield ()

}

