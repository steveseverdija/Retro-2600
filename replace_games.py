import sys

def replace_between(text, start_marker, end_marker, replacement):
    start_idx = text.find(start_marker)
    if start_idx == -1: return text
    end_idx = text.find(end_marker, start_idx)
    if end_idx == -1: return text
    end_idx += len(end_marker)
    return text[:start_idx] + replacement + text[end_idx:]

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# 1. State Declarations
old_states = """  var tetrisBlock by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(40f, 0f)) }
  var tetrisStack by remember { mutableStateOf(emptyList<androidx.compose.ui.geometry.Offset>()) }
  var tetrisScore by remember { mutableStateOf(0) }
  var tetrisGameOver by remember { mutableStateOf(false) }

  var pacPlayer by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(40f, 30f)) }
  var pacGhosts by remember { mutableStateOf(listOf(androidx.compose.ui.geometry.Offset(10f, 10f), androidx.compose.ui.geometry.Offset(70f, 50f))) }
  var pacFood by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(20f, 20f)) }
  var pacScore by remember { mutableStateOf(0) }
  var pacGameOver by remember { mutableStateOf(false) }

  var flappyY by remember { mutableStateOf(30f) }
  var flappyVel by remember { mutableStateOf(0f) }
  var flappyPipes by remember { mutableStateOf(listOf(androidx.compose.ui.geometry.Offset(80f, 30f))) }
  var flappyScore by remember { mutableStateOf(0) }
  var flappyGameOver by remember { mutableStateOf(false) }

  var asteroidPlayer by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(40f, 30f)) }
  var asteroidsList by remember { mutableStateOf(listOf(androidx.compose.ui.geometry.Offset(10f, 10f), androidx.compose.ui.geometry.Offset(70f, 50f))) }
  var asteroidScore by remember { mutableStateOf(0) }
  var asteroidGameOver by remember { mutableStateOf(false) }"""

new_states = """  var pongBall by remember { mutableStateOf(Offset(40f, 30f)) }
  var pongVel by remember { mutableStateOf(Offset(1.5f, 1.5f)) }
  var pongPlayerY by remember { mutableStateOf(30f) }
  var pongEnemyY by remember { mutableStateOf(30f) }
  var pongScore by remember { mutableStateOf(0) }
  var pongGameOver by remember { mutableStateOf(false) }

  var pacPlayer by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(40f, 30f)) }
  var pacGhosts by remember { mutableStateOf(listOf(androidx.compose.ui.geometry.Offset(10f, 10f), androidx.compose.ui.geometry.Offset(70f, 50f))) }
  var pacFood by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(20f, 20f)) }
  var pacScore by remember { mutableStateOf(0) }
  var pacGameOver by remember { mutableStateOf(false) }

  var dinoY by remember { mutableStateOf(50f) }
  var dinoVel by remember { mutableStateOf(0f) }
  var dinoCactus by remember { mutableStateOf(listOf(Offset(80f, 50f))) }
  var dinoScore by remember { mutableStateOf(0) }
  var dinoGameOver by remember { mutableStateOf(false) }

  var landerPos by remember { mutableStateOf(Offset(40f, 10f)) }
  var landerVel by remember { mutableStateOf(Offset(0f, 0f)) }
  var landerFuel by remember { mutableStateOf(100) }
  var landerScore by remember { mutableStateOf(0) }
  var landerGameOver by remember { mutableStateOf(false) }"""
content = content.replace(old_states, new_states)

# 2. isGameOver Flag mapping
content = content.replace("RetroGame.PONG_TENNIS -> tetrisGameOver", "RetroGame.PONG_TENNIS -> pongGameOver")
content = content.replace("RetroGame.DINO_RUNNER -> flappyGameOver", "RetroGame.DINO_RUNNER -> dinoGameOver")
content = content.replace("RetroGame.LUNAR_LANDER -> asteroidGameOver", "RetroGame.LUNAR_LANDER -> landerGameOver")

# 3. reset functions
content = content.replace("fun resetTetris() { tetrisBlock = androidx.compose.ui.geometry.Offset(40f, 0f); tetrisStack = emptyList(); tetrisScore = 0; tetrisGameOver = false; showInfoOnScreen = false }", "fun resetPong() { pongBall = Offset(40f, 30f); pongVel = Offset(1.5f, 1.5f); pongPlayerY = 30f; pongEnemyY = 30f; pongScore = 0; pongGameOver = false; showInfoOnScreen = false }")
content = content.replace("fun resetFlappy() { flappyY = 30f; flappyVel = 0f; flappyPipes = listOf(androidx.compose.ui.geometry.Offset(80f, (15..45).random().toFloat())); flappyScore = 0; flappyGameOver = false; showInfoOnScreen = false }", "fun resetDino() { dinoY = 50f; dinoVel = 0f; dinoCactus = listOf(Offset(80f, 50f)); dinoScore = 0; dinoGameOver = false; showInfoOnScreen = false }")
content = content.replace("fun resetAsteroids() { asteroidPlayer = androidx.compose.ui.geometry.Offset(40f, 30f); asteroidsList = emptyList(); asteroidScore = 0; asteroidGameOver = false; showInfoOnScreen = false }", "fun resetLander() { landerPos = Offset(40f, 10f); landerVel = Offset(0f, 0f); landerFuel = 100; landerScore = 0; landerGameOver = false; showInfoOnScreen = false }")

content = content.replace("RetroGame.PONG_TENNIS -> resetTetris()", "RetroGame.PONG_TENNIS -> resetPong()")
content = content.replace("RetroGame.DINO_RUNNER -> resetFlappy()", "RetroGame.DINO_RUNNER -> resetDino()")
content = content.replace("RetroGame.LUNAR_LANDER -> resetAsteroids()", "RetroGame.LUNAR_LANDER -> resetLander()")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
