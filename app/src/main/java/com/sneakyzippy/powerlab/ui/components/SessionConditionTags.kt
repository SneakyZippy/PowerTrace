package com.sneakyzippy.powerlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sneakyzippy.powerlab.ui.theme.AccentBlue
import com.sneakyzippy.powerlab.ui.theme.DarkBackground
import com.sneakyzippy.powerlab.ui.theme.DarkSurfaceVariant
import com.sneakyzippy.powerlab.ui.theme.GreenPrimary
import com.sneakyzippy.powerlab.ui.theme.TextPrimary
import com.sneakyzippy.powerlab.ui.theme.TextSecondary

/**
 * Shared condition chip definitions and serialization helpers for charging session notes.
 * Stores tags inside the existing `ChargeTestSession.notes` field using ` • ` separators
 * so no Room database migration is required.
 */
object SessionConditionTags {

    const val TAG_SCREEN_OFF = "📱 Screen off"
    const val TAG_SCREEN_ON = "🔆 Screen on"
    const val TAG_CASE_OFF = "🛡️ Case off"
    const val TAG_WITH_CASE = "📦 With case"
    const val TAG_WIRELESS = "🔋 Wireless"
    const val TAG_COLD_ROOM = "❄️ Cold room"
    const val TAG_WARM_ROOM = "☀️ Warm room"
    const val TAG_GAMING = "🎮 Gaming"
    const val TAG_AIRPLANE = "✈️ Airplane mode"
    const val TAG_CAR_CHARGER = "🔌 Car charger"

    val ALL_TAGS = listOf(
        TAG_SCREEN_OFF,
        TAG_SCREEN_ON,
        TAG_CASE_OFF,
        TAG_WITH_CASE,
        TAG_WIRELESS,
        TAG_COLD_ROOM,
        TAG_WARM_ROOM,
        TAG_GAMING,
        TAG_AIRPLANE,
        TAG_CAR_CHARGER
    )

    private val LEGACY_ALIASES = mapOf(
        "🎮 Gaming while charging" to TAG_GAMING,
        "📦 Thick case" to TAG_WITH_CASE
    )

    private val EXCLUSIVE_PARTNERS = mapOf(
        TAG_SCREEN_OFF to TAG_SCREEN_ON,
        TAG_SCREEN_ON to TAG_SCREEN_OFF,
        TAG_CASE_OFF to TAG_WITH_CASE,
        TAG_WITH_CASE to TAG_CASE_OFF,
        TAG_COLD_ROOM to TAG_WARM_ROOM,
        TAG_WARM_ROOM to TAG_COLD_ROOM
    )

    data class ParsedNotes(
        val tags: Set<String>,
        val customText: String
    )

    fun parseNotes(notes: String): ParsedNotes {
        if (notes.isBlank()) return ParsedNotes(emptySet(), "")

        val tokens = notes.split("•").map { it.trim() }.filter { it.isNotEmpty() }
        val selectedTags = linkedSetOf<String>()
        val customParts = mutableListOf<String>()

        for (token in tokens) {
            val normalized = LEGACY_ALIASES[token] ?: token
            if (normalized in ALL_TAGS) {
                selectedTags.add(normalized)
            } else {
                customParts.add(token)
            }
        }
        return ParsedNotes(tags = selectedTags, customText = customParts.joinToString(" • "))
    }

    fun formatNotes(tags: Set<String>, customText: String): String {
        val orderedTags = ALL_TAGS.filter { it in tags }
        val trimmedCustom = customText.trim()
        return if (trimmedCustom.isNotEmpty()) {
            (orderedTags + trimmedCustom).joinToString(" • ")
        } else {
            orderedTags.joinToString(" • ")
        }
    }

    fun toggleTag(currentTags: Set<String>, tag: String): Set<String> {
        val next = currentTags.toMutableSet()
        if (tag in next) {
            next.remove(tag)
        } else {
            EXCLUSIVE_PARTNERS[tag]?.let { next.remove(it) }
            next.add(tag)
        }
        return next
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConditionTagChipsSelector(
    selectedTags: Set<String>,
    onToggleTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SessionConditionTags.ALL_TAGS.forEach { tag ->
            val isSelected = tag in selectedTags
            FilterChip(
                selected = isSelected,
                onClick = { onToggleTag(tag) },
                label = {
                    Text(
                        text = tag,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = DarkBackground,
                    labelColor = TextSecondary,
                    selectedContainerColor = GreenPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = GreenPrimary,
                    selectedLeadingIconColor = GreenPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionConditionChipsRow(
    notes: String,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (notes.isBlank()) return
    val parsed = SessionConditionTags.parseNotes(notes)

    Column(modifier = modifier.fillMaxWidth()) {
        if (parsed.tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SessionConditionTags.ALL_TAGS.filter { it in parsed.tags }.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceVariant)
                            .padding(
                                horizontal = if (compact) 7.dp else 9.dp,
                                vertical = if (compact) 3.dp else 4.dp
                            )
                    ) {
                        Text(
                            text = tag,
                            color = TextPrimary,
                            fontSize = if (compact) 10.sp else 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (parsed.customText.isNotBlank()) {
            if (parsed.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = "📝 ${parsed.customText}",
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
                color = if (compact) TextSecondary else AccentBlue,
                maxLines = if (compact) 1 else 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
