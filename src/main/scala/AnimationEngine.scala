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
          val rendered = Renderer.renderShadedForward(w, lightDirection = Coord(-1, -1, -1), ambient = 0.35, xScale = 2)
          val actualRotation = userInteraction.getCurrentRotation
          addRotationDetails(rendered, frameIndex, actualRotation)
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
      f"Frame: $frameIndex%3d",
      f"Yaw:   ${yawDegrees}%6.1f°",
      f"Pitch: ${pitchDegrees}%6.1f°", 
      f"Roll:  ${rollDegrees}%6.1f°"
    ).mkString("  ")
    
    rendered + "\n\n" + details
  }

  def rotateShapes(frameIndex: Int): Either[NoSuchShape, World] = {
    // Use interactive rotation from UserInteraction instead of automatic rotation
    val userRotation = userInteraction.getCurrentRotation
    
    // Reset to start position and apply the user-controlled rotation
    val worldWithReset = world.reset.add(TriangleShapes.cube(shapeId, cubeSize), cubeCenter, userRotation)
    Right(worldWithReset)
  }
  
  // Add control instructions to the rendered frame
  private def addControlInstructions(frame: String): String = {
    val lines = frame.split("\n")
    if (lines.nonEmpty) {
      // Add control instructions at the bottom
      val controlsLine = "\nControls: WASD = Rotate, Z/X = Roll, R = Reset, Q/ESC = Quit"
      
      frame + controlsLine
    } else {
      frame
    }
  }
}
