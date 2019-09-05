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

class ExtendedDBSpec extends TestBase {

  "Extend the database and create nested maps" in {

    import swaydb._
    import swaydb.serializers.Default._ //import default serializers

    //Nested map hierarchy in this test
    //rootMap
    //   |____ subMap1
    //            |____ subMap2
    //

    //Create an extended database by calling .extend.
    //the Key and value should be of type Key[T] and Option[V] respectively for the extension to work.
    //the database returns a rootMap under which all other nested maps can be created.
    val rootMap = extensions.memory.Map[Int, String]().right.get.right.get
    rootMap.put(1, "root map's first key-value").get //insert key-value to root map

    val subMap1 = rootMap.maps.put(1, "sub map 1").get //create the first subMap
    subMap1.put(1, "one value").get //insert key-value to subMap

    val subMap2 = subMap1.maps.put(2, "sub map 2").get //create a nested subMap under subMap1
    subMap2.put(2, "two value").get //insert a key-value into sub-map 2

    subMap1
      .stream
      .materialize
      .get should contain only ((1, "one value")) //fetch all key-values of subMap1

    subMap2
      .stream
      .materialize
      .get should contain only ((2, "two value")) //fetch all key-values of subMap2

    //fetch all subMaps of subMap1
    subMap1
      .maps
      .stream
      .materialize
      .get should contain only ((2, "sub map 2"))

    //Here only the maps of the first
    rootMap
      .maps
      .stream
      .materialize
      .get should contain only ((1, "sub map 1"))
  }
}