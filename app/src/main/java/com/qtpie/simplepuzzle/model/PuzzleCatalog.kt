package com.qtpie.simplepuzzle.model

import android.content.Context
import com.qtpie.simplepuzzle.R
import com.qtpie.simplepuzzle.core.model.assets.PUZZLE_CATALOG_FORMAT_VERSION
import com.qtpie.simplepuzzle.core.model.assets.PUZZLE_FORMAT_VERSION
import com.qtpie.simplepuzzle.core.model.assets.PuzzleCatalogEntry
import com.qtpie.simplepuzzle.core.model.assets.PuzzleCatalogManifest
import com.qtpie.simplepuzzle.core.model.assets.PuzzleManifest
import kotlinx.serialization.json.Json

object AndroidPuzzleCatalog {
    private val json = Json { ignoreUnknownKeys = false }

    data class Snapshot(
        val puzzles: List<PuzzleInfo>,
        val previewCandidates: List<PuzzleInfo>,
    )

    fun load(context: Context): Snapshot {
        val assets = context.applicationContext.assets
        val catalog = runCatching {
            assets.open(CATALOG_ASSET).bufferedReader().use { reader ->
                json.decodeFromString<PuzzleCatalogManifest>(reader.readText())
            }
        }.getOrNull() ?: return Snapshot(fallbackCatalog(), emptyList())
        if (catalog.formatVersion != PUZZLE_CATALOG_FORMAT_VERSION) {
            return Snapshot(fallbackCatalog(), emptyList())
        }

        val valid = catalog.puzzles.mapNotNull { entry ->
            if (!entry.available) return@mapNotNull null
            runCatching {
                validatePackage(assets = assets, entry = entry)
                entry.toPuzzleInfo()
            }.getOrNull()
        }
        return Snapshot(
            puzzles = valid.ifEmpty(::fallbackCatalog),
            previewCandidates = valid,
        )
    }

    private fun validatePackage(
        assets: android.content.res.AssetManager,
        entry: PuzzleCatalogEntry,
    ) {
        val manifest = assets.open("${entry.generatedAssetRoot}/manifest.json")
            .bufferedReader()
            .use { reader -> json.decodeFromString<PuzzleManifest>(reader.readText()) }
        require(manifest.formatVersion == PUZZLE_FORMAT_VERSION)
        require(manifest.puzzleId == entry.puzzleId)
        require(manifest.pieces.size == entry.pieceCount)
        assets.open("${entry.generatedAssetRoot}/${manifest.textureFile}").use { }
    }

    private fun PuzzleCatalogEntry.toPuzzleInfo() = PuzzleInfo(
        id = puzzleId,
        name = title,
        imageResId = thumbnailResource(thumbnailResourceName),
        assetRoot = generatedAssetRoot,
        totalPieces = pieceCount,
        isLocked = initiallyLocked,
        category = category,
        unlockCost = 0,
    )

    private fun thumbnailResource(name: String): Int = when (name) {
        "cosmic_journey_thumbnail" -> R.drawable.cosmic_journey_thumbnail
        else -> error("Unknown catalog thumbnail resource: $name")
    }

    private fun fallbackCatalog() = listOf(
        PuzzleInfo(
            id = "cosmic-journey",
            name = "Cosmic Journey",
            imageResId = R.drawable.cosmic_journey_thumbnail,
            assetRoot = "puzzles/cosmic-journey",
            totalPieces = 30,
            isLocked = false,
            category = "Space",
            unlockCost = 0,
        ),
    )

    private const val CATALOG_ASSET = "puzzles/catalog.json"
}
