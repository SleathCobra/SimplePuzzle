package com.qtpie.simplepuzzle.renderer.gdx

import com.qtpie.simplepuzzle.core.model.BoardMutationType
import com.qtpie.simplepuzzle.core.model.GraphicsQuality
import kotlin.math.ceil
import kotlin.math.floor

data class PreviewPieceOperation(
    val type: BoardMutationType,
    val pieceIndex: Int,
)

data class PreviewStep(
    val delayMillis: Long,
    val operations: List<PreviewPieceOperation>,
)

/** Pure deterministic title-preview sequence for a supplied seed. */
class PreviewSequenceGenerator(
    private val pieceCount: Int,
    seed: Long,
    quality: GraphicsQuality,
) {
    private val random = StablePreviewRandom(seed)
    private val profile = PreviewQualityProfile.from(quality)
    private val visible = BooleanArray(pieceCount)
    private val recentlyToggled = BooleanArray(pieceCount)
    private val minimumVisible = floor(pieceCount * 0.20).toInt().coerceAtLeast(0)
    private val maximumVisible = ceil(pieceCount * 0.80).toInt().coerceIn(1, pieceCount)
    private var visibleCount = 0

    init {
        require(pieceCount > 0) { "Preview piece count must be positive." }
        val fractionThousandths = random.nextInt(250, 451)
        val target = ((pieceCount * fractionThousandths) / 1_000)
            .coerceIn(minimumVisible.coerceAtLeast(1), maximumVisible)
        val indices = IntArray(pieceCount) { it }
        shuffle(indices)
        repeat(target) { index ->
            visible[indices[index]] = true
            visibleCount++
        }
    }

    fun initialVisiblePieces(): IntArray = IntArray(visibleCount).also { output ->
        var write = 0
        visible.indices.forEach { index -> if (visible[index]) output[write++] = index }
    }

    fun nextStep(): PreviewStep {
        val operations = ArrayList<PreviewPieceOperation>(profile.maximumOperations)
        val requested = random.nextInt(1, profile.maximumOperations + 1)
        val previousRecentlyToggled = recentlyToggled.copyOf()
        recentlyToggled.fill(false)
        repeat(requested) {
            val preferredType = when {
                visibleCount <= minimumVisible -> BoardMutationType.REVEAL
                visibleCount >= maximumVisible -> BoardMutationType.REMOVE
                random.nextInt(0, 100) < 56 -> BoardMutationType.REVEAL
                else -> BoardMutationType.REMOVE
            }
            val operation = chooseOperation(preferredType, previousRecentlyToggled, operations)
                ?: chooseOperation(preferredType.opposite(), previousRecentlyToggled, operations)
                ?: return@repeat
            operations += operation
            recentlyToggled[operation.pieceIndex] = true
            if (operation.type == BoardMutationType.REVEAL) {
                visible[operation.pieceIndex] = true
                visibleCount++
            } else {
                visible[operation.pieceIndex] = false
                visibleCount--
            }
        }
        return PreviewStep(
            delayMillis = random.nextInt(profile.minimumDelayMillis, profile.maximumDelayMillis + 1).toLong(),
            operations = operations,
        )
    }

    fun visibleCount(): Int = visibleCount

    fun occupancyBounds(): IntRange = minimumVisible..maximumVisible

    fun packageDurationMillis(): Long = random.nextInt(15_000, 25_001).toLong()

    private fun chooseOperation(
        type: BoardMutationType,
        previousRecentlyToggled: BooleanArray,
        selected: List<PreviewPieceOperation>,
    ): PreviewPieceOperation? {
        if (type == BoardMutationType.REVEAL && visibleCount >= maximumVisible) return null
        if (type == BoardMutationType.REMOVE && visibleCount <= minimumVisible) return null
        val selectedPieces = selected.mapTo(mutableSetOf()) { it.pieceIndex }
        val candidates = visible.indices.filter { index ->
            index !in selectedPieces &&
                !previousRecentlyToggled[index] &&
                (visible[index] == (type == BoardMutationType.REMOVE))
        }
        val fallback = if (candidates.isEmpty()) {
            visible.indices.filter { index ->
                index !in selectedPieces && (visible[index] == (type == BoardMutationType.REMOVE))
            }
        } else {
            candidates
        }
        if (fallback.isEmpty()) return null
        return PreviewPieceOperation(type, fallback[random.nextInt(0, fallback.size)])
    }

    private fun shuffle(values: IntArray) {
        for (index in values.lastIndex downTo 1) {
            val swap = random.nextInt(0, index + 1)
            val value = values[index]
            values[index] = values[swap]
            values[swap] = value
        }
    }

    private fun BoardMutationType.opposite(): BoardMutationType = when (this) {
        BoardMutationType.REVEAL -> BoardMutationType.REMOVE
        BoardMutationType.REMOVE -> BoardMutationType.REVEAL
    }
}

/** Deterministic non-repeating package order for a process seed. */
class PreviewPackageSequence(
    private val packageCount: Int,
    seed: Long,
) {
    private val random = StablePreviewRandom(seed xor 0x507265766965774CL)
    var currentIndex: Int = if (packageCount > 0) random.nextInt(0, packageCount) else -1
        private set

    init {
        require(packageCount >= 0)
    }

    fun nextIndex(): Int {
        if (packageCount <= 1) return currentIndex
        currentIndex = (currentIndex + random.nextInt(1, packageCount)) % packageCount
        return currentIndex
    }
}

private enum class PreviewQualityProfile(
    val minimumDelayMillis: Int,
    val maximumDelayMillis: Int,
    val maximumOperations: Int,
) {
    LOW(1_300, 1_800, 1),
    MEDIUM(900, 1_300, 2),
    HIGH(800, 1_200, 3),
    ;

    companion object {
        fun from(quality: GraphicsQuality): PreviewQualityProfile = when (quality) {
            GraphicsQuality.LOW -> LOW
            GraphicsQuality.HIGH -> HIGH
            GraphicsQuality.AUTO,
            GraphicsQuality.MEDIUM -> MEDIUM
        }
    }
}

private class StablePreviewRandom(seed: Long) {
    private var state = if (seed == 0L) 0x6A09E667F3BCC909L else seed

    fun nextInt(fromInclusive: Int, untilExclusive: Int): Int {
        require(fromInclusive < untilExclusive)
        state = state xor (state shl 13)
        state = state xor (state ushr 7)
        state = state xor (state shl 17)
        val positive = state ushr 1
        return fromInclusive + (positive % (untilExclusive - fromInclusive).toLong()).toInt()
    }
}
