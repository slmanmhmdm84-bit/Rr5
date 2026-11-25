package com.example.ui.canvas

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.model.AllyStrikerState
import com.example.model.BossType
import com.example.model.FighterAction
import com.example.model.FighterProfile
import com.example.model.FighterState
import com.example.model.FighterStyle
import com.example.model.FloatingCallout
import com.example.model.HitParticle
import com.example.model.ImpactRing
import com.example.model.Projectile
import com.example.ui.theme.CrimsonFlame
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SolarGold
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private val InkOutline = Color(0xFF0A0912)

/** Pre-allocated Path pool to avoid per-frame GC allocations inside Canvas draw loops */
class FighterRenderScratch {
    val limbPath = Path()
    val capePath = Path()
    val hairPath = Path()
    val torsoPath = Path()
    val gearPath = Path()
    val fxPath = Path()
}

private fun worldX(wx: Float, widthPx: Float): Float = wx * 0.001f * widthPx

fun DrawScope.drawArenaFloorEffects(
    floorY: Float,
    accentColor: Color,
    floorColor: Color
) {
    val floorHeight = (size.height - floorY).coerceAtLeast(0f)
    drawRect(
        color = Color(0xFF06050A),
        topLeft = Offset(0f, floorY),
        size = Size(size.width, floorHeight)
    )
    drawRect(
        color = floorColor.copy(alpha = 0.72f),
        topLeft = Offset(0f, floorY),
        size = Size(size.width, floorHeight * 0.58f)
    )
    drawRect(
        color = accentColor.copy(alpha = 0.08f),
        topLeft = Offset(0f, floorY),
        size = Size(size.width, floorHeight * 0.18f)
    )

    // Stage floor horizon rim & soft under-glow
    drawLine(
        color = accentColor.copy(alpha = 0.28f),
        start = Offset(0f, floorY + 5f),
        end = Offset(size.width, floorY + 5f),
        strokeWidth = 10f
    )
    drawLine(
        color = accentColor.copy(alpha = 0.94f),
        start = Offset(0f, floorY),
        end = Offset(size.width, floorY),
        strokeWidth = 3.8f
    )
    drawLine(
        color = Color.White.copy(alpha = 0.35f),
        start = Offset(0f, floorY - 1f),
        end = Offset(size.width, floorY - 1f),
        strokeWidth = 1.2f
    )

    // Perspective arena grid lines + center stage combat ring
    val centerX = size.width * 0.5f
    val gridColor = accentColor.copy(alpha = 0.20f)
    for (i in -5..5) {
        val topX = centerX + i * (size.width * 0.095f)
        val botX = centerX + i * (size.width * 0.195f)
        drawLine(
            color = gridColor,
            start = Offset(topX, floorY),
            end = Offset(botX, size.height),
            strokeWidth = 1.6f
        )
    }
    val subLineY1 = floorY + floorHeight * 0.28f
    val subLineY2 = floorY + floorHeight * 0.62f
    drawLine(
        color = accentColor.copy(alpha = 0.14f),
        start = Offset(0f, subLineY1),
        end = Offset(size.width, subLineY1),
        strokeWidth = 1.4f
    )
    drawLine(
        color = accentColor.copy(alpha = 0.08f),
        start = Offset(0f, subLineY2),
        end = Offset(size.width, subLineY2),
        strokeWidth = 1.2f
    )
    drawOval(
        color = accentColor.copy(alpha = 0.22f),
        topLeft = Offset(centerX - size.width * 0.16f, floorY + floorHeight * 0.09f),
        size = Size(size.width * 0.32f, (floorHeight * 0.34f).coerceAtLeast(10f)),
        style = Stroke(width = 2f)
    )
}

fun DrawScope.drawFighter(
    fighter: FighterState,
    widthPx: Float,
    floorY: Float,
    scale: Float,
    scratch: FighterRenderScratch
) {
    val screenX = worldX(fighter.x, widthPx)
    val screenY = floorY - fighter.y * scale
    val shadowScale = (1f - (fighter.y / 380f)).coerceIn(0.35f, 1f)

    // Floor aura light pool & crisp contact shadow
    val hasAura = fighter.energy >= 50f || fighter.bossType != BossType.NONE
    if (hasAura && fighter.action != FighterAction.KNOCKDOWN) {
        val poolColor = if (fighter.bossPhase >= 2) CrimsonFlame else fighter.profile.auraColor
        drawOval(
            color = poolColor.copy(alpha = 0.18f * shadowScale),
            topLeft = Offset(screenX - 66f * scale * shadowScale, floorY - 11f * scale),
            size = Size(132f * scale * shadowScale, 25f * scale)
        )
    }
    drawOval(
        color = Color.Black.copy(alpha = 0.52f * shadowScale),
        topLeft = Offset(screenX - 46f * scale * shadowScale, floorY - 8f * scale),
        size = Size(92f * scale * shadowScale, 18f * scale)
    )

    val ghosts = fighter.afterimages
    for (i in ghosts.indices) {
        val ghost = ghosts[i]
        val gx = worldX(ghost.x, widthPx)
        val gy = floorY - ghost.y * scale
        drawFighterBody(
            profile = fighter.profile,
            action = ghost.action,
            actionProgress = 0.5f,
            animPhase = fighter.animPhase,
            facingRight = ghost.facingRight,
            vx = 0f,
            vy = 0f,
            attackVariant = fighter.attackVariant,
            bossType = fighter.bossType,
            bossPhase = fighter.bossPhase,
            hasArmor = false,
            cx = gx,
            footY = gy,
            scale = scale,
            alpha = ghost.alpha * 0.42f,
            isFlashing = false,
            hasSuperAura = false,
            scratch = scratch
        )
    }

    val progress = if (fighter.actionDuration > 0f) {
        (fighter.actionTimer / fighter.actionDuration).coerceIn(0f, 1f)
    } else 0f

    val teleportFade = if (fighter.action == FighterAction.BOSS_TELEPORT && progress < 0.4f) {
        (1f - progress * 2.2f).coerceAtLeast(0.15f)
    } else 1f

    drawFighterBody(
        profile = fighter.profile,
        action = fighter.action,
        actionProgress = progress,
        animPhase = fighter.animPhase,
        facingRight = fighter.facingRight,
        vx = fighter.vx,
        vy = fighter.vy,
        attackVariant = fighter.attackVariant,
        bossType = fighter.bossType,
        bossPhase = fighter.bossPhase,
        hasArmor = fighter.superArmorTimer > 0f,
        cx = screenX,
        footY = screenY,
        scale = scale,
        alpha = teleportFade,
        isFlashing = fighter.flashTimer > 0f,
        hasSuperAura = hasAura,
        scratch = scratch
    )
}

