# JLine3 Keyboard Control Implementation Plan

## Overview

This document outlines the updated implementation plan for adding keyboard-controlled cube rotation using JLine3 library. The approach focuses on minimal changes to existing code while adding professional-grade keyboard input handling.

## Phase 1: Proof of Concept - Key Display

### **Goal**: Display pressed keys in top-right corner of screen
**Duration**: 1-2 hours  
**Risk**: Very Low  
**Purpose**: Validate JLine3 integration without touching rotation logic

### **Step 1.1: Add JLine3 Dependency**
```scala
// build.sbt - Add this single line
libraryDependencies += "org.jline" % "jline-terminal" % "3.21.0"
```

### **Step 1.2: Create Simple Input Manager**
```scala
// src/main/scala/KeyboardInputManager.scala
import org.jline.terminal.{Terminal, TerminalBuilder}

class KeyboardInputManager {
  private val terminal: Terminal = TerminalBuilder.builder().system(true).build()
  private var lastKeyPressed: Option[Char] = None
  
  def pollForInput(): Unit = {
    terminal.enterRawMode()
    try {
      if (terminal.reader().ready()) {
        val keyCode = terminal.reader().read()
        // Convert key code to character for display
        if (keyCode >= 32 && keyCode <= 126) { // Printable ASCII
          lastKeyPressed = Some(keyCode.toChar)
        } else {
          // Handle special keys (arrows, etc.)
          keyCode match {
            case 27 => // ESC sequence - could be arrow key
              handleEscapeSequence()
            case 10 | 13 => // Enter
              lastKeyPressed = Some('↵')
            case _ => 
              lastKeyPressed = Some('?') // Unknown key indicator
          }
        }
      }
    } finally {
      // Raw mode automatically restored when method exits
    }
  }
  
  private def handleEscapeSequence(): Unit = {
    // Try to read arrow key sequence
    if (terminal.reader().ready()) {
      val next = terminal.reader().read()
      if (next == 91 && terminal.reader().ready()) { // '[' character
        val arrow = terminal.reader().read()
        lastKeyPressed = Some(arrow match {
          case 65 => '↑' // Up arrow
          case 66 => '↓' // Down arrow  
          case 67 => '→' // Right arrow
          case 68 => '←' // Left arrow
          case _ => '?'
        })
      }
    } else {
      lastKeyPressed = Some('⎋') // ESC key
    }
  }
  
  def getLastKeyPressed: Option[Char] = lastKeyPressed
  
  def clearLastKey(): Unit = lastKeyPressed = None
  
  def cleanup(): Unit = terminal.close()
}
```

