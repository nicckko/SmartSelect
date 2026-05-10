package com.smartselect.ui.compare

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.smartselect.R
import com.smartselect.data.model.Phone
import com.smartselect.databinding.FragmentCompareBinding
import com.smartselect.utils.GlideImageLoader
import com.smartselect.utils.toPeso
import com.smartselect.viewmodel.PhoneViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CompareFragment : Fragment() {

    private var _binding: FragmentCompareBinding? = null
    private val binding get() = _binding!!
    private val phoneViewModel: PhoneViewModel by activityViewModels()

    // ─── Spec definitions ────────────────────────────────────────────────────

    data class SpecDef(
        val emoji: String,
        val label: String,
        val extract: (Phone) -> String,
        val parse: ((Phone) -> Double)?  // null = no winner (just info)
    )

    private val specs = listOf(
        SpecDef("💰", "Price",     { it.price.toPeso() },  { it.price * -1 }),  // lower = better
        SpecDef("⚡", "Chipset",   { it.chipset },          null),
        SpecDef("🧠", "RAM",       { it.ram },              { parseGb(it.ram) }),
        SpecDef("💾", "Storage",   { it.storage },          { parseGb(it.storage) }),
        SpecDef("📷", "Camera",    { it.camera },           { parseMp(it.camera) }),
        SpecDef("🔋", "Battery",   { it.battery },          { parseMah(it.battery) }),
        SpecDef("🖥️", "Display",  { it.display },          { parseInch(it.display) }),
        SpecDef("📱", "Category",  { it.category },         null),
    )

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnClear.setOnClickListener { phoneViewModel.clearCompare() }
        observeCompareList()
    }

    private fun observeCompareList() {
        lifecycleScope.launch {
            phoneViewModel.compareList.collect { phones ->
                if (_binding == null) return@collect
                if (phones.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.scrollView.visibility  = View.GONE
                    binding.layoutLegend.visibility = View.GONE
                    binding.tvCount.text = "Add up to 3 phones"
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.scrollView.visibility  = View.VISIBLE
                    binding.layoutLegend.visibility = View.VISIBLE
                    binding.tvCount.text = "${phones.size}/3 phones selected"
                    buildCompareUI(phones)
                }
            }
        }
    }

    // ─── Main builder ────────────────────────────────────────────────────────

    private fun buildCompareUI(phones: List<Phone>) {
        buildPhoneHeaders(phones)
        buildSpecCards(phones)
    }

    // ─── Phone header cards ───────────────────────────────────────────────────

    private fun buildPhoneHeaders(phones: List<Phone>) {
        val container = binding.layoutPhoneHeaders
        container.removeAllViews()

        phones.forEach { phone ->
            val weight = when (phones.size) {
                1 -> 1.0f
                2 -> 1.0f
                else -> 1.0f
            }
            val margin = dp(6)

            // Outer card
            val card = MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, weight
                ).also {
                    it.marginStart = margin
                    it.marginEnd   = margin
                }
                radius = dp(20).toFloat()
                strokeWidth = dp(1)
                setStrokeColor(ContextCompat.getColorStateList(context, R.color.divider))
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                cardElevation = 0f
            }

            val inner = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity     = Gravity.CENTER
                setPadding(dp(12), dp(16), dp(12), dp(14))
            }

            // Phone image
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(72), dp(72))
                scaleType    = ImageView.ScaleType.FIT_CENTER
            }
            GlideImageLoader.loadImage(requireContext(), phone.imageUrl, imageView)
            inner.addView(imageView)

            // Brand label
            val tvBrand = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text      = phone.brand.uppercase()
                textSize  = 9f
                gravity   = Gravity.CENTER
                letterSpacing = 0.1f
                setTextColor(ContextCompat.getColor(context, R.color.text_tertiary))
                setPadding(0, dp(8), 0, dp(2))
            }
            inner.addView(tvBrand)

            // Model name
            val tvModel = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text      = phone.model
                textSize  = 12f
                gravity   = Gravity.CENTER
                maxLines  = 2
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            inner.addView(tvModel)

            // Price
            val tvPrice = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = dp(6) }
                text      = phone.price.toPeso()
                textSize  = 13f
                gravity   = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.accent))
            }
            inner.addView(tvPrice)

            // Remove (×) button
            val btnRemove = MaterialButton(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(32)
                ).also { it.topMargin = dp(10); it.gravity = Gravity.CENTER_HORIZONTAL }
                text      = "Remove"
                textSize  = 10f
                minWidth  = 0
                minHeight = 0
                setPadding(dp(12), 0, dp(12), 0)
                setTextColor(ContextCompat.getColor(context, R.color.error))
                strokeColor = ContextCompat.getColorStateList(context, R.color.error_light)
                strokeWidth = dp(1)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                cornerRadius = dp(10)
                setOnClickListener { phoneViewModel.removeFromCompare(phone) }
            }
            inner.addView(btnRemove)

            card.addView(inner)
            container.addView(card)
        }
    }

    // ─── Spec cards ──────────────────────────────────────────────────────────

    private fun buildSpecCards(phones: List<Phone>) {
        val container = binding.layoutSpecs
        container.removeAllViews()

        specs.forEach { spec ->
            val values     = phones.map { spec.extract(it) }
            val allSame    = values.toSet().size == 1
            val winnerIdx  = if (!allSame && spec.parse != null) {
                phones.indices.maxByOrNull { spec.parse.invoke(phones[it]) }
            } else null

            // Outer spec card
            val specCard = MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dp(10) }
                radius = dp(20).toFloat()
                strokeWidth = dp(1)
                setStrokeColor(ContextCompat.getColorStateList(context, R.color.divider))
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                cardElevation = 0f
            }

            val cardInner = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(16))
            }

            // ── Spec label row ─────────────────────────────────────────────
            val labelRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dp(12) }
            }

            val tvEmoji = TextView(requireContext()).apply {
                text     = spec.emoji
                textSize = 16f
                setPadding(0, 0, dp(8), 0)
            }
            labelRow.addView(tvEmoji)

            val tvLabel = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text     = spec.label.uppercase()
                textSize = 10f
                letterSpacing = 0.1f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
            labelRow.addView(tvLabel)

            // "Different" badge when values differ
            if (!allSame) {
                val badge = TextView(requireContext()).apply {
                    text     = "DIFFERENT"
                    textSize = 8f
                    letterSpacing = 0.05f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(context, R.color.warning))
                    setBackgroundColor(ContextCompat.getColor(context, R.color.warning_light))
                    setPadding(dp(8), dp(3), dp(8), dp(3))
                }
                labelRow.addView(badge)
            }

            cardInner.addView(labelRow)

            // Thin divider
            val divider = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
                ).also { it.bottomMargin = dp(12) }
                setBackgroundColor(ContextCompat.getColor(context, R.color.divider))
            }
            cardInner.addView(divider)

            // ── Value columns ──────────────────────────────────────────────
            val valuesRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            values.forEachIndexed { idx, value ->
                val isWinner = winnerIdx == idx
                val phone    = phones[idx]

                val colOuter = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity     = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                        if (idx > 0) it.marginStart = dp(8)
                    }

                    // Winner gets a tinted background, loser gets nothing
                    if (isWinner) {
                        setBackgroundColor(ContextCompat.getColor(context, R.color.success_light))
                        setPadding(dp(8), dp(10), dp(8), dp(10))
                    } else {
                        setBackgroundColor(ContextCompat.getColor(context, R.color.surface_variant))
                        setPadding(dp(8), dp(10), dp(8), dp(10))
                    }
                }

                // Phone name mini label on top
                val tvPhoneName = TextView(requireContext()).apply {
                    text      = phone.model
                    textSize  = 9f
                    gravity   = Gravity.CENTER
                    maxLines  = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = dp(5) }
                    setTextColor(ContextCompat.getColor(context, if (isWinner) R.color.success else R.color.text_tertiary))
                }
                colOuter.addView(tvPhoneName)

                // The value itself
                val tvValue = TextView(requireContext()).apply {
                    text      = value
                    textSize  = 13f
                    gravity   = Gravity.CENTER
                    maxLines  = 3
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    if (isWinner) {
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(ContextCompat.getColor(context, R.color.success))
                    } else {
                        setTypeface(null, if (allSame) Typeface.NORMAL else Typeface.NORMAL)
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    }
                }
                colOuter.addView(tvValue)

                // Winner crown badge
                if (isWinner) {
                    val crown = TextView(requireContext()).apply {
                        text     = "👑 Best"
                        textSize = 9f
                        gravity  = Gravity.CENTER
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(ContextCompat.getColor(context, R.color.success))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { it.topMargin = dp(4) }
                    }
                    colOuter.addView(crown)
                }

                valuesRow.addView(colOuter)

                // Vertical separator between columns
                if (idx < values.lastIndex) {
                    val sep = View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(1), LinearLayout.LayoutParams.MATCH_PARENT)
                        setBackgroundColor(ContextCompat.getColor(context, R.color.divider))
                    }
                    valuesRow.addView(sep)
                }
            }

            cardInner.addView(valuesRow)
            specCard.addView(cardInner)
            container.addView(specCard)
        }
    }

    // ─── Spec parsers ────────────────────────────────────────────────────────

    private fun parseGb(s: String): Double =
        Regex("""(\d+)\s*(?:GB|gb)""").find(s)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

    private fun parseMah(s: String): Double =
        Regex("""(\d+)\s*(?:mAh|mah)""", RegexOption.IGNORE_CASE).find(s)
            ?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

    private fun parseMp(s: String): Double =
        Regex("""(\d+)\s*MP""", RegexOption.IGNORE_CASE).find(s)
            ?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

    private fun parseInch(s: String): Double {
        // Extract last number from display string (e.g. "6.7 inch", "6.7"")
        return Regex("[0-9]+(?:\\.[0-9]+)?").findAll(s)
            .lastOrNull()?.value?.toDoubleOrNull() ?: 0.0
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}