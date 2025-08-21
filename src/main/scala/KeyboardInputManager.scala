import java.io.IOException

class KeyboardInputManager extends UserInteraction {
  @volatile private var quitRequested = false
  @volatile private var resetRequested = false
  @volatile private var viewportResetRequested = false
  @volatile private var easterEggToggleRequested = false
  @volatile private var running = true
  
  // Delta accumulation - changes since last clearDeltas()
  @volatile private var accumulatedRotationDelta = Rotation.ZERO
  @volatile private var accumulatedViewportDelta = ViewportDelta.IDENTITY
  @volatile private var accumulatedScaleFactor = 1.0
  
  private val rotationStep = Math.PI / 18  // 10 degrees per key press
  
  // Terminal input handling
  private var inputThread: Thread = _
  private var lastKeyPressed: Option[Int] = None
  
  // Arrow key sequence detection
  private var escapeSequenceBuffer: List[Int] = List()
  
  // UserInteraction interface implementation
  def getRotationDelta: Rotation = accumulatedRotationDelta
  def getViewportDelta: ViewportDelta = accumulatedViewportDelta
  def getScaleFactor: Double = accumulatedScaleFactor
  def isQuitRequested: Boolean = quitRequested
  def isResetRequested: Boolean = resetRequested
  def isViewportResetRequested: Boolean = viewportResetRequested
  def isEasterEggToggleRequested: Boolean = easterEggToggleRequested
  
  def start(): Unit = {
    enableRawMode()
    startInputThread()
  }
  
  def update(): Unit = {
    // No additional processing needed - deltas are accumulated in real-time
  }
  
  def clearDeltas(): Unit = {
    accumulatedRotationDelta = Rotation.ZERO
    accumulatedViewportDelta = ViewportDelta.IDENTITY
    accumulatedScaleFactor = 1.0
    resetRequested = false
    viewportResetRequested = false
    easterEggToggleRequested = false
  }
  
  def cleanup(): Unit = {
    running = false
    if (inputThread != null) {
      inputThread.interrupt()
    }
    disableRawMode()
  }
  
  private def enableRawMode(): Unit = {
    try {
      // Use cbreak instead of raw to preserve ANSI escape sequences
      val pb = new ProcessBuilder("stty", "cbreak", "-echo")
      pb.inheritIO()
      val process = pb.start()
      process.waitFor()
    } catch {
      case _: Exception => 
        println("Warning: Could not enable cbreak mode")
    }
  }
  
  private def disableRawMode(): Unit = {
    try {
      // Restore normal terminal settings
      val pb = new ProcessBuilder("stty", "-cbreak", "echo")
      pb.inheritIO()
      val process = pb.start()
      process.waitFor()
    } catch {
      case _: Exception => // Ignore cleanup errors
    }
  }
  
  private def startInputThread(): Unit = {
    inputThread = new Thread(() => {
      try {
        while (running) {
          // Read directly from System.in - should be immediate with raw mode
          val key = System.in.read()
          if (key != -1 && running) {
            lastKeyPressed = Some(key)
            processInput(key)
          }
        }
      } catch {
        case _: IOException => // Stream closed or interrupted
      }
    })
    inputThread.setDaemon(true)
    inputThread.start()
  }
  