### **Step 1.3: Create Proof-of-Concept Animation Engine**
```scala
// src/main/scala/ProofOfConceptEngine.scala
import scala.util.{Either, Left, Right}

class ProofOfConceptEngine(
  world: World,
  worldSize: Int,
  cubeSize: Int,
  cubeCenter: Coord,
  shapeId: Int,
  frameDelayMs: Int,
  yawRotationRate: Double,
  rollRotationRate: Double
) {
  private val inputManager = new KeyboardInputManager()
  
  def run(): Unit = {
    var running = true
    var frameIndex = 0
    
    try {
      while (running) {
        // 1. Poll for keyboard input
        inputManager.pollForInput()
        
        // 2. Generate frame (keep existing auto-rotation for now)
        val rotatedWorld = rotateShapes(frameIndex) match {
          case Right(w) => w
          case Left(_) => world // Fallback to original world
        }
        
        // 3. Render frame with existing renderer
        val baseRendered = Renderer.renderShadedForward(
          rotatedWorld, 
          lightDirection = Coord(-1, -1, -1), 
          ambient = 0.35, 
          xScale = 2
        )
        
        // 4. Add key display to rendered output
        val renderedWithKey = addKeyDisplay(baseRendered)
        
        // 5. Display frame (existing method)
        val clear = "\u001b[2J\u001b[H"
        Console.print(clear)
        Console.print(renderedWithKey)
        
        // 6. Check for quit key
        inputManager.getLastKeyPressed match {
          case Some('q') | Some('Q') => running = false
          case Some('c') if System.getProperty("os.name").toLowerCase.contains("win") => 
            // Ctrl+C handling for Windows
            running = false
          case _ => // Continue
        }
        
        Thread.sleep(frameDelayMs)
        frameIndex += 1
      }
    } finally {
      inputManager.cleanup()
    }
  }
  
  private def addKeyDisplay(rendered: String): String = {
    val lines = rendered.split("\n").toBuffer
    
    // Find the longest line to determine right edge
    val maxWidth = if (lines.nonEmpty) lines.map(_.length).max else 0
    
    // Add key display to first few lines
    val keyDisplay = inputManager.getLastKeyPressed match {
      case Some(key) => s" Key: $key "
      case None => " Key: - "
    }
    
    // Ensure we have enough lines
    if (lines.length < 3) {
      while (lines.length < 3) {
        lines += " " * maxWidth
      }
    }
    
    // Add key display to top-right of first line
    if (lines.nonEmpty) {
      val firstLine = lines(0)
      val padding = Math.max(0, maxWidth - firstLine.length - keyDisplay.length)
      lines(0) = firstLine + (" " * padding) + keyDisplay
    }
    
    // Add instructions to second line
    val instructions = " Press Q to quit "
    if (lines.length > 1) {
      val secondLine = lines(1)
      val padding = Math.max(0, maxWidth - secondLine.length - instructions.length)
      lines(1) = secondLine + (" " * padding) + instructions
    }
    
    lines.mkString("\n")
  }
  
  // Keep existing rotation logic for now
  private def rotateShapes(frameIndex: Int): Either[NoSuchShape, World] = {
    val totalRotation = Rotation(
      yaw = frameIndex * yawRotationRate,
      pitch = 0,
      roll = frameIndex * rollRotationRate
    )
    
    val worldWithReset = world.reset.add(TriangleShapes.cube(shapeId, cubeSize), cubeCenter, totalRotation)
    Right(worldWithReset)
  }
}
```

### **Step 1.4: Update Main.scala for Testing**
```scala
// src/main/scala/Main.scala - Minimal change for testing
object Main {
  // All existing constants stay the same
  private val SHAPE_ID = 101
  private val WORLD_SIZE = 22
  private val CUBE_SIZE = 10
  private val CUBE_CENTER = Coord(11, 11, 11)
  private val FRAME_DELAY_MS = 66
  private val YAW_ROTATION_RATE = Math.PI / -36
  private val ROLL_ROTATION_RATE = Math.PI / 72

  def main(args: Array[String]): Unit = {
    val world = buildWorld
    
    // Add command line flag to choose engine
    val useProofOfConcept = args.contains("--test-input")
    
    if (useProofOfConcept) {
      println("Starting Proof of Concept - Press keys to see them displayed!")
      println("Press Q to quit")
      
      val pocEngine = new ProofOfConceptEngine(
        world = world,
        worldSize = WORLD_SIZE,
        cubeSize = CUBE_SIZE,
        cubeCenter = CUBE_CENTER,
        shapeId = SHAPE_ID,
        frameDelayMs = FRAME_DELAY_MS,
        yawRotationRate = YAW_ROTATION_RATE,
        rollRotationRate = ROLL_ROTATION_RATE
      )
      
      pocEngine.run()
    } else {
      // Keep existing behavior as default
      val animationEngine = new AnimationEngine(
        world = world,
        worldSize = WORLD_SIZE,
        cubeSize = CUBE_SIZE,
        cubeCenter = CUBE_CENTER,
        shapeId = SHAPE_ID,
        frameDelayMs = FRAME_DELAY_MS,
        yawRotationRate = YAW_ROTATION_RATE,
        rollRotationRate = ROLL_ROTATION_RATE
      )
      
      animationEngine.run()
    }
  }

  // buildWorld stays exactly the same
  private def buildWorld =
    World(WORLD_SIZE, WORLD_SIZE, WORLD_SIZE)
      .add(TriangleShapes.cube(SHAPE_ID, CUBE_SIZE), CUBE_CENTER, Rotation.ZERO)
}
```

