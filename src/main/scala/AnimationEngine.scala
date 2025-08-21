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
  private var easterEggActive: Boolean = false
  private var currentScale: Double = 1.0  // Scale factor for size and positions
  
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
    // Use current world (normal or Easter egg mode) and apply current rotation and scale
    val currentWorld = buildCurrentWorld()
    val currentPlacements = currentWorld.placements
    val worldWithReset = currentPlacements.foldLeft(currentWorld.reset) { (w, placement) =>
      // Scale both position and shape size
      val scaledOrigin = {
        val mag = placement.origin.magnitude
        if (mag > 0) {
          val dir = placement.origin * (1.0 / mag)  // Unit vector in same direction
          dir * (mag * currentScale)  // Scale magnitude only
        } else {
          placement.origin  // Zero vector stays zero
        }
      }
      val scaledShape = placement.shape match {
        case mesh: TriangleMesh => 
          // Scale vertices from original mesh, preserving direction but scaling magnitude
          val scaledTriangles = mesh.triangles.map { t =>
            def scaleVertex(v: Coord): Coord = {
              val mag = v.magnitude
              if (mag > 0) {
                // Scale magnitude while preserving direction
                val dir = v * (1.0 / mag)  // Unit vector in same direction
                dir * (mag * currentScale)  // Scale magnitude only
              } else {
                v  // Zero vector stays zero
              }
            }
            Triangle(
              scaleVertex(t.v0),
              scaleVertex(t.v1),
              scaleVertex(t.v2)
            )
          }
          TriangleMesh(mesh.id, scaledTriangles)
        case other => other  // Non-mesh shapes (shouldn't happen)
      }
      w.add(scaledShape, scaledOrigin, currentRotation)
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
  def getCurrentScale: Double = currentScale
  
  // Easter egg methods
  def isEasterEggActive: Boolean = easterEggActive
  def buildCurrentWorld(): World = {
    if (easterEggActive) {
      buildEliteWorld()
    } else {
      world
    }
  }
  
  // Method for tests to manually process deltas
  def processDeltas(): Unit = processUserInputDeltas()
  
  // Method for tests to reset state
  def resetState(): Unit = {
    currentRotation = Rotation.ZERO
    currentViewport = Viewport.centeredAt(cubeCenter)
    currentScale = 1.0
    easterEggActive = false
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
    
    // Apply scale factor
    val scaleFactor = userInteraction.getScaleFactor
    if (scaleFactor != 1.0) {
      currentScale = currentScale * scaleFactor
    }
    
    // Handle reset requests
    if (userInteraction.isResetRequested) {
      currentRotation = Rotation.ZERO
      currentScale = 1.0  // Reset scale too
    }
    
    if (userInteraction.isViewportResetRequested) {
      currentViewport = Viewport.centeredAt(cubeCenter)
    }
    
    // Handle Easter egg toggle requests
    if (userInteraction.isEasterEggToggleRequested) {
      easterEggActive = !easterEggActive
      if (easterEggActive) {
        println("\n*** Elite Mode Activated! ***")
      } else {
        println("\n*** Elite Mode Deactivated ***")
      }
    }
    
    // Clear processed deltas
    userInteraction.clearDeltas()
  }
  
  // Build Elite world with single large Cobra spaceship
  private def buildEliteWorld(): World = {
    // Create one large Cobra spaceship at the center of the scene
    val largeCobraSize = cubeSize * 9  // Make it 9x larger for absolutely massive dramatic effect
    val cobraId = 200  // Single Cobra ID
    val cobra = TriangleShapes.cobra(cobraId, largeCobraSize)
    
    // Position at the center of the scene
    val centerPosition = cubeCenter
    
    // Add a subtle rotation for visual interest
    val eliteRotation = Rotation(
      yaw = 0.2,    // Slight yaw for dramatic angle
      pitch = 0.1,  // Slight pitch to show 3D nature
      roll = 0      // No roll to keep it stable
    )
    
    World.infinite.add(cobra, centerPosition, eliteRotation)
  }

  // Add control instructions to the rendered frame
  private def addControlInstructions(frame: String): String = {
    val lines = frame.split("\n")
    if (lines.nonEmpty) {
      // Add control instructions at the bottom - organized by category
      val easterEggStatus = if (easterEggActive) " [ELITE MODE]" else ""
      val scaleInfo = f"Scale: ${currentScale}%.1fx"
      val controlsLines = Seq(
        "",
        "Rotation: WASD = Rotate, Z/X = Roll, R = Reset",
        "Viewport: +/=/- = Zoom, Arrow Keys = Pan, V = Reset Viewport",
        "Scale: [ = Smaller, ] = Larger",
        s"System: Q/ESC = Quit, Ctrl+E = Elite Mode$easterEggStatus ($scaleInfo)"
      ).mkString("\n")
      
      frame + controlsLines
    } else {
      frame
    }
  }
}
