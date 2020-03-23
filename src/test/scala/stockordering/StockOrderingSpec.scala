package stockordering

import base.TestBase
import swaydb.data.order.KeyOrder
import swaydb.data.slice.Slice
import swaydb.data.util.ByteSizeOf
import swaydb.serializers.Serializer

import scala.util.Random

class StockOrderingSpec extends TestBase {

  "Stock order example demonstrating partial key ordering" in {

    import swaydb._

    implicit val bag = Bag.apiIO

    case class StockOrder(orderId: Int,
                          price: Int,
                          purchaseTime: Long, //should use DateTime here but keeping it simple for the example.
                          stockCode: String)

    //build a custom serialiser for StockOrder type. You can also use Circe for this. See other examples.
    implicit object StockOrderSerializer extends Serializer[StockOrder] {
      override def write(data: StockOrder): Slice[Byte] = {
        val stockCodeBytes = data.stockCode.getBytes
        //2 ints + 1 long + stock bytes
        Slice.create((ByteSizeOf.int * 2) + ByteSizeOf.long + stockCodeBytes.length)
          .addInt(data.orderId)
          .addInt(data.price)
          .addLong(data.purchaseTime)
          .addAll(stockCodeBytes)
      }

      override def read(data: Slice[Byte]): StockOrder = {
        val reader = data.createReader()
        StockOrder(
          orderId = reader.readInt(),
          price = reader.readInt(),
          purchaseTime = reader.readLong(),
          stockCode = reader.readRemainingAsString()
        )
      }
    }

    import scala.math.Ordered.orderingToOrdered

    //typed key-order
    implicit val typedStockOrdering: KeyOrder[StockOrder] =
      (order1: StockOrder, order2: StockOrder) =>
        (order1.stockCode, order1.purchaseTime, order1.orderId) compare
          (order2.stockCode, order2.purchaseTime, order2.orderId)

    val db = persistent.Set[StockOrder, Nothing, IO.ApiIO](dir = dir.resolve("stockOrdersDB")).get

    //shuffle ids so that data gets inserted in random order
    Random.shuffle(1 to 15).foreach {
      i: Int =>
        if (i <= 5) //it's Apple
        db add StockOrder(i, i, System.currentTimeMillis(), "AAPL")
        else if (i <= 10) //it's Google
        db add StockOrder(i, i, System.currentTimeMillis(), "GOOGL")
        else //it's Tesla
        db add StockOrder(i, i, System.currentTimeMillis(), "TSLA")
    }

    val expected =
      List(
        "AAPL", "AAPL", "AAPL", "AAPL", "AAPL",
        "GOOGL", "GOOGL", "GOOGL", "GOOGL", "GOOGL",
        "TSLA", "TSLA", "TSLA", "TSLA", "TSLA"
      )

    //stockCodes are in order of AAPL, GOOGL and then TSLA
    db
      .stream
      .map(_.stockCode)
      .materialize
      .get shouldBe expected

    db
      .stream
      .foreach(println)
      .get
  }

}
