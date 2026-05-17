package com.example.blesstify.presentation.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Spotify-inspired animation utilities for Blesstify app
 * Uses custom easing curves and timing for premium feel
 */
object BlesstifyAnimations {
    
    // ============ EASING CURVES ============
    
    /**
     * Spotify's signature easing curve: cubic-bezier(0.4, 0.0, 0.2, 1)
     * Creates natural, smooth motion
     */
    val SpotifyEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    /**
     * Emphasized easing for important transitions
     */
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    
    /**
     * Decelerate easing for exit animations
     */
    val DecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    
    // ============ DURATION CONSTANTS ============
    
    const val DURATION_SHORT = 250
    const val DURATION_MEDIUM = 300
    const val DURATION_LONG = 400
    const val DURATION_EXTRA_LONG = 600
    
    // ============ NAVIGATION TRANSITIONS ============
    // Redesigned for ZERO white space gaps - seamless overlapping transitions
    
    /**
     * Standard horizontal slide transition (forward navigation)
     * NEW: Full-width slide with NO fade to eliminate white space
     * Incoming screen immediately covers outgoing screen
     */
    fun slideInFromRight(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = SpotifyEasing
            )
        )
    }
    
    /**
     * Standard horizontal slide transition (exit on forward navigation)
     * NEW: Stays in place (no slide) with NO fade - just gets covered
     * This eliminates the parallax gap that causes white space
     */
    fun slideOutToLeft(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 4 },
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = SpotifyEasing
            )
        )
    }
    
    /**
     * Standard horizontal slide transition (back navigation)
     * NEW: Slides in from left with slight offset, NO fade
     */
    fun slideInFromLeft(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 4 },
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = SpotifyEasing
            )
        )
    }
    
    /**
     * Standard horizontal slide transition (exit on back navigation)
     * NEW: Full slide out to right, NO fade to prevent white gaps
     */
    fun slideOutToRight(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = SpotifyEasing
            )
        )
    }
    
    // ============ MODAL TRANSITIONS ============
    // Optimized for seamless full-screen modal experience (Player screen)
    
    /**
     * Modal slide up transition (like Spotify's player)
     * NEW: Slides up with scale, minimal fade to prevent white gaps
     */
    fun modalSlideUp(): EnterTransition {
        return slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(
                durationMillis = DURATION_LONG,
                easing = EmphasizedEasing
            )
        ) + scaleIn(
            initialScale = 0.96f,
            animationSpec = tween(
                durationMillis = DURATION_LONG,
                easing = SpotifyEasing
            )
        )
    }
    
    /**
     * Modal slide down transition (exit)
     * NEW: Slides down with scale, NO fade to prevent white background showing
     */
    fun modalSlideDown(): ExitTransition {
        return slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = DecelerateEasing
            )
        ) + scaleOut(
            targetScale = 0.96f,
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = SpotifyEasing
            )
        )
    }
    
    /**
     * Bottom sheet slide up (faster than modal)
     * NEW: Pure slide with no fade for instant appearance
     */
    fun bottomSheetSlideUp(): EnterTransition {
        return slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EmphasizedEasing
            )
        )
    }
    
    /**
     * Bottom sheet slide down (exit)
     * NEW: Pure slide with no fade for clean dismissal
     */
    fun bottomSheetSlideDown(): ExitTransition {
        return slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(
                durationMillis = DURATION_SHORT,
                easing = DecelerateEasing
            )
        )
    }
    
    // ============ SPECIAL TRANSITIONS ============
    
    /**
     * Splash screen fade with scale
     */
    fun splashFadeIn(): EnterTransition {
        return fadeIn(
            animationSpec = tween(
                durationMillis = DURATION_LONG,
                easing = SpotifyEasing
            )
        ) + scaleIn(
            initialScale = 0.9f,
            animationSpec = tween(
                durationMillis = DURATION_LONG,
                easing = SpotifyEasing
            )
        )
    }
    
    /**
     * Splash screen fade out
     */
    fun splashFadeOut(): ExitTransition {
        return fadeOut(
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = SpotifyEasing
            )
        )
    }
    
    // ============ MICRO-INTERACTION SPECS ============
    
    /**
     * Spring animation for button presses
     */
    fun buttonPressSpring(): SpringSpec<Float> {
        return spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }
    
    /**
     * Tween for card hover/press
     */
    fun cardInteractionTween(): TweenSpec<Float> {
        return tween(
            durationMillis = 150,
            easing = SpotifyEasing
        )
    }
    
    /**
     * Spring for smooth scale animations
     */
    fun smoothScaleSpring(): SpringSpec<Float> {
        return spring(
            dampingRatio = 0.8f,
            stiffness = 500f
        )
    }
    
    // ============ LIST ANIMATIONS ============
    
    /**
     * Staggered delay for list items (30ms per item)
     */
    fun listItemDelay(index: Int): Int {
        return index * 30
    }
    
    /**
     * List item slide in animation
     */
    fun listItemSlideIn(index: Int): EnterTransition {
        return slideInVertically(
            initialOffsetY = { 50 },
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                delayMillis = listItemDelay(index),
                easing = SpotifyEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                delayMillis = listItemDelay(index),
                easing = SpotifyEasing
            )
        )
    }
}

// ============ INTERACTIVE MODIFIERS ============

/**
 * Press scale modifier for interactive elements
 * Scales to 0.95f on press with spring animation
 */
fun Modifier.pressScale() = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                }
            )
        }
}

/**
 * Card hover effect modifier
 * Scales to 1.02f on press with elevation lift
 */
fun Modifier.cardHoverEffect() = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.02f else 1f,
        animationSpec = tween(
            durationMillis = 150,
            easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
        ),
        label = "cardScale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 4.dp,
        animationSpec = tween(
            durationMillis = 150,
            easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
        ),
        label = "cardElevation"
    )
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            shadowElevation = elevation.toPx()
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                }
            )
        }
}

// ============ STAGGERED LIST ANIMATIONS ============

/**
 * Staggered column wrapper for list entry animations
 * Each child slides up 24dp + fades in with 30ms delay per index
 */
@Composable
fun StaggeredColumn(
    modifier: Modifier = Modifier,
    staggerMs: Int = 30,
    content: @Composable ColumnScope.() -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Column(modifier = modifier) {
        content()
    }
}

/**
 * Staggered item wrapper for individual list items
 * Use inside StaggeredColumn or standalone
 */
@Composable
fun StaggeredItem(
    index: Int,
    staggerMs: Int = 30,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val offsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 24.dp,
        animationSpec = tween(
            durationMillis = 350,
            delayMillis = index * staggerMs,
            easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
        ),
        label = "staggerOffsetY"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 350,
            delayMillis = index * staggerMs,
            easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
        ),
        label = "staggerAlpha"
    )
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Box(
        modifier = Modifier
            .offset(y = offsetY)
            .graphicsLayer { this.alpha = alpha }
    ) {
        content()
    }
}

// ============ SHIMMER EFFECT ============

/**
 * Shimmer effect modifier for loading skeletons
 * Animated gradient sweep that loops continuously
 */
fun Modifier.shimmerEffect() = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    
    this.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                start = Offset(translateAnim - 500f, 0f),
                end = Offset(translateAnim, size.height)
            )
        )
    }
}
