package com.qtpie.simplepuzzle.assets

import com.qtpie.simplepuzzle.core.model.assets.PUZZLE_CATALOG_FORMAT_VERSION
import com.qtpie.simplepuzzle.core.model.assets.PUZZLE_FORMAT_VERSION
import com.qtpie.simplepuzzle.core.model.assets.PuzzleCatalogDefinition
import com.qtpie.simplepuzzle.core.model.assets.PuzzleCatalogEntry
import com.qtpie.simplepuzzle.core.model.assets.PuzzleCatalogManifest
import com.qtpie.simplepuzzle.core.model.assets.PuzzleDefinition
import com.qtpie.simplepuzzle.core.model.assets.PuzzleManifest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
class PuzzleCatalogGenerator(
    private val json: Json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = true
        explicitNulls = false
    },
) {
    fun generate(
        catalogDefinitionFile: Path,
        projectRoot: Path,
        outputFile: Path,
    ): PuzzleCatalogManifest {
        require(catalogDefinitionFile.exists()) {
            "Puzzle catalog definition does not exist: $catalogDefinitionFile"
        }
        val definition = json.decodeFromString<PuzzleCatalogDefinition>(
            Files.readString(catalogDefinitionFile),
        )
        require(definition.formatVersion == PUZZLE_CATALOG_FORMAT_VERSION) {
            "Unsupported puzzle catalog definition format ${definition.formatVersion}."
        }
        require(definition.puzzles.isNotEmpty()) { "Puzzle catalog must contain at least one puzzle." }

        val entries = definition.puzzles.map { source ->
            require(source.generatedAssetRoot.matches(Regex("puzzles/[a-z0-9]+(?:-[a-z0-9]+)*"))) {
                "Generated asset root must be puzzles/<kebab-case-id>."
            }
            require(source.thumbnailResourceName.matches(Regex("[a-z][a-z0-9_]*"))) {
                "Thumbnail resource name must be a valid Android resource name."
            }
            val puzzleDefinitionFile = catalogDefinitionFile.parent
                .resolve(source.definitionFile)
                .normalize()
            require(puzzleDefinitionFile.exists()) {
                "Catalog puzzle definition does not exist: $puzzleDefinitionFile"
            }
            val puzzleDefinition = json.decodeFromString<PuzzleDefinition>(
                Files.readString(puzzleDefinitionFile),
            )
            val generatedManifestFile = projectRoot
                .resolve("app/src/main/assets")
                .resolve(source.generatedAssetRoot)
                .resolve("manifest.json")
                .normalize()
            require(generatedManifestFile.exists()) {
                "Generated puzzle manifest does not exist: $generatedManifestFile"
            }
            val manifest = json.decodeFromString<PuzzleManifest>(Files.readString(generatedManifestFile))
            require(manifest.formatVersion == PUZZLE_FORMAT_VERSION) {
                "Catalog package ${manifest.puzzleId} uses unsupported format ${manifest.formatVersion}."
            }
            require(manifest.puzzleId == puzzleDefinition.id) {
                "Catalog definition ${puzzleDefinition.id} does not match package ${manifest.puzzleId}."
            }
            require(source.generatedAssetRoot.substringAfterLast('/') == manifest.puzzleId) {
                "Generated asset root must end with the manifest puzzle ID."
            }
            PuzzleCatalogEntry(
                puzzleId = manifest.puzzleId,
                title = manifest.title,
                category = manifest.category,
                generatedAssetRoot = source.generatedAssetRoot,
                thumbnailResourceName = source.thumbnailResourceName,
                pieceCount = manifest.pieces.size,
                available = source.available,
                initiallyLocked = source.initiallyLocked,
                legacyProgressId = source.legacyProgressId,
            )
        }
        require(entries.map { it.puzzleId }.distinct().size == entries.size) {
            "Puzzle catalog IDs must be unique."
        }
        require(entries.map { it.generatedAssetRoot }.distinct().size == entries.size) {
            "Puzzle catalog asset roots must be unique."
        }

        val catalog = PuzzleCatalogManifest(PUZZLE_CATALOG_FORMAT_VERSION, entries)
        outputFile.parent.createDirectories()
        Files.writeString(outputFile, json.encodeToString(catalog) + "\n")
        return catalog
    }
}
