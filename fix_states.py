import sys, re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Replace block 1
content = re.sub(
    r'  var tetrisBlock by remember \{ mutableStateOf\(androidx\.compose\.ui\.geometry\.Offset\(40f, 0f\)\) \}\n  var tetrisStack by remember \{ mutableStateOf\(emptyList<androidx\.compose\.ui\.geometry\.Offset>\(\)\) \}\n  var tetrisScore by remember \{ mutableStateOf\(0\) \}\n  var tetrisGameOver by remember \{ mutableStateOf\(false\) \}',
    """  var pongBall by remember { mutableStateOf(Offset(40f, 30f)) }
  var pongVel by remember { mutableStateOf(Offset(1.5f, 1.5f)) }
  var pongPlayerY by remember { mutableStateOf(30f) }
  var pongEnemyY by remember { mutableStateOf(30f) }
  var pongScore by remember { mutableStateOf(0) }
  var pongGameOver by remember { mutableStateOf(false) }""", content, flags=re.DOTALL)

# Replace block 2
content = re.sub(
    r'  var flappyY by remember \{ mutableStateOf\(30f\) \}\n  var flappyVel by remember \{ mutableStateOf\(0f\) \}\n  var flappyPipes by remember \{ mutableStateOf\(listOf\(androidx\.compose\.ui\.geometry\.Offset\(80f, 30f\)\)\) \}\n  var flappyScore by remember \{ mutableStateOf\(0\) \}\n  var flappyGameOver by remember \{ mutableStateOf\(false\) \}',
    """  var dinoY by remember { mutableStateOf(50f) }
  var dinoVel by remember { mutableStateOf(0f) }
  var dinoCactus by remember { mutableStateOf(listOf(Offset(80f, 50f))) }
  var dinoScore by remember { mutableStateOf(0) }
  var dinoGameOver by remember { mutableStateOf(false) }""", content, flags=re.DOTALL)

# Replace block 3
content = re.sub(
    r'  var asteroidPlayer by remember \{ mutableStateOf\(androidx\.compose\.ui\.geometry\.Offset\(40f, 30f\)\) \}\n  var asteroidsList by remember \{ mutableStateOf\(listOf\(androidx\.compose\.ui\.geometry\.Offset\(10f, 10f\), androidx\.compose\.ui\.geometry\.Offset\(70f, 50f\)\)\) \}\n  var asteroidScore by remember \{ mutableStateOf\(0\) \}\n  var asteroidGameOver by remember \{ mutableStateOf\(false\) \}',
    """  var landerPos by remember { mutableStateOf(Offset(40f, 10f)) }
  var landerVel by remember { mutableStateOf(Offset(0f, 0f)) }
  var landerFuel by remember { mutableStateOf(100) }
  var landerScore by remember { mutableStateOf(0) }
  var landerGameOver by remember { mutableStateOf(false) }""", content, flags=re.DOTALL)


with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

