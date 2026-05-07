package com.nosoyyo0717.minecraftglyphmatrix

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the user backs out of the menu without picking, cancel the widget placement
        setResult(Activity.RESULT_CANCELED)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val categories = listOf(
            "Random (All)",
            "Minecraft Mob Face",
            "Minecraft Food",
            "Minecraft Armor and Tools",
            "Minecraft Items",
            "Minecraft Music Discs",
            "Minecraft Blocks"
        )

        setContent {
            // Pulls the dynamic theme colors directly from the user's phone/wallpaper!
            val context = androidx.compose.ui.platform.LocalContext.current
            val dynamicColors = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context)
            } else {
                darkColorScheme()
            }

            MaterialTheme(colorScheme = dynamicColors) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "WIDGET SETTINGS",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 16.dp, top = 20.dp)
                        )

                        LazyColumn {
                            items(categories) { category ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clickable { saveSelectionAndFinish(category) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = category,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveSelectionAndFinish(category: String) {
        // Save their choice to the phone's memory
        val prefs = getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("widget_$appWidgetId", category).apply()

        // Force the widget to update immediately with the new category
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val updateIntent = Intent(this, MatrixWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        sendBroadcast(updateIntent)

        // Tell Android it is successful and close the menu
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}