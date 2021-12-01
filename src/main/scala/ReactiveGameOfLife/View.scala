package ReactiveGameOfLife

import java.awt.event.ActionEvent
import java.awt.{BorderLayout, Color, Dimension, FlowLayout, GridLayout, Toolkit}

import ReactiveGameOfLife.GameOfLife.GridDimensions
import Utilities.Implicits.RichButton
import Utilities.swingScheduler
import javax.swing.border.Border
import javax.swing.{BorderFactory, JButton, JFrame, JLabel, JPanel, JTextField, WindowConstants}
import monix.eval.Task
import monix.reactive.Observable

object View {

  case class ButtonCell(row: Int, column: Int, button: JButton)

  def apply(dimension: GridDimensions): View = new View(dimension)

  val ALIVE_CELLS_COLOR: Color = Color.cyan
  private val SCREEN_SIZE: Dimension = Toolkit.getDefaultToolkit.getScreenSize
  private val SCALE: Double = 1.5
  private val TITLE = "Game of Life"
  private val GENERATION_LABEL_TEXT = "Generation: "

  trait ActionCommand
  case object Start extends ActionCommand
  case object Stop extends ActionCommand

  private val START_STRING = Start.toString
  private val STOP_STRING = Stop.toString

  implicit def toActionCommand(cmd: String): ActionCommand = cmd match {
    case START_STRING => Start
    case _ => Stop
  }

  def getEmptyBorderFromOneSize(pxDimension: Int): Border =
    BorderFactory.createEmptyBorder(pxDimension, pxDimension, pxDimension, pxDimension)
}

case class View(dimension: GridDimensions) {

  import View._

  private lazy val frame: Task[JFrame] = for {
    frame <- Task(new JFrame()).asyncBoundary(swingScheduler)
    screenWidth <- Task((SCREEN_SIZE.width / SCALE).toInt)
    screenHeight <- Task((SCREEN_SIZE.height / SCALE).toInt)
    _ <- Task {
      frame.setPreferredSize(new Dimension(screenWidth, screenHeight))
      frame.setTitle(TITLE)
      frame.setResizable(true)
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    }
    mainPanel <- panel
    lowerPanel <- lowerPanel
    _ <- Task {
      frame.getContentPane.add(mainPanel, BorderLayout.CENTER)
      frame.getContentPane.add(lowerPanel, BorderLayout.SOUTH)
    }
  } yield frame

  private lazy val panel: Task[JPanel] = for {
    panel <- Task(new JPanel())
    _ <- Task {
      panel.setBorder(BorderFactory.createLineBorder(Color.gray))
      panel.setLayout(new GridLayout(dimension.rows, dimension.columns))
      buttonCells.map(_.button).foreach(panel.add)
    }
  } yield panel

  private lazy val genPanel: Task[JPanel] = for {
    panel <- Task(new JPanel())
    input <- Task(generationLabel)
    _ <- Task(input.setEnabled(false))
    _ <- Task(input.setDisabledTextColor(Color.BLACK))
    genLabel <- Task(new JLabel(GENERATION_LABEL_TEXT))
    _ <- Task {
      panel.setLayout(new FlowLayout())
      panel.add(genLabel)
      panel.add(input)
    }
  } yield panel

  private lazy val lowerPanel: Task[JPanel] = for {
    lowPanel <- Task(new JPanel())
    startButton <- Task(startAndPauseButton)
    genPanel <- genPanel
    _ <- Task {
      lowPanel.setLayout(new BorderLayout())
      val px = 5
      lowPanel.setBorder(View.getEmptyBorderFromOneSize(px))
      lowPanel.add(BorderLayout.LINE_START, startButton)
      lowPanel.add(BorderLayout.EAST, genPanel)
    }
  } yield lowPanel

  private lazy val buttonCells: Seq[ButtonCell] = for {
    i <- 0 until dimension.rows
    j <- 0 until dimension.columns
  } yield ButtonCell(i, j, new JButton())

  private lazy val startAndPauseButton: JButton = new JButton(Start toString)

  private lazy val generationLabel: JTextField = new JTextField(0 toString)

  lazy val startAndStopInput: Observable[ActionCommand] =
    startAndPauseButton.ActionEventToObservable.doOnNext(executeOnActionCommand).map(_.getActionCommand)

  lazy val cellsInput: Observable[Map[(Int, Int), JButton]] =
    Observable.combineLatestList(
      buttonCells.map(
        buttonCell => Observable(buttonCell) ++
          buttonCell.button.ActionEventToObservable
                           .doOnNext(_ => paintButton(buttonCell.button))
                           .map(_ => buttonCell)): _*)
      .map(buttonCells =>
        buttonCells.map(cell => (cell.row, cell.column) -> cell.button).toMap)

  private def paintButton(btn: JButton): Task[Unit] = Task {
    btn.setBackground(btn.getBackground match {
      case ALIVE_CELLS_COLOR => null
      case _ => ALIVE_CELLS_COLOR
    })
  }

  private def disableCell(cmd: ActionCommand): Task[Unit] = Task {
    buttonCells.tapEach(
      _.button.setEnabled(cmd match {
        case Start => false
        case _ => true
      }))
  }

  private def changeText(button: JButton): Task[Unit] = Task {
    button.getText match {
      case START_STRING => button.setText(STOP_STRING)
      case _ => button.setText(START_STRING)
    }
  }

  private def executeOnActionCommand(event: ActionEvent): Task[Unit] = {
    implicit def sourceToButton(source: Object): JButton = source match {
      case btn: JButton => btn
    }

    for {
      _ <- Task() asyncBoundary swingScheduler
      _ <- changeText(event.getSource)
      _ <- disableCell(event.getActionCommand)
      _ <- Task.shift
    } yield ()
  }

  def render[A](elem: A): Task[Unit] = for {
    _ <- Task {}.asyncBoundary(swingScheduler)
    l <- Task(generationLabel)
    _ <- Task { l.setText("" + elem) }
  } yield ()

  def display(): Task[Unit] = for {
    container <- frame
    _ <- Task {
      container.pack()
      container.setLocationRelativeTo(null)
      container.setVisible(true)
    }
  } yield ()

}

