object Main {
  // Configuration constants
  val SHAPE_ID = 101
  val WORLD_SIZE = 22
  val CUBE_SIZE = 10
  val CUBE_CENTER = Coord(11, 11, 11)
  val FRAME_DELAY_MS = 66

  def main(args: Array[String]): Unit = {
    val world = buildWorld
    val userInteraction = new KeyboardInputManager()
    
    val animationEngine = new AnimationEngine(
      world = world,
      userInteraction = userInteraction,
      worldSize = WORLD_SIZE,
      cubeSize = CUBE_SIZE,
      cubeCenter = CUBE_CENTER,
      shapeId = SHAPE_ID,
      frameDelayMs = FRAME_DELAY_MS
    )
    
    println("Flatland3D Interactive Mode")
    println("Rotation: WASD to rotate the cube, Z/X to roll, R to reset")
    println("Viewport: +/=/- to zoom, Arrow keys to pan, V to reset viewport, Q/ESC to quit")
    
    animationEngine.run()
  }

  def buildWorld =
    World.infinite
      .add(TriangleShapes.cube(SHAPE_ID, CUBE_SIZE), CUBE_CENTER, Rotation.ZERO)
}