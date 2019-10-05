package quickstart

object QuickStart extends App {

  import swaydb._
  import swaydb.serializers.Default._ //import default serializers

  val map = memory.Map[Int, String, Nothing]().get //Create a memory database

  map.put(1, "one").get
  map.get(1).get
  map.remove(1).get

  //write 100 key-values atomically
  map.put(keyValues = (1 to 100).map(key => (key, key.toString)))

  //Iteration: fetch all key-values withing range 10 to 90, update values and atomically write updated key-values
  map
    .from(10)
    .takeWhile(_._1 <= 90)
    .map {
      case (key, value) =>
        (key, value + "_updated")
    }
    .materialize
    .flatMap(map.put)
    .get
}
