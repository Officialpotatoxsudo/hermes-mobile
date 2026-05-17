package com.hermes.mobile.feature.chat

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class MessageInputFieldHitTargetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tappingInputContainerFocusesTextField() {
        composeRule.setContent {
            var value by remember { mutableStateOf("") }
            MessageInputField(
                value = value,
                onValueChange = { value = it },
                agentName = "Hermes Agent",
                modifier = Modifier
                    .width(260.dp)
                    .testTag("message-input-container"),
                textFieldModifier = Modifier.testTag("message-input-field"),
            )
        }

        composeRule.onNodeWithTag("message-input-field").assertIsNotFocused()
        composeRule.onNodeWithTag("message-input-container").performClick()
        composeRule.onNodeWithTag("message-input-field").assertIsFocused()
    }
}
