package Experiments.AkkaStreams

import akka.stream.{ClosedShape, FlowShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source, ZipWith}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.Random

class GraphDSLSpec extends BaseSpec {

  private val allow = afterWord("allow")

  "Graph DSL" should allow {

    "to build complex graph using Sources, Flows, Sinks and junctions" in {

      val sink = Sink.head[Int]

      val start = 100
      val graph = RunnableGraph.fromGraph(GraphDSL.createGraph(sink){ implicit builder =>
        sink =>

        import GraphDSL.Implicits._

        val sourceA = Source[Int](0 to 100)
        val sourceB = Source[Int](start to 200)

        val flowA = Flow[Int].map(_ * 2)
        val stepTwo = Flow[Int].map(_ / 2)

        val zip = builder.add(ZipWith[Int, Int, Int](Keep.right))
        val outputPorts = 2
        val broadcastA = builder.add(Broadcast[Int](outputPorts))

        val flowC = Flow[Int].filter(_ % 2 == 0)

        val broadcastB = builder.add(Broadcast[Int](outputPorts))

        val stepThree = Flow[Int].map(_ * 5)
        val flowE = Flow[Int].map(_.toString)

        val zip2 = builder.add(ZipWith[Int, String, Int](Keep.left))
        val zip3 = builder.add(ZipWith[Int, Int, Int](Keep.right))

        sourceA ~>     flowA     ~> zip.in0
        sourceB ~>     stepTwo   ~> zip.in1
                                    zip.out ~> broadcastA.in
                                               broadcastA.out(0) ~> broadcastB.in
                                                                    broadcastB.out(0) ~> flowC ~>             zip3.in0
                                                                    broadcastB.out(1) ~> flowE ~> zip2.in1
                                               broadcastA.out(1) ~>   stepThree       ~>          zip2.in0
                                                                                                  zip2.out ~> zip3.in1
                                                                                                              zip3.out ~> sink


        ClosedShape
      })

      val stepTwo: Int => Int = _ / 2
      val thirdStep: Int => Int = _ * 5
      val materializedValue = graph.run()
      val resOutOfPipeline = awaitForResult(materializedValue)

      resOutOfPipeline shouldBe thirdStep(stepTwo(start))
    }

    "to create complex data pipelines using composed base blocks" in {
      val complexSource = Source.fromGraph(GraphDSL.create(){ implicit builder =>
       import GraphDSL.Implicits._

        val upperBound = 100
        val n = Random.nextInt(upperBound)
        val evenNumber = 2 * n
        val oddNumber = 2 * n + 1

        val sourceA = Source.single(evenNumber)
        val sourceB = Source.single(oddNumber)

        val zip = builder.add(ZipWith[Int, Int, Double](_ + _))
        val flow = Flow[Double].map(_ * 2)
        val merge = builder.add(Merge[Double](1))

        sourceA ~> zip.in0
        sourceB ~> zip.in1
                   zip.out ~> flow ~> merge

        SourceShape(merge.out)
     })

      val complexFlow = Flow.fromGraph(GraphDSL.create(){ implicit builder =>
       import GraphDSL.Implicits._

        val outPorts = 2
        val broadcast = builder.add(Broadcast[Double](outPorts))
        val checkEvenCondition: Double => Boolean = _ % 2 == 0

        val flowA = Flow[Double].filter(checkEvenCondition).map(_ * 0)
        val flowB = Flow[Double].map(_ * 2)

        val zip = builder.add(ZipWith[Double, Double, Double](Math.min))

        broadcast.out(0) ~> flowA ~> zip.in0
        broadcast.out(1) ~> flowB ~> zip.in1

        FlowShape(broadcast.in, zip.out)
     })

      //fluid DSL
      val complexSink = {
        val mult = Random.nextInt()
        Flow[Double].map(_ * mult)
          .toMat(Sink.head[Double])(Keep.right)
      }

      val materializedValue = complexSource.viaMat(complexFlow)(Keep.right).toMat(complexSink)(Keep.right).run()
      val resultOutOfPipe = awaitForResult(materializedValue)

      resultOutOfPipe shouldBe 0

    }
  }

}
