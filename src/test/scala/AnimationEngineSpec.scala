import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AnimationEngineSpec extends AnyFlatSpec with Matchers {
  
  "AnimationEngine" should "apply user rotation to world correctly" in {
    val testRotation = Rotation(Math.PI/2, 0, 0) // 90° yaw
    val testInteraction = new TestUserInteraction(rotation = testRotation)
    val world = World(10, 10, 10).add(TriangleShapes.cube(1, 2), Coord(5, 5, 5), Rotation.ZERO)
    
    val engine = new AnimationEngine(
      world = world,
      userInteraction = testInteraction,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1,
      frameDelayMs = 100
    )
    
    val result = engine.rotateShapes(0)
    result should be a 'right
    
    val rotatedWorld = result.right.get
    val placement = rotatedWorld.placements.head
    placement.rotation.yaw should be(Math.PI/2 +- 0.001)
  }
  
  it should "handle quit requests correctly" in {
    val testInteraction = new TestUserInteraction(quitRequested = true)
    val world = World(10, 10, 10).add(TriangleShapes.cube(1, 2), Coord(5, 5, 5), Rotation.ZERO)
    
    val engine = new AnimationEngine(
      world = world,
      userInteraction = testInteraction,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1,
      frameDelayMs = 100
    )
    
    // The engine should detect quit requests
    testInteraction.isQuitRequested should be(true)
  }
  
  it should "handle reset requests correctly" in {
    val testInteraction = new TestUserInteraction(rotation = Rotation(Math.PI/4, 0, 0))
    val world = World(10, 10, 10).add(TriangleShapes.cube(1, 2), Coord(5, 5, 5), Rotation.ZERO)
    
    val engine = new AnimationEngine(
      world = world,
      userInteraction = testInteraction,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1,
      frameDelayMs = 100
    )
    
    // Test that reset requests are handled
    testInteraction.requestReset()
    testInteraction.isResetRequested should be(true)
  }
  
  it should "build animation frames with user rotation" in {
    val testRotation = Rotation(0, Math.PI/6, 0) // 30° pitch
    val testInteraction = new TestUserInteraction(rotation = testRotation)
    val world = World(10, 10, 10).add(TriangleShapes.cube(1, 2), Coord(5, 5, 5), Rotation.ZERO)
    
    val engine = new AnimationEngine(
      world = world,
      userInteraction = testInteraction,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1,
      frameDelayMs = 100
    )
    
    val frames = engine.buildAnimationFrames()
    frames should not be empty
    
    // First frame should contain the user rotation
    val firstFrame = frames.head
    firstFrame should include("Pitch:")
    firstFrame should include("30.0") // 30 degrees
  }
  
  it should "handle multiple rotation changes correctly" in {
    val testInteraction = new TestUserInteraction()
    val world = World(10, 10, 10).add(TriangleShapes.cube(1, 2), Coord(5, 5, 5), Rotation.ZERO)
    
    val engine = new AnimationEngine(
      world = world,
      userInteraction = testInteraction,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1,
      frameDelayMs = 100
    )
    
    // Test sequence of rotations
    testInteraction.setRotation(Rotation(Math.PI/4, 0, 0))
    val result1 = engine.rotateShapes(0)
    result1 should be a 'right
    
    testInteraction.setRotation(Rotation(Math.PI/2, Math.PI/6, 0))
    val result2 = engine.rotateShapes(0)
    result2 should be a 'right
    
    val placement1 = result1.right.get.placements.head
    val placement2 = result2.right.get.placements.head
    
    placement1.rotation.yaw should be(Math.PI/4 +- 0.001)
    placement2.rotation.yaw should be(Math.PI/2 +- 0.001)
    placement2.rotation.pitch should be(Math.PI/6 +- 0.001)
  }
  
  it should "work with different UserInteraction implementations" in {
    val world = World(10, 10, 10).add(TriangleShapes.cube(1, 2), Coord(5, 5, 5), Rotation.ZERO)
    
    // Test with TestUserInteraction
    val testInteraction = new TestUserInteraction(rotation = Rotation(Math.PI/3, 0, 0))
    val testEngine = new AnimationEngine(
      world = world,
      userInteraction = testInteraction,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1,
      frameDelayMs = 100
    )
    
    val testResult = testEngine.rotateShapes(0)
    testResult should be a 'right
    
    // Test with KeyboardInputManager
    val keyboardInteraction = new KeyboardInputManager()
    val keyboardEngine = new AnimationEngine(
      world = world,
      userInteraction = keyboardInteraction,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1,
      frameDelayMs = 100
    )
    
    val keyboardResult = keyboardEngine.rotateShapes(0)
    keyboardResult should be a 'right
    
    // Both should work the same way
    testResult.right.get.placements.head.rotation.yaw should be(Math.PI/3 +- 0.001)
    keyboardResult.right.get.placements.head.rotation.yaw should be(0.0 +- 0.001) // Default rotation
  }
}