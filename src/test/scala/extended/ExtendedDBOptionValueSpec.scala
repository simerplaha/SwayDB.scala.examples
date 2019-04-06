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
import swaydb.data.slice.Slice
import swaydb.serializers.Serializer

class ExtendedDBOptionValueSpec extends TestBase {

  "Value = Option[Option[String]]" in {

    import swaydb._
    import swaydb.serializers.Default._

    implicit object OptionOptionStringSerializer extends Serializer[Option[Option[String]]] {
      override def write(data: Option[Option[String]]): Slice[Byte] =
        data.flatten map {
          string =>
            StringSerializer.write(string)
        } getOrElse Slice.emptyBytes

      override def read(data: Slice[Byte]): Option[Option[String]] =
        if (data.isEmpty)
          None
        else
          Some(Some(StringSerializer.read(data)))
    }

    val rootMap = extensions.memory.Map[Int, Option[String]]().get

    rootMap.put(1, None).get
    rootMap.put(2, Some("some value")).get

    rootMap
      .stream
      .materialize.get shouldBe List((1, None), (2, Some("some value")))
  }
}