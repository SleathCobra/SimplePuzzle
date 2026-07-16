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
import ktx.app.KtxApplicationAdapter
import kotlinx.serialization.json.Json
import kotlin.math.sin

internal class PuzzleRenderer(
    private val assetRoot: String,
    private val controller: PuzzleRendererController,
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
    private var revealProgress = 0f
    private var activeRevealDuration = quality.revealDurationSeconds
    private var rendererPaused = false
    private var correctEffectPending = false
    private var shakeRemainingSeconds = 0f

    override fun create() {
        val manifestFile = Gdx.files.internal("$assetRoot/manifest.json")
        val manifest = Json.decodeFromString<PuzzleManifest>(manifestFile.readString("UTF-8"))
        require(manifest.formatVersion == PUZZLE_FORMAT_VERSION) {
            "Unsupported puzzle format ${manifest.formatVersion}; expected $PUZZLE_FORMAT_VERSION."
        }
        meshData = PuzzleMeshDataBuilder.build(manifest)
        revealedPieces = BooleanArray(manifest.pieces.size)

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
            revealedPiecesMesh.render(shader, GL20.GL_TRIANGLES, 0, revealedIndexCount)
        }
        if (activePiece != NO_ACTIVE_PIECE && revealProgress > 0f) {
            shader.setUniformf("u_alpha", revealProgress)
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
                    revealedPieces.fill(false)
                    command.pieceIndices.forEach { index ->
                        if (index in revealedPieces.indices) revealedPieces[index] = true
                    }
                    rebuildRevealedMesh()
                }
                is RendererCommand.RevealPiece -> {
                    if (command.pieceIndex in revealedPieces.indices && !revealedPieces[command.pieceIndex]) {
                        activePiece = command.pieceIndex
                        revealProgress = 0f
                        activeRevealDuration = if (command.reducedMotion) {
                            FIXED_STEP_SECONDS
                        } else {
                            quality.revealDurationSeconds
                        }
                        if (correctEffectPending && !command.reducedMotion) {
                            particles.spawnBurst(
                                meshData.centerX[command.pieceIndex],
                                meshData.centerY[command.pieceIndex],
                                quality.optionalParticleLimit / 2,
                                speed = 0.32f,
                            )
                        }
                        correctEffectPending = false
                    }
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
        if (activePiece == NO_ACTIVE_PIECE) return
        revealProgress = (revealProgress + stepSeconds / activeRevealDuration).coerceAtMost(1f)
        if (revealProgress >= 1f) {
            val completedPiece = activePiece
            revealedPieces[completedPiece] = true
            rebuildRevealedMesh()
            activePiece = NO_ACTIVE_PIECE
            revealProgress = 0f
            controller.notifyRevealFinished(completedPiece)
        }
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

        const val VERTEX_SHADER = """
            attribute vec2 a_position;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec2 v_texCoords;
            void main() {
                v_texCoords = a_texCoord0;
                gl_Position = u_projTrans * vec4(a_position, 0.0, 1.0);
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
