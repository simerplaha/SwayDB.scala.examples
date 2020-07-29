package quickstart

import scala.concurrent.duration._

object QuickStart_Map_Simple extends App {

  import swaydb._
  import swaydb.serializers.Default._ //import default serializers

  implicit val bag = Bag.less

  //Create a memory database
  val map = memory.Map[Int, String, Nothing, Bag.Less]()

  map.put(key = 1, value = "one")
  map.get(key = 1).get //returns "one"
  map.expire(key = 1, after = 2.seconds)
  map.remove(key = 1)

  val keyValues = (1 to 100).map(key => (key, s"$key's value"))
  map.put(keyValues) //write 100 key-values atomically

  //Create a stream that updates all values within range 10 to 90.
  val updatedValuesStream =
    map
      .from(10)
      .stream
      .takeWhile(_._1 <= 90)
      .map {
        case (key, value) =>
          (key, value + " updated via Stream")
      }

  //submit the stream to update the key-values as a single transaction.
  map.put(updatedValuesStream)

  //print all key-values to see the updates.
  map
    .stream
    .foreach(println)
}
