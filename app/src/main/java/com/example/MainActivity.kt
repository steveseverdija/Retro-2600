package com.example

import android.content.pm.ActivityInfo
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RetroBlack
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroGray
import com.example.ui.theme.RetroLightGray
import kotlinx.coroutines.delay

// Sound Chiptune Engine using AudioTrack (Monophonic Retro Square Synth)
object SoundEffects {
  private var isMuted = false

  fun toggleMute(): Boolean {
    isMuted = !isMuted
    return isMuted
  }

  fun isMuted() = isMuted

  private fun playTone(
    startFreq: Double,
    endFreq: Double,
    durationMs: Int,
    waveform: String = "square"
  ) {
    if (isMuted) return
    Thread {
      try {
        val sampleRate = 8000
        val numSamples = (durationMs * sampleRate / 1000)
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
          val t = i.toDouble() / sampleRate
          val progress = i.toDouble() / numSamples
          val freq = startFreq + (endFreq - startFreq) * progress
          val angle = 2.0 * Math.PI * freq * t

          val value = if (waveform == "square") {
            if (Math.sin(angle) > 0) Short.MAX_VALUE else Short.MIN_VALUE
          } else {
            (Math.sin(angle) * Short.MAX_VALUE).toInt().toShort()
          }

          // Safe low volume
          buffer[i] = (value * 0.18f).toInt().toShort()
        }

        val audioAttributes = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .build()

        val audioFormat = AudioFormat.Builder()
          .setSampleRate(sampleRate)
          .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
          .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
          .build()

        val track = AudioTrack.Builder()
          .setAudioAttributes(audioAttributes)
          .setAudioFormat(audioFormat)
          .setBufferSizeInBytes(buffer.size * 2)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()
        track.write(buffer, 0, buffer.size)
        track.play()
        Thread.sleep(durationMs.toLong() + 20)
        track.release()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }.start()
  }

  fun playShoot() {
    playTone(950.0, 180.0, 130, "square")
  }

  fun playExplosion() {
    playTone(220.0, 40.0, 320, "square")
  }

  fun playScore() {
    playTone(523.25, 523.25, 50, "sine")
    try { Thread.sleep(55) } catch (e: Exception) {}
    playTone(659.25, 659.25, 120, "sine")
  }

  fun playLoseLife() {
    playTone(280.0, 60.0, 450, "square")
  }

  fun playTick() {
    playTone(800.0, 800.0, 15, "square")
  }

  fun playStartupIntroSound() {
    Thread {
      val melody = doubleArrayOf(
        261.63, 329.63, 392.00, 523.25,
        329.63, 392.00, 523.25, 659.25,
        392.00, 523.25, 659.25, 783.99,
        523.25, 659.25, 783.99, 1046.50
      )
      val duration = 85
      for (note in melody) {
        if (isMuted) break
        playTone(note, note, duration, "square")
        try { Thread.sleep(duration + 8L) } catch (e: Exception) {}
      }
      if (!isMuted) {
        playTone(1046.50, 1046.50, 120, "square")
        try { Thread.sleep(130L) } catch (e: Exception) {}
        playTone(1318.51, 1318.51, 350, "square")
      }
    }.start()
  }
}

// Particle system for retro arcade juice
data class GameParticle(val x: Float, val y: Float, val vx: Float, val vy: Float, val life: Int)

enum class RetroGame(val title: String) {
  PIXEL_PYTHON("PIXEL PYTHON"),
  BREAKOUT("BREAKOUT CLASSIC"),
  SPACE_DEFENDER("SPACE DEFENDER"),
  PONG_TENNIS("PONG TENNIS"),
  PAC_MAZE("PAC MAZE"),
  TOWER_BUILDER("TOWER BUILDER"),
  LUNAR_LANDER("LUNAR LANDER"),
  RACING_CAR("RACING CAR"),
  PLATFORMER("PLATFORMER"),
  FROG_HOPPER("FROG HOPPER")
}

enum class RetroColorTheme(val displayName: String, val screenColor: Color, val panelColor: Color) {
  MONOCHROME("B&W CLASSIC", Color(0xFFFFFFFF), Color(0xFF000000)),
  AMBER_PHOSPHOR("AMBER CRT", Color(0xFFFFB000), Color(0xFF110500)),
  GREEN_PHOSPHOR("MATRIX GREEN", Color(0xFF33FF33), Color(0xFF001100)),
  GAMEBOY("GAMEBOY GREEN", Color(0xFF8BAC0F), Color(0xFF0F380F)),
  CYBER_CYAN("CYBER GLOW", Color(0xFF00FFFF), Color(0xFF001111))
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
        ) { innerPadding ->
          GameRetroConsole(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          )
        }
      }
    }
  }
}

// Custom Triangle Shape for the Star Button
val TriangleShape = object : Shape {
  override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
    val path = Path().apply {
      moveTo(size.width / 2f, 0f)
      lineTo(size.width, size.height)
      lineTo(0f, size.height)
      close()
    }
    return Outline.Generic(path)
  }
}

// Data class representing a 3D Star for the Startup starfield animation
data class IntroStar(val x: Float, val y: Float, val z: Float)

