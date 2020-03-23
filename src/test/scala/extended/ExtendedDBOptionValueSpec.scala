//package extended
//
//import base.TestBase
//import swaydb.data.slice.Slice
//import swaydb.serializers.Serializer
//
//class ExtendedDBOptionValueSpec extends TestBase {
//
//  "Value = Option[Option[String]]" in {
//
//    import swaydb._
//    import swaydb.serializers.Default._
//
//    implicit object OptionOptionStringSerializer extends Serializer[Option[Option[String]]] {
//      override def write(data: Option[Option[String]]): Slice[Byte] =
//        data.flatten map {
//          string =>
//            StringSerializer.write(string)
//        } getOrElse Slice.emptyBytes
//
//      override def read(data: Slice[Byte]): Option[Option[String]] =
//        if (data.isEmpty)
//          None
//        else
//          Some(Some(StringSerializer.read(data)))
//    }
//
//    val rootMap = extensions.memory.Map[Int, Option[String], Nothing]().right.get.right.get
//
//    rootMap.put(1, None).get
//    rootMap.put(2, Some("some value")).get
//
//    rootMap
//      .stream
//      .materialize.get shouldBe List((1, None), (2, Some("some value")))
//  }
//}