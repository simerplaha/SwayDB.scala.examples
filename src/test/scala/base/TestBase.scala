package base

import java.io.IOException
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

trait TestBase extends WordSpec with Matchers with BeforeAndAfterAll {

  val dir =
    Files.createTempDirectory(this.getClass.getSimpleName)

  def deleteDB: Boolean = true

  //  override protected def afterAll(): Unit =
  //    walkDeleteFolder(dir)

  override protected def beforeAll(): Unit = {
    if (!Files.exists(dir)) Files.createDirectories(dir)
    super.beforeAll()
  }

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

}
