import java.io.IOException

class KeyboardInputManager extends UserInteraction {
  @volatile private var currentRotation = Rotation.ZERO
  @volatile private var quitRequested = false
  @volatile private var resetRequested = false
  @volatile private var running = true
  
  private val rotationStep = Math.PI / 18  // 10 degrees per key press
  
  // Terminal input handling
  private var inputThread: Thread = _
  private var lastKeyPressed: Option[Int] = None
  
  def getCurrentRotation: Rotation = currentRotation
  def isQuitRequested: Boolean = quitRequested
  def isResetRequested: Boolean = resetRequested
  
  def start(): Unit = {
    enableRawMode()
    startInputThread()
  }
  
  def update(): Unit = {
    // Process any pending reset requests
    if (resetRequested) {
      currentRotation = Rotation.ZERO
      resetRequested = false
    }
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
      // WASD Controls
      case 119 | 87 => // 'w' or 'W' - Pitch up (look up)
        currentRotation = currentRotation.copy(pitch = currentRotation.pitch + rotationStep)
        
      case 115 | 83 => // 's' or 'S' - Pitch down (look down)
        currentRotation = currentRotation.copy(pitch = currentRotation.pitch - rotationStep)
        
      case 97 | 65 => // 'a' or 'A' - Yaw left (turn left)
        currentRotation = currentRotation.copy(yaw = currentRotation.yaw - rotationStep)
        
      case 100 | 68 => // 'd' or 'D' - Yaw right (turn right)
        currentRotation = currentRotation.copy(yaw = currentRotation.yaw + rotationStep)
        
      case 122 | 90 => // 'z' or 'Z' - Roll left (roll counterclockwise)
        currentRotation = currentRotation.copy(roll = currentRotation.roll - rotationStep)
        
      case 120 | 88 => // 'x' or 'X' - Roll right (roll clockwise)
        currentRotation = currentRotation.copy(roll = currentRotation.roll + rotationStep)
        
      case 114 | 82 => // 'r' or 'R' - Reset rotation
        resetRequested = true
        
      case 113 | 81 => // 'q' or 'Q' - Quit
        quitRequested = true
        
      case 27 => // ESC key - Quit
        quitRequested = true
        
      case _ => // Ignore other keys
    }
  }
  
  def reset(): Unit = {
    currentRotation = Rotation.ZERO
  }
}
