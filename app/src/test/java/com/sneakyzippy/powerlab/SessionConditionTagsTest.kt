package com.sneakyzippy.powerlab

import com.sneakyzippy.powerlab.ui.components.SessionConditionTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionConditionTagsTest {

    @Test
    fun `toggleTag switches mutually exclusive pairs`() {
        var tags = setOf(SessionConditionTags.TAG_SCREEN_OFF, SessionConditionTags.TAG_CASE_OFF)
        tags = SessionConditionTags.toggleTag(tags, SessionConditionTags.TAG_SCREEN_ON)

        assertTrue(SessionConditionTags.TAG_SCREEN_ON in tags)
        assertFalse(SessionConditionTags.TAG_SCREEN_OFF in tags)
        assertTrue(SessionConditionTags.TAG_CASE_OFF in tags)

        tags = SessionConditionTags.toggleTag(tags, SessionConditionTags.TAG_WITH_CASE)
        assertTrue(SessionConditionTags.TAG_WITH_CASE in tags)
        assertFalse(SessionConditionTags.TAG_CASE_OFF in tags)
    }

    @Test
    fun `parseNotes separates known tags, legacy aliases, and custom text`() {
        val raw = "🔋 Wireless • 📦 Thick case • 🎮 Gaming while charging • Desk stand"
        val parsed = SessionConditionTags.parseNotes(raw)

        assertEquals(
            setOf(
                SessionConditionTags.TAG_WIRELESS,
                SessionConditionTags.TAG_WITH_CASE,
                SessionConditionTags.TAG_GAMING
            ),
            parsed.tags
        )
        assertEquals("Desk stand", parsed.customText)
    }

    @Test
    fun `formatNotes preserves canonical order and appends custom text`() {
        val formatted = SessionConditionTags.formatNotes(
            tags = setOf(SessionConditionTags.TAG_WIRELESS, SessionConditionTags.TAG_SCREEN_ON),
            customText = "  Anker pad  "
        )
        assertEquals("🔆 Screen on • 🔋 Wireless • Anker pad", formatted)
    }
}