  def processInput(key: Int): Unit = {
    // Handle escape sequences for arrow keys
    if (escapeSequenceBuffer.nonEmpty) {
      escapeSequenceBuffer = escapeSequenceBuffer :+ key
      
      // Check for complete arrow key sequences
      escapeSequenceBuffer match {
        case List(27, 91, 65) => // ESC[A - Up arrow - Pan up
          val newPan = accumulatedViewportDelta.panOffset + Coord(0, -2, 0)
          accumulatedViewportDelta = accumulatedViewportDelta.copy(panOffset = newPan)
          escapeSequenceBuffer = List()
          
        case List(27, 91, 66) => // ESC[B - Down arrow - Pan down
          val newPan = accumulatedViewportDelta.panOffset + Coord(0, 2, 0)
          accumulatedViewportDelta = accumulatedViewportDelta.copy(panOffset = newPan)
          escapeSequenceBuffer = List()
          
        case List(27, 91, 68) => // ESC[D - Left arrow - Pan left
          val newPan = accumulatedViewportDelta.panOffset + Coord(-2, 0, 0)
          accumulatedViewportDelta = accumulatedViewportDelta.copy(panOffset = newPan)
          escapeSequenceBuffer = List()
          
        case List(27, 91, 67) => // ESC[C - Right arrow - Pan right
          val newPan = accumulatedViewportDelta.panOffset + Coord(2, 0, 0)
          accumulatedViewportDelta = accumulatedViewportDelta.copy(panOffset = newPan)
          escapeSequenceBuffer = List()
          
        case List(27) if key != 91 => // ESC followed by non-[ means quit, not arrow key
          quitRequested = true
          escapeSequenceBuffer = List()
          
        case _ if escapeSequenceBuffer.length >= 3 => // Invalid sequence, reset
          escapeSequenceBuffer = List()
          
        case _ => // Incomplete sequence, wait for more
      }
      return
    }
    
    // Start escape sequence detection
    if (key == 27) { // ESC
      escapeSequenceBuffer = List(27)
      return
    }
    
    // Handle regular keys
    key match {
      // WASD Controls - accumulate rotation deltas
      case 119 | 87 => // 'w' or 'W' - Pitch up (look up)
        accumulatedRotationDelta = accumulatedRotationDelta.copy(pitch = accumulatedRotationDelta.pitch + rotationStep)
        
      case 115 | 83 => // 's' or 'S' - Pitch down (look down)
        accumulatedRotationDelta = accumulatedRotationDelta.copy(pitch = accumulatedRotationDelta.pitch - rotationStep)
        
      case 97 | 65 => // 'a' or 'A' - Yaw left (turn left)
        accumulatedRotationDelta = accumulatedRotationDelta.copy(yaw = accumulatedRotationDelta.yaw - rotationStep)
        
      case 100 | 68 => // 'd' or 'D' - Yaw right (turn right)
        accumulatedRotationDelta = accumulatedRotationDelta.copy(yaw = accumulatedRotationDelta.yaw + rotationStep)
        
      case 122 | 90 => // 'z' or 'Z' - Roll left (roll counterclockwise)
        accumulatedRotationDelta = accumulatedRotationDelta.copy(roll = accumulatedRotationDelta.roll - rotationStep)
        
      case 120 | 88 => // 'x' or 'X' - Roll right (roll clockwise)
        accumulatedRotationDelta = accumulatedRotationDelta.copy(roll = accumulatedRotationDelta.roll + rotationStep)
        
      case 114 | 82 => // 'r' or 'R' - Reset rotation
        resetRequested = true
        
      case 113 | 81 => // 'q' or 'Q' - Quit
        quitRequested = true
        
      case 5 => // Ctrl+E - Easter egg toggle
        easterEggToggleRequested = true
        
      // Viewport Controls - accumulate viewport deltas
      case 43 | 61 => // '+' or '=' - Zoom in (= is easier on Mac without SHIFT)
        val newZoom = accumulatedViewportDelta.zoomFactor * 1.2
        accumulatedViewportDelta = accumulatedViewportDelta.copy(zoomFactor = newZoom)
        
      case 45 => // '-' - Zoom out
        val newZoom = accumulatedViewportDelta.zoomFactor * 0.8
        accumulatedViewportDelta = accumulatedViewportDelta.copy(zoomFactor = newZoom)
        
      case 118 | 86 => // 'v' or 'V' - Viewport reset
        viewportResetRequested = true
        
      // Scale Controls
      case 91 => // '[' - Scale down
        accumulatedScaleFactor = 0.9  // Scale down by 10%
        
      case 93 => // ']' - Scale up
        accumulatedScaleFactor = 1.1  // Scale up by 10%
        
      case _ => // Ignore other keys
    }
  }
  
  // Legacy methods for testing compatibility - delegate to delta-based approach
  def processInput(key: Char): Unit = processInput(key.toInt)
}
