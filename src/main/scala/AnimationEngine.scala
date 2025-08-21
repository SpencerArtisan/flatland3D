import scala.util.{Either, Left, Right}

class AnimationEngine(
  world: World,
  userInteraction: UserInteraction,  // Dependency injection
  worldSize: Int,
  cubeSize: Int,
  cubeCenter: Coord,
  shapeId: Int,
  frameDelayMs: Int
) {
  @volatile private var running = true
  
  // State management - AnimationEngine now owns the state
  private var currentRotation: Rotation = Rotation.ZERO
  private var currentViewport: Viewport = Viewport.centeredAt(cubeCenter)
  
  def run(): Unit = {
    // Start the user interaction system
    userInteraction match {
      case keyboard: KeyboardInputManager => keyboard.start()
      case _ => // Other implementations don't need startup
    }
    
    try {
      val frames = buildAnimationFrames()
      animate(frames)
    } finally {
      userInteraction.cleanup()
    }
  }
  
  def buildAnimationFrames(): LazyList[String] = {
    LazyList.from(0).map { frameIndex =>
      rotateShapes(frameIndex) match {
        case Right(w) =>
          val rendered = Renderer.renderShadedForward(
            w, 
            lightDirection = Coord(-1, -1, -1), 
            ambient = 0.35, 
            xScale = 2,
            viewport = Some(currentViewport)
          )
          addRotationDetails(rendered, frameIndex, currentRotation)
        case Left(_) => "" // Skip errors
      }
    }
  }

  private def animate(frames: LazyList[String]): Unit = {
    var frameIndex = 0
    val frameIterator = frames.iterator
    
    while (frameIterator.hasNext && running) {
      // Update user interaction state
      userInteraction.update()
      
      // Process deltas from user interaction
      processUserInputDeltas()
      
      // Check for quit request
      if (userInteraction.isQuitRequested) {
        println(s"\nQuit requested - Exiting...")
        running = false
        return
      }
      
      val frame = frameIterator.next()
      
      // Add control instructions to frame
      val frameWithControls = addControlInstructions(frame)
      
      // Clear screen and move cursor to top-left
      print("\u001b[2J\u001b[H")
      print(frameWithControls)
      System.out.flush() // Ensure output is flushed
      Thread.sleep(frameDelayMs)
      
      frameIndex += 1
    }
  }

  private def addRotationDetails(rendered: String, frameIndex: Int, rotation: Rotation): String = {
    val yawDegrees = (rotation.yaw * 180 / Math.PI) % 360
    val pitchDegrees = (rotation.pitch * 180 / Math.PI) % 360  
    val rollDegrees = (rotation.roll * 180 / Math.PI) % 360
    
    val details = Seq(
      f"Yaw:   ${yawDegrees}%6.1f°",
      f"Pitch: ${pitchDegrees}%6.1f°", 
      f"Roll:  ${rollDegrees}%6.1f°"
    ).mkString("  ")
    
    rendered + "\n\n" + details
  }

  def rotateShapes(frameIndex: Int): Either[NoSuchShape, World] = {
    // Use current rotation state managed by AnimationEngine
    // Recreate all shapes from the original world with the current rotation
    val originalPlacements = world.placements
    val worldWithReset = originalPlacements.foldLeft(world.reset) { (w, placement) =>
      w.add(placement.shape, placement.origin, currentRotation)
    }
    Right(worldWithReset)
  }

  // Viewport management methods
  def zoomViewport(factor: Double): Either[String, Unit] = {
    try {
      currentViewport = currentViewport.zoom(factor)
      Right(())
    } catch {
      case e: IllegalArgumentException => Left(e.getMessage)
    }
  }

  def panViewport(offset: Coord): Either[String, Unit] = {
    try {
      currentViewport = currentViewport.pan(offset)
      Right(())
    } catch {
      case e: Exception => Left(e.getMessage)
    }
  }

  def resetViewport(): Either[String, Unit] = {
    try {
      currentViewport = Viewport.centeredAt(cubeCenter)
      Right(())
    } catch {
      case e: Exception => Left(e.getMessage)
    }
  }

  def setViewport(viewport: Viewport): Unit = {
    currentViewport = viewport
  }

  def getCurrentViewport: Option[Viewport] = Some(currentViewport)
  def getCurrentRotation: Rotation = currentRotation
  
  // Method for tests to manually process deltas
  def processDeltas(): Unit = processUserInputDeltas()
  
  // Method for tests to reset state
  def resetState(): Unit = {
    currentRotation = Rotation.ZERO
    currentViewport = Viewport.centeredAt(cubeCenter)
  }

  // Process deltas from user interaction and apply to internal state
  private def processUserInputDeltas(): Unit = {
    // Apply rotation delta
    val rotationDelta = userInteraction.getRotationDelta
    if (rotationDelta != Rotation.ZERO) {
      currentRotation = Rotation(
        yaw = currentRotation.yaw + rotationDelta.yaw,
        pitch = currentRotation.pitch + rotationDelta.pitch,
        roll = currentRotation.roll + rotationDelta.roll
      )
    }
    
    // Apply viewport delta
    val viewportDelta = userInteraction.getViewportDelta
    if (!viewportDelta.isIdentity) {
      // Apply zoom
      if (viewportDelta.zoomFactor != 1.0) {
        currentViewport = currentViewport.zoom(viewportDelta.zoomFactor)
      }
      
      // Apply pan
      if (viewportDelta.panOffset != Coord.ZERO) {
        currentViewport = currentViewport.pan(viewportDelta.panOffset)
      }
    }
    
    // Handle reset requests
    if (userInteraction.isResetRequested) {
      currentRotation = Rotation.ZERO
    }
    
    if (userInteraction.isViewportResetRequested) {
      currentViewport = Viewport.centeredAt(cubeCenter)
    }
    
    // Clear processed deltas
    userInteraction.clearDeltas()
  }
  
  // Add control instructions to the rendered frame
  private def addControlInstructions(frame: String): String = {
    val lines = frame.split("\n")
    if (lines.nonEmpty) {
      // Add control instructions at the bottom - organized by category
      val controlsLines = Seq(
        "",
        "Rotation: WASD = Rotate, Z/X = Roll, R = Reset",
        "Viewport: +/=/- = Zoom, Arrow Keys = Pan, V = Reset Viewport",
        "System: Q/ESC = Quit"
      ).mkString("\n")
      
      frame + controlsLines
    } else {
      frame
    }
  }
}
