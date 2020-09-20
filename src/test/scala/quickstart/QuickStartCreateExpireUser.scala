package quickstart

import base.UserTable
import swaydb.data.Functions

object QuickStartCreateExpireUser extends App {

  import base.UserTable._
  import swaydb._

  //functions should always be registered on database startup.
  implicit val functions = Functions[PureFunction.Map[UserKey, UserValue]](UserTable.expireUserFunction)

  val map = memory.Map[UserKey, UserValue, PureFunction.Map[UserKey, UserValue], Bag.Less]() //Create a memory database

  map.put(
    key = UserKey.UserName("iron_man"),
    value = UserValue.ActiveUser(name = "Tony Stark", email = "tony@stark.com", lastLogin = System.nanoTime())
  )

  //get the user
  println("User status: " + map.get(UserKey.UserName("iron_man")).get) //User status: Some(ActiveUser(Tony Stark,tony@stark.com,705876613043474))

  //expire user using the registered ExpireFunction
  map.applyFunction(UserKey.UserName("iron_man"), UserTable.expireUserFunction)

  println("User status: " + map.get(UserKey.UserName("iron_man")).get) //User status: Some(ExpiredUser)
}
