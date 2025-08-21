# Keyboard Control Implementation Plan

## Overview

This document outlines the implementation plan for converting the Flatland3D cube animation from automatic rotation to keyboard-controlled rotation using arrow keys. The cube will only rotate when the user presses arrow keys, providing interactive control over the 3D visualization.

## Current System Analysis

### **Current Animation System**
```scala
// Current automatic rotation in AnimationEngine.scala
def rotateShapes(frameIndex: Int): Either[NoSuchShape, World] = {
  val totalRotation = Rotation(
    yaw = frameIndex * yawRotationRate,    // Automatic yaw rotation
    pitch = 0,                             // No pitch rotation
    roll = frameIndex * rollRotationRate   // Automatic roll rotation
  )
  // Apply rotation to world
}
```

### **Current Problems**
1. **Automatic rotation**: Cube rotates continuously based on `frameIndex`
2. **No user input**: System doesn't listen for keyboard events
3. **Frame-based timing**: Rotation tied to animation frame counter
4. **Console limitations**: Standard Scala console doesn't handle non-blocking keyboard input

## Design Goals

### **Primary Objectives**
1. **Interactive Control**: Cube rotates only when user presses arrow keys
2. **Responsive Input**: Real-time response to keyboard events
3. **Smooth Animation**: Maintain smooth rotation transitions
4. **Visual Feedback**: Show current rotation state and controls
5. **Graceful Exit**: Allow user to quit the application cleanly

### **Technical Requirements**
1. **Non-blocking Input**: Handle keyboard input without blocking the render loop
2. **Cross-platform**: Work on different operating systems
3. **Terminal Compatibility**: Function in standard terminal environments
4. **Minimal Dependencies**: Avoid heavy GUI frameworks
5. **Maintain Architecture**: Preserve existing code structure and patterns

## Implementation Strategy

### **Phase 1: Input Handling System**

#### **1.1 Keyboard Input Manager**
Create a new `KeyboardInputManager` class to handle terminal input:

```scala
class KeyboardInputManager {
  private var currentRotation = Rotation.ZERO
  private val rotationStep = Math.PI / 18  // 10 degrees per key press
  
  def getCurrentRotation: Rotation = currentRotation
  
  def processInput(key: Char): Unit = {
    key match {
      case 'K' | 'k' => // 'K' key - Pitch up
        currentRotation = currentRotation.copy(pitch = currentRotation.pitch + rotationStep)
      case 'L' | 'l' => // 'L' key - Pitch down
        currentRotation = currentRotation.copy(pitch = currentRotation.pitch - rotationStep)
      case 'I' | 'i' => // 'I' key - Yaw left
        currentRotation = currentRotation.copy(yaw = currentRotation.yaw - rotationStep)
      case 'O' | 'o' => // 'O' key - Yaw right
        currentRotation = currentRotation.copy(yaw = currentRotation.yaw + rotationStep)
      case 'q' | 'Q' => // Quit
        // Signal to exit application
      case 'r' | 'R' => // Reset
        currentRotation = Rotation.ZERO
      case _ => // Ignore other keys
    }
  }
}
```

#### **1.2 Terminal Input Handler**
Implement platform-specific terminal input handling:

```scala
object TerminalInputHandler {
  def enableRawMode(): Unit = {
    // Enable raw mode to capture individual key presses
    // Platform-specific implementation
  }
  
  def disableRawMode(): Unit = {
    // Restore normal terminal mode
  }
  
  def readKeyNonBlocking(): Option[Char] = {
    // Non-blocking key reading
    // Returns None if no key pressed
  }
}
```

### **Phase 2: Interactive Animation Engine**

#### **2.1 New InteractiveAnimationEngine**
Replace the current `AnimationEngine` with an interactive version:

