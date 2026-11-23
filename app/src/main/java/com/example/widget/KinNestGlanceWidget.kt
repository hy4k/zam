package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.telecom.FamilyTelecomManager

class KinNestGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KinNestGlanceWidget()
}

/**
 * Android Home-Screen Widget (Jetpack Glance):
 * One-tap 3-Way Call Launcher and Heartbeat "I Miss You" trigger right from the home screen.
 */
class KinNestGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent(context)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        val callIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(FamilyTelecomManager.EXTRA_ACTION, FamilyTelecomManager.ACTION_QUICK_CALL)
        }
        val heartbeatIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(FamilyTelecomManager.EXTRA_ACTION, FamilyTelecomManager.ACTION_QUICK_HEARTBEAT)
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFFE6FFFA))
                .cornerRadius(24.dp)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KinNest • Mithun, Mafitha & Zayn",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF0F766E)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(10.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(Color(0xFF2563EB))
                        .cornerRadius(18.dp)
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                        .clickable(actionStartActivity(callIntent)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚀 Family Call",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.width(10.dp))
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(Color(0xFFF43F5E))
                        .cornerRadius(18.dp)
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                        .clickable(actionStartActivity(heartbeatIntent)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💖 Miss You!",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
