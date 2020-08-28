package configuringlevels

import java.nio.file.Files
import java.util.concurrent.Executors

import swaydb.configs.level.SingleThreadFactory
import swaydb.data.accelerate.{Accelerator, LevelZeroMeter}
import swaydb.data.compaction.{CompactionExecutionContext, LevelMeter, Throttle}
import swaydb.data.compression.{LZ4Compressor, LZ4Decompressor, LZ4Instance}
import swaydb.data.config.IOAction
import swaydb.data.config.{ConfigWizard, MMAP, RecoveryMode, _}
import swaydb.data.order.KeyOrder
import swaydb.data.util.OperatingSystem

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object ConfiguringLevels extends App {

  import swaydb._
  import swaydb.serializers.Default._

  val dir = Files.createTempDirectory(this.getClass.getSimpleName)

  val myTestSingleThreadExecutionContext =
    new ExecutionContext {
      val threadPool = Executors.newSingleThreadExecutor(SingleThreadFactory.create())

      def execute(runnable: Runnable) =
        threadPool execute runnable

      def reportFailure(exception: Throwable): Unit =
        println(s"REPORT FAILURE! ${exception.getMessage}")
    }

  val config =
    ConfigWizard
      .withPersistentLevel0( //level0
        dir = dir.resolve("level0"),
        mapSize = 4.mb,
        mmap = MMAP.Enabled(OperatingSystem.isWindows, ForceSave.Disabled),
        compactionExecutionContext = CompactionExecutionContext.Create(myTestSingleThreadExecutionContext),
        recoveryMode = RecoveryMode.ReportFailure,
        acceleration =
          (meter: LevelZeroMeter) =>
            Accelerator.cruise(meter),
        throttle =
          meter => {
            val mapsCount = meter.mapsCount
            if (mapsCount > 3)
              Duration.Zero
            else if (mapsCount > 2)
              1.second
            else
              30.seconds
          }
      )
      .withMemoryLevel1( //level1
        minSegmentSize = 4.mb,
        maxKeyValuesPerSegment = 100000,
        copyForward = false,
        deleteSegmentsEventually = true,
        compactionExecutionContext = CompactionExecutionContext.Shared,
        throttle =
          (levelMeter: LevelMeter) =>
            if (levelMeter.levelSize > 1.gb)
              Throttle(pushDelay = Duration.Zero, segmentsToPush = 10)
            else
              Throttle(pushDelay = Duration.Zero, segmentsToPush = 0)
      )
      .withPersistentLevel( //level2
        dir = dir.resolve("level2"),
        otherDirs = Seq(Dir("/Disk2", 1), Dir("/Disk3", 3)),
        mmapAppendix = MMAP.Enabled(OperatingSystem.isWindows, ForceSave.Disabled),
        appendixFlushCheckpointSize = 4.mb,
        sortedKeyIndex =
          SortedKeyIndex.Enable(
            prefixCompression = PrefixCompression.Disable(normaliseIndexForBinarySearch = true),
            enablePositionIndex = true,
            blockIOStrategy = ioAction => IOStrategy.SynchronisedIO(cacheOnAccess = true),
            compressions = _ => Seq.empty
          ),
        randomKeyIndex =
          RandomKeyIndex.Enable(
            maxProbe = 1,
            minimumNumberOfKeys = 5,
            minimumNumberOfHits = 2,
            indexFormat = IndexFormat.CopyKey,
            allocateSpace = _.requiredSpace,
            blockIOStrategy = ioAction => IOStrategy.SynchronisedIO(cacheOnAccess = true),
            compression = _ => Seq.empty
          ),
        binarySearchIndex =
          BinarySearchIndex.FullIndex(
            minimumNumberOfKeys = 10,
            searchSortedIndexDirectly = true,
            indexFormat = IndexFormat.CopyKey,
            blockIOStrategy = ioAction => IOStrategy.SynchronisedIO(cacheOnAccess = true),
            compression = _ => Seq.empty
          ),
        mightContainKeyIndex =
          MightContainIndex.Enable(
            falsePositiveRate = 0.01,
            minimumNumberOfKeys = 10,
            updateMaxProbe = optimalMaxProbe => 1,
            blockIOStrategy = ioAction => IOStrategy.SynchronisedIO(cacheOnAccess = true),
            compression = _ => Seq.empty
          ),
        valuesConfig =
          ValuesConfig(
            compressDuplicateValues = true,
            compressDuplicateRangeValues = true,
            blockIOStrategy = ioAction => IOStrategy.SynchronisedIO(cacheOnAccess = true),
            compression = _ => Seq.empty
          ),
        segmentConfig =
          SegmentConfig(
            cacheSegmentBlocksOnCreate = true,
            deleteSegmentsEventually = true,
            pushForward = true,
            mmap = MMAP.Disabled(ForceSave.Disabled),
            minSegmentSize = 4.mb,
            maxKeyValuesPerSegment = 100000,
            fileOpenIOStrategy = IOStrategy.SynchronisedIO(cacheOnAccess = true),
            blockIOStrategy = {
              case IOAction.ReadDataOverview =>
                IOStrategy.SynchronisedIO(cacheOnAccess = true)

              case action: IOAction.DecompressAction =>
                IOStrategy.SynchronisedIO(cacheOnAccess = action.isCompressed)
            },
            compression = _ =>
              Seq(
                Compression.LZ4(
                  compressor = (LZ4Instance.FastestJava, LZ4Compressor.Fast(minCompressionSavingsPercent = 20.0)),
                  decompressor = (LZ4Instance.FastestJava, LZ4Decompressor.Fast)
                ),
                Compression.Snappy(minCompressionPercentage = 20.0),
                Compression.None
              )
          ),
        compactionExecutionContext = CompactionExecutionContext.Shared,
        throttle =
          (levelMeter: LevelMeter) => {
            val delay = (5 - levelMeter.segmentsCount).seconds
            val batch = levelMeter.segmentsCount min 5
            Throttle(delay, batch)
          }
      )
      .withTrashLevel //level3

  implicit val ordering = KeyOrder.default //import default sorting

  val db = //initialise the database with the above configuration
    SwayDB[Int, String, Nothing](
      config = config,
      fileCache =
        FileCache.Enable(
          maxOpen = 1000,
          actorConfig = ActorConfig.Basic(ec = myTestSingleThreadExecutionContext, name = "FileCache")
        ),
      memoryCache =
        MemoryCache.ByteCacheOnly(
          minIOSeekSize = 4098.bytes,
          skipBlockCacheSeekSize = 4098.bytes * 10,
          cacheCapacity = 1.gb,
          actorConfig = ActorConfig.Basic(ec = myTestSingleThreadExecutionContext, name = "MemoryCache")
        ),
      threadStateCache =
        ThreadStateCache.Limit(
          hashMapMaxSize = 100,
          maxProbe = 10
        ),
      cacheKeyValueIds = true,
      shutdownTimeout = 10.seconds
    ).get

  //test the database
  db.put(1, "one")
  assert(db.get(1).contains("one"))
}
