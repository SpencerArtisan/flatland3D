class TestUserInteraction(
  var rotation: Rotation = Rotation.ZERO,
  var quitRequested: Boolean = false,
  var resetRequested: Boolean = false
) extends UserInteraction {
  def getCurrentRotation: Rotation = rotation
  def isQuitRequested: Boolean = quitRequested
  def isResetRequested: Boolean = resetRequested
  def update(): Unit = {} // No-op for tests
  def cleanup(): Unit = {} // No-op for tests
  
  // Test helper methods
  def setRotation(newRotation: Rotation): Unit = rotation = newRotation
  def requestQuit(): Unit = quitRequested = true
  def requestReset(): Unit = resetRequested = true
  def clearRequests(): Unit = {
    quitRequested = false
    resetRequested = false
  }
}