```scala
class InteractiveAnimationEngine(
  world: World,
  worldSize: Int,
  cubeSize: Int,
  cubeCenter: Coord,
  shapeId: Int,
  frameDelayMs: Int
) {
  private val inputManager = new KeyboardInputManager()
  private var running = true
  
  def run(): Unit = {
    TerminalInputHandler.enableRawMode()
    try {
      interactiveLoop()
    } finally {
      TerminalInputHandler.disableRawMode()
    }
  }
  
  private def interactiveLoop(): Unit = {
    while (running) {
      // Check for keyboard input
      TerminalInputHandler.readKeyNonBlocking() match {
        case Some(key) => 
          inputManager.processInput(key)
          if (key == 'q' || key == 'Q') running = false
        case None => // No input
      }
      
      // Render current frame
      val currentRotation = inputManager.getCurrentRotation
      val rotatedWorld = applyRotation(currentRotation)
      val rendered = renderFrame(rotatedWorld, currentRotation)
      displayFrame(rendered)
      
      Thread.sleep(frameDelayMs)
    }
  }
  
  private def applyRotation(rotation: Rotation): World = {
    world.reset.add(TriangleShapes.cube(shapeId, cubeSize), cubeCenter, rotation)
  }
}
```

#### **2.2 Enhanced Frame Rendering**
Update frame rendering to show controls and current state:

```scala
private def renderFrame(world: World, rotation: Rotation): String = {
  val rendered = Renderer.renderShadedForward(
    world, 
    lightDirection = Coord(-1, -1, -1), 
    ambient = 0.35, 
    xScale = 2
  )
  
  val controls = createControlsDisplay()
  val rotationInfo = createRotationDisplay(rotation)
  
  rendered + "\n\n" + rotationInfo + "\n\n" + controls
}

private def createControlsDisplay(): String = {
  """Controls:
    |  K: Pitch Up    L: Pitch Down
    |  I: Yaw Left    O: Yaw Right
    |  M: Roll Left   J: Roll Right
    |  R: Reset       Q: Quit""".stripMargin
}

private def createRotationDisplay(rotation: Rotation): String = {
  val yawDegrees = (rotation.yaw * 180 / Math.PI) % 360
  val pitchDegrees = (rotation.pitch * 180 / Math.PI) % 360
  val rollDegrees = (rotation.roll * 180 / Math.PI) % 360
  
  f"Current Rotation - Yaw: ${yawDegrees}%6.1f°  Pitch: ${pitchDegrees}%6.1f°  Roll: ${rollDegrees}%6.1f°"
}
```

### **Phase 3: Platform-Specific Input Implementation**

#### **3.1 JNI-based Solution (Recommended)**
Use JNI to access native terminal functions:

```scala
object NativeTerminal {
  @native def enableRawMode(): Int
  @native def disableRawMode(): Int  
  @native def kbhit(): Int
  @native def getch(): Char
  
  System.loadLibrary("terminal")
}
```

#### **3.2 Process-based Solution (Alternative)**
Use system commands for input handling:

```scala
object ProcessInputHandler {
  def readKeyNonBlocking(): Option[Char] = {
    try {
      val process = Runtime.getRuntime.exec(Array("sh", "-c", "read -n 1 -t 0.1; echo $REPLY"))
      val reader = new BufferedReader(new InputStreamReader(process.getInputStream))
      val result = reader.readLine()
      
      if (result != null && result.nonEmpty) {
        Some(result.charAt(0))
      } else {
        None
      }
    } catch {
      case _: Exception => None
    }
  }
}
```

#### **3.3 Java Robot Class Solution (Fallback)**
Use Java's Robot class for cross-platform compatibility:

```scala
import java.awt.Robot
import java.awt.event.KeyEvent

object RobotInputHandler {
  private val robot = new Robot()
  
  def isKeyPressed(keyCode: Int): Boolean = {
    // Implementation using Robot class
    // Note: This is more complex and less ideal
  }
}
```

### **Phase 4: Configuration and Constants**

