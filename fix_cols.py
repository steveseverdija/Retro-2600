import sys

def run():
    with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
        content = f.read()

    content = content.replace(".fillMaxHeight().then(glitchModifier),", ".fillMaxHeight(),")
    
    with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
        f.write(content)

run()
