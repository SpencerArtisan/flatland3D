import java.io.IOException

class KeyboardInputManager extends UserInteraction {
  @volatile private var quitRequested = false
  @volatile private var resetRequested = false
  @volatile private var viewportResetRequested = false
  @volatile private var running = true
  
  // Delta accumulation - changes since last clearDeltas()
  @volatile private var accumulatedRotationDelta = Rotation.ZERO
  @volatile private var accumulatedViewportDelta = ViewportDelta.IDENTITY
  
  private val rotationStep = Math.PI / 18  // 10 degrees per key press
  
  // Terminal input handling
  private var inputThread: Thread = _
  private var lastKeyPressed: Option[Int] = None
  
  // UserInteraction interface implementation
  def getRotationDelta: Rotation = accumulatedRotationDelta
  def getViewportDelta: ViewportDelta = accumulatedViewportDelta
  def isQuitRequested: Boolean = quitRequested
  def isResetRequested: Boolean = resetRequested
  def isViewportResetRequested: Boolean = viewportResetRequested
  
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
    resetRequested = false
    viewportResetRequested = false
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
        
      case 27 => // ESC key - Quit
        quitRequested = true
        
      // Viewport Controls - accumulate viewport deltas
      case 43 => // '+' - Zoom in
        val newZoom = accumulatedViewportDelta.zoomFactor * 1.2
        accumulatedViewportDelta = accumulatedViewportDelta.copy(zoomFactor = newZoom)
        
      case 45 => // '-' - Zoom out
        val newZoom = accumulatedViewportDelta.zoomFactor * 0.8
        accumulatedViewportDelta = accumulatedViewportDelta.copy(zoomFactor = newZoom)
        
      case 38 => // '&' - Up arrow (simplified) - Pan up
        val newPan = accumulatedViewportDelta.panOffset + Coord(0, -1, 0)
        accumulatedViewportDelta = accumulatedViewportDelta.copy(panOffset = newPan)
        
      case 40 => // '(' - Down arrow (simplified) - Pan down
        val newPan = accumulatedViewportDelta.panOffset + Coord(0, 1, 0)
        accumulatedViewportDelta = accumulatedViewportDelta.copy(panOffset = newPan)
        
      case 60 => // '<' - Left arrow (simplified) - Pan left
        val newPan = accumulatedViewportDelta.panOffset + Coord(-1, 0, 0)
        accumulatedViewportDelta = accumulatedViewportDelta.copy(panOffset = newPan)
        
      case 62 => // '>' - Right arrow (simplified) - Pan right
        val newPan = accumulatedViewportDelta.panOffset + Coord(1, 0, 0)
        accumulatedViewportDelta = accumulatedViewportDelta.copy(panOffset = newPan)
        
      case 118 | 86 => // 'v' or 'V' - Viewport reset
        viewportResetRequested = true
        
      case _ => // Ignore other keys
    }
  }
  
  // Legacy methods for testing compatibility - delegate to delta-based approach
  def processInput(key: Char): Unit = processInput(key.toInt)
}
