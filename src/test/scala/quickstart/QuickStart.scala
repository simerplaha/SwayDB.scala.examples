package quickstart

object QuickStart extends App {

  import swaydb._
  import swaydb.serializers.Default._ //import default serializers

  val db = memory.Map[Int, String]().get //Create a memory database

  db.put(1, "one").get
  db.get(1).get
  db.remove(1).get

  //write 100 key-values atomically
  (1 to 100) map {
    key =>
      (key, key.toString)
  } andThen {
    keyValues =>
      db.put(keyValues)
  }

  //Iteration: fetch all key-values withing range 10 to 90, update values and atomically write updated key-values
  db
    .from(10)
    .takeWhileKey(_ <= 90)
    .map {
      case (key, value) =>
        (key, value + "_updated")
    }
    .flatMap(_.toSeq.flatMap(db.put))
    .get
}
