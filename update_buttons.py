import sys

def run():
    with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
        content = f.read()

    # Add import
    if "import androidx.compose.ui.draw.drawWithContent" not in content:
        content = content.replace("import androidx.compose.ui.draw.clip", "import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.drawWithContent")

    old_modifier = "  val glitchModifier = Modifier.offset(glitchOffsetX.dp, glitchOffsetY.dp)"
    new_modifier = """  val glitchModifier = if (isGameOver) {
      Modifier.offset(glitchOffsetX.dp, glitchOffsetY.dp).drawWithContent {
          drawContent()
          if (glitchOffsetX > 1f) {
              drawRect(Color(0x4400FFFF)) // Cyan
          } else if (glitchOffsetX < -1f) {
              drawRect(Color(0x44FF00FF)) // Magenta
          }
          if (glitchOffsetY > 2f) {
              drawLine(Color(0x66FFFFFF), Offset(0f, size.height/2f), Offset(size.width, size.height/2f), strokeWidth = 2f)
          }
      }
  } else {
      Modifier
  }"""
    
    content = content.replace(old_modifier, new_modifier)

    # Now we find all button modifier assignments
    # We can inject `.then(glitchModifier)` before `.align` for alignment-based ones, 
    # or before `.pointerInput` for the box ones.

    # 1. UP
    content = content.replace(".align(Alignment.TopCenter)\n            )", ".align(Alignment.TopCenter)\n                .then(glitchModifier)\n            )")
    
    # 2. LEFT
    content = content.replace(".align(Alignment.CenterStart)\n            )", ".align(Alignment.CenterStart)\n                .then(glitchModifier)\n            )")

    # 3. RIGHT
    content = content.replace(".align(Alignment.CenterEnd)\n            )", ".align(Alignment.CenterEnd)\n                .then(glitchModifier)\n            )")

    # 4. DOWN
    content = content.replace(".align(Alignment.BottomCenter)\n            )", ".align(Alignment.BottomCenter)\n                .then(glitchModifier)\n            )")
    
    # 5. SELECT Box
    content = content.replace(".background(if (btnSelectPressed) RetroWhite else Color.Black)\n              .pointerInput(Unit)", ".background(if (btnSelectPressed) RetroWhite else Color.Black)\n              .then(glitchModifier)\n              .pointerInput(Unit)")
    
    # 6. START Box
    content = content.replace(".background(if (btnStartPressed) RetroWhite else Color.Black)\n              .pointerInput(Unit)", ".background(if (btnStartPressed) RetroWhite else Color.Black)\n              .then(glitchModifier)\n              .pointerInput(Unit)")
    
    # 7. ABXY - Wait, ABXY have the same alignments. 
    # Y = TopCenter, X = CenterStart, A = CenterEnd, B = BottomCenter
    # So the replace above might have already modified ABXY! Let's check.
    # We replaced 4 instances? Let's verify how many `.align(...)` are there.

    with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
        f.write(content)

run()