@Composable
fun GameRetroConsole(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("GameRetroPrefs", Context.MODE_PRIVATE) }

  // Shared preferences high scores
  var highScoreDefender by remember { mutableStateOf(prefs.getInt("SD_HS", 0)) }
  var highScoreSnake by remember { mutableStateOf(prefs.getInt("SNAKE_HS", 0)) }

  // Console active game state
  var currentGame by remember { mutableStateOf(RetroGame.PIXEL_PYTHON) }
  var activeTheme by remember { mutableStateOf(RetroColorTheme.MONOCHROME) }
  var soundMuted by remember { mutableStateOf(SoundEffects.isMuted()) }

  // Button Press States (for continuous held checks & custom drawing)
  var dpadUpPressed by remember { mutableStateOf(false) }
  var dpadDownPressed by remember { mutableStateOf(false) }
  var dpadLeftPressed by remember { mutableStateOf(false) }
  var dpadRightPressed by remember { mutableStateOf(false) }

  var btnAPressed by remember { mutableStateOf(false) }
  var btnBPressed by remember { mutableStateOf(false) }
  var btnXPressed by remember { mutableStateOf(false) }
  var btnYPressed by remember { mutableStateOf(false) }

  var btnSelectPressed by remember { mutableStateOf(false) }
  var btnStarPressed by remember { mutableStateOf(false) }

  // Startup Intro state
  var isStartupActive by remember { mutableStateOf(true) }
  var introStars by remember {
    mutableStateOf(
      (0..45).map {
        IntroStar(
          x = (Math.random() * 160 - 80).toFloat(),
          y = (Math.random() * 120 - 60).toFloat(),
          z = (Math.random() * 79).toFloat() + 1f
        )
      }
    )
  }
  var introFlashText by remember { mutableStateOf(true) }

  fun handleButtonPress(): Boolean {
    if (isStartupActive) {
      isStartupActive = false
      SoundEffects.playTick()
      return true
    }
    return false
  }

  // Info card display
    var showInfoOnScreen by remember { mutableStateOf(true) }


  // 1. SPACE DEFENDER STATES
  var defenderPlayerX by remember { mutableStateOf(40f) }
  var defenderLasers by remember { mutableStateOf(emptyList<Offset>()) }
  var defenderEnemies by remember { mutableStateOf(emptyList<Offset>()) }
  var defenderScore by remember { mutableStateOf(0) }
  var defenderLives by remember { mutableStateOf(3) }
  var defenderGameOver by remember { mutableStateOf(false) }
  var defenderShieldTime by remember { mutableStateOf(0L) }
  var defenderParticles by remember { mutableStateOf(emptyList<GameParticle>()) }

  // 3. PIXEL PYTHON STATES
  var snakeBody by remember { mutableStateOf(listOf(Offset(40f, 30f), Offset(40f, 32f), Offset(40f, 34f))) }
  var snakeDir by remember { mutableStateOf(Offset(0f, -2f)) }
  var snakeFood by remember { mutableStateOf(Offset(24f, 18f)) }
  var snakeScore by remember { mutableStateOf(0) }
  var snakeGameOver by remember { mutableStateOf(false) }
  var snakeSpeedMultiplier by remember { mutableStateOf(1.0f) }
  var snakeParticles by remember { mutableStateOf(emptyList<GameParticle>()) }
  var foodBlinkToggle by remember { mutableStateOf(true) }

  // 5. BREAKOUT CLASSIC STATES
  var breakoutPaddleX by remember { mutableStateOf(33f) }
  var breakoutBall by remember { mutableStateOf(Offset(40f, 45f)) }
  var breakoutBallVel by remember { mutableStateOf(Offset(1.2f, -1.2f)) }
  var breakoutBricks by remember { mutableStateOf(emptyList<Rect>()) }
  var breakoutScore by remember { mutableStateOf(0) }
  var breakoutLives by remember { mutableStateOf(3) }
  var breakoutGameOver by remember { mutableStateOf(false) }
  var breakoutParticles by remember { mutableStateOf(emptyList<GameParticle>()) }

  var pongBall by remember { mutableStateOf(Offset(40f, 30f)) }
  var pongVel by remember { mutableStateOf(Offset(1.5f, 1.5f)) }
  var pongPlayerY by remember { mutableStateOf(30f) }
  var pongEnemyY by remember { mutableStateOf(30f) }
  var pongScore by remember { mutableStateOf(0) }
  var pongGameOver by remember { mutableStateOf(false) }

  var pacPlayer by remember { mutableStateOf(Offset(6f, 18f)) }
  var pacDir by remember { mutableStateOf(Offset(0f, 0f)) }
  var pacGhosts by remember { mutableStateOf(listOf(Offset(74f, 54f))) }
  var pacFoodList by remember { mutableStateOf(emptyList<Offset>()) }
  var pacMazeWalls by remember { mutableStateOf(emptyList<Rect>()) }
  var pacScore by remember { mutableStateOf(0) }
  var pacGameOver by remember { mutableStateOf(false) }
  var pacWin by remember { mutableStateOf(false) }

  var towerBlocks by remember { mutableStateOf(listOf(Rect(30f, 55f, 50f, 60f))) }
  var towerX by remember { mutableStateOf(0f) }
  var towerDir by remember { mutableStateOf(1.5f) }
  var towerWidth by remember { mutableStateOf(20f) }
  var towerScore by remember { mutableStateOf(0) }
  var towerGameOver by remember { mutableStateOf(false) }

  var landerPos by remember { mutableStateOf(Offset(40f, 10f)) }
  var landerVel by remember { mutableStateOf(Offset(0f, 0f)) }
  var landerFuel by remember { mutableStateOf(100) }
  var landerScore by remember { mutableStateOf(0) }
  var landerGameOver by remember { mutableStateOf(false) }

  var racingCarX by remember { mutableStateOf(40f) }
  var racingEnemies by remember { mutableStateOf(emptyList<androidx.compose.ui.geometry.Offset>()) }
  var racingScore by remember { mutableStateOf(0) }
  var racingGameOver by remember { mutableStateOf(false) }

  var platPlayer by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(40f, 50f)) }
  var platVelY by remember { mutableStateOf(0f) }
  var platPlatforms by remember { mutableStateOf(listOf(androidx.compose.ui.geometry.Offset(40f, 55f), androidx.compose.ui.geometry.Offset(60f, 40f), androidx.compose.ui.geometry.Offset(20f, 25f))) }
  var platScore by remember { mutableStateOf(0) }
  var platGameOver by remember { mutableStateOf(false) }

  var frogPlayer by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(40f, 55f)) }
  var frogCars by remember { mutableStateOf(emptyList<androidx.compose.ui.geometry.Offset>()) }
  var frogScore by remember { mutableStateOf(0) }
  var frogGameOver by remember { mutableStateOf(false) }

  // Create isGameOver flag so we can use it everywhere
  val isGameOver = when (currentGame) {
    RetroGame.PIXEL_PYTHON -> snakeGameOver
    RetroGame.BREAKOUT -> breakoutGameOver
    RetroGame.SPACE_DEFENDER -> defenderGameOver
    RetroGame.PONG_TENNIS -> pongGameOver
    RetroGame.PAC_MAZE -> pacGameOver || pacWin
    RetroGame.TOWER_BUILDER -> towerGameOver
    RetroGame.LUNAR_LANDER -> landerGameOver
    RetroGame.RACING_CAR -> racingGameOver
    RetroGame.PLATFORMER -> platGameOver
    RetroGame.FROG_HOPPER -> frogGameOver
  }

  // Glitch effect logic
  var glitchOffsetX by remember { mutableStateOf(0f) }
  var glitchOffsetY by remember { mutableStateOf(0f) }
  var isGlitching by remember { mutableStateOf(false) }
  LaunchedEffect(isGameOver) {
      if (isGameOver) {
          isGlitching = true
          val startTime = System.currentTimeMillis()
          while (System.currentTimeMillis() - startTime < 3000) {
              glitchOffsetX = (-3..3).random().toFloat()
              glitchOffsetY = (-3..3).random().toFloat()
              kotlinx.coroutines.delay(50)
          }
          isGlitching = false
          glitchOffsetX = 0f
          glitchOffsetY = 0f
      } else {
          isGlitching = false
          glitchOffsetX = 0f
          glitchOffsetY = 0f
      }
  }
  val glitchModifier = if (isGlitching) {
      Modifier.offset(glitchOffsetX.dp, glitchOffsetY.dp).drawWithContent {
          drawContent()
          if (glitchOffsetX > 1f) {
              drawRect(Color(0x4400FFFF)) // Cyan
          } else if (glitchOffsetX < -1f) {
              drawRect(Color(0x44FF00FF)) // Magenta
          }
          if (glitchOffsetY > 2f) {
              drawLine(Color(0x66FFFFFF), Offset(0f, size.height/2f), Offset(size.width, size.height/2f), strokeWidth = 2f)
          }
      }
  } else {
      Modifier
  }
  var highScoreBreakout by remember { mutableStateOf(prefs.getInt("BREAKOUT_HS", 0)) }

  // Startup sound trigger
  LaunchedEffect(Unit) {
    SoundEffects.playStartupIntroSound()
  }

  // Startup starfield and flash text update loop
  LaunchedEffect(isStartupActive) {
    if (isStartupActive) {
      var ticks = 0
      while (isStartupActive) {
        delay(40) // 25 FPS
        ticks++
        if (ticks % 10 == 0) {
          introFlashText = !introFlashText
        }
        introStars = introStars.map { star ->
          val nextZ = star.z - 1.8f
          if (nextZ <= 0.2f) {
            IntroStar(
              x = (Math.random() * 160 - 80).toFloat(),
              y = (Math.random() * 120 - 60).toFloat(),
              z = 80f
            )
          } else {
            star.copy(z = nextZ)
          }
        }
      }
    }
  }

  // Helper functions for game resetting
  fun resetSpaceDefender() {
    defenderPlayerX = 40f
    defenderLasers = emptyList()
    defenderEnemies = emptyList()
    defenderScore = 0
    defenderLives = 3
    defenderGameOver = false
    defenderShieldTime = 0L
    defenderParticles = emptyList()
    showInfoOnScreen = false
  }

  fun resetPixelPython() {
    snakeBody = listOf(Offset(40f, 30f), Offset(40f, 32f), Offset(40f, 34f))
    snakeDir = Offset(0f, -2f)
    snakeFood = Offset(24f, 18f)
    snakeScore = 0
    snakeGameOver = false
    snakeSpeedMultiplier = 1.0f
    snakeParticles = emptyList()
    showInfoOnScreen = false
  }


    fun generateRandomMaze(): List<Rect> {
      val walls = mutableListOf<Rect>()
      walls.add(Rect(0f, 12f, 80f, 16f)) // Top
      walls.add(Rect(0f, 56f, 80f, 60f)) // Bottom
      walls.add(Rect(0f, 12f, 4f, 60f)) // Left
      walls.add(Rect(76f, 12f, 80f, 60f)) // Right
      
      for (x in 12..60 step 12) {
          for (y in 20..44 step 12) {
              if (Math.random() < 0.75) {
                  val w = if (Math.random() < 0.5) 8f else 4f
                  val h = if (w == 4f && Math.random() < 0.5) 8f else 4f
                  walls.add(Rect(x.toFloat(), y.toFloat(), x + w, y + h))
              }
          }
      }
      return walls
  }

  fun generatePacFoodList(walls: List<Rect>): List<Offset> {
      val foods = mutableListOf<Offset>()
      for (x in 1..18) {
          for (y in 4..13) {
              val cx = x * 4f + 2f
              val cy = y * 4f + 2f
              if (walls.none { it.contains(Offset(cx, cy)) }) {
                  foods.add(Offset(cx, cy))
              }
          }
      }
      return foods
  }

  fun resetPong() { pongBall = Offset(40f, 30f); pongVel = Offset(1.5f, 1.5f); pongPlayerY = 30f; pongEnemyY = 30f; pongScore = 0; pongGameOver = false; showInfoOnScreen = false }
  fun resetPacMaze() { pacMazeWalls = generateRandomMaze(); pacFoodList = generatePacFoodList(pacMazeWalls); pacPlayer = Offset(6f, 18f); pacDir = Offset(0f, 0f); pacGhosts = listOf(Offset(74f, 54f)); pacScore = 0; pacGameOver = false; pacWin = false; showInfoOnScreen = false }
  fun resetTower() { towerBlocks = listOf(Rect(30f, 55f, 50f, 60f)); towerX = 0f; towerDir = 1.5f; towerWidth = 20f; towerScore = 0; towerGameOver = false; showInfoOnScreen = false }
  fun resetLander() { landerPos = Offset(40f, 10f); landerVel = Offset(0f, 0f); landerFuel = 100; landerScore = 0; landerGameOver = false; showInfoOnScreen = false }
  fun resetRacing() { racingCarX = 40f; racingEnemies = emptyList(); racingScore = 0; racingGameOver = false; showInfoOnScreen = false }
  fun resetPlatformer() { platPlayer = androidx.compose.ui.geometry.Offset(40f, 50f); platVelY = 0f; platPlatforms = listOf(androidx.compose.ui.geometry.Offset(40f, 55f), androidx.compose.ui.geometry.Offset(60f, 40f), androidx.compose.ui.geometry.Offset(20f, 25f)); platScore = 0; platGameOver = false; showInfoOnScreen = false }
  fun resetFrogHopper() { frogPlayer = androidx.compose.ui.geometry.Offset(40f, 55f); frogCars = emptyList(); frogScore = 0; frogGameOver = false; showInfoOnScreen = false }
  fun resetBreakout() {
    breakoutPaddleX = 33f
    breakoutBall = Offset(40f, 45f)
    breakoutBallVel = Offset(if (Math.random() > 0.5) 1.2f else -1.2f, -1.2f)
    val bricksList = mutableListOf<Rect>()
    for (row in 0..3) {
      for (col in 0..7) {
        bricksList.add(
          Rect(
            left = 3f + col * 9f,
            top = 14f + row * 4f,
            right = 3f + col * 9f + 8f,
            bottom = 14f + row * 4f + 3f
          )
        )
      }
    }
    breakoutBricks = bricksList
    breakoutScore = 0
    breakoutLives = 3
    breakoutGameOver = false
    breakoutParticles = emptyList()
    showInfoOnScreen = false
  }

  // Handle D-pad continuous inputs (held buttons) in loop
  LaunchedEffect(dpadLeftPressed, dpadRightPressed, dpadUpPressed, dpadDownPressed, currentGame, defenderGameOver, breakoutGameOver) {
    while (true) {
      when (currentGame) {
        RetroGame.SPACE_DEFENDER -> {
          if (dpadLeftPressed) defenderPlayerX = (defenderPlayerX - 2f).coerceAtLeast(0f)
          if (dpadRightPressed) defenderPlayerX = (defenderPlayerX + 2f).coerceAtMost(80f)
        }
        RetroGame.PONG_TENNIS -> {
          if (dpadUpPressed) pongPlayerY = (pongPlayerY - 2f).coerceAtLeast(14f)
          if (dpadDownPressed) pongPlayerY = (pongPlayerY + 2f).coerceAtMost(54f)
        }
        RetroGame.PAC_MAZE -> {
          if (!pacGameOver && !pacWin) {
              if (dpadLeftPressed) pacDir = Offset(-4f, 0f)
              if (dpadRightPressed) pacDir = Offset(4f, 0f)
              if (dpadUpPressed) pacDir = Offset(0f, -4f)
              if (dpadDownPressed) pacDir = Offset(0f, 4f)
          }
        }
        RetroGame.LUNAR_LANDER -> {
          if (dpadLeftPressed && landerFuel > 0) { landerVel = landerVel.copy(x = landerVel.x - 0.2f); landerFuel-- }
          if (dpadRightPressed && landerFuel > 0) { landerVel = landerVel.copy(x = landerVel.x + 0.2f); landerFuel-- }
          if (dpadUpPressed && landerFuel > 0) { landerVel = landerVel.copy(y = landerVel.y - 0.3f); landerFuel-- }
        }
        RetroGame.RACING_CAR -> {
          if (dpadLeftPressed) racingCarX = (racingCarX - 2f).coerceAtLeast(0f)
          if (dpadRightPressed) racingCarX = (racingCarX + 2f).coerceAtMost(80f)
        }
        RetroGame.PLATFORMER -> {
          if (dpadLeftPressed) platPlayer = platPlayer.copy(x = (platPlayer.x - 2f).coerceAtLeast(0f))
          if (dpadRightPressed) platPlayer = platPlayer.copy(x = (platPlayer.x + 2f).coerceAtMost(80f))
        }
        else -> {}
      }
      if (currentGame == RetroGame.BREAKOUT && !breakoutGameOver) {
        if (dpadLeftPressed) {
          breakoutPaddleX = (breakoutPaddleX - 2.5f).coerceAtLeast(0f)
        }
        if (dpadRightPressed) {
          breakoutPaddleX = (breakoutPaddleX + 2.5f).coerceAtMost(66f)
        }
      }
      delay(30)
    }
  }

  // General 25 FPS Game Loop (ticks Space Defender, Pong physics, and particles)
  LaunchedEffect(currentGame, defenderGameOver, breakoutGameOver) {
    while (true) {
      delay(40) // 25 FPS

      // food blink speed helper
      if (currentGame == RetroGame.PIXEL_PYTHON) {
        foodBlinkToggle = !foodBlinkToggle
      }

      when (currentGame) {

        RetroGame.SPACE_DEFENDER -> {
          if (!defenderGameOver && !showInfoOnScreen) {
             if (Math.random() < 0.05) defenderEnemies = defenderEnemies + androidx.compose.ui.geometry.Offset((5..75).random().toFloat(), 0f)
             defenderEnemies = defenderEnemies.map { it.copy(y = it.y + 1f) }.filter { it.y < 60f }
             defenderLasers = defenderLasers.map { it.copy(y = it.y - 2f) }.filter { it.y > 0f }
             val toRemoveE = mutableSetOf<androidx.compose.ui.geometry.Offset>()
             val toRemoveL = mutableSetOf<androidx.compose.ui.geometry.Offset>()
             for (e in defenderEnemies) {
                if (Math.abs(e.x - defenderPlayerX) < 4f && e.y > 50f) { defenderGameOver = true }
                for (l in defenderLasers) {
                   if (Math.abs(e.x - l.x) < 3f && Math.abs(e.y - l.y) < 3f) {
                      toRemoveE.add(e)
                      toRemoveL.add(l)
                      defenderScore += 10
                   }
                }
             }
             defenderEnemies = defenderEnemies - toRemoveE
             defenderLasers = defenderLasers - toRemoveL
          }
        }
        RetroGame.PONG_TENNIS -> {
          if (!pongGameOver && !showInfoOnScreen) {
             pongBall = Offset(pongBall.x + pongVel.x, pongBall.y + pongVel.y)
             // Enemy AI
             if (pongBall.y < pongEnemyY + 4f) pongEnemyY = (pongEnemyY - 1f).coerceAtLeast(14f)
             if (pongBall.y > pongEnemyY + 4f) pongEnemyY = (pongEnemyY + 1f).coerceAtMost(54f)
             // Wall bounce
             if (pongBall.y <= 12f || pongBall.y >= 58f) pongVel = pongVel.copy(y = -pongVel.y)
             // Paddle bounce
             if (pongBall.x <= 8f && pongVel.x < 0 && Math.abs(pongBall.y - pongPlayerY) < 6f) {
                 pongVel = pongVel.copy(x = -pongVel.x * 1.05f)
                 pongScore += 10
                 SoundEffects.playTick()
             }
             if (pongBall.x >= 72f && pongVel.x > 0 && Math.abs(pongBall.y - pongEnemyY) < 6f) {
                 pongVel = pongVel.copy(x = -pongVel.x * 1.05f)
                 SoundEffects.playTick()
             }
             if (pongBall.x < 0f || pongBall.x > 80f) pongGameOver = true
          }
        }
        RetroGame.PAC_MAZE -> {}
        RetroGame.TOWER_BUILDER -> {
          if (!towerGameOver && !showInfoOnScreen) {
             towerX += towerDir
             if (towerX < 0f || towerX + towerWidth > 80f) {
                 towerDir = -towerDir
                 towerX = towerX.coerceIn(0f, 80f - towerWidth)
             }
          }
        }
        RetroGame.LUNAR_LANDER -> {
          if (!landerGameOver && !showInfoOnScreen) {
             landerVel = landerVel.copy(y = landerVel.y + 0.05f) // gravity
             landerPos = Offset(landerPos.x + landerVel.x, landerPos.y + landerVel.y)
             
             // Wrap around horizontal
             if (landerPos.x < 0f) landerPos = landerPos.copy(x = 80f)
             if (landerPos.x > 80f) landerPos = landerPos.copy(x = 0f)
             
             val terrainY = when {
                 landerPos.x < 30f -> 45f + (Math.abs(landerPos.x - 20f) * 0.5f) // Mountain peak at 20f
                 landerPos.x > 50f -> 40f + (Math.abs(landerPos.x - 65f) * 0.5f) // Mountain peak at 65f
                 else -> 56f // Pad
             }
             if (landerPos.y >= terrainY - 2f) {
                if (landerPos.x in 30f..50f && landerVel.y < 0.5f && Math.abs(landerVel.x) < 0.3f) {
                    // Safe landing
                    landerScore += 100
                    landerPos = Offset(40f, 10f)
                    landerVel = Offset(0f, 0f)
                    landerFuel += 50
                    SoundEffects.playShoot()
                } else {
                    landerGameOver = true
                }
             }
          }
        }
        RetroGame.RACING_CAR -> {
          if (!racingGameOver && !showInfoOnScreen) {
             if (Math.random() < 0.1) racingEnemies = racingEnemies + androidx.compose.ui.geometry.Offset((10..70).random().toFloat(), 0f)
             racingEnemies = racingEnemies.map { it.copy(y = it.y + 2f) }.filter { it.y < 60f }
             if (racingEnemies.any { Math.abs(it.x - racingCarX) < 4f && Math.abs(it.y - 50f) < 4f }) racingGameOver = true
             racingScore += 1
          }
        }
        RetroGame.PLATFORMER -> {
          if (!platGameOver && !showInfoOnScreen) {
             platVelY += 0.2f
             platPlayer = platPlayer.copy(y = platPlayer.y + platVelY)
             platPlatforms = platPlatforms.map { it.copy(y = it.y + 0.5f) }.filter { it.y < 60f }
             if (platPlatforms.isEmpty() || platPlatforms.last().y > 20f) {
                platPlatforms = platPlatforms + androidx.compose.ui.geometry.Offset((10..70).random().toFloat(), 0f)
             }
             if (platPlayer.y > 60f) platGameOver = true
             for (p in platPlatforms) {
                if (platVelY > 0f && Math.abs(p.x - platPlayer.x) < 8f && Math.abs(p.y - platPlayer.y) < 2f) {
                   platVelY = -4f
                   platScore += 10
                }
             }
          }
        }
        RetroGame.FROG_HOPPER -> {
          if (!frogGameOver && !showInfoOnScreen) {
             if (Math.random() < 0.1) frogCars = frogCars + androidx.compose.ui.geometry.Offset(0f, listOf(10f, 20f, 30f, 40f).random())
             frogCars = frogCars.map { it.copy(x = it.x + 1.5f) }.filter { it.x < 80f }
             if (frogCars.any { Math.abs(it.x - frogPlayer.x) < 4f && Math.abs(it.y - frogPlayer.y) < 4f }) frogGameOver = true
             if (frogPlayer.y <= 5f) { frogScore += 50; frogPlayer = androidx.compose.ui.geometry.Offset(40f, 55f) }
          }
        }
        RetroGame.BREAKOUT -> {
          if (!breakoutGameOver && !showInfoOnScreen) {
            // Ball movement
            var nextBallX = breakoutBall.x + breakoutBallVel.x
            var nextBallY = breakoutBall.y + breakoutBallVel.y

            // Left / Right Wall Bounce
            if (nextBallX <= 2f) {
              nextBallX = 2f
              breakoutBallVel = breakoutBallVel.copy(x = -breakoutBallVel.x)
              SoundEffects.playTick()
            } else if (nextBallX >= 78f) {
              nextBallX = 78f
              breakoutBallVel = breakoutBallVel.copy(x = -breakoutBallVel.x)
              SoundEffects.playTick()
            }

            // Top Wall Bounce
            if (nextBallY <= 12f) {
              nextBallY = 12f
              breakoutBallVel = breakoutBallVel.copy(y = -breakoutBallVel.y)
              SoundEffects.playTick()
            }

            // Player Paddle Bounce
            if (nextBallY >= 53f && nextBallY <= 55f) {
              if (nextBallX >= breakoutPaddleX && nextBallX <= breakoutPaddleX + 14f) {
                nextBallY = 53f
                val midX = breakoutPaddleX + 7f
                val deltaX = nextBallX - midX
                val bounceVx = (deltaX / 7f) * 1.5f
                breakoutBallVel = Offset(bounceVx, -Math.abs(breakoutBallVel.y))
                SoundEffects.playTick()

                // Spawn splash particles
                val bounceParticles = (0..3).map {
                  GameParticle(
                    x = nextBallX,
                    y = nextBallY,
                    vx = (Math.random() - 0.5).toFloat() * 1.5f,
                    vy = -1.2f,
                    life = 6
                  )
                }
                breakoutParticles = breakoutParticles + bounceParticles
              }
            }

            // Ball vs Bricks Collision
            var hitBrickIndex = -1
            for (idx in breakoutBricks.indices) {
              val brick = breakoutBricks[idx]
              if (nextBallX >= brick.left - 1f && nextBallX <= brick.right + 1f &&
                  nextBallY >= brick.top - 1f && nextBallY <= brick.bottom + 1f) {
                hitBrickIndex = idx
                break
              }
            }

            if (hitBrickIndex != -1) {
              val hitBrick = breakoutBricks[hitBrickIndex]
              
              // Bounce y
              breakoutBallVel = breakoutBallVel.copy(y = -breakoutBallVel.y)
              
              // Remove brick
              breakoutBricks = breakoutBricks.filterIndexed { idx, _ -> idx != hitBrickIndex }
              
              breakoutScore += 15
              SoundEffects.playScore()

              // Spark particles
              val midBrickX = (hitBrick.left + hitBrick.right) / 2f
              val midBrickY = (hitBrick.top + hitBrick.bottom) / 2f
              val bounceParticles = (0..5).map { bIdx ->
                val angle = bIdx * (2 * Math.PI / 6)
                GameParticle(
                  x = midBrickX,
                  y = midBrickY,
                  vx = Math.cos(angle).toFloat() * 1.5f,
                  vy = Math.sin(angle).toFloat() * 1.5f,
                  life = 8
                )
              }
              breakoutParticles = breakoutParticles + bounceParticles

              // Reset bricks if all cleared
              if (breakoutBricks.isEmpty()) {
                val bricksList = mutableListOf<Rect>()
                for (row in 0..3) {
                  for (col in 0..7) {
                    bricksList.add(
                      Rect(
                        left = 3f + col * 9f,
                        top = 14f + row * 4f,
                        right = 3f + col * 9f + 8f,
                        bottom = 14f + row * 4f + 3f
                      )
                    )
                  }
                }
                breakoutBricks = bricksList
                breakoutBallVel = Offset(breakoutBallVel.x * 1.1f, breakoutBallVel.y * 1.1f)
              }
            }

            // Ball Miss check
            if (nextBallY > 58f) {
              breakoutLives--
              SoundEffects.playLoseLife()
              nextBallX = 40f
              nextBallY = 40f
              breakoutBallVel = Offset(if (Math.random() > 0.5) 1.2f else -1.2f, -1.2f)

              if (breakoutLives <= 0) {
                breakoutGameOver = true
                if (breakoutScore > highScoreBreakout) {
                  highScoreBreakout = breakoutScore
                  prefs.edit().putInt("BREAKOUT_HS", breakoutScore).apply()
                }
              }
            }

            breakoutBall = Offset(nextBallX, nextBallY)
          }

          breakoutParticles = breakoutParticles.map {
            it.copy(x = it.x + it.vx, y = it.y + it.vy, life = it.life - 1)
          }.filter { it.life > 0 }
        }

        else -> {}
      }
    }
  }

  // Dedicated Pac Maze Loop (grid based movement)
  LaunchedEffect(currentGame, pacGameOver, pacWin, showInfoOnScreen) {
    var tick = 0
    while (currentGame == RetroGame.PAC_MAZE && !pacGameOver && !pacWin && !showInfoOnScreen) {
      delay(200) // 5 steps per second
      tick++
      
      // Move Player
      if (pacDir.x != 0f || pacDir.y != 0f) {
          val newX = pacPlayer.x + pacDir.x
          val newY = pacPlayer.y + pacDir.y
          val pRect = Rect(newX - 1.9f, newY - 1.9f, newX + 1.9f, newY + 1.9f)
          if (!pacMazeWalls.any { it.left < pRect.right && it.right > pRect.left && it.top < pRect.bottom && it.bottom > pRect.top }) {
              pacPlayer = Offset(newX, newY)
          }
      }
      
      // Move Ghosts
      if (tick % 2 == 0) {
      pacGhosts = pacGhosts.map { g ->
          val dx = if (g.x < pacPlayer.x) 4f else if (g.x > pacPlayer.x) -4f else 0f
          val dy = if (g.y < pacPlayer.y) 4f else if (g.y > pacPlayer.y) -4f else 0f
          
          var moved = false
          var newGx = g.x
          var newGy = g.y
          
          // Try horizontal first if it's the primary direction, else vertical
          val tryXFirst = Math.abs(pacPlayer.x - g.x) > Math.abs(pacPlayer.y - g.y)
          
          if (tryXFirst && dx != 0f) {
              val gRectX = Rect(g.x + dx - 1.9f, g.y - 1.9f, g.x + dx + 1.9f, g.y + 1.9f)
              if (!pacMazeWalls.any { it.left < gRectX.right && it.right > gRectX.left && it.top < gRectX.bottom && it.bottom > gRectX.top }) {
                  newGx = g.x + dx
                  moved = true
              }
          }
          
          if (!moved && dy != 0f) {
              val gRectY = Rect(g.x - 1.9f, g.y + dy - 1.9f, g.x + 1.9f, g.y + dy + 1.9f)
              if (!pacMazeWalls.any { it.left < gRectY.right && it.right > gRectY.left && it.top < gRectY.bottom && it.bottom > gRectY.top }) {
                  newGy = g.y + dy
                  moved = true
              }
          }
          
          if (!moved && !tryXFirst && dx != 0f) {
              val gRectX = Rect(g.x + dx - 1.9f, g.y - 1.9f, g.x + dx + 1.9f, g.y + 1.9f)
              if (!pacMazeWalls.any { it.left < gRectX.right && it.right > gRectX.left && it.top < gRectX.bottom && it.bottom > gRectX.top }) {
                  newGx = g.x + dx
                  moved = true
              }
          }
          
          Offset(newGx, newGy)
      }
      }
      
      // Check Food
      val remainingFood = pacFoodList.filter { food ->
          Math.abs(pacPlayer.x - food.x) > 2f || Math.abs(pacPlayer.y - food.y) > 2f
      }
      if (remainingFood.size < pacFoodList.size) {
          pacScore += 10 * (pacFoodList.size - remainingFood.size)
          pacFoodList = remainingFood
          SoundEffects.playTick()
      }
      
      if (pacFoodList.isEmpty() && pacMazeWalls.isNotEmpty()) {
          pacWin = true
          SoundEffects.playScore()
      }
      
      // Check Death
      if (pacGhosts.any { Math.abs(it.x - pacPlayer.x) < 2f && Math.abs(it.y - pacPlayer.y) < 2f }) {
          pacGameOver = true
          SoundEffects.playExplosion()
      }
    }
  }
  // Dedicated Snake Loop (requires custom speed tick controls)
  LaunchedEffect(currentGame, snakeGameOver, snakeSpeedMultiplier, showInfoOnScreen) {
    while (currentGame == RetroGame.PIXEL_PYTHON && !snakeGameOver && !showInfoOnScreen) {
      val baseTickDelay = 160L
      val actualDelay = (baseTickDelay / snakeSpeedMultiplier).toLong().coerceAtLeast(50L)
      delay(actualDelay)

      val head = snakeBody.first()
      // Warp boundaries (classic Game wrapping screen)
      var nextX = head.x + snakeDir.x
      var nextY = head.y + snakeDir.y

      if (nextX < 0f) nextX = 78f
      if (nextX > 78f) nextX = 0f
      if (nextY < 12f) nextY = 58f
      if (nextY > 58f) nextY = 12f

      val nextHead = Offset(nextX, nextY)

      // Crash into self check
      if (snakeBody.contains(nextHead)) {
        snakeGameOver = true
        SoundEffects.playLoseLife()
        if (snakeScore > highScoreSnake) {
          highScoreSnake = snakeScore
          prefs.edit().putInt("SNAKE_HS", snakeScore).apply()
        }
        continue
      }

      val newBody = mutableListOf<Offset>()
      newBody.add(nextHead)
      newBody.addAll(snakeBody)

      // Food Eaten Check (within proximity)
      if (Math.abs(nextHead.x - snakeFood.x) < 3f && Math.abs(nextHead.y - snakeFood.y) < 3f) {
        snakeScore += 10
        SoundEffects.playScore()

        // Spawn food particles
        val newParticles = (0..7).map { idx ->
          val angle = idx * (2 * Math.PI / 8)
          GameParticle(
            x = snakeFood.x,
            y = snakeFood.y,
            vx = Math.cos(angle).toFloat() * 1.3f,
            vy = Math.sin(angle).toFloat() * 1.3f,
            life = 7
          )
        }
        snakeParticles = snakeParticles + newParticles

        // Generate next food (within grid spacing)
        var foodX = (4..74).random().toFloat()
        var foodY = (16..54).random().toFloat()
        // Align to grid of 2
        foodX = (foodX.toInt() / 2 * 2).toFloat()
        foodY = (foodY.toInt() / 2 * 2).toFloat()
        snakeFood = Offset(foodX, foodY)
      } else {
        newBody.removeAt(newBody.lastIndex)
      }

      snakeBody = newBody

      // Update snake particles
      snakeParticles = snakeParticles.map {
        it.copy(x = it.x + it.vx, y = it.y + it.vy, life = it.life - 1)
      }.filter { it.life > 0 }
    }
  }

  // Master UI Scaffold (handheld dark body)
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0C0C0C)) // Outer console plastic chassis
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // --- LEFT COLUMN: D-PAD & SELECT ---
      Column(
        modifier = Modifier
          .width(160.dp)
          .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
      ) {
        // 1. SELECT button: Square outline style
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = "SELECT",
            color = RetroLightGray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
          )
          Box(
            modifier = Modifier
              .size(width = 64.dp, height = 34.dp)
              .border(2.dp, RetroWhite, RectangleShape) // Square outline
              .background(if (btnSelectPressed) RetroWhite else Color.Black)
              .then(glitchModifier)
              .pointerInput(Unit) {
                detectTapGestures(
                  onPress = {
                    if (!handleButtonPress()) {
                      btnSelectPressed = true
                      SoundEffects.playTick()
                      // Cycle game selection
                      val allGames = RetroGame.values()
                      val nextIdx = (currentGame.ordinal + 1) % allGames.size
                      currentGame = allGames[nextIdx]
                      showInfoOnScreen = true // Open instruction cards
                      try {
                        awaitRelease()
                      } finally {
                        btnSelectPressed = false
                      }
                    }
                  }
                )
              },
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(width = 20.dp, height = 8.dp)
                .background(if (btnSelectPressed) Color.Black else RetroWhite)
            )
          }
        }

        // 2. D-PAD AREA
        Box(
          modifier = Modifier
            .size(136.dp)
            .border(1.dp, Color(0xFF222222), CircleShape) // Back plate bezel circle
            .padding(4.dp),
          contentAlignment = Alignment.Center
        ) {
          Box(modifier = Modifier.size(122.dp)) {
            // UP Arrow button (Top Center)
            RetroDpadButton(
              text = "▲",
              isPressed = dpadUpPressed,
              onPressStateChange = { pressed ->
                dpadUpPressed = pressed
                if (pressed) {
                  if (!handleButtonPress()) {
                    if (currentGame == RetroGame.FROG_HOPPER && !frogGameOver) { frogPlayer = frogPlayer.copy(y = (frogPlayer.y - 10f).coerceAtLeast(0f)); SoundEffects.playTick() }
                    if (currentGame == RetroGame.PIXEL_PYTHON && snakeDir.y == 0f) {
                      snakeDir = Offset(0f, -2f)
                    }
                  }
                }
              },
              modifier = Modifier
                .size(38.dp)
                .align(Alignment.TopCenter)
                .then(glitchModifier)
            )

            // LEFT Arrow button (Center Left)
            RetroDpadButton(
              text = "◀",
              isPressed = dpadLeftPressed,
              onPressStateChange = { pressed ->
                dpadLeftPressed = pressed
                if (pressed) {
                  if (!handleButtonPress()) {
                    if (currentGame == RetroGame.PIXEL_PYTHON && snakeDir.x == 0f) {
                      snakeDir = Offset(-2f, 0f)
                    }
                  }
                }
              },
              modifier = Modifier
                .size(38.dp)
                .align(Alignment.CenterStart)
                .then(glitchModifier)
            )

            // Dynamic Decorative Center square
            Box(
              modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center)
                .border(1.dp, RetroGray, RectangleShape)
            )

            // RIGHT Arrow button (Center Right)
            RetroDpadButton(
              text = "▶",
              isPressed = dpadRightPressed,
              onPressStateChange = { pressed ->
                dpadRightPressed = pressed
                if (pressed) {
                  if (!handleButtonPress()) {
                    if (currentGame == RetroGame.PIXEL_PYTHON && snakeDir.x == 0f) {
                      snakeDir = Offset(2f, 0f)
                    }
                  }
                }
              },
              modifier = Modifier
                .size(38.dp)
                .align(Alignment.CenterEnd)
                .then(glitchModifier)
            )

            // DOWN Arrow button (Bottom Center)
            RetroDpadButton(
              text = "▼",
              isPressed = dpadDownPressed,
              onPressStateChange = { pressed ->
                dpadDownPressed = pressed
                if (pressed) {
                  if (!handleButtonPress()) {
                    if (currentGame == RetroGame.FROG_HOPPER && !frogGameOver) { frogPlayer = frogPlayer.copy(y = (frogPlayer.y + 10f).coerceAtMost(60f)); SoundEffects.playTick() }
                    if (currentGame == RetroGame.PIXEL_PYTHON && snakeDir.y == 0f) {
                      snakeDir = Offset(0f, 2f)
                    }
                  }
                }
              },
              modifier = Modifier
                .size(38.dp)
                .align(Alignment.BottomCenter)
                .then(glitchModifier)
            )
          }
        }
      }

      // --- CENTER COLUMN: BRANDING AND SCREEN ---
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        // Game Screen
        Box(
          modifier = Modifier
            .fillMaxHeight(0.95f)
            .aspectRatio(1.33f)
            .border(4.dp, RetroWhite) // Custom bold white outline screen border
            .background(Color.Black)
            .padding(4.dp)
        ) {
          val activeColor = activeTheme.screenColor

          Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleX = size.width / 80f
            val scaleY = size.height / 60f

            // Back buffer solid black
            drawRect(Color.Black)

            // ----------------- RENDER SCREEN STATUS HEADER -----------------
            // Header boundary line
            drawLine(
              color = activeColor,
              start = Offset(0f, 10f * scaleY),
              end = Offset(size.width, 10f * scaleY),
              strokeWidth = 1f * scaleY
            )
          }

          // Beautiful Compose Overlay for HUD & Scores, keeping text razor sharp and theme-colored
          Column(modifier = Modifier.fillMaxSize()) {
            // Status bar Row
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(horizontal = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "[${currentGame.title.take(10)}]",
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (soundMuted) "🔇" else "🔊",
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.pointerInput(Unit) {
                  detectTapGestures {
                    soundMuted = SoundEffects.toggleMute()
                  }
                }
              )
              Text(
                text = when (currentGame) {
                  RetroGame.PIXEL_PYTHON -> "S:${snakeScore}"
                  RetroGame.BREAKOUT -> "L:${breakoutLives} S:${breakoutScore}"

                  RetroGame.SPACE_DEFENDER -> "S:${defenderScore}"
                  RetroGame.PONG_TENNIS -> "S:${pongScore}"
                  RetroGame.PAC_MAZE -> "S:${pacScore}"
                  RetroGame.TOWER_BUILDER -> "S:${towerScore}"
                  RetroGame.LUNAR_LANDER -> "S:${landerScore}"
                  RetroGame.RACING_CAR -> "S:${racingScore}"
                  RetroGame.PLATFORMER -> "S:${platScore}"
                  RetroGame.FROG_HOPPER -> "S:${frogScore}"
                  else -> "SCORE: 0"
                },
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }

            // Main display box
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                  detectTapGestures {
                    if (isStartupActive) {
                      isStartupActive = false
                      SoundEffects.playTick()
                    }
                  }
                }
            ) {
              if (isStartupActive) {
                // START-UP INTRO DISPLAY
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val scaleX = size.width / 80f
                  val scaleY = size.height / 60f
                  val centerX = 40f
                  val centerY = 30f

                  for (star in introStars) {
                    val px = centerX + (star.x / star.z) * 40f
                    val py = centerY + (star.y / star.z) * 30f
                    if (px in 0f..80f && py in 0f..60f) {
                      val starSize = if (star.z < 25f) 1.5f else 0.8f
                      drawRect(
                        color = activeColor,
                        topLeft = Offset((px - starSize / 2f) * scaleX, (py - starSize / 2f) * scaleY),
                        size = Size(starSize * scaleX, starSize * scaleY)
                      )
                    }
                  }
                }

                // Overlaid text content
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  // Beautiful retro logo with border
                  Box(
                    modifier = Modifier
                      .border(2.dp, activeColor)
                      .background(Color.Black.copy(alpha = 0.85f))
                      .padding(horizontal = 14.dp, vertical = 6.dp)
                  ) {
                    Text(
                      text = "★ RETRO CORE 2600 ★",
                      color = activeColor,
                      fontFamily = FontFamily.Monospace,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 1.sp
                    )
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  Text(
                    text = "INITIALIZING SYSTEM CHIP...",
                    color = activeColor.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                  )

                  Spacer(modifier = Modifier.height(18.dp))

                  if (introFlashText) {
                    Text(
                      text = "PRESS ANY KEY TO PLAY",
                      color = activeColor,
                      fontFamily = FontFamily.Monospace,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 1.sp
                    )
                  }
                }
              } else if (showInfoOnScreen) {
                // INSTRUCTION SCREEN DISPLAY
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    text = "GAME SELECTION",
                    color = activeColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                  )
                  Text(
                    text = "PRESS [SELECT] TO CYCLE GAMES\nPRESS [STAR] TO LAUNCH GAME",
                    color = activeColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                  )

                  Spacer(modifier = Modifier.height(4.dp))

                  Text(
                    text = when (currentGame) {
                      RetroGame.PIXEL_PYTHON -> "PYTHON: D-pad to steer snake.\nHold A for SPEED TURBO boost!"
                      RetroGame.BREAKOUT -> "BREAKOUT: D-pad L/R to slide paddle.\nPress A to boost ball speed. B for normal!"
                      else -> "${currentGame.title}\nPress START to play"
                    },
                    color = activeColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp
                  )

                  Spacer(modifier = Modifier.height(8.dp))

                  Text(
                    text = when (currentGame) {
                      RetroGame.PIXEL_PYTHON -> "HIGH SCORE: $highScoreSnake"
                      RetroGame.BREAKOUT -> "HIGH SCORE: $highScoreBreakout"
                      else -> "HIGH SCORE: 0"
                    },
                    color = activeColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                  )
                }
              } else {
                // ACTIVE GAME DRAW OVERLAYS
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val scaleX = size.width / 80f
                  val scaleY = size.height / 60f

                  when (currentGame) {

                    RetroGame.SPACE_DEFENDER -> {
                      if (defenderGameOver) return@Canvas
                      drawRect(activeColor.copy(alpha=0.4f), Offset(2f*scaleX,12f*scaleY), Size(76f*scaleX,46f*scaleY), style=Stroke(width=1f))
                      
                      // Player ship (hollow triangle-like)
                      val path = androidx.compose.ui.graphics.Path().apply {
                          moveTo((defenderPlayerX + 3f) * scaleX, 48f * scaleY)
                          lineTo(defenderPlayerX * scaleX, 54f * scaleY)
                          lineTo((defenderPlayerX + 6f) * scaleX, 54f * scaleY)
                          close()
                      }
                      drawPath(path, activeColor, style = Stroke(width = 1.5f * scaleX))
                      
                      // Lasers
                      defenderLasers.forEach { 
                          drawLine(activeColor, Offset(it.x * scaleX, it.y * scaleY), Offset(it.x * scaleX, (it.y + 3f) * scaleY), strokeWidth = 1.5f * scaleX) 
                      }
                      
                      // Enemies
                      defenderEnemies.forEach { 
                          drawRect(activeColor, Offset(it.x * scaleX, it.y * scaleY), Size(4f * scaleX, 4f * scaleY), style = Stroke(width = 1.2f * scaleX)) 
                          drawLine(activeColor, Offset((it.x+1f) * scaleX, (it.y+1f) * scaleY), Offset((it.x+3f) * scaleX, (it.y+3f) * scaleY), strokeWidth = 1f)
                          drawLine(activeColor, Offset((it.x+3f) * scaleX, (it.y+1f) * scaleY), Offset((it.x+1f) * scaleX, (it.y+3f) * scaleY), strokeWidth = 1f)
                      }
                    }
                                                RetroGame.PONG_TENNIS -> {
                      if (pongGameOver) return@Canvas
                      drawRect(activeColor.copy(alpha=0.4f), Offset(2f*scaleX,12f*scaleY), Size(76f*scaleX,46f*scaleY), style=Stroke(width=1f))
                      // Center line
                      drawLine(activeColor.copy(alpha=0.3f), Offset(40f*scaleX, 12f*scaleY), Offset(40f*scaleX, 58f*scaleY), strokeWidth=1f, pathEffect=androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
                      // Player paddle
                      drawRect(activeColor, Offset(4f*scaleX, (pongPlayerY-4f)*scaleY), Size(2f*scaleX, 8f*scaleY))
                      // Enemy paddle
                      drawRect(activeColor, Offset(74f*scaleX, (pongEnemyY-4f)*scaleY), Size(2f*scaleX, 8f*scaleY))
                      // Ball
                      drawRect(activeColor, Offset((pongBall.x-1f)*scaleX, (pongBall.y-1f)*scaleY), Size(2f*scaleX, 2f*scaleY))
                    }
                    RetroGame.PAC_MAZE -> {
                      if (pacGameOver) {
                          drawRect(activeColor.copy(alpha=0.2f))
                          return@Canvas
                      }
                      if (pacWin) {
                          drawRect(activeColor.copy(alpha=0.2f))
                          // Draw a big star for win
                          drawCircle(activeColor, 10f * scaleX, center)
                          return@Canvas
                      }
                      
                      drawRect(activeColor, Offset(0f, 12f * scaleY), Size(80f * scaleX, 48f * scaleY), style = Stroke(width = 1f * scaleX))
                      drawRect(activeColor, Offset(4f * scaleX, 16f * scaleY), Size(72f * scaleX, 40f * scaleY), style = Stroke(width = 1f * scaleX))
                      pacMazeWalls.drop(4).forEach { wall ->
                          drawRect(activeColor, Offset(wall.left * scaleX, wall.top * scaleY), Size((wall.right - wall.left) * scaleX, (wall.bottom - wall.top) * scaleY), style = Stroke(width = 1f * scaleX))
                      }
                      
                      // Pac player (open mouth representation)
                      drawArc(activeColor, 45f, 270f, useCenter = true, topLeft = Offset((pacPlayer.x-2f) * scaleX, (pacPlayer.y-2f) * scaleY), size = Size(4f * scaleX, 4f * scaleY), style = Stroke(width = 1f * scaleX))
                      drawCircle(activeColor, 0.5f * scaleX, Offset((pacPlayer.x)*scaleX, (pacPlayer.y-1f)*scaleY))
                      
                      // Food
                      pacFoodList.forEach { food ->
                          drawCircle(activeColor, 0.5f * scaleX, Offset(food.x * scaleX, food.y * scaleY))
                          drawCircle(activeColor.copy(alpha=0.3f), 1.5f * scaleX, Offset(food.x * scaleX, food.y * scaleY), style = Stroke(width = 0.5f))
                      }
                      
                      // Ghosts (wavy bottom or just rectangle with eyes)
                      pacGhosts.forEach { 
                          drawRect(activeColor, Offset((it.x-2f) * scaleX, (it.y-2f) * scaleY), Size(4f * scaleX, 4f * scaleY), style = Stroke(width = 1f * scaleX)) 
                          drawCircle(activeColor, 0.5f * scaleX, Offset((it.x-0.8f) * scaleX, (it.y-0.8f) * scaleY))
                          drawCircle(activeColor, 0.5f * scaleX, Offset((it.x+0.8f) * scaleX, (it.y-0.8f) * scaleY))
                      }
                    }
                                                RetroGame.TOWER_BUILDER -> {
                      if (towerGameOver) return@Canvas
                      drawRect(activeColor.copy(alpha=0.4f), Offset(2f*scaleX,12f*scaleY), Size(76f*scaleX,46f*scaleY), style=Stroke(width=1f))
                      val currentY = if (towerBlocks.isEmpty()) 55f else towerBlocks.last().top - 5f
                      val offsetDown = maxOf(0f, 40f - currentY)
                      towerBlocks.forEach { b ->
                          drawRect(activeColor, Offset(b.left*scaleX, (b.top + offsetDown)*scaleY), Size((b.right-b.left)*scaleX, (b.bottom-b.top)*scaleY))
                          // Inner detail to look like a construction block
                          drawRect(activeColor.copy(alpha=0.5f), Offset((b.left + 0.5f)*scaleX, (b.top + offsetDown + 0.5f)*scaleY), Size((b.right-b.left - 1f)*scaleX, (b.bottom-b.top - 1f)*scaleY), style=Stroke(width=1f))
                          drawLine(activeColor.copy(alpha=0.3f), Offset(b.left*scaleX, (b.top + offsetDown)*scaleY), Offset(b.right*scaleX, (b.bottom + offsetDown)*scaleY), strokeWidth=1f)
                      }
                      // Moving block
                      drawRect(activeColor, Offset(towerX*scaleX, (currentY + offsetDown)*scaleY), Size(towerWidth*scaleX, 5f*scaleY))
                      drawRect(activeColor.copy(alpha=0.5f), Offset((towerX + 0.5f)*scaleX, (currentY + offsetDown + 0.5f)*scaleY), Size((towerWidth - 1f)*scaleX, 4f*scaleY), style=Stroke(width=1f))
                      // Crane hook connecting to moving block
                      drawLine(activeColor, Offset((towerX + towerWidth/2f)*scaleX, 12f*scaleY), Offset((towerX + towerWidth/2f)*scaleX, (currentY + offsetDown)*scaleY), strokeWidth=0.5f*scaleX)
                    }
                                                RetroGame.LUNAR_LANDER -> {
                      if (landerGameOver) return@Canvas
                      drawRect(activeColor.copy(alpha=0.4f), Offset(2f*scaleX,12f*scaleY), Size(76f*scaleX,46f*scaleY), style=Stroke(width=1f))
                      // Terrain
                      val path = androidx.compose.ui.graphics.Path().apply {
                          moveTo(2f*scaleX, 58f*scaleY)
                          lineTo(20f*scaleX, 45f*scaleY)
                          lineTo(30f*scaleX, 56f*scaleY)
                          lineTo(50f*scaleX, 56f*scaleY) // Landing Pad
                          lineTo(65f*scaleX, 40f*scaleY)
                          lineTo(78f*scaleX, 58f*scaleY)
                      }
                      drawPath(path, activeColor, style = Stroke(width = 1.2f * scaleX))
                      // Pad marker
                      drawLine(activeColor, Offset(30f*scaleX, 56f*scaleY), Offset(50f*scaleX, 56f*scaleY), strokeWidth = 2f*scaleX)
                      // Lander
                      drawRect(activeColor, Offset((landerPos.x-2f)*scaleX, (landerPos.y-2f)*scaleY), Size(4f*scaleX, 4f*scaleY), style=Stroke(width=1.2f*scaleX))
                      drawLine(activeColor, Offset((landerPos.x-2f)*scaleX, (landerPos.y+2f)*scaleY), Offset((landerPos.x-4f)*scaleX, (landerPos.y+4f)*scaleY), strokeWidth=1f)
                      drawLine(activeColor, Offset((landerPos.x+2f)*scaleX, (landerPos.y+2f)*scaleY), Offset((landerPos.x+4f)*scaleX, (landerPos.y+4f)*scaleY), strokeWidth=1f)
                      // Engine thrust (if moving up)
                      if (landerVel.y < 0) {
                          drawRect(activeColor.copy(alpha=0.7f), Offset((landerPos.x-1f)*scaleX, (landerPos.y+2f)*scaleY), Size(2f*scaleX, 3f*scaleY))
                      }
                    }
                    RetroGame.RACING_CAR -> {
                      if (racingGameOver) return@Canvas
                      drawRect(activeColor.copy(alpha=0.4f), Offset(2f*scaleX,12f*scaleY), Size(76f*scaleX,46f*scaleY), style=Stroke(width=1f))
                      
                      // Road lanes
                      drawLine(activeColor.copy(alpha=0.3f), Offset(25f*scaleX, 12f*scaleY), Offset(25f*scaleX, 58f*scaleY), strokeWidth=1f, pathEffect=androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                      drawLine(activeColor.copy(alpha=0.3f), Offset(55f*scaleX, 12f*scaleY), Offset(55f*scaleX, 58f*scaleY), strokeWidth=1f, pathEffect=androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                      
                      // Player Car
                      drawRect(activeColor, Offset(racingCarX * scaleX, 48f * scaleY), Size(4f * scaleX, 8f * scaleY), style = Stroke(width = 1.2f * scaleX))
                      drawRect(activeColor, Offset((racingCarX+1f) * scaleX, 50f * scaleY), Size(2f * scaleX, 4f * scaleY))
                      
                      // Enemy Cars
                      racingEnemies.forEach { 
                          drawRect(activeColor, Offset(it.x * scaleX, it.y * scaleY), Size(4f * scaleX, 8f * scaleY), style = Stroke(width = 1.2f * scaleX)) 
                          drawLine(activeColor, Offset(it.x * scaleX, it.y * scaleY), Offset((it.x+4f) * scaleX, (it.y+8f) * scaleY), strokeWidth = 1f)
                          drawLine(activeColor, Offset((it.x+4f) * scaleX, it.y * scaleY), Offset(it.x * scaleX, (it.y+8f) * scaleY), strokeWidth = 1f)
                      }
                    }
                    RetroGame.PLATFORMER -> {
                      if (platGameOver) return@Canvas
                      drawRect(activeColor.copy(alpha=0.4f), Offset(2f*scaleX,12f*scaleY), Size(76f*scaleX,46f*scaleY), style=Stroke(width=1f))
                      
                      // Player
                      drawRect(activeColor, Offset((platPlayer.x - 1f) * scaleX, (platPlayer.y - 2f) * scaleY), Size(2f * scaleX, 4f * scaleY), style = Stroke(width = 1f * scaleX))
                      
                      // Platforms
                      platPlatforms.forEach { 
                          drawRect(activeColor, Offset((it.x - 6f) * scaleX, it.y * scaleY), Size(12f * scaleX, 2f * scaleY), style = Stroke(width = 1.2f * scaleX)) 
                          drawLine(activeColor, Offset((it.x - 6f) * scaleX, it.y * scaleY), Offset((it.x + 6f) * scaleX, (it.y + 2f) * scaleY), strokeWidth = 0.5f)
                      }
                    }
                    RetroGame.FROG_HOPPER -> {
                      if (frogGameOver) return@Canvas
                      drawRect(activeColor.copy(alpha=0.4f), Offset(2f*scaleX,12f*scaleY), Size(76f*scaleX,46f*scaleY), style=Stroke(width=1f))
                      
                      // Frog
                      drawCircle(activeColor, 1.5f * scaleX, Offset(frogPlayer.x * scaleX, frogPlayer.y * scaleY), style = Stroke(width = 1.2f * scaleX))
                      drawLine(activeColor, Offset((frogPlayer.x-1.5f) * scaleX, frogPlayer.y * scaleY), Offset((frogPlayer.x-2.5f) * scaleX, (frogPlayer.y+1.5f) * scaleY), strokeWidth = 1f)
                      drawLine(activeColor, Offset((frogPlayer.x+1.5f) * scaleX, frogPlayer.y * scaleY), Offset((frogPlayer.x+2.5f) * scaleX, (frogPlayer.y+1.5f) * scaleY), strokeWidth = 1f)
                      
                      // Cars
                      frogCars.forEach { 
                          drawRect(activeColor, Offset((it.x-4f) * scaleX, (it.y-2f) * scaleY), Size(8f * scaleX, 4f * scaleY), style = Stroke(width = 1.2f * scaleX)) 
                          drawCircle(activeColor, 1f * scaleX, Offset((it.x-2f) * scaleX, it.y * scaleY))
                          drawCircle(activeColor, 1f * scaleX, Offset((it.x+2f) * scaleX, it.y * scaleY))
                      }
                    }
                    RetroGame.PIXEL_PYTHON -> {
                      if (snakeGameOver) return@Canvas

                      // Bounding field outline
                      drawRect(
                        color = activeColor.copy(alpha = 0.4f),
                        topLeft = Offset(2f * scaleX, 12f * scaleY),
                        size = Size(76f * scaleX, 46f * scaleY),
                        style = Stroke(width = 1f)
                      )

                      // Draw Snake Body
                      snakeBody.forEachIndexed { idx, segment ->
                        val isHead = idx == 0
                        drawRect(
                          color = activeColor,
                          topLeft = Offset((segment.x - 1f) * scaleX, (segment.y - 1f) * scaleY),
                          size = Size(2f * scaleX, 2f * scaleY),
                          style = if (isHead) Fill else Stroke(width = 0.8f * scaleX)
                        )
                      }

                      // Draw Food (blinking)
                      if (foodBlinkToggle) {
                        drawRect(
                          color = activeColor,
                          topLeft = Offset((snakeFood.x - 1f) * scaleX, (snakeFood.y - 1f) * scaleY),
                          size = Size(2f * scaleX, 2f * scaleY)
                        )
                      }

                      // Draw snake eat sparks
                      for (p in snakeParticles) {
                        drawRect(
                          color = activeColor.copy(alpha = p.life / 7f),
                          topLeft = Offset(p.x * scaleX, p.y * scaleY),
                          size = Size(1.2f * scaleX, 1.2f * scaleY)
                        )
                      }
                    }

                    RetroGame.BREAKOUT -> {
                      if (breakoutGameOver) return@Canvas

                      // Bounding field outline
                      drawRect(
                        color = activeColor.copy(alpha = 0.4f),
                        topLeft = Offset(2f * scaleX, 12f * scaleY),
                        size = Size(76f * scaleX, 46f * scaleY),
                        style = Stroke(width = 1f)
                      )

                      // Draw Player Paddle
                      drawRect(
                        color = activeColor,
                        topLeft = Offset(breakoutPaddleX * scaleX, 53f * scaleY),
                        size = Size(14f * scaleX, 2f * scaleY),
                        style = Stroke(width = 1f * scaleY)
                      )

                      // Draw Ball
                      drawRect(
                        color = activeColor,
                        topLeft = Offset((breakoutBall.x - 1f) * scaleX, (breakoutBall.y - 1f) * scaleY),
                        size = Size(2f * scaleX, 2f * scaleY)
                      )

                      // Draw Bricks
                      for (brick in breakoutBricks) {
                        drawRect(
                          color = activeColor,
                          topLeft = Offset(brick.left * scaleX, brick.top * scaleY),
                          size = Size((brick.right - brick.left) * scaleX, (brick.bottom - brick.top) * scaleY),
                          style = Stroke(width = 0.8f * scaleX)
                        )
                      }

                      // Draw Breakout particles
                      for (p in breakoutParticles) {
                        drawRect(
                          color = activeColor.copy(alpha = p.life / 8f),
                          topLeft = Offset(p.x * scaleX, p.y * scaleY),
                          size = Size(1.1f * scaleX, 1.1f * scaleY)
                        )
                      }
                    }
                    else -> {}
                  }
                }

                // Sharp overlay for Game Over screens
                val isGameOver = when (currentGame) {
                  RetroGame.PIXEL_PYTHON -> snakeGameOver
                  RetroGame.BREAKOUT -> breakoutGameOver

                  RetroGame.SPACE_DEFENDER -> defenderGameOver
                  RetroGame.PONG_TENNIS -> pongGameOver
                  RetroGame.PAC_MAZE -> pacGameOver || pacWin
                  RetroGame.TOWER_BUILDER -> towerGameOver
                  RetroGame.LUNAR_LANDER -> landerGameOver
                  RetroGame.RACING_CAR -> racingGameOver
                  RetroGame.PLATFORMER -> platGameOver
                  RetroGame.FROG_HOPPER -> frogGameOver
                  else -> false
                }

                if (isGameOver) {
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(Color.Black.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      Text(
                        text = if (currentGame == RetroGame.PAC_MAZE && pacWin) "YOU WIN!" else "GAME OVER",
                        color = activeColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                      )

                      Spacer(modifier = Modifier.height(8.dp))

                      Text(
                        text = "FINAL SCORE: " + when (currentGame) {
                          RetroGame.PIXEL_PYTHON -> snakeScore
                          RetroGame.BREAKOUT -> breakoutScore

                          RetroGame.SPACE_DEFENDER -> defenderScore
                          RetroGame.PONG_TENNIS -> pongScore
                          RetroGame.PAC_MAZE -> pacScore
                          RetroGame.TOWER_BUILDER -> towerScore
                          RetroGame.LUNAR_LANDER -> landerScore
                          RetroGame.RACING_CAR -> racingScore
                          RetroGame.PLATFORMER -> platScore
                          RetroGame.FROG_HOPPER -> frogScore
                          else -> 0
                        },
                        color = activeColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                      )

                      Spacer(modifier = Modifier.height(10.dp))

                      Text(
                        text = "PRESS [STAR] TO RESTART",
                        color = activeColor.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                      )
                    }
                  }
                }
              }
            }

            // 3. SLEEK BOTTOM UI STATUS BAR (Inside CRT display)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(Color.Black)
                .padding(horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "LIVES: " + when (currentGame) {
                  RetroGame.PIXEL_PYTHON -> if (snakeGameOver) "00" else "01"
                  RetroGame.BREAKOUT -> "0$breakoutLives"
                  else -> "00"
                },
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "LVL: " + when (currentGame) {
                  RetroGame.PIXEL_PYTHON -> "02-A"
                  RetroGame.BREAKOUT -> "03-C"
                  else -> "01-A"
                },
                color = activeColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // 3. CRT SCREEN SCREEN SCANLINES & DUST GLASS MATTE OVERLAY (Hardware feel)
          Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {}) {
            // Horizontal scanlines
            for (y in 0 until size.height.toInt() step 5) {
              drawLine(
                color = Color.Black.copy(alpha = 0.15f),
                start = Offset(0f, y.toFloat()),
                end = Offset(size.width, y.toFloat()),
                strokeWidth = 1.2f
              )
            }

            // Screen CRT Vignette / corner shadow
            drawRect(
              brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                center = center,
                radius = size.width * 0.72f
              )
            )
          }
        }
      }

      // --- RIGHT COLUMN: ABXY BUTTONS & STAR BUTTON ---
      Column(
        modifier = Modifier
          .width(160.dp)
          .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
      ) {
        // 1. STAR button: Triangular outline button
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "STAR",
            color = RetroLightGray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
          )
          Box(
            modifier = Modifier
              .size(width = 54.dp, height = 34.dp)
              .border(2.dp, RetroWhite, TriangleShape) // Triangular shape with white outline
              .clip(TriangleShape)
              .background(if (btnStarPressed) RetroWhite else Color.Black)
              .then(glitchModifier)
              .pointerInput(Unit) {
                detectTapGestures(
                  onPress = {
                    if (!handleButtonPress()) {
                      btnStarPressed = true
                      SoundEffects.playTick()

                      if (showInfoOnScreen) {
                        // Launch current game
                        showInfoOnScreen = false
                        when (currentGame) {
                          RetroGame.PIXEL_PYTHON -> resetPixelPython()
                          RetroGame.BREAKOUT -> resetBreakout()

                          RetroGame.SPACE_DEFENDER -> resetSpaceDefender()
                          RetroGame.PONG_TENNIS -> resetPong()
                          RetroGame.PAC_MAZE -> resetPacMaze()
                          RetroGame.TOWER_BUILDER -> resetTower()
                          RetroGame.LUNAR_LANDER -> resetLander()
                          RetroGame.RACING_CAR -> resetRacing()
                          RetroGame.PLATFORMER -> resetPlatformer()
                          RetroGame.FROG_HOPPER -> resetFrogHopper()
                          else -> {}
                        }
                      } else {
                        // Restart active game or trigger demo
                        when (currentGame) {
                          RetroGame.PIXEL_PYTHON -> resetPixelPython()
                          RetroGame.BREAKOUT -> resetBreakout()

                          RetroGame.SPACE_DEFENDER -> resetSpaceDefender()
                          RetroGame.PONG_TENNIS -> resetPong()
                          RetroGame.PAC_MAZE -> resetPacMaze()
                          RetroGame.TOWER_BUILDER -> resetTower()
                          RetroGame.LUNAR_LANDER -> resetLander()
                          RetroGame.RACING_CAR -> resetRacing()
                          RetroGame.PLATFORMER -> resetPlatformer()
                          RetroGame.FROG_HOPPER -> resetFrogHopper()
                          else -> {}
                        }
                      }

                      try {
                        awaitRelease()
                      } finally {
                        btnStarPressed = false
                      }
                    }
                  }
                )
              },
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "★",
              color = if (btnStarPressed) Color.Black else RetroWhite,
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }

        // 2. ABXY AREA
        Box(
          modifier = Modifier
            .size(136.dp)
            .border(1.dp, Color(0xFF222222), CircleShape) // Back plate bezel circle
            .padding(4.dp),
          contentAlignment = Alignment.Center
        ) {
          Box(modifier = Modifier.size(122.dp)) {
            // Y Button (Top Center)
            RetroCircularButton(
              text = "Y",
              isPressed = btnYPressed,
              onPressStateChange = { pressed ->
                btnYPressed = pressed
                if (pressed) {
                  if (!handleButtonPress()) {
                    // Show game information card/instruction screen
                    showInfoOnScreen = !showInfoOnScreen
                  }
                }
              },
              modifier = Modifier
                .size(36.dp)
                .align(Alignment.TopCenter)
                .then(glitchModifier)
            )

            // X Button (Center Left)
            RetroCircularButton(
              text = "X",
              isPressed = btnXPressed,
              onPressStateChange = { pressed ->
                btnXPressed = pressed
                if (pressed) {
                  handleButtonPress()
                }
              },
              modifier = Modifier
                .size(36.dp)
                .align(Alignment.CenterStart)
                .then(glitchModifier)
            )

            // A Button (Center Right) - PRIMARY GAME ACTION
            RetroCircularButton(
              text = "A",
              isPressed = btnAPressed,
              onPressStateChange = { pressed ->
                btnAPressed = pressed
                if (pressed) {
                  if (!handleButtonPress()) {
                    when (currentGame) {
                      RetroGame.SPACE_DEFENDER -> {
                         if (!defenderGameOver && !showInfoOnScreen) { defenderLasers = defenderLasers + Offset(defenderPlayerX + 2.5f, 50f); SoundEffects.playShoot() }
                      }
                      RetroGame.TOWER_BUILDER -> {
                         if (!towerGameOver && !showInfoOnScreen) {
                             val prev = if (towerBlocks.isEmpty()) Rect(0f, 55f, 80f, 60f) else towerBlocks.last()
                             val currentLeft = towerX
                             val currentRight = towerX + towerWidth
                             val overlapLeft = maxOf(prev.left, currentLeft)
                             val overlapRight = minOf(prev.right, currentRight)
                             if (overlapLeft < overlapRight) {
                                 towerWidth = overlapRight - overlapLeft
                                 towerBlocks = towerBlocks + Rect(overlapLeft, prev.top - 5f, overlapRight, prev.top)
                                 towerX = 0f
                                 towerDir = (Math.abs(towerDir) + 0.15f) * (if (towerBlocks.size % 2 == 0) 1f else -1f)
                                 towerScore += 10
                                 SoundEffects.playTick()
                             } else {
                                 towerGameOver = true
                                 SoundEffects.playExplosion()
                             }
                         }
                      }
                      RetroGame.LUNAR_LANDER -> {
                         if (!landerGameOver && !showInfoOnScreen && landerFuel > 0) { landerVel = landerVel.copy(y = landerVel.y - 1f); landerFuel -= 2; SoundEffects.playShoot() }
                      }
                      RetroGame.BREAKOUT -> {
                        if (!breakoutGameOver && !showInfoOnScreen) {
                          breakoutBallVel = breakoutBallVel.copy(x = breakoutBallVel.x * 1.5f, y = breakoutBallVel.y * 1.5f)
                        }
                      }
                      RetroGame.PLATFORMER -> {
                        if (!platGameOver && !showInfoOnScreen && platVelY == 0f) { platVelY = -4f; SoundEffects.playTick() }
                      }
                      else -> {}
                    }
                  }
                }
              },
              modifier = Modifier
                .size(36.dp)
                .align(Alignment.CenterEnd)
                .then(glitchModifier)
            )
            // B Button (Bottom Center) - SECONDARY GAME ACTION
            RetroCircularButton(
              text = "B",
              isPressed = btnBPressed,
              onPressStateChange = { pressed ->
                btnBPressed = pressed
                if (pressed) {
                  if (!handleButtonPress()) {
                    when (currentGame) {
                      RetroGame.BREAKOUT -> {
                        if (!breakoutGameOver && !showInfoOnScreen) {
                          // Slow down breakout ball speed to baseline
                          val vx = if (breakoutBallVel.x > 0) 1.2f else -1.2f
                          val vy = if (breakoutBallVel.y > 0) 1.2f else -1.2f
                          breakoutBallVel = Offset(vx, vy)
                          SoundEffects.playTick()
                        }
                      }
                      RetroGame.PIXEL_PYTHON -> {
                        // Freeze Snake position briefly / Pause game toggle
                        showInfoOnScreen = true
                      }
                      else -> {}
                    }
                  }
                }
              },
              modifier = Modifier
                .size(36.dp)
                .align(Alignment.BottomCenter)
                .then(glitchModifier)
            )
          }
        }
      }
    }
  }
}

// Reusable Retro D-pad Button widget (Square outline with arrow label)
@Composable
fun RetroDpadButton(
  text: String,
  isPressed: Boolean,
  onPressStateChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .border(2.dp, RetroWhite, RectangleShape) // Clear square outline
      .background(if (isPressed) RetroWhite else Color.Black)
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            onPressStateChange(true)
            SoundEffects.playTick()
            try {
              awaitRelease()
            } finally {
              onPressStateChange(false)
            }
          }
        )
      },
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      color = if (isPressed) Color.Black else RetroWhite,
      fontFamily = FontFamily.Monospace,
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

// Reusable Retro Circular Button widget (Round outline circle with value label)
@Composable
fun RetroCircularButton(
  text: String,
  isPressed: Boolean,
  onPressStateChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .border(2.dp, RetroWhite, CircleShape) // Clear circular outline
      .clip(CircleShape)
      .background(if (isPressed) RetroWhite else Color.Black)
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            onPressStateChange(true)
            SoundEffects.playTick()
            try {
              awaitRelease()
            } finally {
              onPressStateChange(false)
            }
          }
        )
      },
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      color = if (isPressed) Color.Black else RetroWhite,
      fontFamily = FontFamily.Monospace,
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

