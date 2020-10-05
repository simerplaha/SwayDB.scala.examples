package configuringlevels

import org.scalatest.{Matchers, WordSpec}
import swaydb.data.config.{ForceSave, MMAP}
import swaydb.data.util.OperatingSystem

class ConfiguringMMAPAndForceSave extends WordSpec with Matchers {

  "Example configuration for MMAP and ForceSave" in {
    /**
     * For Java version of this test see
     * https://github.com/simerplaha/SwayDB.java.examples/blob/master/src/main/java/configurations/ConfiguringMMAPAndForceSaveTest.java
     */
    import swaydb._
    import swaydb.serializers.Default._

    val mmapEnabled =
      MMAP.On(
        //delete after MMAP are cleaned only on Windows.
        deleteAfterClean = OperatingSystem.isWindows,
        forceSave =
          //enable force safe to run before cleaning MMAP files.
          ForceSave.BeforeClean(
            enableBeforeCopy = false, //disabled applying forceSave before copying MMAP files
            enableForReadOnlyMode = false, //disable forceSave for read-only MMAP files
            logBenchmark = true //log time take to execute force-save.
          )
      )

    //create map and apply the above MMAP setting to all files - maps, appendices and segments.
    val map =
      persistent.Map[Int, String, Nothing, Bag.Less](
        dir = s"target/${classOf[ConfiguringMMAPAndForceSave].getSimpleName}",
        mmapMaps = mmapEnabled,
        mmapAppendix = mmapEnabled,
        segmentConfig = persistent.DefaultConfigs.segmentConfig().copyWithMmap(mmapEnabled)
      )

    map.put(1, "one")
    map.get(1) shouldBe Some("one")
    map.delete()

    /**
     * The following demos other [[ForceSave]] configurations.
     */
    //disabled force save
    val disabledForceSave =
    ForceSave.Off

    //enables forceSave before clean
    val forceSaveBeforeClean =
      ForceSave.BeforeClean(
        enableBeforeCopy = false,
        enableForReadOnlyMode = false,
        logBenchmark = true
      )

    //forceSave before copying files
    val forceSave =
      ForceSave.BeforeCopy(
        enableForReadOnlyMode = false,
        logBenchmark = true
      )

    //forceSave before closing files
    val forceSaveBeforeClose =
      ForceSave.BeforeClose(
        enableBeforeCopy = false,
        enableForReadOnlyMode = false,
        logBenchmark = true
      )
  }
}
