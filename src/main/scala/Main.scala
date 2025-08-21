object Main {
  // Configuration constants  
  val CUBE_ID = 101
  val TETRAHEDRON_ID = 102
  val PYRAMID_ID = 103
  val WORLD_SIZE = 60  // Increased to accommodate multiple shapes
  val SHAPE_SIZE = 8   // Slightly smaller shapes to fit better
  // Shape positions - arranged in a triangle formation for better viewing
  val CUBE_CENTER = Coord(20, 30, 30)
  val TETRAHEDRON_CENTER = Coord(40, 30, 30)
  val PYRAMID_CENTER = Coord(30, 45, 30)
  val SHAPES_CENTROID = Coord(30, 35, 30)  // Center point between all shapes

  def main(args: Array[String]): Unit = {
    val world = buildWorld
    val userInteraction = new KeyboardInputManager()
    
    val animationEngine = new AnimationEngine(
      world = world,
      userInteraction = userInteraction,
      worldSize = WORLD_SIZE,
      cubeSize = SHAPE_SIZE,
      cubeCenter = SHAPES_CENTROID,
      shapeId = CUBE_ID
    )
    
    println("Flatland3D Interactive Mode - Multi-Shape Demo")
    println("Shapes: Cube, Tetrahedron, and Pyramid")
    println("Rotation: WASD to rotate all shapes, Z/X to roll, R to reset")
    println("Viewport: +/=/- to zoom, Arrow keys to pan, V to reset viewport, Q/ESC to quit")
    
    animationEngine.run()
  }

  def buildWorld =
    World.infinite
      .add(TriangleShapes.cube(CUBE_ID, SHAPE_SIZE), CUBE_CENTER, Rotation.ZERO)
      .add(TriangleShapes.tetrahedron(TETRAHEDRON_ID, SHAPE_SIZE), TETRAHEDRON_CENTER, Rotation.ZERO)
      .add(TriangleShapes.pyramid(PYRAMID_ID, SHAPE_SIZE, SHAPE_SIZE * 1.2), PYRAMID_CENTER, Rotation.ZERO)
}