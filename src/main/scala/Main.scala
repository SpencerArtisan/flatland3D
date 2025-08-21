object Main {
  private val CLEAR = "\u001b[2J"
  private val BLOCK = '\u2588'
  private val SHAPE_ID = 101

  def main(args: Array[String]): Unit =
    animate(buildAnimationFrames(buildWorld))

  private def buildWorld =
    World(22, 22, 22)
      .add(TriangleShapes.cube(SHAPE_ID, 10), Coord(11, 11, 11), Rotation.ZERO)

  private def buildAnimationFrames(world: World): Seq[String] =
    LazyList.from(0).map { frameIndex =>
      rotateShapes(world, frameIndex) match {
        case Right(w) =>
          val rendered = Renderer.renderShadedForward(w, lightDirection = Coord(-1, -1, -1), ambient = 0.35, xScale = 2)
          val rotation = Rotation(
            yaw = frameIndex * Math.PI / -36,
            pitch = 0,
            roll = frameIndex * Math.PI / 72
          )
          addRotationDetails(rendered, frameIndex, rotation)
        case Left(_) => "" // Skip errors
      }
    }

  private def animate(frames: Seq[String]): Unit = {
    frames.foreach { frame =>
      Console.print(CLEAR)
      Console.print(frame)
      Thread.sleep(66)
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

  private def rotateShapes(world: World, frameIndex: Int): Either[NoSuchShape, World] = {
    // Apply cumulative rotation from the start position for smooth animation
    val totalRotation = Rotation(
      yaw = frameIndex * Math.PI / -36,    // Total yaw rotation up to this frame
      pitch = 0,                           // No pitch rotation
      roll = frameIndex * Math.PI / 72     // Total roll rotation up to this frame
    )
    
    // Reset to start position and apply the total rotation
    val worldWithReset = world.reset.add(TriangleShapes.cube(SHAPE_ID, 10), Coord(11, 11, 11), totalRotation)
    Right(worldWithReset)
  }
}