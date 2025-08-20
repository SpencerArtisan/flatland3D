object Main {
  private val CLEAR = "\u001b[2J"
  private val BLOCK = '\u2588'
  private val SHAPE_ID = 101

  def main(args: Array[String]): Unit =
    animate(buildAnimationFrames(buildWorld))

  private def buildWorld =
    World(300, 180, 60)
      .add(Box(SHAPE_ID, 40, 70, 20), Coord(40, 90, 40), Rotation.ZERO)

  private def buildAnimationFrames(world: World): Seq[String] =
    LazyList.from(0).map(rotateShapes(world, _)).collect {
      case Right(w) => Renderer.renderShaded(w, lightDirection = Coord(-1, -1, -1), ambient = 0.35, xScale = 2)
    }

  private def animate(frames: Seq[String]): Unit = {
    frames.foreach { frame =>
      Console.print(CLEAR)
      Console.print(frame)
      Thread.sleep(66)
    }
  }

  private def rotateShapes(world: World, frameIndex: Int): Either[NoSuchShape, World] = {
    // Apply cumulative rotation from the start position for smooth animation
    val totalRotation = Rotation(
      yaw = frameIndex * Math.PI / -36,    // Total yaw rotation up to this frame
      pitch = 0,                           // No pitch rotation
      roll = frameIndex * Math.PI / 72     // Total roll rotation up to this frame
    )
    
    // Reset to start position and apply the total rotation
    val worldWithReset = world.reset.add(Box(SHAPE_ID, 40, 70, 20), Coord(40, 90, 40), totalRotation)
    Right(worldWithReset)
  }
}