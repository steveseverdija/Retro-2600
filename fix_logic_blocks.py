import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

bad_pong_logic = """                                    RetroGame.PONG_TENNIS -> {
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
                    }"""

good_pong_logic = """        RetroGame.PONG_TENNIS -> {
          if (!pongGameOver && !showInfoOnScreen) {
             pongBall = Offset(pongBall.x + pongVel.x, pongBall.y + pongVel.y)
             // Enemy AI
             if (pongBall.y < pongEnemyY + 4f) pongEnemyY = (pongEnemyY - 1f).coerceAtLeast(14f)
             if (pongBall.y > pongEnemyY + 4f) pongEnemyY = (pongEnemyY + 1f).coerceAtMost(54f)
             // Wall bounce
             if (pongBall.y <= 12f || pongBall.y >= 58f) pongVel = pongVel.copy(y = -pongVel.y)
             // Paddle bounce
             if (pongBall.x <= 8f && Math.abs(pongBall.y - pongPlayerY) < 6f) {
                 pongVel = pongVel.copy(x = -pongVel.x * 1.05f)
                 pongScore += 10
                 SoundEffects.playTick()
             }
             if (pongBall.x >= 72f && Math.abs(pongBall.y - pongEnemyY) < 6f) {
                 pongVel = pongVel.copy(x = -pongVel.x * 1.05f)
                 SoundEffects.playTick()
             }
             if (pongBall.x < 0f || pongBall.x > 80f) pongGameOver = true
          }
        }"""

bad_dino_logic = """                                    RetroGame.DINO_RUNNER -> {
                      if (dinoGameOver) return@Canvas
                      drawRect(activeColor.copy(alpha=0.4f), Offset(2f*scaleX,12f*scaleY), Size(76f*scaleX,46f*scaleY), style=Stroke(width=1f))
                      // Ground
                      drawLine(activeColor, Offset(2f*scaleX, 56f*scaleY), Offset(78f*scaleX, 56f*scaleY), strokeWidth=1f*scaleX)
                      // Dino
                      drawRect(activeColor, Offset(38f*scaleX, (dinoY-4f)*scaleY), Size(4f*scaleX, 6f*scaleY), style=Stroke(width=1.2f*scaleX))
                      drawRect(activeColor, Offset(40f*scaleX, (dinoY-6f)*scaleY), Size(4f*scaleX, 4f*scaleY))
                      // Cactus
                      dinoCactus.forEach { 
                          drawRect(activeColor, Offset((it.x-1.5f)*scaleX, (it.y-2f)*scaleY), Size(3f*scaleX, 8f*scaleY))
                          drawRect(activeColor, Offset((it.x-3.5f)*scaleX, (it.y-4f)*scaleY), Size(2f*scaleX, 4f*scaleY))
                          drawRect(activeColor, Offset((it.x+1.5f)*scaleX, (it.y-2f)*scaleY), Size(2f*scaleX, 4f*scaleY))
                      }
                    }"""

good_dino_logic = """        RetroGame.DINO_RUNNER -> {
          if (!dinoGameOver && !showInfoOnScreen) {
             dinoVel += 0.5f // gravity
             dinoY += dinoVel
             if (dinoY > 50f) { dinoY = 50f; dinoVel = 0f }
             dinoCactus = dinoCactus.map { it.copy(x = it.x - 2f) }.filter { it.x > -10f }
             if (dinoCactus.isEmpty() || dinoCactus.last().x < 40f && Math.random() < 0.05) {
                 dinoCactus = dinoCactus + Offset(80f, 50f)
             }
             if (dinoCactus.any { Math.abs(it.x - 40f) < 4f && Math.abs(it.y - dinoY) < 6f }) dinoGameOver = true
             dinoScore += 1
          }
        }"""

bad_lander_logic = """                                    RetroGame.LUNAR_LANDER -> {
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
                    }"""

good_lander_logic = """        RetroGame.LUNAR_LANDER -> {
          if (!landerGameOver && !showInfoOnScreen) {
             landerVel = landerVel.copy(y = landerVel.y + 0.05f) // gravity
             landerPos = Offset(landerPos.x + landerVel.x, landerPos.y + landerVel.y)
             
             // Wrap around horizontal
             if (landerPos.x < 0f) landerPos = landerPos.copy(x = 80f)
             if (landerPos.x > 80f) landerPos = landerPos.copy(x = 0f)
             
             if (landerPos.y >= 54f) { // Landing pad is at 56f
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
        }"""

content = content.replace(bad_pong_logic, good_pong_logic, 1)
content = content.replace(bad_dino_logic, good_dino_logic, 1)
content = content.replace(bad_lander_logic, good_lander_logic, 1)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

