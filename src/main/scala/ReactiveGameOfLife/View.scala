package ReactiveGameOfLife

import java.awt.{BorderLayout, Color, Dimension, FlowLayout, GridLayout, Toolkit}

import ReactiveGameOfLife.View.Cell
import Utilities.system
import akka.stream.scaladsl.{Sink, Source}
import cats.effect.IO
import javax.swing.{BorderFactory, JButton, JFrame, JPanel, WindowConstants}

case class View(rows: Int, columns: Int) {

  val SCREEN_SIZE: Dimension = Toolkit.getDefaultToolkit.getScreenSize
  val SCALE: Double = 1.5
  val TITLE = "Game of Life"
  val START = "Start"
  val STOP = "Stop"

  private lazy val frame: IO[JFrame] = for {
    frame <- IO(new JFrame())
    screenWidth <- IO((SCREEN_SIZE.width / SCALE).toInt)
    screenHeight <- IO((SCREEN_SIZE.height / SCALE).toInt)
    _ <- IO {
      frame.setPreferredSize(new Dimension(screenWidth, screenHeight))
      frame.setTitle(TITLE)
      frame.setResizable(true)
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    }
    mainPanel <- panel
    lowerPanel <- lowerPanel
    _ <- IO {
      frame.getContentPane.add(mainPanel, BorderLayout.CENTER)
      frame.getContentPane.add(lowerPanel, BorderLayout.SOUTH)
    }
  } yield frame

  private lazy val panel: IO[JPanel] = for {
    panel <- IO(new JPanel())
    _ <- IO {
      panel.setBorder(BorderFactory.createLineBorder(Color.gray))
      panel.setLayout(new GridLayout(rows, columns))
      cells.map(_.button).foreach(panel.add)
    }
  } yield panel

  private def changeText(currentText: String): String = if(currentText == START) STOP else START
  private lazy val textChanger = Sink.foreach[JButton](btn => btn.setText(changeText(btn.getText)))
  private lazy val inputPrinter = Sink.foreach[JButton](b => println(b.getText))

  private def button(): IO[JButton] = for {
    button <- IO(new JButton(START))
    _ <- IO (button.addActionListener(_ => input.run().success(Some(button))))
  } yield button

  private lazy val input = Source.maybe[JButton].alsoTo(textChanger)
                                                .to(inputPrinter)

  private lazy val lowerPanel: IO[JPanel] = for {
    lowPanel <- IO(new JPanel())
    startButton <- button()
    _ <- IO {
      lowPanel.setLayout(new FlowLayout())
      lowPanel.add(BorderLayout.CENTER, startButton)
    }
  } yield lowPanel

  private lazy val cells: Seq[Cell] = for {
    i <- 0 until rows
    j <- 0 until columns
  } yield Cell(i, j, new JButton())

  def display(): IO[Unit] = for {
    container <- frame
    _ <- IO {
      container.pack()
      container.setLocationRelativeTo(null)
      container.setVisible(true)
    }
  } yield ()

}

object View {
  case class Cell(i: Int, j: Int, button: JButton)
  def apply(rows: Int = 10, columns: Int = 10): View = new View(rows, columns)
}

