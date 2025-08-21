# JLine3 Simple Proof-of-Concept Plan

## Goal
Temporarily modify existing code to validate JLine3 works for keyboard input detection. Display pressed keys in top-right corner while keeping everything else exactly the same.

## Implementation (30 minutes max)

### **Step 1: Add JLine3 Dependency**
```scala
// build.sbt - Add this single line
libraryDependencies += "org.jline" % "jline-terminal" % "3.21.0"
```

### **Step 2: Temporarily Modify AnimationEngine.scala**
Add JLine3 input detection directly to the existing `AnimationEngine`:

```scala
// Add to top of AnimationEngine.scala
import org.jline.terminal.{Terminal, TerminalBuilder}

class AnimationEngine(
  // ... existing parameters stay the same
) {
  // Add these fields
  private val terminal: Terminal = TerminalBuilder.builder().system(true).build()
  private var lastKeyPressed: Option[Char] = None
  
  // Keep existing methods, just modify animate()
  private def animate(frames: LazyList[String]): Unit = {
    val clear = "\u001b[2J\u001b[H"
    frames.foreach { frame =>
      // NEW: Poll for keyboard input
      pollForInput()
      
      // NEW: Add key display to frame
      val frameWithKey = addKeyDisplay(frame)
      
      Console.print(clear)
      Console.print(frameWithKey)  // Display modified frame
      Thread.sleep(frameDelayMs)
    }
  }
  
  // NEW: Simple input polling
  private def pollForInput(): Unit = {
    try {
      terminal.enterRawMode()
      if (terminal.reader().ready()) {
        val keyCode = terminal.reader().read()
        lastKeyPressed = Some(keyCode match {
          case k if k >= 32 && k <= 126 => k.toChar  // Printable ASCII
          case 27 => '⎋'  // ESC
          case 10 | 13 => '↵'  // Enter
          case _ => '?'  // Unknown
        })
      }
    } catch {
      case _: Exception => // Ignore input errors for now
    }
  }
  
  // NEW: Add key display to existing rendered frame
  private def addKeyDisplay(frame: String): String = {
    val lines = frame.split("\n")
    if (lines.nonEmpty) {
      val keyDisplay = lastKeyPressed match {
        case Some(key) => s" Key: $key "
        case None => " Key: - "
      }
      
      // Add to first line, right side
      val firstLine = lines(0)
      val paddedLine = firstLine + (" " * Math.max(0, 60 - firstLine.length)) + keyDisplay
      
      (paddedLine +: lines.tail).mkString("\n")
    } else {
      frame
    }
  }
  
  // All other existing methods stay exactly the same
  // ...
}
```

### **Step 3: Test**
```bash
sbt compile
sbt run
# Press keys and see them appear in top-right corner
# Ctrl+C to exit
```

### **Expected Behavior**
- Cube rotates automatically (unchanged)
- Top-right shows: `Key: a`, `Key: 5`, `Key: ⎋`, etc.
- Everything else works exactly as before

### **Validation Checklist**
- [ ] Application compiles
- [ ] Cube animation works as before  
- [ ] Key presses appear in display
- [ ] No crashes or errors
- [ ] Can exit with Ctrl+C

### **After Testing**
Once we confirm JLine3 works, we'll:
1. Revert these temporary changes
2. Implement proper keyboard-controlled rotation
3. Remove the temporary key display code

## Rollback Plan
If anything goes wrong:
1. Remove the JLine3 dependency from build.sbt
2. Revert AnimationEngine.scala to original state
3. Everything back to normal

**This is just temporary validation code - not the final implementation!**

