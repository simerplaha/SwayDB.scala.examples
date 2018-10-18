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

import swaydb.data.accelerate.{Accelerator, Level0Meter}
import swaydb.data.api.grouping.{Compression, GroupGroupingStrategy, KeyValueGroupingStrategy}
import swaydb.data.compaction.{LevelMeter, Throttle}
import swaydb.data.compression.{LZ4Compressor, LZ4Decompressor, LZ4Instance}
import swaydb.data.config.{ConfigWizard, MMAP, RecoveryMode}

import scala.concurrent.duration._

object ConfiguringLevels extends App {

  import swaydb._
  import swaydb.serializers.Default._

  val config =
    ConfigWizard
      .addPersistentLevel0( //level0
        dir = "/Disk1/myDB",
        mapSize = 4.mb,
        mmap = true,
        recoveryMode = RecoveryMode.ReportFailure,
        minTimeLeftToUpdateExpiration = 10.seconds,
        acceleration =
          (level0Meter: Level0Meter) =>
            Accelerator.cruise(level0Meter)
      )
      .addMemoryLevel1( //level1
        segmentSize = 4.mb,
        pushForward = false,
        bloomFilterFalsePositiveRate = 0.1,
        minTimeLeftToUpdateExpiration = 10.seconds,
        compressDuplicateValues = true,
        groupingStrategy = None,
        throttle =
          (levelMeter: LevelMeter) =>
            if (levelMeter.levelSize > 1.gb)
              Throttle(pushDelay = Duration.Zero, segmentsToPush = 10)
            else
              Throttle(pushDelay = Duration.Zero, segmentsToPush = 0)
      )
      .addPersistentLevel( //level2
        dir = "/Disk1/myDB",
        otherDirs = Seq("/Disk2/myDB", "/Disk3/myDB"),
        segmentSize = 4.mb,
        mmapSegment = MMAP.WriteAndRead,
        mmapAppendix = true,
        appendixFlushCheckpointSize = 4.mb,
        pushForward = false,
        bloomFilterFalsePositiveRate = 0.1,
        minTimeLeftToUpdateExpiration = 10.seconds,
        compressDuplicateValues = true,
        groupingStrategy =
          Some(
            KeyValueGroupingStrategy.Size(
              size = 1.mb,
              indexCompressions =
                Seq(
                  Compression.LZ4(
                    compressor = (LZ4Instance.FastestJavaInstance, LZ4Compressor.FastCompressor(minCompressionPercentage = 20.0)),
                    decompressor = (LZ4Instance.FastestJavaInstance, LZ4Decompressor.FastDecompressor)
                  ),
                  Compression.Snappy(minCompressionPercentage = 20.0),
                  Compression.UnCompressedGroup
                ),
              valueCompressions =
                Seq(
                  Compression.LZ4(
                    compressor = (LZ4Instance.FastestJavaInstance, LZ4Compressor.FastCompressor(minCompressionPercentage = 20.0)),
                    decompressor = (LZ4Instance.FastestJavaInstance, LZ4Decompressor.FastDecompressor)
                  ),
                  Compression.Snappy(minCompressionPercentage = 20.0),
                  Compression.UnCompressedGroup
                ),
              groupGroupingStrategy =
                Some(
                  GroupGroupingStrategy.Count(
                    count = 1000,
                    indexCompression = Compression.UnCompressedGroup,
                    valueCompression = Compression.UnCompressedGroup
                  )
                )
            )
          ),
        throttle =
          (levelMeter: LevelMeter) =>
            if (levelMeter.segmentsCount > 100)
              Throttle(pushDelay = 1.second, segmentsToPush = 2)
            else
              Throttle(pushDelay = Duration.Zero, segmentsToPush = 0)
      )
      .addTrashLevel //level3

  implicit val ordering =  swaydb.order.KeyOrder.default //import default sorting
  implicit val ec = SwayDB.defaultExecutionContext //import default ExecutionContext

  val db = //initialise the database with the above configuration
    SwayDB[Int, String](
      config = config,
      maxSegmentsOpen = 1000,
      cacheSize = 1.gb,
      cacheCheckDelay = 5.seconds,
      segmentsOpenCheckDelay = 5.seconds
    )

}
