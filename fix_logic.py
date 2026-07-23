import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

pong_logic = """        RetroGame.PONG_TENNIS -> {
          if (!pongGameOver && !showInfoOnScreen) {
             pongBall = Offset(pongBall.x + pongVel.x, pongBall.y + pongVel.y)
             // Enemy AI
             if (pongBall.y < pongEnemyY + 4f) pongEnemyY = (pongEnemyY - 1.5f).coerceAtLeast(14f)
             if (pongBall.y > pongEnemyY + 4f) pongEnemyY = (pongEnemyY + 1.5f).coerceAtMost(54f)
             // Wall bounce
             if (pongBall.y <= 12f || pongBall.y >= 58f) pongVel = pongVel.copy(y = -pongVel.y)
             // Paddle bounce
             if (pongBall.x <= 8f && Math.abs(pongBall.y - pongPlayerY) < 6f) {
                 pongVel = pongVel.copy(x = -pongVel.x * 1.1f)
                 pongScore += 10
                 SoundEffects.playTick()
             }
             if (pongBall.x >= 72f && Math.abs(pongBall.y - pongEnemyY) < 6f) {
                 pongVel = pongVel.copy(x = -pongVel.x * 1.1f)
                 SoundEffects.playTick()
             }
             if (pongBall.x < 0f || pongBall.x > 80f) pongGameOver = true
          }
        }"""

dino_logic = """        RetroGame.DINO_RUNNER -> {
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

lander_logic = """        RetroGame.LUNAR_LANDER -> {
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

import re
content = re.sub(r'RetroGame\.PONG_TENNIS -> \{.*?(?=\s*RetroGame\.PAC_MAZE)', pong_logic, content, flags=re.DOTALL)
content = re.sub(r'RetroGame\.DINO_RUNNER -> \{.*?(?=\s*RetroGame\.LUNAR_LANDER)', dino_logic, content, flags=re.DOTALL)
content = re.sub(r'RetroGame\.LUNAR_LANDER -> \{.*?(?=\s*RetroGame\.RACING_CAR)', lander_logic, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
