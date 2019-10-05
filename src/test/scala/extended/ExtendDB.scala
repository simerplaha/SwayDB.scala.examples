package extended

object ExtendDB extends App {

  import swaydb._
  import swaydb.serializers.Default._
  //including the above import all include the extension api.

  //add .extend to enable extension
  val rootMap = extensions.memory.Map[String, String, Nothing]().right.get.right.get

  //Nested map hierarchy
  //rootMap
  //   |____ subMap1
  //            |____ subMap2
  //
  //now we can create nested maps.
  val subMap1 =
  rootMap
    .maps
    .put(key = "sub map 1", value = "another map").get

  val subMap2 =
    subMap1
      .maps
      .put(key = "sub map 2", value = "another nested map").get

}
