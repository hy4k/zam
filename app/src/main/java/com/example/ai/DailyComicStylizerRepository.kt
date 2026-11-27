package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.data.model.FamilyMemberId
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private const val TAG = "DailyComicStylizer"

enum class ComicPhotoStyle(
    val id: String,
    val label: String,
    val emoji: String,
    val promptInstruction: String
) {
    SUNNY_COMIC(
        id = "SUNNY_COMIC",
        label = "Sunny Comic",
        emoji = "💥",
        promptInstruction = "Transform this real family photograph into a joyful, vibrant Sunday comic book illustration. Keep the exact people, their faces, smiles, clothing, poses, and background recognizable, but redraw the entire image with crisp bold black ink outlines, bright cel-shaded comic colors, playful halftone dot shading in the shadows, and warm family storybook charm. Do not add any random text or speech bubbles."
    ),
    POP_ART(
        id = "POP_ART",
        label = "Pop Art",
        emoji = "🎨",
        promptInstruction = "Transform this real family photograph into a colorful retro pop-art comic panel. Preserve the real faces, expressions, poses, and setting accurately, while rendering bold graphic ink contours, punchy primary comic colors, energetic Ben-Day halftone dots, and expressive comic-book highlights. Keep it wholesome, bright, and kid-friendly with no text overlays."
    ),
    STORYBOOK(
        id = "STORYBOOK",
        label = "Storybook",
        emoji = "📖",
        promptInstruction = "Transform this real family photograph into a charming European ligne-claire comic storybook illustration (clean crisp ink linework with rich, warm gouache and watercolor comic fills). Keep all people's likenesses, clothing, and background faithful to the photo, making it look like a beloved children's adventure comic frame with no text overlays."
    )
}

sealed interface DailyComicGenerationState {
    data object Idle : DailyComicGenerationState
    data class Loading(val styleLabel: String) : DailyComicGenerationState
    data class Error(val message: String) : DailyComicGenerationState
}

data class DailyComicResult(
    val localImagePath: String,
    val shareBase64: String
)

/**
 * Deterministic daily turn scheduler rotating across Mithun (Vapi), Mafitha (Ummi), and Zayn Adam.
 * One shared family comic photo per calendar day.
 */
object DailyTurnSchedule {
    private val turnOrder = listOf(
        FamilyMemberId.ZAYN,
        FamilyMemberId.MITHUN,
        FamilyMemberId.MAFITHA
    )

    fun todayDateKey(nowMs: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))
    }

    fun formatFriendlyDate(dateKey: String, nowMs: Long = System.currentTimeMillis()): String {
        val today = todayDateKey(nowMs)
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs; add(Calendar.DAY_OF_YEAR, -1) }
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        val parsed = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey)
        }.getOrNull()
        val shortDate = parsed?.let {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(it)
        } ?: dateKey

        return when (dateKey) {
            today -> "Today • $shortDate"
            yesterday -> "Yesterday • $shortDate"
            else -> parsed?.let {
                SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(it)
            } ?: dateKey
        }
    }

    fun turnMemberForDay(nowMs: Long = System.currentTimeMillis()): FamilyMemberId {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayIndex = ((cal.timeInMillis / 86_400_000L) % turnOrder.size).toInt()
        val normalized = if (dayIndex < 0) dayIndex + turnOrder.size else dayIndex
        return turnOrder[normalized]
    }

    fun nextTurnMember(current: FamilyMemberId): FamilyMemberId {
        val idx = turnOrder.indexOf(current).takeIf { it >= 0 } ?: 0
        return turnOrder[(idx + 1) % turnOrder.size]
    }

    fun orderedTurnLineup(): List<FamilyMemberId> = turnOrder
}

/**
 * Turns a captured or uploaded family photo into a stylized comic-book panel
 * using Firebase AI Logic (`gemini-nano-banana-2.1`) image-to-image editing.
 */
class DailyComicStylizerRepository(private val appContext: Context) {

    private val imageModel by lazy {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config
        )
    }

    suspend fun stylizePhotoToComic(
        sourceUri: Uri,
        style: ComicPhotoStyle,
        dateKey: String,
        author: FamilyMemberId
    ): Result<DailyComicResult> = withContext(Dispatchers.IO) {
        try {
            val inputBitmap = decodeAndOrientBitmap(sourceUri, maxDimension = 1024)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Could not read the selected photo. Please pick another picture.")
                )

            val fullPrompt = buildString {
                append(style.promptInstruction)
                append(" This is a warm daily family photo shared by ${author.displayName} (${author.title}) for their family comic album.")
            }

            val comicBitmap = withTimeout(60_000L) {
                val response = imageModel.generateContent(
                    content {
                        image(inputBitmap)
                        text(fullPrompt)
                    }
                )
                response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                    ?.image
            }

            if (comicBitmap == null) {
                return@withContext Result.failure(
                    IllegalStateException("No comic picture was returned. Please tap Try Again.")
                )
            }

            val savedFile = saveComicBitmap(comicBitmap, dateKey)
            val shareBase64 = encodeShareThumbnailBase64(comicBitmap)
            Result.success(
                DailyComicResult(
                    localImagePath = savedFile.absolutePath,
                    shareBase64 = shareBase64
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Comic photo transformation timed out", e)
            Result.failure(
                IllegalStateException("Turning your photo into a comic took too long. Please try again.")
            )
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Comic photo transformation rate limit exceeded", e)
            Result.failure(
                IllegalStateException("Our comic artist is busy right now. Please try again in a minute.")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Comic photo transformation failed", e)
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "Couldn't turn your photo into a comic right now. Please try again."
                )
            )
        }
    }

    fun saveReceivedBase64Comic(dateKey: String, base64Data: String): String? {
        if (base64Data.isBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val dir = File(appContext.filesDir, "daily_comics").apply { mkdirs() }
            val outFile = File(dir, "comic_${dateKey}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { it.write(bytes) }
            outFile.absolutePath
        }.onFailure { err ->
            Log.e(TAG, "Failed saving received daily comic image", err)
        }.getOrNull()
    }

    private fun saveComicBitmap(bitmap: Bitmap, dateKey: String): File {
        val dir = File(appContext.filesDir, "daily_comics").apply { mkdirs() }
        val file = File(dir, "comic_${dateKey}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file
    }

    private fun encodeShareThumbnailBase64(bitmap: Bitmap): String {
        val maxSide = 640
        val scale = min(1f, maxSide.toFloat() / max(bitmap.width, bitmap.height).toFloat())
        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        val stream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        if (resized !== bitmap) resized.recycle()
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun decodeAndOrientBitmap(uri: Uri, maxDimension: Int): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        appContext.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

        var sampleSize = 1
        while (boundsOpts.outWidth / sampleSize > maxDimension * 2 ||
            boundsOpts.outHeight / sampleSize > maxDimension * 2
        ) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = appContext.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        val rotationDegrees = runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { exifStream ->
                when (
                    ExifInterface(exifStream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)

        val scale = min(1f, maxDimension.toFloat() / max(decoded.width, decoded.height).toFloat())
        if (rotationDegrees == 0f && scale >= 1f) return decoded

        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            if (rotationDegrees != 0f) postRotate(rotationDegrees)
        }
        val transformed = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (transformed !== decoded) decoded.recycle()
        return transformed
    }
}
