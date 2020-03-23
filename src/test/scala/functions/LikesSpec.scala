package functions

import base.TestBase
import swaydb._
import swaydb.serializers.Default._ //import default serializers

import scala.collection.parallel.CollectionConverters._

class LikesSpec extends TestBase {

  "increment likes count" in {
    //function that increments likes by 1
    //in SQL this would be "UPDATE LIKES_TABLE SET LIKES = LIKES + 1"

    val incrementLikes: PureFunction.OnValue[Int, Apply.Map[Int]] =
      (currentLikes: Int) =>
        Apply.Update(currentLikes + 1)

    implicit val functions = Map.Functions[String, Int, PureFunction[String, Int, Apply.Map[Int]]](incrementLikes)

    val likesMap = memory.Map[String, Int, PureFunction[String, Int, Apply.Map[Int]], IO.ApiIO]().get //create likes database map.

    likesMap.put(key = "SwayDB", value = 0) //initial entry with 0 likes.

    //concurrently apply 100 likes to key SwayDB using the above registered function's functionId.
    (1 to 100).par foreach {
      _ =>
        likesMap.applyFunction("SwayDB", incrementLikes).get
    }

    //total likes == 100
    likesMap.get("SwayDB").get should contain(100)
  }
}