#### **4.1 Input Configuration**
```scala
object InputConfig {
  // Rotation step per key press (in radians)
  val ROTATION_STEP = Math.PI / 18  // 10 degrees
  val FINE_ROTATION_STEP = Math.PI / 36  // 5 degrees (with modifier key)
  
  // Input polling rate
  val INPUT_POLL_RATE_MS = 16  // ~60 FPS input polling
  
  // Key mappings
  val PITCH_UP_KEYS = Set('K', 'k')    // K key
  val PITCH_DOWN_KEYS = Set('L', 'l')  // L key  
  val YAW_LEFT_KEYS = Set('I', 'i')    // I key
  val YAW_RIGHT_KEYS = Set('O', 'o')   // O key
  val RESET_KEYS = Set('R', 'r')
  val QUIT_KEYS = Set('Q', 'q', '\u0003')          // Q or Ctrl+C
}
```

#### **4.2 Updated Main Configuration**
```scala
object Main {
  // Remove automatic rotation rates
  private val SHAPE_ID = 101
  private val WORLD_SIZE = 22
  private val CUBE_SIZE = 10
  private val CUBE_CENTER = Coord(11, 11, 11)
  private val FRAME_DELAY_MS = 33  // ~30 FPS for smoother interaction
  
  def main(args: Array[String]): Unit = {
    val world = buildWorld
    val interactiveEngine = new InteractiveAnimationEngine(
      world = world,
      worldSize = WORLD_SIZE,
      cubeSize = CUBE_SIZE,
      cubeCenter = CUBE_CENTER,
      shapeId = SHAPE_ID,
      frameDelayMs = FRAME_DELAY_MS
    )
    
    println("Flatland3D Interactive Mode")
    println("Use arrow keys to rotate the cube, Q to quit, R to reset")
    println("Press any key to start...")
    
    interactiveEngine.run()
  }
}
```

## Technical Challenges & Solutions

### **Challenge 1: Non-blocking Terminal Input**
**Problem**: Standard Scala console input is blocking
**Solutions**:
1. **JNI Approach**: Create native library for terminal control
2. **System Process**: Use shell commands for input detection
3. **NIO Approach**: Use Java NIO for non-blocking I/O
4. **Thread-based**: Separate input thread with shared state

### **Challenge 2: Arrow Key Detection**
**Problem**: Arrow keys send escape sequences, not single characters
**Solutions**:
1. **Escape Sequence Parsing**: Parse `\u001B[A`, `\u001B[B`, etc.
2. **Alternative Keys**: Use WASD or IJKL as alternatives
3. **Raw Mode**: Enable terminal raw mode to capture escape sequences

### **Challenge 3: Cross-platform Compatibility**
**Problem**: Terminal behavior differs across operating systems
**Solutions**:
1. **Platform Detection**: Detect OS and use appropriate methods
2. **Abstraction Layer**: Create platform-agnostic input interface
3. **Fallback Mechanisms**: Provide multiple input methods

### **Challenge 4: State Management**
**Problem**: Managing rotation state between input events
**Solutions**:
1. **Immutable State**: Use immutable rotation objects
2. **State Validation**: Ensure rotation values stay within bounds
3. **Smooth Transitions**: Interpolate between rotation states

## Implementation Phases

### **Phase 1: Basic Implementation (Week 1)**
- [ ] Create `KeyboardInputManager` class
- [ ] Implement basic key mapping (WASD instead of arrows)
- [ ] Replace `AnimationEngine` with `InteractiveAnimationEngine`
- [ ] Add control display to rendered output
- [ ] Test basic functionality

### **Phase 2: Advanced Input (Week 2)**
- [ ] Implement arrow key detection
- [ ] Add non-blocking input handling
- [ ] Create platform-specific input handlers
- [ ] Add smooth rotation transitions
- [ ] Test on multiple platforms

### **Phase 3: Polish & Features (Week 3)**
- [ ] Add fine rotation control (modifier keys)
- [ ] Implement rotation bounds checking
- [ ] Add visual feedback for input events
- [ ] Create comprehensive error handling
- [ ] Performance optimization

### **Phase 4: Testing & Documentation (Week 4)**
- [ ] Write comprehensive tests
- [ ] Create user documentation
- [ ] Test cross-platform compatibility
- [ ] Performance benchmarking
- [ ] Code review and cleanup

