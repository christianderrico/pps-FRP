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
- La versione `Akka Streams` risulta più minimale e avvia il calcolo delle iterazioni di gioco partendo da una configurazione iniziale random del world di gioco: `main.scala.ReactiveGameOfLife.Akka`

## Authors
- Christian D'Errico ([christianderrico](https://github.com/christianderrico))

