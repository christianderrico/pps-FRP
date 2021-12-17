package ReactiveGameOfLife.ReactiveMonix.View

import java.awt.event.ActionEvent
import java.awt._

import ReactiveGameOfLife.ReactiveMonix.Model.GameOfLife.GridDimensions
import ReactiveGameOfLife.Utilities.Implicits.{Liftable, RichButton, RichTextField}
import ReactiveGameOfLife.Utilities.swingScheduler
import javax.swing.border.Border
import javax.swing._
import monix.eval.Task
import monix.reactive.Observable

object View {

  def apply(dimension: GridDimensions): View = new View(dimension)

  /**
   * It models the visual representation of Model according to the protocol adopted by this implementation of [[Scene]]
   */
  trait ViewInput {
    type ButtonCoordinates = (Int, Int)
    type BoardToDraw = Map[ButtonCoordinates, Color]
    def generationCount: Int
    def tiles: BoardToDraw
  }

  /**
   * It models output produced by this implementation of [[Scene]]
   */
  trait ViewOutput

  /**
   * It's a request that regards the cyclical computation of generations
   * @param cmd directive that aims to start or stop the game
   * @param tiles sequence of [[Tile]], forming the current gen
   * @param genNumber progressive id number of gen
   */
  case class CycleComputationRequest(cmd: ActionCommand, tiles: Seq[Tile], genNumber: Int) extends ViewOutput

  /**
   * Visual representation of cells on the board
   * @param row row position of cell
   * @param column column position of cell
   * @param button JButton that display cell state
   */
  case class Tile(row: Int, column: Int, button: JButton)

  val TURNED_ON_CELLS_COLOR: Color = Color.cyan
  val TURNED_OFF_CELLS_COLOR: Null = null
  private val SCREEN_SIZE: Dimension = Toolkit.getDefaultToolkit.getScreenSize
  private val SCALE: Double = 1.5
  private val TITLE = "Game of Life"
  private val GENERATION_LABEL_TEXT = "Generation: "
  private val FIRST_GENERATION = "0"

  type ActionCommand = String

  val START: ActionCommand = "Start"
  val STOP: ActionCommand = "Stop"

  def getEmptyBorderFromOneSize(pxDimension: Int): Border =
    BorderFactory.createEmptyBorder(pxDimension, pxDimension, pxDimension, pxDimension)
}

import View._
case class View(dimension: GridDimensions) extends Scene[ViewInput, ViewOutput] {

  import View._

  private lazy val frame: Task[JFrame] = for {
    frame <- new JFrame().liftToTask
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
    panel <- new JPanel().liftToTask
    _ <- Task {
      panel.setBorder(BorderFactory.createLineBorder(Color.gray))
      panel.setLayout(new GridLayout(dimension.rows, dimension.columns))
      tiles.map(_.button).foreach(panel.add)
    }
  } yield panel

  private lazy val textPanel: Task[JPanel] = for {
    panel <- new JPanel().liftToTask
    textField <- textField.liftToTask
    _ <- Task {
      val columns = 6
      textField.setColumns(columns)
      textField.setEnabled(false)
      textField.setDisabledTextColor(Color.BLACK)
    }
    genLabel <- new JLabel(GENERATION_LABEL_TEXT).liftToTask
    _ <- Task {
      panel.setLayout(new FlowLayout())
      panel.add(genLabel)
      panel.add(textField)
    }
  } yield panel

  private lazy val lowerPanel: Task[JPanel] = for {
    lowPanel <- new JPanel().liftToTask
    switch <- switch.liftToTask
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
    Observable.combineLatestList(tiles.map(tile => tile.liftToObservable ++ onPressedAsObservable(tile)) : _*)

  private lazy val labelInput: Observable[Int] =
    textField.getText.toInt.liftToObservable ++ textField.onTextChangeAsObservable.map(_.toInt)

  private def onPressedAsObservable(tile: Tile): Observable[Tile] =
    tile.button.ActionEventAsObservable
      .doOnNext(_ => paintButton(tile.button))
      .map(_ => tile)

  private def paintButton(btn: JButton): Task[Unit] = Task {
    btn.setBackground(btn.getBackground match {
      case TURNED_ON_CELLS_COLOR => TURNED_OFF_CELLS_COLOR
      case _ => TURNED_ON_CELLS_COLOR
    })
  }.executeOn(swingScheduler)

  private def disableButton(cmd: ActionCommand): Task[Unit] = Task {
    tiles.foreach(_.button.setEnabled(cmd match {
        case START => false
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
  }.executeOn(swingScheduler)

  private def updateTiles(tiles: Seq[Tile], boardToDraw: ViewInput#BoardToDraw): Task[Unit] = Task {
    boardToDraw foreach {
      case ((row, column), color) => tiles.find(tile => tile.row == row && tile.column == column) match {
        case Some(tile) => tile.button.setBackground(color)
      }
    }
  }

  private def updateLabel(textField: JTextField, newGeneration: String): Task[Unit] = Task {
    textField.setText(newGeneration)
  }

  override def render(gameState: ViewInput): Task[Unit] = for {
    _ <- Task.shift(swingScheduler)
    tiles <- tiles.liftToTask
    generationTextField <- textField.liftToTask
    _ <- updateTiles(tiles, gameState.tiles)
    _ <- updateLabel(generationTextField, gameState.generationCount toString)
  } yield ()

  override def display: Task[Unit] = for {
    _ <- Task.shift(swingScheduler)
    container <- frame
    _ <- Task {
      container.pack()
      container.setLocationRelativeTo(null)
      container.setVisible(true)
    }
  } yield ()

  override def emit: Observable[ViewOutput] =
    Observable.combineLatest3(switchInput, tilesInput, labelInput).map {
      case (cmd, tiles, gen) => CycleComputationRequest(cmd, tiles, gen)
    }
}

