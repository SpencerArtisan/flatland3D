import scala.util.{Either, Left, Right}
import java.io.IOException

class AnimationEngine(
  world: World,
  worldSize: Int,
  cubeSize: Int,
  cubeCenter: Coord,
  shapeId: Int,
  frameDelayMs: Int,
  yawRotationRate: Double,
  rollRotationRate: Double
) {
  @volatile private var lastKeyPressed: Option[Int] = None
  @volatile private var running = true
  
  println("Immediate keyboard input enabled")
  println("Press Q or ESC to quit the animation")
  
  // Try to enable cbreak mode (immediate input but preserve control sequences)
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
  
  // Enable raw mode at startup
  enableRawMode()
  
  // Start input thread with raw System.in
  private val inputThread = new Thread(() => {
    try {
      while (running) {
        // Read directly from System.in - should be immediate with raw mode
        val key = System.in.read()
        if (key != -1 && running) {
          lastKeyPressed = Some(key)
        }
      }
    } catch {
      case _: IOException => // Stream closed or interrupted
    }
  })
  inputThread.setDaemon(true)
  inputThread.start()
  
  def run(): Unit = {
    val frames = buildAnimationFrames()
    animate(frames)
  }
  
  def buildAnimationFrames(): LazyList[String] = {
    LazyList.from(0).map { frameIndex =>
      rotateShapes(frameIndex) match {
        case Right(w) =>
          val rendered = Renderer.renderShadedForward(w, lightDirection = Coord(-1, -1, -1), ambient = 0.35, xScale = 2)
          val rotation = Rotation(
            yaw = frameIndex * yawRotationRate,
            pitch = 0,
            roll = frameIndex * rollRotationRate
          )
          addRotationDetails(rendered, frameIndex, rotation)
        case Left(_) => "" // Skip errors
      }
    }
  }

  private def animate(frames: LazyList[String]): Unit = {
    try {
      var frameIndex = 0
      val frameIterator = frames.iterator
      
      while (frameIterator.hasNext && running) {
        // Check for quit keys before rendering frame
        lastKeyPressed match {
          case Some(113) | Some(81) => // 'q' or 'Q'
            println(s"\nDetected quit key - Quitting...")
            running = false
            return
          case Some(27) => // ESC key
            println(s"\nDetected ESC key - Exiting...")
            running = false
            return
          case _ => // Continue with animation
        }
        
        val frame = frameIterator.next()
        
        // Add key display to frame
        val frameWithKey = addKeyDisplay(frame)
        
        // Clear screen and move cursor to top-left
        print("\u001b[2J\u001b[H")
        print(frameWithKey)
        System.out.flush() // Ensure output is flushed
        Thread.sleep(frameDelayMs)
        
        frameIndex += 1
      }
    } finally {
      running = false
      inputThread.interrupt()
      disableRawMode()
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
    // Apply cumulative rotation from the start position for smooth animation
    val totalRotation = Rotation(
      yaw = frameIndex * yawRotationRate,    // Total yaw rotation up to this frame
      pitch = 0,                               // No pitch rotation
      roll = frameIndex * rollRotationRate   // Total roll rotation up to this frame
    )
    
    // Reset to start position and apply the total rotation
    val worldWithReset = world.reset.add(TriangleShapes.cube(shapeId, cubeSize), cubeCenter, totalRotation)
    Right(worldWithReset)
  }
  

  
  // Add control instructions to the rendered frame
  private def addKeyDisplay(frame: String): String = {
    val lines = frame.split("\n")
    if (lines.nonEmpty) {
      // Add control instructions at the bottom
      val controlsLine = "\nControls: Q/ESC = Quit"
      
      frame + controlsLine
    } else {
      frame
    }
  }
}
