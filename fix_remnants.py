import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("tetrisScore", "pongScore")
content = content.replace("flappyScore", "dinoScore")
content = content.replace("asteroidScore", "landerScore")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

