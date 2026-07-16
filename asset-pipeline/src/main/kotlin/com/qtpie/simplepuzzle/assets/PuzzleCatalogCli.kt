package com.qtpie.simplepuzzle.assets

import java.nio.file.Path

fun main(arguments: Array<String>) {
    require(arguments.size == 3) {
        "Usage: puzzle-catalog <catalog-definition.json> <project-root> <output-file>"
    }
    val catalog = PuzzleCatalogGenerator().generate(
        catalogDefinitionFile = Path.of(arguments[0]),
        projectRoot = Path.of(arguments[1]),
        outputFile = Path.of(arguments[2]),
    )
    println("Generated puzzle catalog: ${catalog.puzzles.size} package(s)")
}
