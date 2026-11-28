package com.example.media

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.TextureView
import org.webrtc.EglBase
import org.webrtc.EglRenderer
import org.webrtc.GlRectDrawer
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Compose-safe Native WebRTC TextureView VideoSink backed by org.webrtc.EglRenderer.
 *
 * Unlike SurfaceViewRenderer (which punches a separate window layer behind Compose and breaks
 * rounded-corner clipping, Z-ordering, and multi-tile splits), TextureView renders cleanly inside
 * Jetpack Compose's graphics pipeline so 2-way & 3-way family video tiles, rounded corners, AR
 * filters, emoji bursts, and live drawing overlays stay 100% glitch-free.
 */
class WebRtcTextureVideoView(context: Context) :
    TextureView(context),
    TextureView.SurfaceTextureListener,
    VideoSink {

    private val eglRenderer = EglRenderer("HolyMolyWebRtcVideo")
    private var isInitialized = false
    private var mirrorEnabled = false

    init {
        isOpaque = false
        surfaceTextureListener = this
    }

    fun init(sharedContext: EglBase.Context) {
        if (isInitialized) return
        eglRenderer.init(sharedContext, EglBase.CONFIG_PLAIN, GlRectDrawer())
        eglRenderer.setMirror(mirrorEnabled)
        isInitialized = true
        updateLayoutAspectRatio(width, height)
        val existingSurface = surfaceTexture
        if (isAvailable && existingSurface != null) {
            eglRenderer.createEglSurface(existingSurface)
        }
    }

    fun setMirror(mirror: Boolean) {
        mirrorEnabled = mirror
        if (isInitialized) {
            eglRenderer.setMirror(mirror)
        }
    }

    override fun onFrame(frame: VideoFrame) {
        if (isInitialized) {
            eglRenderer.onFrame(frame)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateLayoutAspectRatio(w, h)
    }

    private fun updateLayoutAspectRatio(w: Int, h: Int) {
        if (isInitialized && w > 0 && h > 0) {
            eglRenderer.setLayoutAspectRatio(w.toFloat() / h.toFloat())
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (isInitialized) {
            eglRenderer.createEglSurface(surface)
            updateLayoutAspectRatio(width, height)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        updateLayoutAspectRatio(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        if (isInitialized) {
            val latch = CountDownLatch(1)
            eglRenderer.releaseEglSurface { latch.countDown() }
            runCatching { latch.await(400L, TimeUnit.MILLISECONDS) }
        }
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // Rendered frame committed to SurfaceTexture.
    }

    fun release() {
        if (!isInitialized) return
        isInitialized = false
        runCatching {
            val latch = CountDownLatch(1)
            eglRenderer.releaseEglSurface { latch.countDown() }
            latch.await(300L, TimeUnit.MILLISECONDS)
        }
        runCatching { eglRenderer.release() }
    }
}
