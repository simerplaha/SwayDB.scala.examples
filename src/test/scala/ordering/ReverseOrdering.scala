package ordering

import org.scalatest.{Matchers, WordSpec}
import swaydb.data.order.KeyOrder

class ReverseOrdering extends WordSpec with Matchers {

  "reverse" in {
    import swaydb._
    import swaydb.serializers.Default._

    implicit val reverseOrder =
      new KeyOrder[Int] {
        override def compare(x: Int, y: Int): Int =
          (x compare y) * -1
      }

    val map = memory.Map[Int, Int, Nothing, Bag.Less]()

    (1 to 100).foreach(int => map.put(int, int))

    map
      .keys
      .stream
      .materialize
      .toList should contain inOrderElementsOf (1 to 100).reverse
  }
}
