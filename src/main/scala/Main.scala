object Main {
  // Configuration constants
  private val SHAPE_ID = 101
  private val WORLD_SIZE = 22
  private val CUBE_SIZE = 10
  private val CUBE_CENTER = Coord(11, 11, 11)
  private val FRAME_DELAY_MS = 66

  def main(args: Array[String]): Unit = {
    val world = buildWorld
    val animationEngine = new AnimationEngine(
      world = world,
      worldSize = WORLD_SIZE,
      cubeSize = CUBE_SIZE,
      cubeCenter = CUBE_CENTER,
      shapeId = SHAPE_ID,
      frameDelayMs = FRAME_DELAY_MS
    )
    
    println("Flatland3D Interactive Mode")
    println("Use WASD to rotate the cube, Z/X to roll, R to reset, Q/ESC to quit")
    
    animationEngine.run()
  }

  private def buildWorld =
    World(WORLD_SIZE, WORLD_SIZE, WORLD_SIZE)
      .add(TriangleShapes.cube(SHAPE_ID, CUBE_SIZE), CUBE_CENTER, Rotation.ZERO)
}