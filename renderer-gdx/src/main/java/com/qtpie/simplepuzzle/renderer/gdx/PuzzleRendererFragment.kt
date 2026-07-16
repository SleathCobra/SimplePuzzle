package com.qtpie.simplepuzzle.renderer.gdx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication

class PuzzleRendererFragment : AndroidFragmentApplication() {
    private val hostViewModel: PuzzleRendererHostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val configuration = AndroidApplicationConfiguration().apply {
            useGL30 = false
            numSamples = 2
            useImmersiveMode = false
        }
        return initializeForView(
            PuzzleRenderer(
                assetRoot = requireArguments().getString(ARG_ASSET_ROOT) ?: DEFAULT_ASSET_ROOT,
                controller = hostViewModel.controller,
            ),
            configuration,
        )
    }

    companion object {
        const val TAG = "jigsaw-math-puzzle-renderer"
        const val DEFAULT_ASSET_ROOT = "puzzles/cosmic-journey"
        private const val ARG_ASSET_ROOT = "asset-root"

        fun newInstance(assetRoot: String = DEFAULT_ASSET_ROOT): PuzzleRendererFragment =
            PuzzleRendererFragment().apply {
                arguments = Bundle().apply { putString(ARG_ASSET_ROOT, assetRoot) }
            }
    }
}
