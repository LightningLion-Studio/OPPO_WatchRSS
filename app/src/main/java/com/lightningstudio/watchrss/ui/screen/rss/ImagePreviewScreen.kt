package com.lightningstudio.watchrss.ui.screen.rss

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.lightningstudio.watchrss.ui.components.WatchSurface
import com.lightningstudio.watchrss.ui.util.RssImageLoader
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import android.os.SystemClock

@Composable
fun ImagePreviewScreen(
    url: String,
    alt: String?
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val scaleAnimator = remember { Animatable(1f) }
    val offsetXAnimator = remember { Animatable(0f) }
    val offsetYAnimator = remember { Animatable(0f) }
    var scaleAnimJob by remember { mutableStateOf<Job?>(null) }
    var offsetAnimJob by remember { mutableStateOf<Job?>(null) }
    val panDecay = remember { exponentialDecay<Float>(frictionMultiplier = 0.15f) }
    val scaleDecay = remember { exponentialDecay<Float>(frictionMultiplier = 0.2f) }
    val springSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    }
    val springOffsetSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    }

    WatchSurface {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val containerWidthPx = with(density) { maxWidth.toPx() }
            val containerHeightPx = with(density) { maxHeight.toPx() }
            val containerSize = IntSize(
                containerWidthPx.roundToInt().coerceAtLeast(1),
                containerHeightPx.roundToInt().coerceAtLeast(1)
            )

            val bitmap by produceState<Bitmap?>(initialValue = null, url, containerSize) {
                val maxWidthPx = Int.MAX_VALUE
                value = RssImageLoader.loadBitmap(context, url, maxWidthPx)
            }
            val imageSize = bitmap?.let { IntSize(it.width, it.height) } ?: IntSize.Zero
            val baseSize = remember(containerSize, imageSize) {
                calculateBaseSize(containerSize, imageSize)
            }
    val maxScale = remember(containerSize, imageSize, baseSize) {
        calculateMaxScale(containerSize, imageSize, baseSize)
    }
    val minScale = 0.5f

            LaunchedEffect(containerSize, imageSize, minScale) {
                scale = max(1f, minScale)
                offset = Offset.Zero
                scaleAnimator.snapTo(scale)
                offsetXAnimator.snapTo(offset.x)
                offsetYAnimator.snapTo(offset.y)
            }
            LaunchedEffect(maxScale) {
                if (scale > maxScale) {
                    scale = maxScale
                    scaleAnimator.snapTo(scale)
                }
                offset = clampOffset(offset, scale, containerSize, baseSize)
                offsetXAnimator.snapTo(offset.x)
                offsetYAnimator.snapTo(offset.y)
            }

            if (bitmap == null) {
                CircularProgressIndicator(color = Color.White)
                return@BoxWithConstraints
            }

            val renderScale = sanitizeScale(scale, minScale, maxScale)
            if (renderScale != scale) {
                scale = renderScale
            }
            val renderOffset = sanitizeOffset(offset)
            if (renderOffset != offset) {
                offset = renderOffset
            }

            val imageModifier = Modifier
                .size(
                    with(density) { baseSize.width.toDp() },
                    with(density) { baseSize.height.toDp() }
                )
                .graphicsLayer(
                    translationX = renderOffset.x,
                    translationY = renderOffset.y,
                    scaleX = renderScale,
                    scaleY = renderScale,
                    transformOrigin = TransformOrigin.Center
                )
            val gestureModifier = Modifier
                .fillMaxSize()
                .pointerInput(url, containerSize, imageSize, maxScale) {
                    detectTapGestures(
                        onDoubleTap = { tap ->
                            val nextScale = nextDoubleTapScale(scale, maxScale)
                            scope.launch {
                                scaleAnimJob?.cancel()
                                offsetAnimJob?.cancel()
                                val center = Offset(
                                    containerSize.width / 2f,
                                    containerSize.height / 2f
                                )
                                val tapFromCenter = tap - center
                                val content = (tapFromCenter - offset) / scale
                                val targetOffset = tapFromCenter - content * nextScale
                                val clampedOffset = clampOffset(
                                    targetOffset,
                                    nextScale,
                                    containerSize,
                                    baseSize
                                )
                                scaleAnimJob = launch {
                                    scaleAnimator.snapTo(scale)
                                    scaleAnimator.animateTo(nextScale, springSpec) {
                                        scale = value
                                        offset = clampOffset(offset, scale, containerSize, baseSize)
                                    }
                                }
                                offsetAnimJob = launch {
                                    offsetXAnimator.snapTo(offset.x)
                                    offsetYAnimator.snapTo(offset.y)
                                    launch {
                                        offsetXAnimator.animateTo(clampedOffset.x, springOffsetSpec) {
                                            offset = clampOffset(
                                                Offset(value, offsetYAnimator.value),
                                                scale,
                                                containerSize,
                                                baseSize
                                            )
                                        }
                                    }
                                    launch {
                                        offsetYAnimator.animateTo(clampedOffset.y, springOffsetSpec) {
                                            offset = clampOffset(
                                                Offset(offsetXAnimator.value, value),
                                                scale,
                                                containerSize,
                                                baseSize
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
                .pointerInput(url, containerSize, imageSize, maxScale) {
                    if (containerSize.width <= 0 || containerSize.height <= 0 || imageSize.width <= 0) {
                        return@pointerInput
                    }
                    awaitEachGesture {
                        scaleAnimJob?.cancel()
                        offsetAnimJob?.cancel()
                        var lastScale = scale
                        var lastTime = 0L
                        var scaleVelocity = 0f
                        var panVelocity = Offset.Zero
                        var totalPanDistance = 0f
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Main)
                            if (event.changes.none { it.pressed }) {
                                break
                            }
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid()
                            val rawTime = event.changes.firstOrNull()?.uptimeMillis ?: 0L
                            val time = if (rawTime != 0L) rawTime else SystemClock.uptimeMillis()

                            if (!centroid.isFinite() || !panChange.isFinite() || !zoomChange.isFinite()) {
                                continue
                            }
                            val currentScale = sanitizeScale(scale, minScale, maxScale)
                            val newScale = (currentScale * zoomChange).coerceIn(minScale, maxScale)
                            val scaleChange = if (currentScale == 0f) 1f else newScale / currentScale
                            val center = Offset(
                                containerSize.width / 2f,
                                containerSize.height / 2f
                            )
                            val currentOffset = sanitizeOffset(offset)
                            val newOffset = currentOffset + panChange +
                                (centroid - center - currentOffset) * (1 - scaleChange)

                            scale = newScale
                            offset = clampOffset(newOffset, newScale, containerSize, baseSize)
                            if (lastTime != 0L) {
                                val dt = (time - lastTime).coerceAtLeast(1)
                                val scaleVelocityCandidate = ((newScale - lastScale) / dt) * 1000f
                                scaleVelocity = scaleVelocity * 0.25f + scaleVelocityCandidate * 0.75f
                                if (panChange.x != 0f || panChange.y != 0f) {
                                    totalPanDistance += panChange.getDistance()
                                    val panVelocityCandidate = panChange * (1000f / dt)
                                    panVelocity = panVelocity * 0.3f + panVelocityCandidate * 0.7f
                                }
                            }
                            lastScale = newScale
                            lastTime = time

                            event.changes.forEach { change ->
                                if (change.positionChanged()) {
                                    change.consume()
                                }
                            }
                            if (event.changes.all { !it.pressed }) break
                        }

                        if (abs(scaleVelocity) > 0.0001f) {
                            scaleAnimJob = scope.launch {
                                scaleAnimator.snapTo(sanitizeScale(scale, minScale, maxScale))
                                scaleAnimator.animateDecay(scaleVelocity * 4f, scaleDecay) {
                                    val clamped = sanitizeScale(value, minScale, maxScale)
                                    scale = clamped
                                    offset = clampOffset(offset, scale, containerSize, baseSize)
                                }
                            }
                        }

                        val panVelocityDistance = panVelocity.getDistance()
                        val minPanVelocity = 200f
                        val resolvedPanVelocity = if (
                            panVelocityDistance >= 0.01f && panVelocityDistance < minPanVelocity
                        ) {
                            panVelocity / panVelocityDistance * minPanVelocity
                        } else {
                            panVelocity
                        }
                        if (totalPanDistance > 2f && panVelocityDistance > 0.01f && resolvedPanVelocity.isFinite()) {
                            offsetAnimJob = scope.launch {
                                offsetXAnimator.snapTo(offset.x)
                                offsetYAnimator.snapTo(offset.y)
                                launch {
                                    offsetXAnimator.animateDecay(resolvedPanVelocity.x * 3f, panDecay) {
                                        offset = clampOffset(
                                            Offset(value, offsetYAnimator.value),
                                            scale,
                                            containerSize,
                                            baseSize
                                        )
                                    }
                                }
                                launch {
                                    offsetYAnimator.animateDecay(resolvedPanVelocity.y * 3f, panDecay) {
                                        offset = clampOffset(
                                            Offset(offsetXAnimator.value, value),
                                            scale,
                                            containerSize,
                                            baseSize
                                        )
                                    }
                                }
                            }
                        } else {
                            offset = clampOffset(offset, scale, containerSize, baseSize)
                        }
                    }
                }

            Box(
                modifier = gestureModifier,
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = alt,
                    contentScale = ContentScale.Fit,
                    modifier = imageModifier
                )
            }
        }
    }
}

private fun calculateBaseSize(container: IntSize, image: IntSize): Size {
    if (container.width <= 0 || container.height <= 0 || image.width <= 0 || image.height <= 0) {
        return Size.Zero
    }
    val containerRatio = container.width.toFloat() / container.height.toFloat()
    val imageRatio = image.width.toFloat() / image.height.toFloat()
    return if (imageRatio >= containerRatio) {
        val width = container.width.toFloat()
        Size(width, width / imageRatio)
    } else {
        val height = container.height.toFloat()
        Size(height * imageRatio, height)
    }
}

private fun calculateMaxScale(container: IntSize, image: IntSize, baseSize: Size): Float {
    if (baseSize.width <= 0f || baseSize.height <= 0f) return 1f
    val screenScale = max(
        container.width * 4f / baseSize.width,
        container.height * 4f / baseSize.height
    )
    val imageScale = max(
        image.width * 4f / baseSize.width,
        image.height * 4f / baseSize.height
    )
    return max(screenScale, imageScale).coerceAtLeast(1f)
}

private fun clampOffset(
    rawOffset: Offset,
    scale: Float,
    container: IntSize,
    baseSize: Size
): Offset {
    if (baseSize.width <= 0f || baseSize.height <= 0f) return Offset.Zero
    if (!rawOffset.isFinite()) return Offset.Zero
    val scaledWidth = baseSize.width * scale
    val scaledHeight = baseSize.height * scale
    val maxX = ((scaledWidth - container.width) / 2f).coerceAtLeast(0f)
    val maxY = ((scaledHeight - container.height) / 2f).coerceAtLeast(0f)
    return Offset(
        rawOffset.x.coerceIn(-maxX, maxX),
        rawOffset.y.coerceIn(-maxY, maxY)
    )
}

private fun nextDoubleTapScale(current: Float, maxScale: Float): Float {
    val first = minOf(2f, maxScale)
    val second = minOf(4f, maxScale)
    return when {
        current < first - 0.05f -> first
        current < second - 0.05f -> second
        else -> 1f
    }
}

private operator fun Offset.minus(other: Offset): Offset {
    return Offset(x - other.x, y - other.y)
}

private operator fun Offset.plus(other: Offset): Offset {
    return Offset(x + other.x, y + other.y)
}

private operator fun Offset.times(factor: Float): Offset {
    return Offset(x * factor, y * factor)
}

private operator fun Offset.div(factor: Float): Offset {
    return Offset(x / factor, y / factor)
}

private fun Offset.isFinite(): Boolean {
    return x.isFinite() && y.isFinite()
}

private fun sanitizeScale(value: Float, minScale: Float, maxScale: Float): Float {
    if (!value.isFinite()) return minScale
    if (value <= 0f) return minScale
    return value.coerceIn(minScale, maxScale)
}

private fun sanitizeOffset(value: Offset): Offset {
    return if (value.isFinite()) value else Offset.Zero
}
