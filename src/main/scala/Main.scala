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
      case Right(w) => Renderer.renderShaded(w, lightDirection = Coord(-1, -1, -1), ambient = 0.35, xScale = 2, cullBackfaces = true)
    }

  private def animate(frames: Seq[String]): Unit = {
    var frameIndex = 0
    frames.foreach { frame =>
      Console.print(CLEAR)
      Console.print(frame)
      printFrameDiagnostics(frameIndex)
      frameIndex += 1
      Thread.sleep(66)
    }
  }

  private def printFrameDiagnostics(frameIndex: Int): Unit = {
    val world = buildWorld
    val rotatedWorld = rotateShapes(world, frameIndex)
    rotatedWorld match {
      case Right(w) =>
        val rendered = Renderer.renderShaded(w, lightDirection = Coord(-1, -1, -1), ambient = 0.35, xScale = 2, cullBackfaces = true)
        val lines = rendered.split("\n")
        
        // Find bounding box of rendered content
        var minX = Int.MaxValue
        var maxX = Int.MinValue
        var minY = Int.MaxValue
        var maxY = Int.MinValue
        
        for (y <- lines.indices; x <- lines(y).indices) {
          if (lines(y)(x) != ' ') {
            minX = Math.min(minX, x)
            maxX = Math.max(maxX, x)
            minY = Math.min(minY, y)
            maxY = Math.max(maxY, y)
          }
        }
        
        if (minX != Int.MaxValue) {
          val width = maxX - minX + 1
          val height = maxY - minY + 1
          println(s"\nFrame $frameIndex: bounds ${width}x${height} at ($minX,$minY), aspect ${width.toDouble / height}")
        }
        
      case Left(error) =>
        println(s"\nFrame $frameIndex: Error - $error")
    }
  }

  private def rotateShapes(world: World, frameIndex: Int): Either[NoSuchShape, World] = {
    val delta = Rotation(
      yaw = frameIndex * Math.PI / -36,
      pitch = 0,
      roll = frameIndex * Math.PI / 72
    )
    world.rotate(SHAPE_ID, delta)
  }
}