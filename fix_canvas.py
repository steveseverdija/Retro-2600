import sys, re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

pong_draw = """                    RetroGame.PONG_TENNIS -> {
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

dino_draw = """                    RetroGame.DINO_RUNNER -> {
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

lander_draw = """                    RetroGame.LUNAR_LANDER -> {
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

content = re.sub(r'RetroGame\.PONG_TENNIS -> \{.*?(?=\s*RetroGame\.PAC_MAZE)', pong_draw, content, flags=re.DOTALL)
content = re.sub(r'RetroGame\.DINO_RUNNER -> \{.*?(?=\s*RetroGame\.LUNAR_LANDER)', dino_draw, content, flags=re.DOTALL)
content = re.sub(r'RetroGame\.LUNAR_LANDER -> \{.*?(?=\s*RetroGame\.RACING_CAR)', lander_draw, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
