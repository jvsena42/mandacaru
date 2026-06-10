package com.github.jvsena42.mandacaru.presentation.ui.screens.node

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UtreexoPasteSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun prefilledText_isDisplayed_andImportEnabled() {
        val payload = "{\"version\":1,\"network\":\"signet\"}"
        composeRule.setContent {
            UtreexoPasteSheetContent(
                text = payload,
                errorMessage = null,
                onTextChange = {},
                onPasteFromClipboard = {},
                onPayloadSubmitted = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithTag("input_utreexo_payload")
            .assertTextContains(payload, substring = true)
        composeRule.onNodeWithTag("button_import_payload", useUnmergedTree = true)
            .assertIsEnabled()
    }

    @Test
    fun emptyText_disablesImport() {
        composeRule.setContent {
            UtreexoPasteSheetContent(
                text = "",
                errorMessage = null,
                onTextChange = {},
                onPasteFromClipboard = {},
                onPayloadSubmitted = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithTag("button_import_payload", useUnmergedTree = true)
            .assertIsNotEnabled()
    }

    @Test
    fun pasteButton_invokesCallback() {
        var pasteClicked = false
        composeRule.setContent {
            UtreexoPasteSheetContent(
                text = "",
                errorMessage = null,
                onTextChange = {},
                onPasteFromClipboard = { pasteClicked = true },
                onPayloadSubmitted = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithTag("button_paste_clipboard", useUnmergedTree = true)
            .performClick()

        assertTrue(pasteClicked)
    }

    @Test
    fun errorMessage_isDisplayed() {
        val error = "Clipboard does not contain a valid accumulator."
        composeRule.setContent {
            UtreexoPasteSheetContent(
                text = "",
                errorMessage = error,
                onTextChange = {},
                onPasteFromClipboard = {},
                onPayloadSubmitted = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithText(error).assertIsDisplayed()
    }

    @Test
    fun typing_invokesOnTextChange() {
        var captured = ""
        composeRule.setContent {
            UtreexoPasteSheetContent(
                text = "",
                errorMessage = null,
                onTextChange = { captured = it },
                onPasteFromClipboard = {},
                onPayloadSubmitted = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithTag("input_utreexo_payload").performTextInput("abc")

        assertEquals("abc", captured)
    }

    @Test
    fun pasteButton_readsClipboardIntoField() {
        val payload = "{\"version\":1,\"network\":\"signet\"}"
        composeRule.setContent {
            val clipboard = LocalClipboardManager.current
            LaunchedEffect(Unit) { clipboard.setText(AnnotatedString(payload)) }
            var text by remember { mutableStateOf("") }
            UtreexoPasteSheetContent(
                text = text,
                errorMessage = null,
                onTextChange = { text = it },
                onPasteFromClipboard = { text = clipboard.getText()?.text.orEmpty() },
                onPayloadSubmitted = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithTag("button_paste_clipboard", useUnmergedTree = true)
            .performClick()

        composeRule.onNodeWithTag("input_utreexo_payload")
            .assertTextContains(payload, substring = true)
    }
}
