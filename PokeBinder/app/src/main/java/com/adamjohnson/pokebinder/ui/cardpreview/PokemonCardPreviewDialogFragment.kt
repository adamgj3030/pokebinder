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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
        val previewSubtitleTextView: TextView = view.findViewById(R.id.previewSubtitleTextView)
        val previewImageView: ImageView = view.findViewById(R.id.previewImageView)
        val previewMessageTextView: TextView = view.findViewById(R.id.previewMessageTextView)
        val detailsButton: Button = view.findViewById(R.id.detailsButton)
        val closeButton: Button = view.findViewById(R.id.closeButton)

        val cardId = arguments?.getString(ARG_CARD_ID).orEmpty()
        val pokemonCard = if (cardId.isBlank()) {
            null
        } else {
            cardRepository.getPokemonCard(cardId)
        }

        if (pokemonCard == null) {
            previewTitleTextView.text = getString(R.string.card_preview_title_fallback)
            previewSubtitleTextView.text = cardId.ifBlank {
                getString(R.string.card_preview_unknown_value)
            }
            previewImageView.setImageDrawable(null)
            previewMessageTextView.text = getString(R.string.card_preview_unknown_card)
            previewMessageTextView.visibility = View.VISIBLE
            detailsButton.isEnabled = false
        } else {
            bindPreviewCard(
                pokemonCard = pokemonCard,
                previewTitleTextView = previewTitleTextView,
                previewSubtitleTextView = previewSubtitleTextView,
                previewImageView = previewImageView,
                previewMessageTextView = previewMessageTextView,
                detailsButton = detailsButton
            )
        }

        closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun bindPreviewCard(
        pokemonCard: PokemonCard,
        previewTitleTextView: TextView,
        previewSubtitleTextView: TextView,
        previewImageView: ImageView,
        previewMessageTextView: TextView,
        detailsButton: Button
    ) {
        previewTitleTextView.text = pokemonCard.name.ifBlank {
            getString(R.string.card_preview_title_fallback)
        }

        previewSubtitleTextView.text = getString(
            R.string.card_preview_set_and_id,
            pokemonCard.setName.ifBlank {
                getString(R.string.card_preview_unknown_value)
            },
            pokemonCard.cardId.ifBlank {
                getString(R.string.card_preview_unknown_value)
            }
        )

        previewMessageTextView.visibility = View.GONE
        PokemonCardImageBinder.bind(previewImageView, pokemonCard)

        detailsButton.isEnabled = true
        detailsButton.setOnClickListener {
            showCardDetails(pokemonCard)
        }
    }

    private fun showCardDetails(pokemonCard: PokemonCard) {
        val typesText = if (pokemonCard.types.isEmpty()) {
            getString(R.string.card_preview_unknown_value)
        } else {
            pokemonCard.types.joinToString(", ")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.card_preview_details_title)
            .setMessage(
                getString(
                    R.string.card_preview_details_message,
                    pokemonCard.name.ifBlank { getString(R.string.card_preview_unknown_value) },
                    pokemonCard.setName.ifBlank { getString(R.string.card_preview_unknown_value) },
                    typesText,
                    pokemonCard.cardId.ifBlank { getString(R.string.card_preview_unknown_value) }
                )
            )
            .setPositiveButton(android.R.string.ok, null)
            .show()
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