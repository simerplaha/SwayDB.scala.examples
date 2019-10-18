package quickstart

import scala.concurrent.duration._

object QuickStart extends App {

  import swaydb._
  import swaydb.serializers.Default._ //import default serializers

  type FunctionType = PureFunction[Int, String, Apply.Map[String]] //create the type of Function that can be registered in this Map.

  val map = memory.Map[Int, String, FunctionType, IO.ApiIO]().get //Create a memory database

  map.put(key = 1, value = "one").get //basic put
  map.get(key = 1).get //basic get
  map.expire(key = 1, after = 2.seconds).get //basic expire
  map.remove(key = 1).get //basic remove

  val keyValues = (1 to 100).map(key => (key, s"$key's value"))
  map.put(keyValues).get //write 100 key-values atomically

  //Read and update: Stream all key-values withing range 10 to 90, update values and atomically write updated key-values
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

  //create a function that reads key & value and applies modifications.
  val function: PureFunction.OnKeyValue[Int, String, Apply.Map[String]] =
    (key: Int, value: String, deadline: Option[Deadline]) =>
      if (key < 25) //remove if key is less than 25
        Apply.Remove
      else if (key < 50) //expire after 2 seconds if key is less than 50
        Apply.Expire(2.seconds)
      else if (key < 75) //update value and increment deadline by 10.seconds if key is < 75.
        Apply.Update(value + " value updated from function", deadline.map(_ + 10.seconds))
      else
        Apply.Nothing //or else do nothing

  map.registerFunction(function).get //register the function

  //apply the function to all key-values ranging 1 to 100.
  map.applyFunction(from = 1, to = 100, function = function).get

  //print all key-values to see the updates.
  map
    .foreach(println)
    .materialize
    .get
}
