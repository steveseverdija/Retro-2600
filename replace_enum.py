import sys
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace('TETRIS_CLONE("TETRIS CLONE")', 'PONG_TENNIS("PONG TENNIS")')
content = content.replace('FLAPPY_BIRD("FLAPPY BIRD")', 'DINO_RUNNER("DINO RUNNER")')
content = content.replace('ASTEROIDS("ASTEROIDS")', 'LUNAR_LANDER("LUNAR LANDER")')
content = content.replace('RetroGame.TETRIS_CLONE', 'RetroGame.PONG_TENNIS')
content = content.replace('RetroGame.FLAPPY_BIRD', 'RetroGame.DINO_RUNNER')
content = content.replace('RetroGame.ASTEROIDS', 'RetroGame.LUNAR_LANDER')

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
