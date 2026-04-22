package com.adamjohnson.pokebinder.ui.cardpreview

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.local.PokemonCardLocalDataSource
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.adamjohnson.pokebinder.data.repository.CardRepository

class PokemonCardPreviewDialogFragment : DialogFragment(R.layout.dialog_pokemon_card_preview) {

    private val cardRepository by lazy {
        CardRepository(PokemonCardLocalDataSource(requireContext().applicationContext))
    }

    override fun onStart() {
        super.onStart()

        dialog?.setCanceledOnTouchOutside(true)
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val params = attributes
            params.dimAmount = 0.72f
            attributes = params
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val previewTitleTextView: TextView = view.findViewById(R.id.previewTitleTextView)
        val previewSetTextView: TextView = view.findViewById(R.id.previewSetTextView)
        val previewMetadataTextView: TextView = view.findViewById(R.id.previewMetadataTextView)
        val previewImageView: ImageView = view.findViewById(R.id.previewImageView)
        val previewMessageTextView: TextView = view.findViewById(R.id.previewMessageTextView)
        val closeButton: Button = view.findViewById(R.id.closeButton)

        val cardId = arguments?.getString(ARG_CARD_ID).orEmpty()
        val pokemonCard = if (cardId.isBlank()) {
            null
        } else {
            cardRepository.getPokemonCard(cardId)
        }

        if (pokemonCard == null) {
            previewTitleTextView.text = getString(R.string.card_preview_title_fallback)
            previewSetTextView.text = getString(R.string.card_preview_unknown_value)
            previewMetadataTextView.text = ""
            previewMetadataTextView.visibility = View.GONE
            previewImageView.setImageResource(android.R.drawable.ic_menu_report_image)
            previewImageView.alpha = 0.35f
            previewMessageTextView.text = getString(R.string.card_preview_unknown_card)
            previewMessageTextView.visibility = View.VISIBLE
        } else {
            bindPreviewCard(
                pokemonCard = pokemonCard,
                previewTitleTextView = previewTitleTextView,
                previewSetTextView = previewSetTextView,
                previewMetadataTextView = previewMetadataTextView,
                previewImageView = previewImageView,
                previewMessageTextView = previewMessageTextView
            )
        }

        closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun bindPreviewCard(
        pokemonCard: PokemonCard,
        previewTitleTextView: TextView,
        previewSetTextView: TextView,
        previewMetadataTextView: TextView,
        previewImageView: ImageView,
        previewMessageTextView: TextView
    ) {
        previewTitleTextView.text = pokemonCard.name.ifBlank {
            getString(R.string.card_preview_title_fallback)
        }

        previewSetTextView.text = pokemonCard.setName.ifBlank {
            getString(R.string.card_preview_unknown_value)
        }

        val typesText = if (pokemonCard.types.isEmpty()) {
            getString(R.string.card_preview_unknown_value)
        } else {
            pokemonCard.types.joinToString(", ")
        }

        val rarityText = pokemonCard.rarity.ifBlank {
            getString(R.string.card_preview_unknown_value)
        }

        previewMetadataTextView.text = getString(
            R.string.card_preview_type_and_rarity,
            typesText,
            rarityText
        )
        previewMetadataTextView.visibility = View.VISIBLE

        previewMessageTextView.visibility = View.GONE
        PokemonCardImageBinder.bind(previewImageView, pokemonCard)
    }

    companion object {
        const val TAG = "PokemonCardPreviewDialog"

        private const val ARG_CARD_ID = "cardId"

        fun newInstance(cardId: String): PokemonCardPreviewDialogFragment {
            return PokemonCardPreviewDialogFragment().apply {
                arguments = bundleOf(ARG_CARD_ID to cardId)
            }
        }
    }
}