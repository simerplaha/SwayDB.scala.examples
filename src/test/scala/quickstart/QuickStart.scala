package quickstart

import scala.concurrent.duration._

object QuickStart extends App {

  import swaydb._
  import swaydb.serializers.Default._ //import default serializers

  type FunctionType = PureFunction[Int, String, Apply.Map[String]] //create the type of Function that can be registered in this Map.

  val map = memory.Map[Int, String, FunctionType, IO.ApiIO]().get //Create a memory database
  val set = memory.Set[String, FunctionType, IO.ApiIO]().get

//just for demo lets create a function that updates the value of a key with some String.
val function: PureFunction.OnKey[Int, String, Apply.Map[String]] =
  (key: Int, deadline: Option[Deadline]) => Apply.Update("value update for key: " + key)

//All APIs can be combined to perform a batch transaction.
map.commit(
  Prepare.Put(key = 1, value = "one value"),
  Prepare.Put(key = 1, value = "one value", expireAfter = 10.seconds),
  Prepare.Remove(key = 1),
  Prepare.Remove(from = 2, to = 100),
  Prepare.Expire(key = 1, after = 10.seconds),
  Prepare.Expire(from = 2, to = 100, after = 1.day),
  Prepare.Update(key = 1, value = "value updated"),
  Prepare.Update(from = 2, to = 100, value = "range update"),
  Prepare.ApplyFunction(key = 1, function = function),
  Prepare.ApplyFunction(from = 2, to = 100, function = function)
)

}