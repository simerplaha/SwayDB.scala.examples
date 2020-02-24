package quickstart

import base.UserTable

object QuickStartCreateExpireUser extends App {

  import base.UserTable._
  import swaydb._

  //functions should always be registered on database startup.
  implicit val functions = Map.Functions[UserKeys, UserValues, UserTable.UserFunctions]()
  functions.register(UserTable.UserFunctions.ExpireUserFunction)

  val map = memory.Map[UserKeys, UserValues, UserTable.UserFunctions, IO.ApiIO]().get //Create a memory database

  map.put(
    key = UserKeys.UserName("iron_man"),
    value = UserValues.ActiveUser(name = "Tony Stark", email = "tony@stark.com", lastLogin = System.nanoTime())
  ).get

  //get the user
  println("User status: " + map.get(UserKeys.UserName("iron_man")).get) //User status: Some(ActiveUser(Tony Stark,tony@stark.com,705876613043474))

  //expire user using the registered ExpireFunction
  map.applyFunction(UserKeys.UserName("iron_man"), UserTable.UserFunctions.ExpireUserFunction).get

  println("User status: " + map.get(UserKeys.UserName("iron_man")).get) //User status: Some(ExpiredUser)
}
