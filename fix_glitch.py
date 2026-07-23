import sys

def run():
    with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
        content = f.read()

    # Update the glitch loop to last for 3 seconds
    old_glitch_loop = """  LaunchedEffect(isGameOver) {
      if (isGameOver) {
          while (true) {
              glitchOffsetX = (-3..3).random().toFloat()
              glitchOffsetY = (-3..3).random().toFloat()
              kotlinx.coroutines.delay(50)
          }
      } else {
          glitchOffsetX = 0f
          glitchOffsetY = 0f
      }
  }"""
    
    new_glitch_loop = """  var isGlitching by remember { mutableStateOf(false) }
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
  }"""

    content = content.replace(old_glitch_loop, new_glitch_loop)
    
    # Update the val glitchModifier definition
    old_glitch_modifier_def = "  val glitchModifier = if (isGameOver) {"
    new_glitch_modifier_def = "  val glitchModifier = if (isGlitching) {"
    content = content.replace(old_glitch_modifier_def, new_glitch_modifier_def)

    # Add glitchModifier to STAR button
    star_btn = ".background(if (btnStarPressed) RetroWhite else Color.Black)\\n              .pointerInput(Unit)"
    star_btn_new = ".background(if (btnStarPressed) RetroWhite else Color.Black)\\n              .then(glitchModifier)\\n              .pointerInput(Unit)"
    # Using string manipulation directly, so watch out for literal \n
    content = content.replace(".background(if (btnStarPressed) RetroWhite else Color.Black)\n              .pointerInput(Unit)", 
                              ".background(if (btnStarPressed) RetroWhite else Color.Black)\n              .then(glitchModifier)\n              .pointerInput(Unit)")

    with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
        f.write(content)

run()
