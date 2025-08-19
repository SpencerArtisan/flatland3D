object Main {
  private val CLEAR = "\u001b[2J"
  private val BLOCK = '\u2588'
  private val SHAPE_1_ID = 100
  private val SHAPE_2_ID = 101

  def main(args: Array[String]): Unit =
    animate(buildAnimationFrames3D(buildWorld3D))

  private def buildWorld =
    World(300, 200)
      .add(Rectangle(SHAPE_1_ID, 170, 70), Coord(20, 60), angle = 0, z = 1)
      .add(Rectangle(SHAPE_2_ID, 30, 70), Coord(200, 110), angle = 0, z = 2)

  private def buildWorld3D =
    World3D(300, 180, 60)
      .add(Box(SHAPE_1_ID, 80, 40, 30), Coord3(60, 60, 20), Rotation3.ZERO)
      .add(Box(SHAPE_2_ID, 40, 70, 20), Coord3(180, 90, 40), Rotation3.ZERO)

  private def buildAnimationFrames(world: World): Seq[String] =
    LazyList.from(0).map(rotateShapes(world, _)).collect {
      case Right(world) => Scene.from(world).render(BLOCK, ' ', 2)
    }

  private def buildAnimationFrames3D(world: World3D): Seq[String] =
    LazyList.from(0).map(rotateShapes3D(world, _)).collect {
      case Right(w) => Renderer3D.renderShaded(w, lightDirection = Coord3(-1, -1, -1), ambient = 0.35, xScale = 2, cullBackfaces = true)
    }

  private def animate(frames: Seq[String]): Unit =
    frames.foreach { frame =>
      Console.print(CLEAR)
      Console.print(frame)
      Thread.sleep(66)
    }

  private def rotateShapes(world: World, frameIndex: Int): Either[NoSuchShape, World] =
    for {
      world1 <- world.rotate(SHAPE_1_ID, frameIndex * Math.PI / 20)
      world2 <- world1.rotate(SHAPE_2_ID, frameIndex * Math.PI / -12)
    } yield world2

  private def rotateShapes3D(world: World3D, frameIndex: Int): Either[NoSuchShape, World3D] = {
    val delta1 = Rotation3(
      yaw = frameIndex * Math.PI / 60,
      pitch = frameIndex * Math.PI / 90,
      roll = frameIndex * Math.PI / 120
    )
    val delta2 = Rotation3(
      yaw = frameIndex * Math.PI / -36,
      pitch = 0,
      roll = frameIndex * Math.PI / 72
    )
    for {
      w1 <- world.rotate(SHAPE_1_ID, delta1)
      w2 <- w1.rotate(SHAPE_2_ID, delta2)
    } yield w2
  }
}