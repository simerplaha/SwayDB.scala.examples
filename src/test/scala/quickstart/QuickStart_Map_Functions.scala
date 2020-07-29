package quickstart

import scala.concurrent.duration._

object QuickStart_Map_Functions extends App {

  import swaydb._
  import swaydb.serializers.Default._ //import default serializers

  implicit val bag = Bag.less

  //create a function that reads key & value and applies modifications.
  val function: PureFunction.OnKeyValue[Int, String, Apply.Map[String]] =
    (key: Int, value: String, deadline: Option[Deadline]) =>
      //some random logic demoing all Apply types.
      if (key < 25)
        Apply.Remove //remove if key is less than 25
      else if (key < 50)
        Apply.Expire(2.seconds) //expire after 2 seconds if key is less than 50
      else if (key < 75)
        Apply.Update(value + " and then updated from function", deadline.map(_ + 10.seconds)) //update value and increment deadline by 10.seconds if key is < 75.
      else //or else do nothing
        Apply.Nothing

  type FunctionType = PureFunction[Int, String, Apply.Map[String]] //create the type of Function that can be registered in this Map.

  //register the function
  implicit val functions = swaydb.Map.Functions[Int, String, FunctionType](function)

  //Create a memory database
  val map = memory.Map[Int, String, FunctionType, Bag.Less]()

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

  //apply the function to all key-values ranging 1 to 100.
  map.applyFunction(from = 1, to = 100, function = function)

  //print all key-values to see the updates.
  map
    .stream
    .foreach(println)
}
