package product

import io.circe.{Decoder, Encoder, Json}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import swaydb.data.slice.Slice
import swaydb.serializers.Serializer

import scala.concurrent.duration.Deadline
import scala.concurrent.duration._

//An example using Joda-time.
object ProductExample extends App {

  import io.circe.generic.auto._
  import io.circe.parser._
  import io.circe.syntax._
  import swaydb._
  import swaydb.serializers.Default._

  val dateFormatter = DateTimeFormat.forPattern("yyyyMMdd")

  implicit val decodeDateTime: Decoder[DateTime] =
    Decoder.decodeString.emap {
      string =>
        Right(DateTime.parse(string, dateFormatter))
    }

  implicit val encodeDateTime: Encoder[DateTime] =
    new Encoder[DateTime] {
      override def apply(a: DateTime): Json =
        dateFormatter.print(a).asJson
    }

  implicit object UserKeySerializer extends Serializer[Product] {
    override def write(data: Product): Slice[Byte] =
      Slice.writeString(data.asJson.noSpaces)

    override def read(data: Slice[Byte]): Product =
      decode[Product](data.readString()).right.get
  }

  case class Product(name: String,
                     price: Double,
                     discountPrice: Double,
                     lastPurchased: DateTime)

  type Function = PureFunction[Int, Product, Apply.Map[Product]]

  val function: PureFunction.OnKeyValue[Int, Product, Apply.Map[Product]] =
    (_: Int, product: Product, deadline: Option[Deadline]) =>
      if (deadline.exists(_.isOverdue()) || product.discountPrice < 0)
        Apply.Remove
      else if (product.lastPurchased.plusDays(10).isAfterNow)
        Apply.Expire(deadline.get.timeLeft + 1.day)
      else if (product.lastPurchased.plusDays(10).isBeforeNow)
        Apply.Update(product.copy(discountPrice = product.discountPrice / 2))
      else
        Apply.Nothing

  implicit val functions: Map.Functions[Int, Product, Function] = Map.Functions(function)

  val map = memory.Map[Int, Product, Function, Bag.Less]()

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
