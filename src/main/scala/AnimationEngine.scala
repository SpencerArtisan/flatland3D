import scala.util.{Either, Left, Right}

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
    val clear = "\u001b[2J\u001b[H"
    frames.foreach { frame =>
      Console.print(clear)
      Console.print(frame)
      Thread.sleep(frameDelayMs)
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
}
