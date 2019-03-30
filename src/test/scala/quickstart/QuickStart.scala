package quickstart

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import swaydb.Wrap._

object QuickStart extends App {

  import swaydb._
  import swaydb.serializers.Default._ //import default serializers

  val db = memory.Map[Int, String]().get.blockingAPI[Try] //Create a memory database

  db.put(1, "one").get
  db.get(1).get
  db.remove(1).get

  //write 100 key-values atomically
  (1 to 100) map {
    key =>
      (key, key.toString)
  } andThen {
    keyValues =>
      db.put(keyValues).get
  }

  //write 100 key-values
  (1 to 100) foreach { i => db.put(key = i, value = i.toString).get }

  //Iteration: fetch all key-values withing range 10 to 90, update values and atomically write updated key-values
  db
    .from(10)
    .takeWhileKey(_ <= 90)
    .map {
      case (key, value) =>
        (key, value + "_updated")
    }
    .flatMap(_.toSeq)
    .flatMap(db.put)
    .get
}
