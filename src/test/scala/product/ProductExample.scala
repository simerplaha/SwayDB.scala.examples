package product

import boopickle.Default._
import org.joda.time.DateTime
import swaydb.PureFunctionScala.OnValueDeadline
import swaydb._
import swaydb.data.Functions
import swaydb.serializers.Default._

import scala.concurrent.duration.{Deadline, _}

//An example using Joda-time.
object ProductExample extends App {

  implicit val deadlinePickler = transformPickler(DateTime.parse)(_.toString)
  implicit val serializer = swaydb.serializers.BooPickle[Product]

  case class Product(name: String,
                     price: Double,
                     discountPrice: Double,
                     lastPurchased: DateTime)

  val function: OnValueDeadline[Product] =
    (product: Product, deadline: Option[Deadline]) =>
      if (deadline.exists(_.isOverdue()) || product.discountPrice < 0)
        Apply.Remove
      else if (product.lastPurchased.plusDays(10).isAfterNow)
        Apply.Expire(deadline.get.timeLeft + 1.day)
      else if (product.lastPurchased.plusDays(10).isBeforeNow)
        Apply.Update(product.copy(discountPrice = product.discountPrice / 2))
      else
        Apply.Nothing

  implicit val functions: Functions[PureFunction.Map[Int, Product]] = Functions(function)

  val map = memory.Map[Int, Product, PureFunction.Map[Int, Product], Bag.Less]()

  val product =
    Product(
      name = "1",
      price = 1,
      discountPrice = 1,
      lastPurchased = DateTime.now()
    )

  map.put(key = 1, value = product, expireAfter = 10.days)

  println(map.get(1))

}