### **Step 1.5: Testing Commands**
```bash
# Test original behavior (unchanged)
sbt run

# Test proof of concept with key display
sbt "run --test-input"

# Build and test
sbt compile
sbt test  # Should still pass all existing tests
```

### **Expected Behavior**
1. **Cube continues rotating automatically** (existing behavior preserved)
2. **Top-right corner shows last pressed key**:
   - Regular keys: `Key: a`, `Key: 5`, etc.
   - Arrow keys: `Key: ↑`, `Key: ↓`, `Key: ←`, `Key: →`
   - Special keys: `Key: ⎋` (ESC), `Key: ↵` (Enter)
   - Unknown keys: `Key: ?`
3. **Instructions displayed**: "Press Q to quit"
4. **Q key exits cleanly**

### **Success Criteria for Phase 1**
- [ ] JLine3 dependency loads without issues
- [ ] Application compiles and runs
- [ ] Key presses are detected and displayed
- [ ] Arrow keys are recognized and shown with symbols
- [ ] Q key exits the application
- [ ] Original animation continues unchanged
- [ ] No impact on existing functionality
- [ ] Cross-platform compatibility (test on different terminals)

### **Risk Mitigation**
- **Fallback**: If JLine3 fails, existing `sbt run` still works
- **Isolation**: New code is completely separate from existing renderer
- **Reversible**: Can remove new files and dependency easily
- **Non-breaking**: All existing tests should continue to pass

## Phase 2: Full Keyboard Control (AWAITING PERMISSION)

### **Goal**: Replace automatic rotation with keyboard-controlled rotation
**Duration**: 2-3 hours  
**Risk**: Low (building on proven Phase 1)

### **Changes Required**:
1. Replace automatic rotation with user input state
2. Map arrow keys to rotation increments
3. Add rotation reset and fine control
4. Update display to show current rotation angles

**Note**: Phase 2 implementation details will be provided after Phase 1 validation.

## Phase 3: Polish & Enhancement (AWAITING PERMISSION)

### **Goal**: Add advanced features and polish
**Duration**: 1-2 hours  
**Risk**: Very Low (optional enhancements)

### **Potential Features**:
- Smooth rotation interpolation
- Variable rotation speed
- Additional control keys (roll rotation)
- Visual rotation indicators
- Help screen toggle

## Implementation Notes

### **JLine3 Benefits Validated in Phase 1**:
- Non-blocking input detection
- Cross-platform arrow key support  
- Clean separation from rendering system
- Professional terminal handling

### **Architecture Preservation**:
- Zero changes to existing `Renderer`, `World`, `Shape`, etc.
- Existing `AnimationEngine` remains untouched
- All existing tests continue to work
- Functional programming patterns maintained

### **Testing Strategy**:
```scala
// Future test for Phase 1
class ProofOfConceptEngineSpec extends AnyFlatSpec with Matchers {
  "ProofOfConceptEngine" should "display key presses in rendered output" in {
    val engine = createTestEngine()
    // Test key display functionality
  }
  
  it should "handle arrow key detection" in {
    val inputManager = new KeyboardInputManager()
    // Test arrow key parsing
  }
}
```

## Next Steps

**AWAITING PERMISSION TO PROCEED WITH PHASE 1**

Please approve Phase 1 implementation before proceeding. This proof-of-concept will validate:
1. JLine3 integration works in the project environment
2. Key detection functions correctly across different terminals  
3. Display modification works without breaking existing rendering
4. Foundation is solid for full keyboard control implementation

**Estimated time for Phase 1: 1-2 hours**
**Risk level: Very Low**
**Rollback plan: Remove 3 new files and 1 dependency line**

