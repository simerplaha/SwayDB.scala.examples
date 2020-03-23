package monixExample

import base.TestBase
import monix.eval.Task
import swaydb.serializers.Default._

import scala.util.Random

class MonixSimpleExample extends TestBase {

  "Simple monix example without functions" in {
    implicit val scheduler = monix.execution.Scheduler.global
    import swaydb.monix.Bag._ //provide monix tag to support Task.

    //Create a memory database without functions support (F: Nothing). See MonixExample.scala for an example with function.
    val map = swaydb.memory.Map[Int, String, Nothing, Task]().get

    //create some random key-values
    val keyValues =
      (1 to 100) map {
        key =>
          (key, Random.alphanumeric.take(10).mkString)
      }

    val mapSize: Task[Int] =
      map
        .put(keyValues) //write 100 key-values as single transaction/batch.
        .flatMap {
          _ =>
            //print all key-values
            map.stream.foreach(println).materialize
        }
        .flatMap {
          _ =>
            //result the size of the database.
            map.stream.size
        }

    //above tasks are not executed yet so the database stream will return empty
    map.stream.materialize.awaitTask shouldBe empty
    //execute the task above so it persists the above 100 key-values.
    mapSize.awaitTask shouldBe 100
  }
}
