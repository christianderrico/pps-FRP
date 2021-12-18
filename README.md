# PPS-FRP
Studio e sperimentazione del paradigma Functional Reactive Programming in Scala.

Sono stati presi in considerazione due solution framework scala per il supporto alla programmazione reattiva:
- `Reactive Monix`
- `Akka Streams`

Gli esperimenti condotti sono stati concepiti come behavioral test di alcuni aspetti ritenuti più significativi durante l'esperienza maturata con tali tecnologie:
- Reactive Monix: `test.scala.Experiments.ReactiveMonix`
- AkkaStreams: `test.scala.Experiments.AkkaStreams`

Come mini-app che fornisse un esempio d'uso di dev-ops non banale, sono state prodotte due versioni nei due toolkit del [John Conway's Game of Life](https://it.wikipedia.org/wiki/Gioco_della_vita):
- La versione `Reactive Monix` comprende una view che permette di visualizzare e manipolare l'evoluzione dell'automa e di avviare o arrestare la ciclica computazione delle generazioni: `main.scala.ReactiveGameOfLife.ReactiveMonix`
- La versione `Akka Streams` risulta più minimale e avvia il calcolo delle iterazioni partendo da una configurazione iniziale random del world di gioco: `main.scala.ReactiveGameOfLife.Akka`

Il report del progetto è disponibile nella main directory: `Report PPS_FRP`

## Usage
- Clone del repository
- Monix Game Of Life cmd: `sbt "runMain ReactiveGameOfLife.ReactiveMonix.Main"`
- Akka Game Of Life cmd: `sbt "runMain ReactiveGameOfLife.Akka.AkkaGameOfLife"`

## Authors
- Christian D'Errico ([christianderrico](https://github.com/christianderrico))

