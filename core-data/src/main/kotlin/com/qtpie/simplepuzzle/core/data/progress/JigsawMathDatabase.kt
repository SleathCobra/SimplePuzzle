package com.qtpie.simplepuzzle.core.data.progress

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PuzzleProgressEntity::class, GameSessionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class JigsawMathDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
}
