/*
 * Copyright (C) 2018 Simer Plaha (@simerplaha)
 *
 * This file is a part of SwayDB.
 *
 * SwayDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * SwayDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with SwayDB. If not, see <https://www.gnu.org/licenses/>.
 */

package stockordering

import base.TestBase
import swaydb.data.slice.Slice
import swaydb.data.util.ByteSizeOf
import swaydb.serializers.Serializer

import scala.util.Random

class StockOrderingSpec extends TestBase {

  "Stock order example demonstrating partial key ordering" in {

    import swaydb._

    case class StockOrder(orderId: Int,
                          price: Int,
                          purchaseTime: Long, //should use DateTime here but keeping it simple for the example.
                          stockCode: String)

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

    implicit val stockOrderOrdering: Ordering[Slice[Byte]] =
      Ordering.by[Slice[Byte], StockOrder](StockOrderSerializer.read) {
        (order1: StockOrder, order2: StockOrder) =>
          (order1.stockCode, order1.purchaseTime, order1.orderId) compare
            (order2.stockCode, order2.purchaseTime, order2.orderId)
      }

    val db = SwayDB.persistentSet[StockOrder](dir = dir.resolve("stockOrdersDB")).assertSuccess

    //shuffle ids so that data gets inserted in random order
    Random.shuffle(1 to 15) foreach {
      i =>
        if (i <= 5) //it's Apple
          db add StockOrder(i, i, System.currentTimeMillis(), "AAPL")
        else if (i <= 10) //it's Google
          db add StockOrder(i, i, System.currentTimeMillis(), "GOOGL")
        else //it's Tesla
          db add StockOrder(i, i, System.currentTimeMillis(), "TSLA")
    }
    //stockCodes are in order of AAPL, GOOGL and then TSLA
    db.map(_.stockCode) shouldBe
      List(
        "AAPL", "AAPL", "AAPL", "AAPL", "AAPL",
        "GOOGL", "GOOGL", "GOOGL", "GOOGL", "GOOGL",
        "TSLA", "TSLA", "TSLA", "TSLA", "TSLA"
      )

    db foreach println

  }

}
