package com.qtpie.simplepuzzle.assets

import java.nio.file.Path

fun main(arguments: Array<String>) {
    require(arguments.size == 2) {
        "Usage: asset-pipeline <puzzle-definition.json> <output-directory>"
    }
    val manifest = PuzzleAssetGenerator().generate(
        definitionFile = Path.of(arguments[0]),
        outputDirectory = Path.of(arguments[1]),
    )
    println("Generated ${manifest.puzzleId}: ${manifest.pieces.size} pieces, format ${manifest.formatVersion}")
}
