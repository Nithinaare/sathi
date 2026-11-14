package com.farmsaathi.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.farmsaathi.core.designsystem.components.DropdownOption
import com.farmsaathi.core.designsystem.components.FarmAmountInput
import com.farmsaathi.core.designsystem.components.FarmDropdown
import com.farmsaathi.core.designsystem.components.FarmImageGridPicker
import com.farmsaathi.core.designsystem.components.FarmImagePickerOption
import com.farmsaathi.core.designsystem.components.FarmListItem
import com.farmsaathi.core.designsystem.components.FarmMoreDetailsAccordion
import com.farmsaathi.core.designsystem.components.FarmNumberInput
import com.farmsaathi.core.designsystem.components.FarmPageHeader
import com.farmsaathi.core.designsystem.components.FarmPrimaryButton
import com.farmsaathi.core.designsystem.components.FarmRupeeChipInput
import com.farmsaathi.core.designsystem.components.FarmSearchField
import com.farmsaathi.core.designsystem.components.FarmStatusIndicator
import com.farmsaathi.core.designsystem.components.FarmStatusType
import com.farmsaathi.core.designsystem.components.FarmStickyFooterBar
import com.farmsaathi.core.designsystem.components.FarmTextInput
import com.farmsaathi.core.designsystem.components.FarmTimeSlotPicker
import com.farmsaathi.core.designsystem.components.FarmVisualGridTile
import com.farmsaathi.core.designsystem.localization.FarmImages
import com.farmsaathi.core.designsystem.localization.LocalFarmSpeakHandler
import com.farmsaathi.core.designsystem.localization.ProvideFarmLocale
import com.farmsaathi.core.designsystem.localization.farmString
import com.farmsaathi.core.designsystem.theme.FarmSaathiTheme
import com.farmsaathi.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
class DesignSystemComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `primary button and status indicator render and handle clicks`() {
        var clicked = false
        composeRule.setContent {
            FarmSaathiTheme {
                FarmPrimaryButton(
                    text = "సేవ్ చేయండి",
                    onClick = { clicked = true },
                )
                FarmStatusIndicator(
                    type = FarmStatusType.WARNING,
                    label = "బాకీ ఉంది",
                )
            }
        }
        composeRule.onNodeWithText("సేవ్ చేయండి").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("బాకీ ఉంది").assertIsDisplayed()
        assertTrue(clicked)
    }

    @Test
    fun `locale switch toggles strings between telugu and english instantaneously`() {
        var lang by mutableStateOf(AppLanguage.TELUGU)
        composeRule.setContent {
            FarmSaathiTheme {
                ProvideFarmLocale(language = lang) {
                    FarmAmountInput(
                        value = "1200",
                        onValueChange = {},
                        label = farmString(R.string.common_save),
                    )
                }
            }
        }
        composeRule.onNodeWithText("సేవ్ చేయండి").assertIsDisplayed()
        lang = AppLanguage.ENGLISH
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun `holding visual tile speaks custom meaning without triggering click`() {
        var clicked = false
        var spoken: String? = null
        val customMeaning = "విత్తనాలు, మందులు లేదా కూలీల ఖర్చులు ఇక్కడ రాయండి."

        composeRule.setContent {
            FarmSaathiTheme {
                CompositionLocalProvider(LocalFarmSpeakHandler provides { spoken = it }) {
                    FarmVisualGridTile(
                        title = "ఖర్చు రాయండి",
                        icon = Icons.Rounded.Payments,
                        speakDescription = customMeaning,
                        onClick = { clicked = true },
                    )
                }
            }
        }

        composeRule.onNodeWithText("ఖర్చు రాయండి").performTouchInput { longClick() }
        composeRule.waitForIdle()
        assertEquals(customMeaning, spoken)
        assertFalse(clicked)

        composeRule.onNodeWithText("ఖర్చు రాయండి").performClick()
        composeRule.waitForIdle()
        assertTrue(clicked)
    }

    @Test
    fun `rupee chip input, accordion, and image grid picker behave cleanly`() {
        var amount by mutableStateOf("")
        var expanded by mutableStateOf(false)
        var picked by mutableStateOf("paddy")

        composeRule.setContent {
            FarmSaathiTheme {
                androidx.compose.foundation.layout.Column {
                    FarmRupeeChipInput(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "మొత్తం (₹)",
                        useTelugu = true,
                    )
                    FarmImageGridPicker(
                        label = "పంట ఎంచుకోండి",
                        options =
                        listOf(
                            FarmImagePickerOption("paddy", "వరి", FarmImages.cropPaddy),
                            FarmImagePickerOption("cotton", "పత్తి", FarmImages.forCropName("cotton")),
                        ),
                        selected = picked,
                        onSelect = { picked = it },
                    )
                    FarmMoreDetailsAccordion(
                        expanded = expanded,
                        onToggle = { expanded = !expanded },
                        useTelugu = true,
                    ) {
                        FarmAmountInput(value = "", onValueChange = {}, label = "దాగిన ఫీల్డ్")
                    }
                }
            }
        }

        composeRule.onNodeWithText("+₹500").performClick()
        assertEquals("500", amount)

        composeRule.onNodeWithText("పత్తి").performClick()
        assertEquals("cotton", picked)

        composeRule.onNodeWithText("దాగిన ఫీల్డ్").assertDoesNotExist()
        composeRule.onNodeWithText("మరిన్ని వివరాలు").performClick()
        composeRule.onNodeWithText("దాగిన ఫీల్డ్").assertIsDisplayed()
    }

    @Test
    fun `full bleed tile speaker badge is 48dp independent clickable and invokes speak handler`() {
        var tileClicked = false
        var spoken: String? = null
        val customMeaning = "ఖర్చు వివరాలు వినండి"

        composeRule.setContent {
            FarmSaathiTheme {
                ProvideFarmLocale(language = AppLanguage.TELUGU) {
                    CompositionLocalProvider(LocalFarmSpeakHandler provides { spoken = it }) {
                        FarmVisualGridTile(
                            title = "ఖర్చు",
                            icon = Icons.Rounded.Payments,
                            imageRes = FarmImages.expense,
                            fullBleedImage = true,
                            speakDescription = customMeaning,
                            onClick = { tileClicked = true },
                        )
                    }
                }
            }
        }

        val speakerNode = composeRule.onNodeWithContentDescription("వినండి: ఖర్చు")
        speakerNode
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.waitForIdle()
        assertEquals(customMeaning, spoken)
        assertFalse(tileClicked)

        composeRule.onNodeWithContentDescription("ఖర్చు").performClick()
        composeRule.waitForIdle()
        assertTrue(tileClicked)
    }

    @Test
    fun `toggleable list item exposes Role Switch and toggles checked state`() {
        var enabled by mutableStateOf(false)
        composeRule.setContent {
            FarmSaathiTheme {
                FarmListItem(
                    title = "మాట మార్గదర్శి",
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                )
            }
        }

        val row = composeRule.onNodeWithText("మాట మార్గదర్శి")
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
        row.assertIsOff()
        row.performClick()
        composeRule.waitForIdle()
        assertTrue(enabled)
        row.assertIsOn()
    }

    @Test
    fun `header listen and search clear descriptions localize dynamically`() {
        var lang by mutableStateOf(AppLanguage.TELUGU)
        composeRule.setContent {
            FarmSaathiTheme {
                ProvideFarmLocale(language = lang) {
                    Column {
                        FarmPageHeader(
                            title = "శీర్షిక",
                            audioCueKey = "cue_1",
                            onToggleAudio = {},
                        )
                        FarmSearchField(
                            query = "వరి",
                            onQueryChange = {},
                            placeholder = "వెతకండి",
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithContentDescription("వినండి").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("వెతుకుడు చెరపండి").assertIsDisplayed()

        lang = AppLanguage.ENGLISH
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Listen").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear search").assertIsDisplayed()
    }

    @Test
    fun `non-notched number input and dropdown align flush in row and dropdown marks selected`() {
        var numberTop = 0f
        var dropdownTop = 0f
        var selectedUnit by mutableStateOf("kg")

        composeRule.setContent {
            FarmSaathiTheme {
                ProvideFarmLocale(language = AppLanguage.ENGLISH) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FarmNumberInput(
                                value = "10",
                                onValueChange = {},
                                label = "Quantity",
                                modifier =
                                Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { numberTop = it.boundsInRoot().top },
                            )
                            FarmDropdown(
                                label = "Unit",
                                options =
                                listOf(
                                    DropdownOption("kg", "Kg"),
                                    DropdownOption("bag", "Bags"),
                                ),
                                selectedValue = selectedUnit,
                                onSelect = { selectedUnit = it.value },
                                placeholder = "Choose",
                                modifier =
                                Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { dropdownTop = it.boundsInRoot().top },
                            )
                        }
                        FarmTextInput(
                            value = "Ramulu",
                            onValueChange = {},
                            label = "Name",
                        )
                        FarmStickyFooterBar(modifier = Modifier.testTag("sticky_footer")) {
                            Text("Save")
                        }
                    }
                }
            }
        }

        composeRule.waitForIdle()
        assertTrue("Expected flush top alignment, diff=${abs(numberTop - dropdownTop)}", abs(numberTop - dropdownTop) < 1f)
        composeRule.onNodeWithTag("sticky_footer").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsDisplayed()

        composeRule.onNodeWithText("Kg").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Kg").onLast().assertIsSelected()
        composeRule.onNodeWithText("Bags").assertIsDisplayed().assertIsNotSelected()
        composeRule.onNodeWithTag("sticky_footer").assertIsDisplayed()
        composeRule.onNodeWithText("Bags").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals("bag", selectedUnit)
        composeRule.onNodeWithText("Bags").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Bags").onLast().assertIsSelected()
    }

    @Test
    fun `time slot picker updates hour and minute via presets and 30m steppers`() {
        val baseCal =
            Calendar.getInstance().apply {
                set(2026, Calendar.NOVEMBER, 12, 9, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        var millis by mutableLongStateOf(baseCal.timeInMillis)

        composeRule.setContent {
            FarmSaathiTheme {
                ProvideFarmLocale(language = AppLanguage.TELUGU) {
                    FarmTimeSlotPicker(
                        label = "సమయం",
                        epochMillis = millis,
                        onEpochMillisChange = { millis = it },
                    )
                }
            }
        }

        composeRule.onNodeWithText("ఉదయం 07:00").performClick()
        composeRule.waitForIdle()
        val afterPreset = Calendar.getInstance().apply { timeInMillis = millis }
        assertEquals(7, afterPreset.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, afterPreset.get(Calendar.MINUTE))

        composeRule.onNodeWithText("+30 నిమి").performClick()
        composeRule.waitForIdle()
        val afterPlus = Calendar.getInstance().apply { timeInMillis = millis }
        assertEquals(7, afterPlus.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, afterPlus.get(Calendar.MINUTE))
    }
}
