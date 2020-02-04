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

package configuringlevels

import java.nio.file.Files
import java.util.concurrent.Executors

import swaydb.configs.level.SingleThreadFactory
import swaydb.data.accelerate.{Accelerator, LevelZeroMeter}
import swaydb.data.compaction.{CompactionExecutionContext, LevelMeter, Throttle}
import swaydb.data.compression.{LZ4Compressor, LZ4Decompressor, LZ4Instance}
import swaydb.data.config.{ConfigWizard, MMAP, RecoveryMode, _}
import swaydb.data.order.KeyOrder

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
      .addPersistentLevel0( //level0
        dir = dir.resolve("level0"),
        mapSize = 4.mb,
        mmap = true,
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
      .addMemoryLevel1( //level1
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
      .addPersistentLevel( //level2
        dir = dir.resolve("level2"),
        otherDirs = Seq(dir.resolve("level2-1"), dir.resolve("level2-3")),
        mmapAppendix = true,
        appendixFlushCheckpointSize = 4.mb,
        sortedKeyIndex =
          SortedKeyIndex.Enable(
            prefixCompression = PrefixCompression.Disable(normaliseIndexForBinarySearch = true),
            enablePositionIndex = true,
            ioStrategy = ioAction => IOStrategy.SynchronisedIO(cacheOnAccess = true),
            compressions = _ => Seq.empty
          ),
        randomKeyIndex =
          RandomKeyIndex.Enable(
            maxProbe = 1,
            minimumNumberOfKeys = 5,
            minimumNumberOfHits = 2,
            indexFormat = IndexFormat.Reference,
            allocateSpace = _.requiredSpace,
            ioStrategy = ioAction => IOStrategy.SynchronisedIO(cacheOnAccess = true),
            compression = _ => Seq.empty
          ),
        binarySearchIndex =
          BinarySearchIndex.FullIndex(
            minimumNumberOfKeys = 10,
            searchSortedIndexDirectly = true,
            indexFormat = IndexFormat.CopyKey,
            ioStrategy = ioAction => IOStrategy.SynchronisedIO(cacheOnAccess = true),
            compression = _ => Seq.empty
          ),
        mightContainKeyIndex =
          MightContainIndex.Enable(
            falsePositiveRate = 0.01,
            minimumNumberOfKeys = 10,
            updateMaxProbe = optimalMaxProbe => 1,
            ioStrategy = ioAction => IOStrategy.SynchronisedIO(cacheOnAccess = true),
            compression = _ => Seq.empty
          ),
        valuesConfig =
          ValuesConfig(
            compressDuplicateValues = true,
            compressDuplicateRangeValues = true,
            ioStrategy = ioAction => IOStrategy.SynchronisedIO(cacheOnAccess = true),
            compression = _ => Seq.empty
          ),
        segmentConfig =
          SegmentConfig(
            cacheSegmentBlocksOnCreate = true,
            deleteSegmentsEventually = true,
            pushForward = true,
            mmap = MMAP.Disabled,
            minSegmentSize = 4.mb,
            maxKeyValuesPerSegmentGroup = 100000,
            ioStrategy = {
              case IOAction.OpenResource => IOStrategy.SynchronisedIO(cacheOnAccess = true)
              case IOAction.ReadDataOverview => IOStrategy.SynchronisedIO(cacheOnAccess = true)
              case action: IOAction.DataAction => IOStrategy.SynchronisedIO(cacheOnAccess = action.isCompressed)
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
      .addTrashLevel //level3

  implicit val ordering = KeyOrder.default //import default sorting

  val db = //initialise the database with the above configuration
    SwayDB[Int, String, Nothing](
      config = config,
      fileCache =
        FileCache.Enable(
          maxOpen = 1000,
          actorConfig = ActorConfig.Basic(ec = myTestSingleThreadExecutionContext)
        ),
      memoryCache =
        MemoryCache.ByteCacheOnly(
          minIOSeekSize = 4098.bytes,
          skipBlockCacheSeekSize = 4098.bytes * 10,
          cacheCapacity = 1.gb,
          actorConfig = ActorConfig.Basic(ec = myTestSingleThreadExecutionContext)
        ),
      threadStateCache =
        ThreadStateCache.Limit(
          hashMapMaxSize = 100,
          maxProbe = 10
        ),
      cacheKeyValueIds = true
    ).get


  //test the database
  db.put(1, "one")
  assert(db.get(1).contains("one"))
}