## Testing Strategy

### **Unit Tests**
```scala
class KeyboardInputManagerSpec extends AnyFlatSpec with Matchers {
  "KeyboardInputManager" should "update rotation on key press" in {
    val manager = new KeyboardInputManager()
    val initialRotation = manager.getCurrentRotation
    
    manager.processInput('d')  // Left arrow
    val afterLeft = manager.getCurrentRotation
    
    afterLeft.yaw should be < initialRotation.yaw
  }
  
  it should "reset rotation on 'r' key" in {
    val manager = new KeyboardInputManager()
    manager.processInput('d')  // Rotate first
    manager.processInput('r')  // Reset
    
    manager.getCurrentRotation should equal(Rotation.ZERO)
  }
}
```

### **Integration Tests**
```scala
class InteractiveAnimationEngineSpec extends AnyFlatSpec with Matchers {
  "InteractiveAnimationEngine" should "apply rotation to world correctly" in {
    val engine = createTestEngine()
    val rotation = Rotation(Math.PI / 4, 0, 0)
    val world = engine.applyRotation(rotation)
    
    val placement = world.placements.head
    placement.rotation.yaw should be(Math.PI / 4 +- 0.001)
  }
}
```

### **Manual Testing Checklist**
- [ ] Arrow keys rotate cube in correct directions
- [ ] WASD keys work as alternatives
- [ ] Q key exits application cleanly
- [ ] R key resets rotation to zero
- [ ] Controls display shows correct information
- [ ] Rotation values update in real-time
- [ ] No automatic rotation occurs
- [ ] Performance remains smooth during interaction

## Risk Assessment

### **High Risk**
1. **Platform Compatibility**: Input handling may not work on all systems
2. **Terminal Limitations**: Some terminals may not support required features
3. **Performance Impact**: Input polling may affect rendering performance

### **Medium Risk**
1. **User Experience**: Controls may not be intuitive for all users
2. **Error Handling**: Unexpected input sequences may cause issues
3. **State Synchronization**: Race conditions between input and rendering

### **Low Risk**
1. **Code Complexity**: Additional complexity in codebase
2. **Testing Coverage**: Ensuring all input scenarios are tested
3. **Documentation**: Keeping documentation up-to-date

## Success Criteria

### **Functional Requirements**
- [ ] Cube rotates only when arrow keys are pressed
- [ ] Rotation is smooth and responsive
- [ ] All control keys work as specified
- [ ] Application exits cleanly on 'Q' key
- [ ] Reset functionality works correctly

### **Performance Requirements**
- [ ] Input response time < 50ms
- [ ] Rendering frame rate ≥ 20 FPS
- [ ] Memory usage remains stable
- [ ] CPU usage acceptable for terminal application

### **Usability Requirements**
- [ ] Controls are clearly displayed
- [ ] Rotation feedback is immediate and visible
- [ ] Error messages are helpful and clear
- [ ] Application startup is fast and reliable

## Future Enhancements

### **Advanced Controls**
- Roll rotation with additional keys (Q/E)
- Variable rotation speed based on key hold duration
- Smooth animation interpolation between rotation states
- Multiple rotation modes (continuous vs. discrete)

### **Visual Improvements**
- Color-coded control hints
- Rotation axis indicators
- Grid or reference frame display
- Multiple viewing angles

### **Input Extensions**
- Mouse support for rotation
- Touch/swipe gestures (if applicable)
- Gamepad controller support
- Voice commands (experimental)

## Conclusion

This implementation plan provides a comprehensive approach to converting the Flatland3D automatic rotation system to keyboard-controlled interactive rotation. The plan prioritizes maintainability, cross-platform compatibility, and user experience while preserving the existing architecture and design patterns.

The phased approach allows for incremental development and testing, reducing risk and enabling early feedback. The focus on platform-specific solutions ensures broad compatibility while the fallback mechanisms provide robustness.

Upon completion, users will have full interactive control over the cube rotation, creating a more engaging and educational 3D visualization experience.

