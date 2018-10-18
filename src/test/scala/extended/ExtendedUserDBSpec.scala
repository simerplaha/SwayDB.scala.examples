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

package extended

import base.TestBase
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import swaydb.data.slice.Slice
import swaydb.serializers.Serializer

/**
  * Each User has a dedicated User Map which stores Email, Password & a Map of Product user purchased.
  */
//Map hierarchy looks like this
//RootMap
//   |____ User1Map
//   |        |_____ Email
//   |        |_____ Password
//   |        |_____ ProductsMap
//   |                    |__________ Product("MacBook Pro")
//   |                    |__________ Product("Some other product")
//   |____ User2Map
//            |_____ Email
//            |_____ Password
//            |_____ ProductsMap
//                       |__________ Product("some product1")
//                       |__________ Product("some product2")

sealed trait MapKey
case object MapKey {
  case class UserMap(userId: Int) extends MapKey
  case object Email extends MapKey
  case object Password extends MapKey
  case object Products extends MapKey
  case class Product(name: String) extends MapKey
}

class ExtendedUserDBSpec extends TestBase {

  "Extend the database" in {

    import swaydb._
    import swaydb.extension._
    import swaydb.serializers.Default._

    /**
      * Circe JSON serializer
      */
    implicit val serializer = new Serializer[MapKey] {
      override def write(data: MapKey): Slice[Byte] =
        Slice.writeString(data.asJson.noSpaces)

      override def read(data: Slice[Byte]): MapKey =
        decode[MapKey](data.readString()).toTry.get
    }

    //creating
    val rootMap = SwayDB.memory[Key[MapKey], Option[String]]().assertSuccess.extend.assertSuccess

    //create User1's map under rootMap
    val userMap = rootMap.maps.put(MapKey.UserMap(1), "User 1's map").assertSuccess
    //under userMap write the Email and Password of the User
    userMap.put(MapKey.Email, "user1@email.com").assertSuccess
    userMap.put(MapKey.Password, "thePassword").assertSuccess
    //under userMap also create another Map that contains the Products this user owns
    val productsMap = userMap.maps.put(MapKey.Products, "Map for products User1 owns").assertSuccess
    productsMap.put(MapKey.Product("MacBook Pro"), "2.8 GHz Intel Core i7").assertSuccess

    //read and assert the above map hierarchy.
    val user1Map = rootMap.maps.get(MapKey.UserMap(1)).assertGet
    user1Map.get(MapKey.Email).assertGet shouldBe "user1@email.com"
    user1Map.get(MapKey.Password).assertGet shouldBe "thePassword"
    val user1sProductsMap = user1Map.maps.get(MapKey.Products).assertGet
    user1sProductsMap.get(MapKey.Product("MacBook Pro")).assertGet shouldBe "2.8 GHz Intel Core i7"

  }
}