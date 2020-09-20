package functions

import base.TestBase
import swaydb.PureFunctionScala.OnValue
import swaydb._
import swaydb.data.Functions
import swaydb.serializers.Default._

import scala.collection.parallel.CollectionConverters._

class LikesSpec extends TestBase {

  "increment likes count" in {
    //function that increments likes by 1
    //in SQL this would be "UPDATE LIKES_TABLE SET LIKES = LIKES + 1"

    val incrementLikes: OnValue[Int] =
      (likes: Int) =>
        Apply.Update(likes + 1)

    implicit val functions: Functions[PureFunction.Map[String, Int]] = Functions(incrementLikes)

    val likesMap = memory.Map[String, Int, PureFunction.Map[String, Int], IO.ApiIO]().get //create likes database map.

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
