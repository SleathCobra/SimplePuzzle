package com.qtpie.simplepuzzle.renderer.gdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.FitViewport
import com.qtpie.simplepuzzle.core.model.assets.PUZZLE_FORMAT_VERSION
import com.qtpie.simplepuzzle.core.model.assets.PuzzleManifest
import com.qtpie.simplepuzzle.core.model.BoardMutationId
import com.qtpie.simplepuzzle.core.model.BoardMutationType
import ktx.app.KtxApplicationAdapter
import kotlinx.serialization.json.Json
import kotlin.math.sin
import kotlin.math.PI
import java.util.ArrayDeque

internal data class PuzzlePreviewConfiguration(
    val seed: Long,
    val quality: com.qtpie.simplepuzzle.core.model.GraphicsQuality,
    val reducedMotion: Boolean,
    val packageSwitchSignal: PreviewPackageSwitchSignal? = null,
)

internal class PuzzleRenderer(
    private val assetRoot: String,
    private val controller: PuzzleRendererController,
    private val previewConfiguration: PuzzlePreviewConfiguration? = null,
) : KtxApplicationAdapter {
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(1f, 1f, camera)
    private val clock = FixedStepClock()
    private lateinit var activePieceMesh: Mesh
    private lateinit var revealedPiecesMesh: Mesh
    private lateinit var meshData: PuzzleMeshData
    private lateinit var texture: Texture
    private lateinit var shader: ShaderProgram
    private lateinit var particleBatch: SpriteBatch
    private lateinit var glowTexture: Texture
    private lateinit var revealedPieces: BooleanArray
    private lateinit var revealedIndices: ShortArray
    private var revealedIndexCount = 0
    private val particles = ParticlePool(capacity = 64)
    private var quality = RendererQualityProfile.MEDIUM
    private var activePiece = NO_ACTIVE_PIECE
    private var activeMutationId: BoardMutationId? = null
    private var activeMutationType = BoardMutationType.REVEAL
    private var mutationProgress = 0f
    private var activeMutationDuration = quality.revealDurationSeconds
    private var lastCompletedMutationId: BoardMutationId? = null
    private var lastCompletedMutationType = BoardMutationType.REVEAL
    private var lastCompletedPiece = NO_ACTIVE_PIECE
    private var currentSessionGeneration = 1L
    private var rendererPaused = false
    private var correctEffectPending = false
    private var shakeRemainingSeconds = 0f
    private var previewSequence: PreviewSequenceGenerator? = null
    private val previewOperations = ArrayDeque<PreviewPieceOperation>(3)
    private var previewCountdownSeconds = 0f
    private var previewMutationSequence = 1L
    private var previewPackageRemainingSeconds = Float.POSITIVE_INFINITY
    private var previewAwaitingPackageSwitch = false

    override fun create() {
        val manifestFile = Gdx.files.internal("$assetRoot/manifest.json")
        val manifest = Json.decodeFromString<PuzzleManifest>(manifestFile.readString("UTF-8"))
        require(manifest.formatVersion == PUZZLE_FORMAT_VERSION) {
            "Unsupported puzzle format ${manifest.formatVersion}; expected $PUZZLE_FORMAT_VERSION."
        }
        meshData = PuzzleMeshDataBuilder.build(manifest)
        revealedPieces = BooleanArray(manifest.pieces.size)
        previewConfiguration?.let { configuration ->
            Gdx.graphics.setForegroundFPS(PREVIEW_FRAMES_PER_SECOND)
            quality = RendererQualityProfile.from(configuration.quality)
            previewSequence = PreviewSequenceGenerator(
                pieceCount = manifest.pieces.size,
                seed = configuration.seed,
                quality = configuration.quality,
            ).also { sequence ->
                sequence.initialVisiblePieces().forEach { revealedPieces[it] = true }
                if (configuration.packageSwitchSignal != null && !configuration.reducedMotion) {
                    previewPackageRemainingSeconds = sequence.packageDurationMillis() / 1_000f
                }
            }
        }

        activePieceMesh = createMesh().apply {
            setIndices(meshData.indices)
        }
        revealedPiecesMesh = createMesh()
        revealedIndices = ShortArray(meshData.indices.size)
        rebuildRevealedMesh()
        texture = Texture(Gdx.files.internal("$assetRoot/${manifest.textureFile}"), true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        }
        shader = ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        check(shader.isCompiled) { "Puzzle shader failed to compile: ${shader.log}" }
        particleBatch = SpriteBatch(64)
        glowTexture = createGlowTexture()
        camera.position.set(0.5f, 0.5f, 0f)
        camera.position.x = if (shakeRemainingSeconds > 0f) {
            0.5f + sin(shakeRemainingSeconds * 80f) * 0.012f * (shakeRemainingSeconds / SHAKE_DURATION_SECONDS)
        } else {
            0.5f
        }
        camera.update()
        if (previewConfiguration?.reducedMotion == false) prepareNextPreviewStep()
    }

    private fun createMesh(): Mesh = Mesh(
            true,
            meshData.vertices.size / COMPONENTS_PER_VERTEX,
            meshData.indices.size,
            VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
            VertexAttribute(
                VertexAttributes.Usage.TextureCoordinates,
                2,
                ShaderProgram.TEXCOORD_ATTRIBUTE + "0",
            ),
        ).apply {
            setVertices(meshData.vertices)
        }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun render() {
        drainCommands()
        if (!rendererPaused) {
            var steps = clock.consume(Gdx.graphics.deltaTime)
            while (steps-- > 0) updateSimulation(FIXED_STEP_SECONDS)
        }

        Gdx.gl.glClearColor(0.025f, 0.035f, 0.13f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        camera.update()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        texture.bind(0)
        shader.bind()
        shader.setUniformMatrix("u_projTrans", camera.combined)
        shader.setUniformi("u_texture", 0)

        if (revealedIndexCount > 0) {
            shader.setUniformf("u_alpha", 1f)
            setPieceTransform(centerX = 0f, centerY = 0f, scale = 1f, rotation = 0f, offsetY = 0f)
            revealedPiecesMesh.render(shader, GL20.GL_TRIANGLES, 0, revealedIndexCount)
        }
        if (activePiece != NO_ACTIVE_PIECE) {
            val progress = mutationProgress.coerceIn(0f, 1f)
            if (activeMutationType == BoardMutationType.REVEAL) {
                shader.setUniformf("u_alpha", progress)
                setPieceTransform(
                    centerX = meshData.centerX[activePiece],
                    centerY = meshData.centerY[activePiece],
                    scale = 0.92f + progress * 0.08f,
                    rotation = 0f,
                    offsetY = 0.025f * (1f - progress),
                )
            } else {
                shader.setUniformf("u_alpha", 1f - progress)
                setPieceTransform(
                    centerX = meshData.centerX[activePiece],
                    centerY = meshData.centerY[activePiece],
                    scale = 1f - 0.35f * progress,
                    rotation = 0.14f * progress,
                    offsetY = 0.035f * sin(PI.toFloat() * progress) - 0.16f * progress,
                )
            }
            activePieceMesh.render(
                shader,
                GL20.GL_TRIANGLES,
                meshData.indexOffsets[activePiece],
                meshData.indexCounts[activePiece],
            )
        }
        Gdx.gl.glDisable(GL20.GL_BLEND)
        drawParticles()
    }

    private fun drainCommands() {
        while (true) {
            when (val command = controller.poll() ?: return) {
                is RendererCommand.SetVisiblePieces -> {
                    if (command.sessionGeneration != currentSessionGeneration) {
                        currentSessionGeneration = command.sessionGeneration
                        activePiece = NO_ACTIVE_PIECE
                        activeMutationId = null
                        lastCompletedMutationId = null
                        lastCompletedPiece = NO_ACTIVE_PIECE
                        mutationProgress = 0f
                    }
                    revealedPieces.fill(false)
                    command.pieceIndices.forEach { index ->
                        if (index in revealedPieces.indices) revealedPieces[index] = true
                    }
                    if (activePiece in revealedPieces.indices) {
                        revealedPieces[activePiece] = false
                    }
                    keepCompletedMutationVisuallyCommitted(command.pieceIndices)
                    rebuildRevealedMesh()
                }
                is RendererCommand.RevealPiece -> {
                    startMutation(
                        mutationId = command.mutationId,
                        pieceIndex = command.pieceIndex,
                        type = BoardMutationType.REVEAL,
                        reducedMotion = command.reducedMotion,
                    )
                }
                is RendererCommand.RemovePiece -> {
                    startMutation(
                        mutationId = command.mutationId,
                        pieceIndex = command.pieceIndex,
                        type = BoardMutationType.REMOVE,
                        reducedMotion = command.reducedMotion,
                    )
                }
                is RendererCommand.SetQuality -> quality = RendererQualityProfile.from(command.quality)
                RendererCommand.CompletionEffect -> particles.spawnBurst(
                    originX = 0.5f,
                    originY = 0.5f,
                    requestedCount = quality.optionalParticleLimit,
                    speed = 0.55f,
                )
                RendererCommand.CorrectAnswerEffect -> correctEffectPending = true
                RendererCommand.IncorrectAnswerEffect -> shakeRemainingSeconds = SHAKE_DURATION_SECONDS
                RendererCommand.Pause -> {
                    rendererPaused = true
                    clock.reset()
                }
                RendererCommand.Resume -> {
                    rendererPaused = false
                    clock.reset()
                }
            }
        }
    }

    private fun updateSimulation(stepSeconds: Float) {
        particles.update(stepSeconds)
        shakeRemainingSeconds = (shakeRemainingSeconds - stepSeconds).coerceAtLeast(0f)
        if (activePiece != NO_ACTIVE_PIECE) {
            mutationProgress = (mutationProgress + stepSeconds / activeMutationDuration).coerceAtMost(1f)
        }
        if (activePiece != NO_ACTIVE_PIECE && mutationProgress >= 1f) {
            val completedPiece = activePiece
            val completedId = requireNotNull(activeMutationId)
            if (activeMutationType == BoardMutationType.REVEAL) {
                revealedPieces[completedPiece] = true
            }
            rebuildRevealedMesh()
            lastCompletedMutationId = completedId
            lastCompletedMutationType = activeMutationType
            lastCompletedPiece = completedPiece
            activePiece = NO_ACTIVE_PIECE
            activeMutationId = null
            mutationProgress = 0f
            controller.notifyMutationFinished(completedId)
        }
        updatePreview(stepSeconds)
    }

    private fun updatePreview(stepSeconds: Float) {
        if (previewSequence == null || previewConfiguration?.reducedMotion != false || previewAwaitingPackageSwitch) return
        previewPackageRemainingSeconds = (previewPackageRemainingSeconds - stepSeconds).coerceAtLeast(0f)
        if (activePiece != NO_ACTIVE_PIECE) return
        if (previewPackageRemainingSeconds == 0f && previewOperations.isEmpty()) {
            previewAwaitingPackageSwitch = true
            previewConfiguration?.packageSwitchSignal?.markIdleBoundary()
            return
        }
        if (previewCountdownSeconds > 0f) {
            previewCountdownSeconds = (previewCountdownSeconds - stepSeconds).coerceAtLeast(0f)
            return
        }
        if (previewOperations.isEmpty()) {
            prepareNextPreviewStep()
            return
        }
        val operation = previewOperations.removeFirst()
        if (operation.type == BoardMutationType.REVEAL && quality != RendererQualityProfile.LOW) {
            correctEffectPending = true
        }
        startMutation(
            mutationId = BoardMutationId(PREVIEW_SESSION_GENERATION, previewMutationSequence++),
            pieceIndex = operation.pieceIndex,
            type = operation.type,
            reducedMotion = false,
        )
    }

    private fun prepareNextPreviewStep() {
        val step = previewSequence?.nextStep() ?: return
        previewOperations.clear()
        step.operations.forEach(previewOperations::addLast)
        previewCountdownSeconds = step.delayMillis / 1_000f
    }

    private fun startMutation(
        mutationId: BoardMutationId,
        pieceIndex: Int,
        type: BoardMutationType,
        reducedMotion: Boolean,
    ) {
        if (mutationId == lastCompletedMutationId) {
            controller.notifyMutationFinished(mutationId)
            return
        }
        if (mutationId == activeMutationId || activePiece != NO_ACTIVE_PIECE) return
        if (mutationId.sessionGeneration != currentSessionGeneration) return
        if (pieceIndex !in revealedPieces.indices) return
        val targetHasExpectedVisibility = when (type) {
            BoardMutationType.REVEAL -> !revealedPieces[pieceIndex]
            BoardMutationType.REMOVE -> revealedPieces[pieceIndex]
        }
        if (!targetHasExpectedVisibility) return

        activePiece = pieceIndex
        activeMutationId = mutationId
        activeMutationType = type
        mutationProgress = 0f
        activeMutationDuration = if (reducedMotion) {
            FIXED_STEP_SECONDS
        } else if (type == BoardMutationType.REVEAL) {
            quality.revealDurationSeconds
        } else {
            quality.removalDurationSeconds
        }
        if (type == BoardMutationType.REMOVE) {
            revealedPieces[pieceIndex] = false
            rebuildRevealedMesh()
            if (!reducedMotion && quality.removalParticleLimit > 0) {
                particles.spawnBurst(
                    meshData.centerX[pieceIndex],
                    meshData.centerY[pieceIndex],
                    quality.removalParticleLimit,
                    speed = 0.22f,
                )
            }
        } else if (correctEffectPending && !reducedMotion) {
            particles.spawnBurst(
                meshData.centerX[pieceIndex],
                meshData.centerY[pieceIndex],
                quality.optionalParticleLimit / 2,
                speed = 0.32f,
            )
        }
        correctEffectPending = false
    }

    private fun keepCompletedMutationVisuallyCommitted(snapshot: IntArray) {
        val piece = lastCompletedPiece
        if (piece !in revealedPieces.indices || lastCompletedMutationId == null) return
        val snapshotContainsPiece = snapshot.any { it == piece }
        val domainHasCommitted = when (lastCompletedMutationType) {
            BoardMutationType.REVEAL -> snapshotContainsPiece
            BoardMutationType.REMOVE -> !snapshotContainsPiece
        }
        if (domainHasCommitted) {
            lastCompletedMutationId = null
            lastCompletedPiece = NO_ACTIVE_PIECE
        } else {
            revealedPieces[piece] = lastCompletedMutationType == BoardMutationType.REVEAL
        }
    }

    private fun setPieceTransform(
        centerX: Float,
        centerY: Float,
        scale: Float,
        rotation: Float,
        offsetY: Float,
    ) {
        shader.setUniformf("u_center", centerX, centerY)
        shader.setUniformf("u_scale", scale)
        shader.setUniformf("u_rotation", rotation)
        shader.setUniformf("u_offset", 0f, offsetY)
    }

    private fun drawParticles() {
        particleBatch.projectionMatrix = camera.combined
        particleBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        particleBatch.begin()
        var index = 0
        while (index < particles.capacity) {
            if (particles.isActive(index)) {
                val size = particles.size(index)
                particleBatch.color.set(1f, 0.75f, 0.28f, particles.alpha(index))
                particleBatch.draw(
                    glowTexture,
                    particles.x(index) - size * 0.5f,
                    particles.y(index) - size * 0.5f,
                    size,
                    size,
                )
            }
            index++
        }
        particleBatch.end()
        particleBatch.color.set(1f, 1f, 1f, 1f)
    }

    private fun createGlowTexture(): Texture {
        val size = 32
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        var y = 0
        while (y < size) {
            var x = 0
            while (x < size) {
                val dx = (x + 0.5f) / size * 2f - 1f
                val dy = (y + 0.5f) / size * 2f - 1f
                val alpha = (1f - (dx * dx + dy * dy)).coerceIn(0f, 1f)
                pixmap.setColor(1f, 0.82f, 0.35f, alpha * alpha)
                pixmap.drawPixel(x, y)
                x++
            }
            y++
        }
        return Texture(pixmap).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            pixmap.dispose()
        }
    }

    /** Rebuilds the static revealed-piece index buffer only on coarse commands. */
    private fun rebuildRevealedMesh() {
        var writeIndex = 0
        var pieceIndex = 0
        while (pieceIndex < revealedPieces.size) {
            if (revealedPieces[pieceIndex]) {
                val sourceOffset = meshData.indexOffsets[pieceIndex]
                val count = meshData.indexCounts[pieceIndex]
                meshData.indices.copyInto(
                    destination = revealedIndices,
                    destinationOffset = writeIndex,
                    startIndex = sourceOffset,
                    endIndex = sourceOffset + count,
                )
                writeIndex += count
            }
            pieceIndex++
        }
        revealedIndexCount = writeIndex
        revealedPiecesMesh.setIndices(revealedIndices, 0, revealedIndexCount)
    }

    override fun pause() {
        rendererPaused = true
        clock.reset()
    }

    override fun resume() {
        rendererPaused = false
        clock.reset()
    }

    override fun dispose() {
        if (::shader.isInitialized) shader.dispose()
        if (::activePieceMesh.isInitialized) activePieceMesh.dispose()
        if (::revealedPiecesMesh.isInitialized) revealedPiecesMesh.dispose()
        if (::texture.isInitialized) texture.dispose()
        if (::particleBatch.isInitialized) particleBatch.dispose()
        if (::glowTexture.isInitialized) glowTexture.dispose()
    }

    private companion object {
        const val COMPONENTS_PER_VERTEX = 4
        const val FIXED_STEP_SECONDS = 1f / 60f
        const val NO_ACTIVE_PIECE = -1
        const val SHAKE_DURATION_SECONDS = 0.2f
        const val PREVIEW_SESSION_GENERATION = 1L
        const val PREVIEW_FRAMES_PER_SECOND = 30

        const val VERTEX_SHADER = """
            attribute vec2 a_position;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            uniform vec2 u_center;
            uniform vec2 u_offset;
            uniform float u_scale;
            uniform float u_rotation;
            varying vec2 v_texCoords;
            void main() {
                v_texCoords = a_texCoord0;
                vec2 local = (a_position - u_center) * u_scale;
                float sine = sin(u_rotation);
                float cosine = cos(u_rotation);
                vec2 rotated = vec2(
                    local.x * cosine - local.y * sine,
                    local.x * sine + local.y * cosine
                );
                gl_Position = u_projTrans * vec4(rotated + u_center + u_offset, 0.0, 1.0);
            }
        """

        const val FRAGMENT_SHADER = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
            uniform float u_alpha;
            void main() {
                vec4 color = texture2D(u_texture, v_texCoords);
                gl_FragColor = vec4(color.rgb, color.a * u_alpha);
            }
        """
    }
}
