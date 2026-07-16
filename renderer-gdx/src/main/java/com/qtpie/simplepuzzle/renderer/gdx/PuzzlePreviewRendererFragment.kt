package com.qtpie.simplepuzzle.renderer.gdx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.qtpie.simplepuzzle.core.model.GraphicsQuality
import androidx.fragment.app.activityViewModels

/** Decorative title renderer with no game, persistence, audio, or haptic dependencies. */
class PuzzlePreviewRendererFragment : AndroidFragmentApplication() {
    private val hostViewModel: PuzzlePreviewRendererHostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val arguments = requireArguments()
        val configuration = AndroidApplicationConfiguration().apply {
            useGL30 = false
            numSamples = 0
            useImmersiveMode = false
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            useRotationVectorSensor = false
        }
        return initializeForView(
            PuzzleRenderer(
                assetRoot = arguments.getString(ARG_ASSET_ROOT) ?: DEFAULT_ASSET_ROOT,
                controller = hostViewModel.controller,
                previewConfiguration = PuzzlePreviewConfiguration(
                    seed = arguments.getLong(ARG_SEED),
                    quality = arguments.getString(ARG_QUALITY)
                        ?.let { value -> runCatching { GraphicsQuality.valueOf(value) }.getOrNull() }
                        ?: GraphicsQuality.MEDIUM,
                    reducedMotion = arguments.getBoolean(ARG_REDUCED_MOTION),
                    packageSwitchSignal = if (arguments.getBoolean(ARG_PACKAGE_SWITCH_ENABLED)) {
                        hostViewModel.packageSwitchSignal
                    } else {
                        null
                    },
                ),
            ),
            configuration,
        )
    }

    companion object {
        const val TAG = "jigsaw-math-title-preview-renderer"
        private const val DEFAULT_ASSET_ROOT = "puzzles/cosmic-journey"
        private const val ARG_ASSET_ROOT = "asset-root"
        private const val ARG_SEED = "seed"
        private const val ARG_QUALITY = "quality"
        private const val ARG_REDUCED_MOTION = "reduced-motion"
        private const val ARG_PACKAGE_SWITCH_ENABLED = "package-switch-enabled"

        fun newInstance(
            assetRoot: String,
            seed: Long,
            quality: GraphicsQuality,
            reducedMotion: Boolean,
            packageSwitchEnabled: Boolean,
        ): PuzzlePreviewRendererFragment = PuzzlePreviewRendererFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ASSET_ROOT, assetRoot)
                putLong(ARG_SEED, seed)
                putString(ARG_QUALITY, quality.name)
                putBoolean(ARG_REDUCED_MOTION, reducedMotion)
                putBoolean(ARG_PACKAGE_SWITCH_ENABLED, packageSwitchEnabled)
            }
        }
    }
}
