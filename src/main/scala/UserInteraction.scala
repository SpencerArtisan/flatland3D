trait UserInteraction {
  // Delta-based input - provides changes since last frame
  def getRotationDelta: Rotation          // Rotation changes to apply
  def getViewportDelta: ViewportDelta     // Viewport changes to apply
  def getScaleFactor: Double              // Scale factor for size and positions (1.0 = no change)
  
  // State queries
  def isQuitRequested: Boolean
  def isResetRequested: Boolean
  def isViewportResetRequested: Boolean
  def isEasterEggToggleRequested: Boolean
  
  // Lifecycle methods
  def update(): Unit          // Called each frame to update state
  def cleanup(): Unit         // Cleanup resources
  def clearDeltas(): Unit     // Clear accumulated deltas after processing
}
