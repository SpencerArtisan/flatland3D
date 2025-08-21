trait UserInteraction {
  def getCurrentRotation: Rotation
  def isQuitRequested: Boolean
  def isResetRequested: Boolean
  def update(): Unit  // Called each frame to update state
  def cleanup(): Unit // Cleanup resources
}
