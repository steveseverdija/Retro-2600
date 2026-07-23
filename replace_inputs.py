import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

old_inputs = """        RetroGame.TETRIS_CLONE -> {
          if (dpadLeftPressed) tetrisBlock = tetrisBlock.copy(x = (tetrisBlock.x - 2f).coerceAtLeast(0f))
          if (dpadRightPressed) tetrisBlock = tetrisBlock.copy(x = (tetrisBlock.x + 2f).coerceAtMost(80f))
        }
        RetroGame.PAC_MAZE -> {
          if (dpadLeftPressed) pacPlayer = pacPlayer.copy(x = (pacPlayer.x - 1f).coerceAtLeast(0f))
          if (dpadRightPressed) pacPlayer = pacPlayer.copy(x = (pacPlayer.x + 1f).coerceAtMost(80f))
          if (dpadUpPressed) pacPlayer = pacPlayer.copy(y = (pacPlayer.y - 1f).coerceAtLeast(0f))
          if (dpadDownPressed) pacPlayer = pacPlayer.copy(y = (pacPlayer.y + 1f).coerceAtMost(60f))
        }
        RetroGame.FLAPPY_BIRD -> {
          if (btnSelectPressed || btnStartPressed) flappyVel = -3f
        }
        RetroGame.ASTEROIDS -> {
          if (dpadLeftPressed) asteroidPlayer = asteroidPlayer.copy(x = (asteroidPlayer.x - 1f).coerceAtLeast(0f))
          if (dpadRightPressed) asteroidPlayer = asteroidPlayer.copy(x = (asteroidPlayer.x + 1f).coerceAtMost(80f))
          if (dpadUpPressed) asteroidPlayer = asteroidPlayer.copy(y = (asteroidPlayer.y - 1f).coerceAtLeast(0f))
          if (dpadDownPressed) asteroidPlayer = asteroidPlayer.copy(y = (asteroidPlayer.y + 1f).coerceAtMost(60f))
        }"""

# Since I already changed the ENUM names!
content = content.replace("RetroGame.TETRIS_CLONE", "RetroGame.PONG_TENNIS")
content = content.replace("RetroGame.FLAPPY_BIRD", "RetroGame.DINO_RUNNER")
content = content.replace("RetroGame.ASTEROIDS", "RetroGame.LUNAR_LANDER")

new_inputs = """        RetroGame.PONG_TENNIS -> {
          if (dpadUpPressed) pongPlayerY = (pongPlayerY - 2f).coerceAtLeast(14f)
          if (dpadDownPressed) pongPlayerY = (pongPlayerY + 2f).coerceAtMost(54f)
        }
        RetroGame.PAC_MAZE -> {
          if (dpadLeftPressed) pacPlayer = pacPlayer.copy(x = (pacPlayer.x - 1f).coerceAtLeast(0f))
          if (dpadRightPressed) pacPlayer = pacPlayer.copy(x = (pacPlayer.x + 1f).coerceAtMost(80f))
          if (dpadUpPressed) pacPlayer = pacPlayer.copy(y = (pacPlayer.y - 1f).coerceAtLeast(0f))
          if (dpadDownPressed) pacPlayer = pacPlayer.copy(y = (pacPlayer.y + 1f).coerceAtMost(60f))
        }
        RetroGame.DINO_RUNNER -> {
          if ((dpadUpPressed || btnSelectPressed || btnStartPressed) && dinoY >= 50f) {
             dinoVel = -5f
             SoundEffects.playShoot()
          }
        }
        RetroGame.LUNAR_LANDER -> {
          if (dpadLeftPressed && landerFuel > 0) { landerVel = landerVel.copy(x = landerVel.x - 0.2f); landerFuel-- }
          if (dpadRightPressed && landerFuel > 0) { landerVel = landerVel.copy(x = landerVel.x + 0.2f); landerFuel-- }
          if (dpadUpPressed && landerFuel > 0) { landerVel = landerVel.copy(y = landerVel.y - 0.3f); landerFuel-- }
        }"""

# Because of the previous regex replacing, wait. The above might fail. Let's do it manually with find/replace or just rewrite the `handleInput` function block.
with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
