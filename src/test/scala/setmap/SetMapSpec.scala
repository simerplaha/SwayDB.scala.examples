package setmap

import org.scalatest.{Matchers, WordSpec}
import org.scalatest.OptionValues._

class SetMapSpec extends WordSpec with Matchers {

  import swaydb._
  import swaydb.serializers.Default._

  "example" in {
    val map = memory.SetMap[Int, String, Glass]()

    map.put(1, "one")
    map.get(1).value shouldBe "one"
  }
}