private fun snapCurve(t: Float): Float {
    val clamped = t.coerceIn(0f, 1f)
    return if (clamped < 0.30f) {
        val p = clamped / 0.30f
        1f - (1f - p).pow(3f)
    } else {
        val p = (clamped - 0.30f) / 0.70f
        1f - p * p * (3f - 2f * p)
    }
}

private fun DrawScope.drawGlowCircle(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float = 1f
) {
    drawCircle(
        color = color.copy(alpha = 0.24f * alpha),
        radius = radius,
        center = center
    )
    drawCircle(
        color = color.copy(alpha = 0.54f * alpha),
        radius = radius * 0.62f,
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.90f * alpha),
        radius = radius * 0.30f,
        center = center
    )
}

private fun DrawScope.drawFighterBody(
    profile: FighterProfile,
    action: FighterAction,
    actionProgress: Float,
    animPhase: Float,
    facingRight: Boolean,
    vx: Float,
    vy: Float,
    attackVariant: Int,
    bossType: BossType,
    bossPhase: Int,
    hasArmor: Boolean,
    cx: Float,
    footY: Float,
    scale: Float,
    alpha: Float,
    isFlashing: Boolean,
    hasSuperAura: Boolean,
    scratch: FighterRenderScratch
) {
    val dir = if (facingRight) 1f else -1f
    val isTitan = profile.style == FighterStyle.HEAVY_TITAN
    val isBoss = bossType != BossType.NONE
    val bossScale = if (isBoss) 1.14f else 1.0f
    val bodyW = (if (isTitan) 1.24f else 1.0f) * bossScale * scale
    val bodyH = (if (isTitan) 1.06f else 1.0f) * bossScale * scale

    val primary = if (isFlashing) Color.White else profile.primaryColor.copy(alpha = alpha)
    val secondary = if (isFlashing) Color(0xFFFFD6DF) else profile.secondaryColor.copy(alpha = alpha)
    val skin = if (isFlashing) Color.White else profile.skinColor.copy(alpha = alpha)
    val aura = if (bossPhase >= 2) CrimsonFlame.copy(alpha = alpha) else profile.auraColor.copy(alpha = alpha)
    val outline = InkOutline.copy(alpha = alpha * 0.92f)

    if (hasSuperAura && alpha > 0.7f && action != FighterAction.KNOCKDOWN) {
        val pulse = 0.86f + 0.16f * sin(animPhase * (if (bossPhase >= 2) 3.6f else 2.2f))
        val auraW = 152f * bodyW * pulse
        val auraH = 178f * bodyH * pulse
        drawOval(
            color = aura.copy(alpha = if (isBoss) 0.24f else 0.15f),
            topLeft = Offset(cx - auraW * 0.5f, footY - 84f * bodyH - auraH * 0.5f),
            size = Size(auraW, auraH)
        )
        drawOval(
            color = aura.copy(alpha = if (isBoss) 0.30f else 0.20f),
            topLeft = Offset(cx - auraW * 0.34f, footY - 84f * bodyH - auraH * 0.36f),
            size = Size(auraW * 0.68f, auraH * 0.72f)
        )
        if (bossPhase >= 2) {
            // Flame spikes for Boss Phase II Enraged
            val flameLift = abs(sin(animPhase * 3.1f)) * 18f * scale
            scratch.fxPath.reset()
            scratch.fxPath.apply {
                moveTo(cx - 46f * bodyW, footY - 24f * bodyH)
                lineTo(cx - 56f * bodyW, footY - 102f * bodyH - flameLift)
                lineTo(cx - 26f * bodyW, footY - 76f * bodyH)
                lineTo(cx, footY - 178f * bodyH - flameLift * 1.2f)
                lineTo(cx + 26f * bodyW, footY - 76f * bodyH)
                lineTo(cx + 56f * bodyW, footY - 102f * bodyH - flameLift)
                lineTo(cx + 46f * bodyW, footY - 24f * bodyH)
                close()
            }
            drawPath(scratch.fxPath, color = aura.copy(alpha = 0.22f))
        }
    }

    if (hasArmor && alpha > 0.7f) {
        drawOval(
            color = SolarGold.copy(alpha = 0.75f),
            topLeft = Offset(cx - 58f * bodyW, footY - 164f * bodyH),
            size = Size(116f * bodyW, 170f * bodyH),
            style = Stroke(width = 4.5f * scale)
        )
    }

    if (action == FighterAction.KNOCKDOWN) {
        withTransform({
            translate(cx, footY - 18f * scale)
            scale(scaleX = dir, scaleY = 1f, pivot = Offset.Zero)
        }) {
            drawRoundRect(
                color = outline,
                topLeft = Offset(-75f * bodyW, -19f * bodyH),
                size = Size(91f * bodyW, 30f * bodyH),
                cornerRadius = CornerRadius(12f * scale)
            )
            drawRoundRect(
                color = secondary,
                topLeft = Offset(-72f * bodyW, -16f * bodyH),
                size = Size(85f * bodyW, 24f * bodyH),
                cornerRadius = CornerRadius(10f * scale)
            )
            drawRoundRect(
                color = outline,
                topLeft = Offset(-23f * bodyW, -25f * bodyH),
                size = Size(61f * bodyW, 34f * bodyH),
                cornerRadius = CornerRadius(12f * scale)
            )
            drawRoundRect(
                color = primary,
                topLeft = Offset(-20f * bodyW, -22f * bodyH),
                size = Size(55f * bodyW, 28f * bodyH),
                cornerRadius = CornerRadius(10f * scale)
            )
            drawCircle(color = outline, radius = 18.5f * scale, center = Offset(48f * bodyW, -14f * bodyH))
            drawCircle(color = skin, radius = 16f * scale, center = Offset(48f * bodyW, -14f * bodyH))
        }
        return
    }

    val breathe = sin(animPhase) * 3.0f
    val isWalking = action == FighterAction.WALK_FORWARD || action == FighterAction.WALK_BACKWARD
    val walkWave = if (isWalking) sin(animPhase * 2.6f) else 0f
    val walkLift = if (isWalking) abs(sin(animPhase * 2.6f)) * 7f else 0f

    val snap = snapCurve(actionProgress)
    val torsoLean = when (action) {
        FighterAction.DASH, FighterAction.BOSS_SLASH -> 24f
        FighterAction.HITSTUN -> -22f * snap
        FighterAction.HEAVY_KICK -> -20f * snap
        FighterAction.LIGHT_PUNCH -> (if (attackVariant == 0) 13f else 17f) * snap
        FighterAction.SPECIAL_ATTACK, FighterAction.BOSS_BARRAGE -> 18f * snap
        FighterAction.BOSS_SLAM -> 26f * snap
        FighterAction.GUARD -> -8f
        FighterAction.JUMP -> (vy * 0.015f).coerceIn(-12f, 12f)
        else -> if (action == FighterAction.WALK_FORWARD) 9f else if (action == FighterAction.WALK_BACKWARD) -5f else 0f
    }

    val hipY = (-66f + walkLift * 0.25f) * bodyH + breathe * 0.4f * scale
    val shoulderY = (-116f + walkLift * 0.25f) * bodyH + breathe * scale
    val headY = (-142f + walkLift * 0.25f) * bodyH + breathe * scale
    val windDrag = (-vx * dir * 0.035f).coerceIn(-18f, 18f) * scale

    withTransform({
        translate(cx, footY)
        scale(scaleX = dir, scaleY = 1f, pivot = Offset.Zero)
    }) {
        // Back leg
        val backFootX = when (action) {
            FighterAction.HEAVY_KICK -> -22f * bodyW
            FighterAction.JUMP -> -18f * bodyW
            FighterAction.DASH -> -36f * bodyW
            else -> (-22f - walkWave * 21f) * bodyW
        }
        val backFootY = when (action) {
            FighterAction.JUMP -> -26f * bodyH
            else -> (-maxOf(0f, -walkWave) * 14f) * bodyH
        }
        drawLimb(
            start = Offset(-8f * bodyW, hipY),
            mid = Offset(backFootX * 0.42f - 4f * bodyW, (hipY + backFootY) * 0.48f),
            end = Offset(backFootX, backFootY),
            thickness = 15f * bodyW,
            color = secondary,
            bootColor = primary,
            accentColor = aura,
            outlineColor = outline,
            alpha = alpha,
            limbPath = scratch.limbPath
        )

        // Front leg or Heavy Kick slash
        when (action) {
            FighterAction.HEAVY_KICK -> {
                val kickExtend = snap.coerceIn(0.2f, 1f)
                val targetY = if (attackVariant == 0) -96f else -122f
                val kickTip = Offset(102f * bodyW * kickExtend, targetY * bodyH * kickExtend - 22f * bodyH)
                drawLimb(
                    start = Offset(10f * bodyW, hipY),
                    mid = Offset(48f * bodyW * kickExtend, hipY - 18f * bodyH),
                    end = kickTip,
                    thickness = 16.5f * bodyW,
                    color = secondary,
                    bootColor = primary,
                    accentColor = aura,
                    outlineColor = outline,
                    alpha = alpha,
                    limbPath = scratch.limbPath
                )
                if (actionProgress in 0.12f..0.80f) {
                    // Tapered crescent kick slash
                    scratch.fxPath.reset()
                    scratch.fxPath.apply {
                        moveTo(22f * bodyW, -148f * bodyH)
                        quadraticTo(132f * bodyW, -112f * bodyH, 92f * bodyW, -28f * bodyH)
                        quadraticTo(106f * bodyW, -102f * bodyH, 16f * bodyW, -132f * bodyH)
                        close()
                    }
                    drawPath(scratch.fxPath, color = aura.copy(alpha = 0.72f * alpha))
                    drawArc(
                        color = Color.White.copy(alpha = 0.90f * alpha),
                        startAngle = -48f,
                        sweepAngle = 72f,
                        useCenter = false,
                        topLeft = Offset(-10f * bodyW, -152f * bodyH),
                        size = Size(134f * bodyW, 126f * bodyH),
                        style = Stroke(width = 4.5f * scale, cap = StrokeCap.Round)
                    )
                }
            }
            else -> {
                val frontFootX = when (action) {
                    FighterAction.JUMP -> 20f * bodyW
                    FighterAction.DASH -> 26f * bodyW
                    else -> (22f + walkWave * 21f) * bodyW
                }
                val frontFootY = when (action) {
                    FighterAction.JUMP -> -34f * bodyH
                    else -> (-maxOf(0f, walkWave) * 14f) * bodyH
                }
                drawLimb(
                    start = Offset(10f * bodyW, hipY),
                    mid = Offset(frontFootX * 0.56f + 8f * bodyW, (hipY + frontFootY) * 0.46f),
                    end = Offset(frontFootX, frontFootY),
                    thickness = 15f * bodyW,
                    color = secondary,
                    bootColor = primary,
                    accentColor = aura,
                    outlineColor = outline,
                    alpha = alpha,
                    limbPath = scratch.limbPath
                )
            }
        }

        // Flowing Assassin Scarf / Cape behind torso
        if (profile.style == FighterStyle.SHADOW_ASSASSIN || isBoss) {
            val flutter = sin(animPhase * 1.9f) * 11f * scale
            val capeColor = if (profile.style == FighterStyle.SHADOW_ASSASSIN) secondary else primary
            scratch.capePath.reset()
            scratch.capePath.apply {
                moveTo(-8f * bodyW, shoulderY)
                quadraticTo(
                    -52f * bodyW + windDrag,
                    shoulderY + 34f * bodyH + flutter,
                    -64f * bodyW + windDrag * 1.35f,
                    hipY + 38f * bodyH + flutter
                )
                lineTo(-36f * bodyW + windDrag * 0.7f, hipY + 26f * bodyH + flutter * 0.5f)
                lineTo(-20f * bodyW, hipY + 14f * bodyH)
                close()
            }
            drawPath(scratch.capePath, color = outline, style = Stroke(width = 4f * scale, join = StrokeJoin.Round))
            drawPath(scratch.capePath, color = capeColor.copy(alpha = alpha * 0.92f))
        }

        withTransform({
            rotate(degrees = torsoLean, pivot = Offset(0f, hipY))
        }) {
            // Back arm
            val backHand = when (action) {
                FighterAction.LIGHT_PUNCH -> if (attackVariant == 1) Offset((24f + 56f * snap) * bodyW, shoulderY + 2f * bodyH) else Offset(-14f * bodyW, shoulderY + 18f * bodyH)
                FighterAction.SPECIAL_ATTACK -> Offset(48f * bodyW * snap, shoulderY + 4f * bodyH)
                FighterAction.VICTORY -> Offset(-22f * bodyW, shoulderY - 38f * bodyH)
                else -> Offset((-18f + walkWave * 12f) * bodyW, shoulderY + 24f * bodyH)
            }
            drawLimb(
                start = Offset(-10f * bodyW, shoulderY + 4f * bodyH),
                mid = Offset(-20f * bodyW, shoulderY + 18f * bodyH),
                end = backHand,
                thickness = 12f * bodyW,
                color = skin,
                bootColor = primary,
                accentColor = aura,
                outlineColor = outline,
                alpha = alpha,
                limbPath = scratch.limbPath
            )

            // Athletic V-taper cel-shaded torso
            val shoulderHalf = (if (isTitan) 28f else 24f) * bodyW
            val waistHalf = (if (isTitan) 20f else 16f) * bodyW
            scratch.torsoPath.reset()
            scratch.torsoPath.apply {
                moveTo(-shoulderHalf, shoulderY - 4f * bodyH)
                lineTo(shoulderHalf, shoulderY - 4f * bodyH)
                lineTo(waistHalf, hipY + 5f * bodyH)
                lineTo(-waistHalf, hipY + 5f * bodyH)
                close()
            }
            drawPath(scratch.torsoPath, color = outline, style = Stroke(width = 5f * scale, join = StrokeJoin.Round))
            drawPath(scratch.torsoPath, color = primary)

            // Cel-shaded shadow lower-back half
            scratch.gearPath.reset()
            scratch.gearPath.apply {
                moveTo(-shoulderHalf * 0.9f, shoulderY + 16f * bodyH)
                lineTo(waistHalf * 0.4f, shoulderY + 26f * bodyH)
                lineTo(waistHalf, hipY + 5f * bodyH)
                lineTo(-waistHalf, hipY + 5f * bodyH)
                close()
            }
            drawPath(scratch.gearPath, color = secondary.copy(alpha = 0.56f * alpha))

            // Style-specific chest armor / gi lapel / sash
            when (profile.style) {
                FighterStyle.MARTIAL_ARTIST -> {
                    scratch.gearPath.reset()
                    scratch.gearPath.apply {
                        moveTo(-8f * bodyW, shoulderY - 4f * bodyH)
                        lineTo(2f * bodyW, shoulderY + 24f * bodyH)
                        lineTo(13f * bodyW, shoulderY - 4f * bodyH)
                        close()
                    }
                    drawPath(scratch.gearPath, color = skin)
                    // Hanging martial arts belt tail
                    val sashWave = sin(animPhase * 2.4f) * 5f * scale
                    drawLine(
                        color = Color(0xFF15121E).copy(alpha = alpha),
                        start = Offset(4f * bodyW, hipY - 2f * bodyH),
                        end = Offset(-8f * bodyW + windDrag * 0.4f, hipY + 20f * bodyH + sashWave),
                        strokeWidth = 6f * scale,
                        cap = StrokeCap.Round
                    )
                }
                FighterStyle.CYBER_RONIN -> {
                    // Glowing cyan cyber chest core diamond
                    scratch.gearPath.reset()
                    scratch.gearPath.apply {
                        moveTo(5f * bodyW, shoulderY + 8f * bodyH)
                        lineTo(13f * bodyW, shoulderY + 18f * bodyH)
                        lineTo(5f * bodyW, shoulderY + 28f * bodyH)
                        lineTo(-3f * bodyW, shoulderY + 18f * bodyH)
                        close()
                    }
                    drawPath(scratch.gearPath, color = aura)
                    drawCircle(color = Color.White.copy(alpha = alpha), radius = 3.2f * scale, center = Offset(5f * bodyW, shoulderY + 18f * bodyH))
                }
                FighterStyle.HEAVY_TITAN -> {
                    // Armored X-strap & shoulder pauldrons
                    drawLine(
                        color = secondary,
                        start = Offset(-shoulderHalf + 4f * bodyW, shoulderY),
                        end = Offset(waistHalf - 2f * bodyW, hipY - 4f * bodyH),
                        strokeWidth = 7f * scale,
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = outline, radius = 14f * scale, center = Offset(17f * bodyW, shoulderY + 2f * bodyH))
                    drawCircle(color = secondary, radius = 12f * scale, center = Offset(17f * bodyW, shoulderY + 2f * bodyH))
                    drawCircle(color = aura, radius = 4.5f * scale, center = Offset(17f * bodyW, shoulderY + 2f * bodyH))
                    drawCircle(color = outline, radius = 13f * scale, center = Offset(-17f * bodyW, shoulderY + 2f * bodyH))
                    drawCircle(color = secondary, radius = 11f * scale, center = Offset(-17f * bodyW, shoulderY + 2f * bodyH))
                }
                FighterStyle.SHADOW_ASSASSIN -> {
                    // Crossed stealth straps + glowing emblem
                    drawLine(
                        color = aura.copy(alpha = 0.85f * alpha),
                        start = Offset(-14f * bodyW, shoulderY + 2f * bodyH),
                        end = Offset(14f * bodyW, hipY - 6f * bodyH),
                        strokeWidth = 3.2f * scale,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Belt
            drawRoundRect(
                color = outline,
                topLeft = Offset(-(waistHalf + 3f * bodyW), hipY - 9f * bodyH),
                size = Size((waistHalf * 2f + 6f * bodyW), 12f * bodyH),
                cornerRadius = CornerRadius(4f * scale)
            )
            drawRoundRect(
                color = if (isTitan) aura else secondary,
                topLeft = Offset(-(waistHalf + 1.5f * bodyW), hipY - 7.5f * bodyH),
                size = Size((waistHalf * 2f + 3f * bodyW), 9f * bodyH),
                cornerRadius = CornerRadius(3.5f * scale)
            )

            // Head with cel outline + fierce expression
            drawCircle(
                color = outline,
                radius = 19.5f * scale,
                center = Offset(2f * bodyW, headY)
            )
            drawCircle(
                color = skin,
                radius = 17f * scale,
                center = Offset(2f * bodyW, headY)
            )

            when (profile.style) {
                FighterStyle.MARTIAL_ARTIST -> {
                    scratch.hairPath.reset()
                    scratch.hairPath.apply {
                        moveTo(-16f * scale, headY - 5f * scale)
                        lineTo(-14f * scale, headY - 25f * scale)
                        lineTo(-3f * scale, headY - 20f * scale)
                        lineTo(3f * scale, headY - 29f * scale)
                        lineTo(10f * scale, headY - 22f * scale)
                        lineTo(18f * scale, headY - 26f * scale)
                        lineTo(19f * scale, headY - 9f * scale)
                        close()
                    }
                    drawPath(scratch.hairPath, color = outline, style = Stroke(width = 3f * scale, join = StrokeJoin.Round))
                    drawPath(scratch.hairPath, color = Color(0xFF1A1625).copy(alpha = alpha))
                    // Fierce eye + angled brow
                    drawLine(
                        color = outline,
                        start = Offset(7f * scale, headY - 4.5f * scale),
                        end = Offset(15f * scale, headY - 2.5f * scale),
                        strokeWidth = 2.8f * scale,
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = Color.White.copy(alpha = alpha), radius = 2.5f * scale, center = Offset(11.5f * scale, headY - 0.5f * scale))
                    // Headband + trailing tails
                    drawRect(
                        color = primary,
                        topLeft = Offset(-16f * scale, headY - 12f * scale),
                        size = Size(35f * scale, 6f * scale)
                    )
                    val tailWave = sin(animPhase * 2.6f) * 7f * scale
                    drawLine(
                        color = primary,
                        start = Offset(-16f * scale, headY - 9f * scale),
                        end = Offset(-39f * scale + windDrag, headY - 16f * scale + tailWave),
                        strokeWidth = 5.5f * scale,
                        cap = StrokeCap.Round
                    )
                }
                FighterStyle.CYBER_RONIN -> {
                    drawArc(
                        color = outline,
                        startAngle = 170f,
                        sweepAngle = 205f,
                        useCenter = true,
                        topLeft = Offset(-19f * scale, headY - 21f * scale),
                        size = Size(42f * scale, 38f * scale)
                    )
                    drawArc(
                        color = Color(0xFF1B2438).copy(alpha = alpha),
                        startAngle = 175f,
                        sweepAngle = 195f,
                        useCenter = true,
                        topLeft = Offset(-17f * scale, headY - 19f * scale),
                        size = Size(38f * scale, 34f * scale)
                    )
                    // Cyber mask lower jaw + glowing neon visor
                    drawRoundRect(
                        color = secondary,
                        topLeft = Offset(2f * scale, headY + 2f * scale),
                        size = Size(16f * scale, 12f * scale),
                        cornerRadius = CornerRadius(4f * scale)
                    )
                    drawLine(
                        color = aura,
                        start = Offset(4f * scale, headY - 3f * scale),
                        end = Offset(19f * scale, headY - 5f * scale),
                        strokeWidth = 5.5f * scale,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White.copy(alpha = alpha),
                        start = Offset(7f * scale, headY - 3.3f * scale),
                        end = Offset(16f * scale, headY - 4.7f * scale),
                        strokeWidth = 2f * scale,
                        cap = StrokeCap.Round
                    )
                }
                FighterStyle.HEAVY_TITAN -> {
                    drawRoundRect(
                        color = outline,
                        topLeft = Offset(-20f * scale, headY - 21f * scale),
                        size = Size(42f * scale, 16f * scale),
                        cornerRadius = CornerRadius(6f * scale)
                    )
                    drawRoundRect(
                        color = secondary,
                        topLeft = Offset(-18f * scale, headY - 19f * scale),
                        size = Size(38f * scale, 13f * scale),
                        cornerRadius = CornerRadius(5f * scale)
                    )
                    // Heavy brow & glowing titan eyes
                    drawLine(
                        color = outline,
                        start = Offset(6f * scale, headY - 4f * scale),
                        end = Offset(16f * scale, headY - 2f * scale),
                        strokeWidth = 3.4f * scale,
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = aura, radius = 2.8f * scale, center = Offset(11.5f * scale, headY))
                }
                FighterStyle.SHADOW_ASSASSIN -> {
                    drawArc(
                        color = outline,
                        startAngle = 148f,
                        sweepAngle = 228f,
                        useCenter = true,
                        topLeft = Offset(-20f * scale, headY - 23f * scale),
                        size = Size(43f * scale, 42f * scale)
                    )
                    drawArc(
                        color = secondary,
                        startAngle = 150f,
                        sweepAngle = 225f,
                        useCenter = true,
                        topLeft = Offset(-18f * scale, headY - 21f * scale),
                        size = Size(39f * scale, 38f * scale)
                    )
                    // Stealth lower face mask
                    drawRoundRect(
                        color = Color(0xFF160E26).copy(alpha = alpha),
                        topLeft = Offset(-1f * scale, headY + 2f * scale),
                        size = Size(19f * scale, 13f * scale),
                        cornerRadius = CornerRadius(5f * scale)
                    )
                    drawCircle(color = aura, radius = 3.8f * scale, center = Offset(11.5f * scale, headY - 2f * scale))
                    drawCircle(color = Color.White.copy(alpha = alpha), radius = 1.6f * scale, center = Offset(12f * scale, headY - 2f * scale))
                }
            }

            // Front arm + weapons + attack impact flares
            when (action) {
                FighterAction.LIGHT_PUNCH -> {
                    val punchReach = snap.coerceIn(0.22f, 1f)
                    val fistY = if (attackVariant == 0) shoulderY + 6f * bodyH else shoulderY - 8f * bodyH
                    val fist = Offset((28f + 68f * punchReach) * bodyW, fistY)
                    drawLimb(
                        start = Offset(8f * bodyW, shoulderY + 6f * bodyH),
                        mid = Offset((18f + 28f * punchReach) * bodyW, shoulderY + 8f * bodyH),
                        end = fist,
                        thickness = (if (isTitan) 16f else 13f) * bodyW,
                        color = skin,
                        bootColor = primary,
                        accentColor = aura,
                        outlineColor = outline,
                        alpha = alpha,
                        limbPath = scratch.limbPath
                    )
                    if (actionProgress in 0.12f..0.74f) {
                        // Tapered punch motion cone
                        scratch.fxPath.reset()
                        scratch.fxPath.apply {
                            moveTo(14f * bodyW, fistY - 9f * scale)
                            lineTo(fist.x + 12f * scale, fistY)
                            lineTo(14f * bodyW, fistY + 9f * scale)
                            close()
                        }
                        drawPath(scratch.fxPath, color = aura.copy(alpha = 0.50f * alpha))
                        drawLine(
                            color = Color.White.copy(alpha = 0.85f * alpha),
                            start = Offset(22f * bodyW, fistY),
                            end = fist,
                            strokeWidth = 4.5f * scale,
                            cap = StrokeCap.Round
                        )
                        drawGlowCircle(center = fist, radius = 26f * scale, color = aura, alpha = alpha)
                    }
                }
                FighterAction.SPECIAL_ATTACK, FighterAction.BOSS_BARRAGE -> {
                    val thrust = Offset((22f + 40f * snap) * bodyW, shoulderY + 6f * bodyH)
                    drawLimb(
                        start = Offset(8f * bodyW, shoulderY + 6f * bodyH),
                        mid = Offset((18f + 16f * snap) * bodyW, shoulderY + 12f * bodyH),
                        end = thrust,
                        thickness = 14f * bodyW,
                        color = skin,
                        bootColor = aura,
                        accentColor = Color.White,
                        outlineColor = outline,
                        alpha = alpha,
                        limbPath = scratch.limbPath
                    )
                    drawGlowCircle(
                        center = thrust,
                        radius = 36f * scale * (0.6f + 0.4f * sin(actionProgress * PI.toFloat())),
                        color = aura,
                        alpha = alpha
                    )
                }
                FighterAction.BOSS_SLAM -> {
                    val slamHand = Offset(34f * bodyW, shoulderY - 38f * bodyH + 74f * bodyH * actionProgress)
                    drawLimb(
                        start = Offset(8f * bodyW, shoulderY + 4f * bodyH),
                        mid = Offset(24f * bodyW, shoulderY - 12f * bodyH),
                        end = slamHand,
                        thickness = 18f * bodyW,
                        color = skin,
                        bootColor = SolarGold,
                        accentColor = Color.White,
                        outlineColor = outline,
                        alpha = alpha,
                        limbPath = scratch.limbPath
                    )
                    drawGlowCircle(center = slamHand, radius = 32f * scale, color = SolarGold, alpha = alpha)
                }
                FighterAction.BOSS_SLASH -> {
                    val slashTip = Offset((42f + 56f * snap) * bodyW, shoulderY - 4f * bodyH)
                    drawLimb(
                        start = Offset(8f * bodyW, shoulderY + 4f * bodyH),
                        mid = Offset(32f * bodyW, shoulderY),
                        end = slashTip,
                        thickness = 14f * bodyW,
                        color = skin,
                        bootColor = ElectricCyan,
                        accentColor = Color.White,
                        outlineColor = outline,
                        alpha = alpha,
                        limbPath = scratch.limbPath
                    )
                    drawLine(
                        color = ElectricCyan.copy(alpha = 0.88f * alpha),
                        start = Offset(10f * bodyW, shoulderY - 6f * bodyH),
                        end = Offset(slashTip.x + 42f * bodyW, slashTip.y - 16f * bodyH),
                        strokeWidth = 10f * scale,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White.copy(alpha = alpha),
                        start = Offset(24f * bodyW, shoulderY - 6f * bodyH),
                        end = Offset(slashTip.x + 32f * bodyW, slashTip.y - 13f * bodyH),
                        strokeWidth = 4.2f * scale,
                        cap = StrokeCap.Round
                    )
                }
                FighterAction.GUARD -> {
                    drawLimb(
                        start = Offset(8f * bodyW, shoulderY + 8f * bodyH),
                        mid = Offset(22f * bodyW, shoulderY + 14f * bodyH),
                        end = Offset(18f * bodyW, headY + 10f * bodyH),
                        thickness = 13f * bodyW,
                        color = skin,
                        bootColor = primary,
                        accentColor = ElectricCyan,
                        outlineColor = outline,
                        alpha = alpha,
                        limbPath = scratch.limbPath
                    )
                }
                FighterAction.VICTORY -> {
                    drawLimb(
                        start = Offset(8f * bodyW, shoulderY + 4f * bodyH),
                        mid = Offset(20f * bodyW, shoulderY - 26f * bodyH),
                        end = Offset(16f * bodyW, headY - 38f * bodyH),
                        thickness = 13f * bodyW,
                        color = skin,
                        bootColor = aura,
                        accentColor = SolarGold,
                        outlineColor = outline,
                        alpha = alpha,
                        limbPath = scratch.limbPath
                    )
                }
                else -> {
                    val guardX = (24f - walkWave * 10f) * bodyW
                    val guardY = shoulderY + 14f * bodyH + breathe * 0.5f * scale
                    drawLimb(
                        start = Offset(8f * bodyW, shoulderY + 6f * bodyH),
                        mid = Offset(18f * bodyW, shoulderY + 24f * bodyH),
                        end = Offset(guardX, guardY),
                        thickness = (if (isTitan) 15f else 12f) * bodyW,
                        color = skin,
                        bootColor = primary,
                        accentColor = aura,
                        outlineColor = outline,
                        alpha = alpha,
                        limbPath = scratch.limbPath
                    )
                    if (profile.style == FighterStyle.CYBER_RONIN) {
                        // Katana blade with dark backing + glowing cyan edge + white core
                        drawLine(
                            color = outline,
                            start = Offset(guardX - 2f * bodyW, guardY + 2f * bodyH),
                            end = Offset(guardX + 32f * bodyW, guardY - 23f * bodyH),
                            strokeWidth = 6.5f * scale,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = aura,
                            start = Offset(guardX, guardY),
                            end = Offset(guardX + 30f * bodyW, guardY - 21f * bodyH),
                            strokeWidth = 4f * scale,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.White.copy(alpha = alpha),
                            start = Offset(guardX + 4f * bodyW, guardY - 3f * bodyH),
                            end = Offset(guardX + 27f * bodyW, guardY - 19f * bodyH),
                            strokeWidth = 1.6f * scale,
                            cap = StrokeCap.Round
                        )
                    } else if (profile.style == FighterStyle.SHADOW_ASSASSIN) {
                        // Curved glowing shadow dagger
                        drawLine(
                            color = outline,
                            start = Offset(guardX, guardY),
                            end = Offset(guardX + 23f * bodyW, guardY - 12f * bodyH),
                            strokeWidth = 6f * scale,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = aura,
                            start = Offset(guardX, guardY),
                            end = Offset(guardX + 22f * bodyW, guardY - 11f * bodyH),
                            strokeWidth = 3.8f * scale,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        // Energy Guard Shield Arc with bright edge + inner hex nodes
        if (action == FighterAction.GUARD) {
            drawArc(
                color = ElectricCyan.copy(alpha = 0.28f * alpha),
                startAngle = -68f,
                sweepAngle = 136f,
                useCenter = false,
                topLeft = Offset(-28f * bodyW, -152f * bodyH),
                size = Size(86f * bodyW, 138f * bodyH),
                style = Stroke(width = 14f * scale, cap = StrokeCap.Round)
            )
            drawArc(
                color = ElectricCyan.copy(alpha = 0.90f * alpha),
                startAngle = -68f,
                sweepAngle = 136f,
                useCenter = false,
                topLeft = Offset(-28f * bodyW, -152f * bodyH),
                size = Size(86f * bodyW, 138f * bodyH),
                style = Stroke(width = 5f * scale, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color.White.copy(alpha = 0.72f * alpha),
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(-26f * bodyW, -150f * bodyH),
                size = Size(82f * bodyW, 134f * bodyH),
                style = Stroke(width = 2f * scale, cap = StrokeCap.Round)
            )
        }

        // Boss Horned Crown Emblem
        if (isBoss && alpha > 0.6f) {
            val crownY = headY - 27f * scale
            val crownColor = if (bossPhase >= 2) CrimsonFlame else SolarGold
            scratch.gearPath.reset()
            scratch.gearPath.apply {
                moveTo(-12f * scale, crownY + 6f * scale)
                lineTo(-15f * scale, crownY - 6f * scale)
                lineTo(-5f * scale, crownY + 2f * scale)
                lineTo(2f * scale, crownY - 10f * scale)
                lineTo(9f * scale, crownY + 2f * scale)
                lineTo(17f * scale, crownY - 6f * scale)
                lineTo(14f * scale, crownY + 6f * scale)
                close()
            }
            drawPath(scratch.gearPath, color = outline, style = Stroke(width = 2.8f * scale, join = StrokeJoin.Round))
            drawPath(scratch.gearPath, color = crownColor)
        }
    }
}

fun DrawScope.drawAllyStriker(
    ally: AllyStrikerState?,
    widthPx: Float,
    floorY: Float,
    scale: Float,
    scratch: FighterRenderScratch
) {
    if (ally == null || !ally.active) return
    val sx = worldX(ally.x, widthPx)
    val sy = floorY - ally.y * scale
    val dir = if (ally.facingRight) 1f else -1f
    val pulse = (ally.timer / ally.duration).coerceIn(0f, 1f)
    val alpha = if (pulse < 0.2f) pulse / 0.2f else if (pulse > 0.8f) (1f - pulse) / 0.2f else 1f
    val outline = InkOutline.copy(alpha = alpha * 0.9f)

    if (ally.isAerialDrone) {
        drawGlowCircle(center = Offset(sx, sy), radius = 34f * scale, color = ally.color, alpha = alpha)
        // Winged Cyber Drone silhouette
        scratch.fxPath.reset()
        scratch.fxPath.apply {
            moveTo(sx - 28f * scale * dir, sy - 8f * scale)
            lineTo(sx + 18f * scale * dir, sy - 14f * scale)
            lineTo(sx + 26f * scale * dir, sy)
            lineTo(sx + 14f * scale * dir, sy + 12f * scale)
            lineTo(sx - 24f * scale * dir, sy + 10f * scale)
            close()
        }
        drawPath(scratch.fxPath, color = outline, style = Stroke(width = 3.5f * scale, join = StrokeJoin.Round))
        drawPath(scratch.fxPath, color = Color(0xFF1A2338).copy(alpha = alpha))
        drawCircle(
            color = ally.color.copy(alpha = alpha),
            radius = 6f * scale,
            center = Offset(sx + dir * 11f * scale, sy)
        )
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = 2.5f * scale,
            center = Offset(sx + dir * 12f * scale, sy)
        )
    } else {
        drawOval(
            color = Color.Black.copy(alpha = 0.44f * alpha),
            topLeft = Offset(sx - 32f * scale, floorY - 6f * scale),
            size = Size(64f * scale, 13f * scale)
        )
        drawOval(
            color = ally.color.copy(alpha = 0.22f * alpha),
            topLeft = Offset(sx - 44f * scale, sy - 135f * scale),
            size = Size(88f * scale, 140f * scale)
        )
        // Shadow Assassin striker silhouette
        scratch.fxPath.reset()
        scratch.fxPath.apply {
            moveTo(sx - 18f * scale, sy - 98f * scale)
            lineTo(sx + 18f * scale, sy - 98f * scale)
            lineTo(sx + 12f * scale, sy - 38f * scale)
            lineTo(sx - 12f * scale, sy - 38f * scale)
            close()
        }
        drawPath(scratch.fxPath, color = outline, style = Stroke(width = 4f * scale, join = StrokeJoin.Round))
        drawPath(scratch.fxPath, color = ally.color.copy(alpha = 0.85f * alpha))
        drawCircle(color = outline, radius = 14f * scale, center = Offset(sx + dir * 2f * scale, sy - 116f * scale))
        drawCircle(color = Color(0xFF1E1330).copy(alpha = alpha), radius = 12f * scale, center = Offset(sx + dir * 2f * scale, sy - 116f * scale))
        drawCircle(color = Color.White.copy(alpha = 0.95f * alpha), radius = 3f * scale, center = Offset(sx + dir * 7f * scale, sy - 117f * scale))
    }
}

private fun DrawScope.drawLimb(
    start: Offset,
    mid: Offset,
    end: Offset,
    thickness: Float,
    color: Color,
    bootColor: Color,
    accentColor: Color,
    outlineColor: Color,
    alpha: Float,
    limbPath: Path
) {
    limbPath.reset()
    limbPath.moveTo(start.x, start.y)
    limbPath.lineTo(mid.x, mid.y)
    limbPath.lineTo(end.x, end.y)

    // Cel ink outline
    drawPath(
        path = limbPath,
        color = outlineColor,
        style = Stroke(width = thickness + 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    // Main limb fill
    drawPath(
        path = limbPath,
        color = color,
        style = Stroke(width = thickness, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    // Specular highlight along upper segment
    drawLine(
        color = Color.White.copy(alpha = 0.16f * alpha),
        start = start,
        end = mid,
        strokeWidth = (thickness * 0.28f).coerceAtLeast(1.5f),
        cap = StrokeCap.Round
    )
    // Gauntlet / Boot cuff with ink rim and accent trim
    drawCircle(
        color = outlineColor,
        radius = thickness * 0.72f,
        center = end
    )
    drawCircle(
        color = bootColor,
        radius = thickness * 0.58f,
        center = end
    )
    drawCircle(
        color = accentColor.copy(alpha = 0.85f * alpha),
        radius = thickness * 0.24f,
        center = end
    )
}

fun DrawScope.drawProjectilesParticlesAndCallouts(
    projectiles: List<Projectile>,
    particles: List<HitParticle>,
    impactRings: List<ImpactRing>,
    callouts: List<FloatingCallout>,
    widthPx: Float,
    floorY: Float,
    scale: Float,
    calloutPaint: Paint,
    scratch: FighterRenderScratch
) {
    for (i in impactRings.indices) {
        val ring = impactRings[i]
        val rx = worldX(ring.x, widthPx)
        val ry = floorY - ring.y * scale
        val progress = 1f - (ring.life / ring.maxLife).coerceIn(0f, 1f)
        val radius = (12f + ring.maxRadius * progress) * scale
        val alpha = (1f - progress).coerceIn(0f, 1f)
        drawCircle(
            color = ring.color.copy(alpha = alpha * 0.85f),
            radius = radius,
            center = Offset(rx, ry),
            style = Stroke(width = (5.5f * (1f - progress) + 1.5f) * scale)
        )
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.65f),
            radius = radius * 0.78f,
            center = Offset(rx, ry),
            style = Stroke(width = (2.2f * (1f - progress) + 0.8f) * scale)
        )
    }

    for (i in projectiles.indices) {
        val proj = projectiles[i]
        val px = worldX(proj.x, widthPx)
        val py = floorY - proj.y * scale
        val r = proj.radius * scale
        val dir = if (proj.vx >= 0f) 1f else -1f

        // Floor light reflection under low projectiles
        if (proj.y < 140f) {
            drawOval(
                color = proj.primaryColor.copy(alpha = 0.24f),
                topLeft = Offset(px - r * 1.4f, floorY - 6f * scale),
                size = Size(r * 2.8f, 12f * scale)
            )
        }

        // Outer energy glow halo
        drawCircle(
            color = proj.primaryColor.copy(alpha = 0.28f),
            radius = r * 1.65f,
            center = Offset(px, py)
        )

        when (proj.style) {
            FighterStyle.MARTIAL_ARTIST -> {
                // Blazing comet fireball with fiery tail
                scratch.fxPath.reset()
                scratch.fxPath.apply {
                    moveTo(px + dir * r * 1.25f, py)
                    quadraticTo(px, py - r * 1.05f, px - dir * r * 2.4f, py - r * 0.25f)
                    lineTo(px - dir * r * 1.4f, py)
                    lineTo(px - dir * r * 2.5f, py + r * 0.35f)
                    quadraticTo(px, py + r * 1.05f, px + dir * r * 1.25f, py)
                    close()
                }
                drawPath(scratch.fxPath, color = proj.primaryColor.copy(alpha = 0.86f))
                drawCircle(color = proj.coreColor.copy(alpha = 0.90f), radius = r * 0.82f, center = Offset(px, py))
                drawCircle(color = Color.White, radius = r * 0.44f, center = Offset(px + dir * r * 0.15f, py))
            }
            FighterStyle.CYBER_RONIN -> {
                // Electric plasma crescent slash wave
                scratch.fxPath.reset()
                scratch.fxPath.apply {
                    moveTo(px - dir * r * 0.6f, py - r * 1.35f)
                    quadraticTo(px + dir * r * 1.55f, py, px - dir * r * 0.6f, py + r * 1.35f)
                    quadraticTo(px + dir * r * 0.55f, py, px - dir * r * 0.6f, py - r * 1.35f)
                    close()
                }
                drawPath(scratch.fxPath, color = proj.primaryColor.copy(alpha = 0.92f))
                drawLine(
                    color = Color.White,
                    start = Offset(px - dir * r * 0.2f, py - r * 0.85f),
                    end = Offset(px + dir * r * 0.85f, py),
                    strokeWidth = 3f * scale,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(px + dir * r * 0.85f, py),
                    end = Offset(px - dir * r * 0.2f, py + r * 0.85f),
                    strokeWidth = 3f * scale,
                    cap = StrokeCap.Round
                )
            }
            FighterStyle.HEAVY_TITAN -> {
                // Jagged seismic magma boulder wave
                scratch.fxPath.reset()
                scratch.fxPath.apply {
                    moveTo(px - dir * r * 1.6f, py + r * 0.6f)
                    lineTo(px - dir * r * 0.8f, py - r * 1.15f)
                    lineTo(px + dir * r * 0.1f, py - r * 0.65f)
                    lineTo(px + dir * r * 1.1f, py - r * 1.35f)
                    lineTo(px + dir * r * 1.4f, py + r * 0.6f)
                    close()
                }
                drawPath(scratch.fxPath, color = InkOutline, style = Stroke(width = 4f * scale, join = StrokeJoin.Round))
                drawPath(scratch.fxPath, color = proj.primaryColor)
                drawCircle(color = proj.coreColor, radius = r * 0.65f, center = Offset(px + dir * r * 0.2f, py - r * 0.2f))
                drawCircle(color = Color.White.copy(alpha = 0.85f), radius = r * 0.30f, center = Offset(px + dir * r * 0.2f, py - r * 0.2f))
            }
            FighterStyle.SHADOW_ASSASSIN -> {
                // Spinning twin-pointed shadow shuriken crescent
                val spin = proj.life * 14f
                val c = cos(spin)
                val s = sin(spin)
                scratch.fxPath.reset()
                scratch.fxPath.apply {
                    moveTo(px + c * r * 1.3f, py + s * r * 1.3f)
                    lineTo(px - s * r * 0.55f, py + c * r * 0.55f)
                    lineTo(px - c * r * 1.3f, py - s * r * 1.3f)
                    lineTo(px + s * r * 0.55f, py - c * r * 0.55f)
                    close()
                }
                drawPath(scratch.fxPath, color = proj.primaryColor.copy(alpha = 0.92f))
                drawCircle(color = proj.coreColor, radius = r * 0.55f, center = Offset(px, py))
                drawCircle(color = Color.White, radius = r * 0.26f, center = Offset(px, py))
            }
        }
    }

    // Directional Spark Streaks + Glowing Embers
    for (i in particles.indices) {
        val p = particles[i]
        val px = worldX(p.x, widthPx)
        val py = floorY - p.y * scale
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        val tailDx = (-p.vx * 0.022f).coerceIn(-22f, 22f) * scale
        val tailDy = (p.vy * 0.022f).coerceIn(-22f, 22f) * scale
        if (abs(tailDx) + abs(tailDy) > 3f) {
            drawLine(
                color = p.color.copy(alpha = alpha * 0.85f),
                start = Offset(px, py),
                end = Offset(px + tailDx, py + tailDy),
                strokeWidth = (p.size * 0.65f * scale * alpha).coerceAtLeast(1.5f),
                cap = StrokeCap.Round
            )
        }
        drawCircle(
            color = p.color.copy(alpha = alpha),
            radius = p.size * 0.72f * scale * alpha,
            center = Offset(px, py)
        )
    }

    if (callouts.isNotEmpty()) {
        val canvas = drawContext.canvas.nativeCanvas
        calloutPaint.textSize = (26f * scale).coerceIn(24f, 56f)
        for (i in callouts.indices) {
            val c = callouts[i]
            val sx = worldX(c.x, widthPx)
            val sy = floorY - c.y * scale
            val alpha = ((c.life / c.maxLife).coerceIn(0f, 1f) * 255).toInt()
            calloutPaint.color = c.color.toArgb()
            calloutPaint.alpha = alpha
            canvas.drawText(c.text, sx, sy, calloutPaint)
        }
    }
}

fun createCalloutPaint(typeface: Typeface?): Paint {
    return Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        this.typeface = typeface ?: Typeface.DEFAULT_BOLD
        setShadowLayer(8f, 0f, 3f, android.graphics.Color.BLACK)
    }
}

@Composable
fun FighterPortraitCanvas(
    profile: FighterProfile,
    modifier: Modifier = Modifier,
    facingRight: Boolean = true
) {
    val scratch = remember { FighterRenderScratch() }
    Canvas(modifier = modifier) {
        val scale = (size.height / 175f).coerceIn(0.4f, 1.5f)
        drawFighterBody(
            profile = profile,
            action = FighterAction.IDLE,
            actionProgress = 0f,
            animPhase = 0.6f,
            facingRight = facingRight,
            vx = 0f,
            vy = 0f,
            attackVariant = 0,
            bossType = BossType.NONE,
            bossPhase = 1,
            hasArmor = false,
            cx = size.width * 0.5f,
            footY = size.height * 0.92f,
            scale = scale,
            alpha = 1f,
            isFlashing = false,
            hasSuperAura = true,
            scratch = scratch
        )
    }
}
