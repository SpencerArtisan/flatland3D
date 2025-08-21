class KeyboardInputManager {
  @volatile private var currentRotation = Rotation.ZERO
  private val rotationStep = Math.PI / 18  // 10 degrees per key press
  
  def getCurrentRotation: Rotation = currentRotation
  
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
        
      case 114 | 82 => // 'r' or 'R' - Reset rotation
        currentRotation = Rotation.ZERO
        
      case _ => // Ignore other keys
    }
  }
  
  def reset(): Unit = {
    currentRotation = Rotation.ZERO
  }
}
