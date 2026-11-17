package com.armonia.app.ui.scores

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armonia.app.domain.model.Hand
import com.armonia.app.domain.music.MusicTheory
import com.armonia.app.domain.scores.ScoreNote

object ScoreLayout {
    const val START_PADDING_DP = 72f
    const val BEAT_WIDTH_DP = 76f
    const val END_PADDING_DP = 24f

    fun widthDp(barCount: Int, beatsPerBar: Int): Float =
        START_PADDING_DP +
            barCount.coerceAtLeast(1) * beatsPerBar.coerceAtLeast(1) * BEAT_WIDTH_DP +
            END_PADDING_DP
}

@Composable
fun ScoreCanvas(
    notes: List<ScoreNote>,
    beatsPerBar: Int,
    beatUnit: Int,
    barCount: Int,
    currentIndex: Int,
    expectedIndex: Int,
    wrongIndices: Set<Int>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    val noteColor = MaterialTheme.colorScheme.onSurface
    val staffColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val highlightColor = MaterialTheme.colorScheme.secondary

    val showTreble = notes.isEmpty() || notes.any { it.hand == Hand.RIGHT }
    val showBass = notes.any { it.hand == Hand.LEFT }
    val twoStaves = showTreble && showBass
    val canvasHeight = if (twoStaves) 420.dp else 300.dp

    Canvas(
        modifier = modifier
            .width(ScoreLayout.widthDp(barCount, beatsPerBar).dp)
            .height(canvasHeight)
    ) {
        val startX = ScoreLayout.START_PADDING_DP.dp.toPx()
        val endX = size.width - ScoreLayout.END_PADDING_DP.dp.toPx()
        val pxPerBeat = ScoreLayout.BEAT_WIDTH_DP.dp.toPx()
        val lineGap = 12.dp.toPx()
        val staffHeight = lineGap * 4
        val trebleBottom = if (twoStaves) 150.dp.toPx() else 195.dp.toPx()
        val bassBottom = if (twoStaves) 360.dp.toPx() else 195.dp.toPx()
        val barWidth = beatsPerBar.coerceAtLeast(1) * pxPerBeat
        val glyphSize = if (twoStaves) 32.sp else 36.sp
        val topStaffBottom = if (showTreble) trebleBottom else bassBottom
        val topStaffTop = topStaffBottom - staffHeight

        fun drawStaff(bottomY: Float, clef: String) {
            val topY = bottomY - staffHeight
            for (line in 0..4) {
                val y = bottomY - line * lineGap
                drawLine(
                    color = staffColor,
                    start = Offset(20.dp.toPx(), y),
                    end = Offset(endX, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            drawText(
                textMeasurer = textMeasurer,
                text = clef,
                topLeft = Offset(16.dp.toPx(), topY - 10.dp.toPx()),
                style = TextStyle(
                    color = mutedColor,
                    fontSize = glyphSize,
                    fontFamily = FontFamily.Serif
                )
            )
            drawText(
                textMeasurer = textMeasurer,
                text = "$beatsPerBar/$beatUnit",
                topLeft = Offset(50.dp.toPx(), topY + 16.dp.toPx()),
                style = TextStyle(
                    color = mutedColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif
                )
            )
        }

        if (showTreble) {
            drawStaff(trebleBottom, "\uD834\uDD1E")
        }
        if (showBass) {
            drawStaff(bassBottom, "\uD834\uDD22")
        }

        for (bar in 0..barCount.coerceAtLeast(1)) {
            val x = startX + bar * barWidth
            if (showTreble) {
                drawLine(
                    color = staffColor,
                    start = Offset(x, trebleBottom - staffHeight),
                    end = Offset(x, trebleBottom),
                    strokeWidth = 1.dp.toPx()
                )
            }
            if (showBass) {
                drawLine(
                    color = staffColor,
                    start = Offset(x, bassBottom - staffHeight),
                    end = Offset(x, bassBottom),
                    strokeWidth = 1.dp.toPx()
                )
            }
            if (bar < barCount) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = (bar + 1).toString(),
                    topLeft = Offset(x + 4.dp.toPx(), topStaffTop - 18.dp.toPx()),
                    style = TextStyle(
                        color = mutedColor.copy(alpha = 0.7f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                )
            }
        }

        val currentStart = notes.getOrNull(currentIndex)?.startBeats
        val expectedStart = notes.getOrNull(expectedIndex)?.startBeats

        if (currentStart != null) {
            val x = startX + currentStart * pxPerBeat
            drawLine(
                color = accentColor.copy(alpha = 0.12f),
                start = Offset(x + 14.dp.toPx(), topStaffTop - 8.dp.toPx()),
                end = Offset(x + 14.dp.toPx(), (if (showBass) bassBottom else trebleBottom) + 8.dp.toPx()),
                strokeWidth = 10.dp.toPx()
            )
        }

        notes.forEachIndexed { index, note ->
            val isRight = note.hand == Hand.RIGHT
            if (isRight && !showTreble) return@forEachIndexed
            if (!isRight && !showBass) return@forEachIndexed

            val staffBottom = if (isRight) trebleBottom else bassBottom
            val reference = if (isRight) 30 else 18
            val diatonicSteps = MusicTheory.diatonicIndex(note.midi) - reference
            val centerY = staffBottom - diatonicSteps * lineGap / 2.0f
            val centerX = startX + note.startBeats * pxPerBeat + 12.dp.toPx()

            val isWrong = index in wrongIndices
            val isExpected = expectedIndex == index ||
                (expectedStart != null && note.startBeats == expectedStart)
            val isCurrent = currentIndex == index ||
                (currentStart != null && note.startBeats == currentStart)

            val color = when {
                isWrong -> errorColor
                isExpected -> accentColor
                isCurrent -> highlightColor
                else -> noteColor
            }

            // Tagli addizionali per le note fuori dal pentagramma.
            if (diatonicSteps < 0) {
                var ledgerStep = -2
                while (ledgerStep >= diatonicSteps) {
                    val ledgerY = staffBottom - ledgerStep * lineGap / 2.0f
                    drawLine(
                        color = staffColor,
                        start = Offset(centerX - 13.dp.toPx(), ledgerY),
                        end = Offset(centerX + 13.dp.toPx(), ledgerY),
                        strokeWidth = 1.dp.toPx()
                    )
                    ledgerStep -= 2
                }
            } else if (diatonicSteps > 8) {
                var ledgerStep = 10
                while (ledgerStep <= diatonicSteps) {
                    val ledgerY = staffBottom - ledgerStep * lineGap / 2.0f
                    drawLine(
                        color = staffColor,
                        start = Offset(centerX - 13.dp.toPx(), ledgerY),
                        end = Offset(centerX + 13.dp.toPx(), ledgerY),
                        strokeWidth = 1.dp.toPx()
                    )
                    ledgerStep += 2
                }
            }

            if (MusicTheory.isSharp(note.midi)) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = "♯",
                    topLeft = Offset(centerX - 18.dp.toPx(), centerY - 10.dp.toPx()),
                    style = TextStyle(
                        color = color,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                )
            }

            if (isExpected) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.5f),
                    radius = 11.dp.toPx(),
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            val openHead = note.durationBeats >= 1.5f
            val headWidth = 10.dp.toPx()
            val headHeight = 7.dp.toPx()
            val headTopLeft = Offset(centerX - headWidth / 2f, centerY - headHeight / 2f)
            if (openHead) {
                drawOval(
                    color = color,
                    topLeft = headTopLeft,
                    size = Size(headWidth, headHeight),
                    style = Stroke(width = 1.8.dp.toPx())
                )
            } else {
                drawOval(
                    color = color,
                    topLeft = headTopLeft,
                    size = Size(headWidth, headHeight)
                )
            }

            if (note.durationBeats < 3.5f) {
                val stemUp = diatonicSteps <= 4
                val stemX = if (stemUp) centerX + 4.dp.toPx() else centerX - 4.dp.toPx()
                val stemEndY = if (stemUp) centerY - 32.dp.toPx() else centerY + 32.dp.toPx()
                drawLine(
                    color = color,
                    start = Offset(stemX, centerY),
                    end = Offset(stemX, stemEndY),
                    strokeWidth = 1.4.dp.toPx()
                )
                if (note.durationBeats <= 0.5f) {
                    val flagEndY = stemEndY + if (stemUp) 11.dp.toPx() else -11.dp.toPx()
                    drawLine(
                        color = color,
                        start = Offset(stemX, stemEndY),
                        end = Offset(stemX + 7.dp.toPx(), flagEndY),
                        strokeWidth = 1.4.dp.toPx()
                    )
                }
            }

            note.finger?.let { finger ->
                val fingerY = if (diatonicSteps > 4) {
                    centerY + 16.dp.toPx()
                } else {
                    centerY - 17.dp.toPx()
                }
                drawText(
                    textMeasurer = textMeasurer,
                    text = finger.toString(),
                    topLeft = Offset(centerX - 2.dp.toPx(), fingerY),
                    style = TextStyle(
                        color = mutedColor,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                )
            }
        }
    }
}
