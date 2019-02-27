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
  * Each User has a dedicated User Map which stores
  * Email, Password & also contains another Map of Products the user owns.
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
    val rootMap = memory.Map[Key[MapKey], Option[String]]().get.extend.get

    /** ************************************************************
      * ************************ WRITES ****************************
      * ************************************************************/
    val userMap = //create User1's map under rootMap
      rootMap
        .maps
        .put(MapKey.UserMap(1), "User 1's map").get
    //under userMap write the Email and Password of the User
    userMap.put(MapKey.Email, "user1@email.com").get
    userMap.put(MapKey.Password, "thePassword").get
    //under userMap also create another Map that contains the Products this user owns
    val productsMap =
      userMap
        .maps
        .put(MapKey.Products, "Map for products User1 owns").get
    //add a product to the User's product Map
    productsMap.put(MapKey.Product("MacBook Pro"), "2.8 GHz Intel Core i7").get

    /** ************************************************************
      * ************************ READS *****************************
      * ************************************************************/
    val user1Map = //get the User map from rootMap
      rootMap
        .maps
        .get(MapKey.UserMap(1)).get.get
    //User map contains User's Email and Password.
    user1Map.get(MapKey.Email).get should contain("user1@email.com")
    user1Map.get(MapKey.Password).get should contain("thePassword")
    //get the product's map from User's map
    val user1sProductsMap =
      user1Map
        .maps
        .get(MapKey.Products).get.get
    //products map contains Product info.
    user1sProductsMap.get(MapKey.Product("MacBook Pro")).get should contain("2.8 GHz Intel Core i7")

  }
}