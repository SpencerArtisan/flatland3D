class TestUserInteraction(
  var quitRequested: Boolean = false,
  var resetRequested: Boolean = false
) extends UserInteraction {
  // Delta accumulation for testing
  private var rotationDelta: Rotation = Rotation.ZERO
  private var viewportDelta: ViewportDelta = ViewportDelta.IDENTITY
  private var viewportResetRequested: Boolean = false
  private var scaleFactor: Double = 1.0
  
  // UserInteraction interface implementation
  def getRotationDelta: Rotation = rotationDelta
  def getViewportDelta: ViewportDelta = viewportDelta
  def getScaleFactor: Double = scaleFactor
  def isQuitRequested: Boolean = quitRequested
  def isResetRequested: Boolean = resetRequested
  def isViewportResetRequested: Boolean = viewportResetRequested
  def isEasterEggToggleRequested: Boolean = false  // No-op for tests
  def update(): Unit = {} // No-op for tests
  def cleanup(): Unit = {} // No-op for tests

  def clearDeltas(): Unit = {
    rotationDelta = Rotation.ZERO
    viewportDelta = ViewportDelta.IDENTITY
    scaleFactor = 1.0
    resetRequested = false
    viewportResetRequested = false
  }
  
  // Test helper methods
  def setRotationDelta(delta: Rotation): Unit = rotationDelta = delta
  def setViewportDelta(delta: ViewportDelta): Unit = viewportDelta = delta
  def requestScaleUp(): Unit = scaleFactor = 1.1
  def requestScaleDown(): Unit = scaleFactor = 0.9
  def requestQuit(): Unit = quitRequested = true
  def requestReset(): Unit = resetRequested = true
  def requestViewportReset(): Unit = viewportResetRequested = true
  def requestEasterEggToggle(): Unit = {} // No-op for base tests  
  def clearRequests(): Unit = {
    quitRequested = false
    resetRequested = false
    viewportResetRequested = false
    scaleFactor = 1.0
  }
  
  // Additional test helper methods for backward compatibility
  def requestZoom(): Unit = {
    viewportDelta = viewportDelta.copy(zoomFactor = viewportDelta.zoomFactor * 1.2)
  }
  def requestPan(): Unit = {
    viewportDelta = viewportDelta.copy(panOffset = viewportDelta.panOffset + Coord(1, 1, 0))
  }
}
