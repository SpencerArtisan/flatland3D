class TestUserInteraction(
  var quitRequested: Boolean = false,
  var resetRequested: Boolean = false
) extends UserInteraction {
  // Delta accumulation for testing
  private var rotationDelta: Rotation = Rotation.ZERO
  private var viewportDelta: ViewportDelta = ViewportDelta.IDENTITY
  private var viewportResetRequested: Boolean = false
  
  // UserInteraction interface implementation
  def getRotationDelta: Rotation = rotationDelta
  def getViewportDelta: ViewportDelta = viewportDelta
  def isQuitRequested: Boolean = quitRequested
  def isResetRequested: Boolean = resetRequested
  def isViewportResetRequested: Boolean = viewportResetRequested
  def update(): Unit = {} // No-op for tests
    def cleanup(): Unit = {} // No-op for tests
  def getScaleFactor: Double = 1.0 // Default scale factor - no change
  def isEasterEggToggleRequested: Boolean = false // Default - no easter egg toggle

  def clearDeltas(): Unit = {
    rotationDelta = Rotation.ZERO
    viewportDelta = ViewportDelta.IDENTITY
    resetRequested = false
    viewportResetRequested = false
  }
  
  // Test helper methods
  def setRotationDelta(delta: Rotation): Unit = rotationDelta = delta
  def setViewportDelta(delta: ViewportDelta): Unit = viewportDelta = delta
  def requestQuit(): Unit = quitRequested = true
  def requestReset(): Unit = resetRequested = true
  def requestViewportReset(): Unit = viewportResetRequested = true
  def clearRequests(): Unit = {
    quitRequested = false
    resetRequested = false
    viewportResetRequested = false
  }
  
  // Additional test helper methods for backward compatibility
  def requestZoom(): Unit = {
    viewportDelta = viewportDelta.copy(zoomFactor = viewportDelta.zoomFactor * 1.2)
  }
  def requestPan(): Unit = {
    viewportDelta = viewportDelta.copy(panOffset = viewportDelta.panOffset + Coord(1, 1, 0))
  }
}
