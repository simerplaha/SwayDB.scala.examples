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

package base

import java.io.IOException
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

trait TestBase extends WordSpec with Matchers with BeforeAndAfterAll {

  def deleteDB: Boolean = true

  val dir =
    Paths.get(getClass.getClassLoader.getResource("").getPath)
      .getParent
      .getParent
      .resolve("TEST_DATABASES")
      .resolve(this.getClass.getSimpleName)

  def randomCharacters(size: Int = 10) = Random.alphanumeric.take(size).mkString

  def randomByte() = (Random.nextInt(256) - 128).toByte

  def randomBytes(size: Int = 10) = Array.fill(size)(randomByte())

  def sleep(time: FiniteDuration) = {
    println(s"Sleeping for: $time")
    Thread.sleep(time.toMillis)
  }

  def walkDeleteFolder(folder: Path): Unit =
    if (deleteDB && Files.exists(folder))
      Files.walkFileTree(folder, new SimpleFileVisitor[Path]() {
        @throws[IOException]
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }

        override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
          if (exc != null) throw exc
          Files.delete(dir)
          FileVisitResult.CONTINUE
        }
      })

  override protected def afterAll(): Unit =
    walkDeleteFolder(dir)

}
