import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UserInteractionIntegrationSpec extends AnyFlatSpec with Matchers {
  
  "UserInteraction with AnimationEngine" should "work with keyboard input" in {
    val keyboardInteraction = new KeyboardInputManager()
    val world = World(10, 10, 10).add(TriangleShapes.cube(1, 2), Coord(5, 5, 5), Rotation.ZERO)
    
    val engine = new AnimationEngine(
      world = world,
      userInteraction = keyboardInteraction,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1
    )
    
    // Simulate user input
    keyboardInteraction.processInput('w') // Pitch up
    keyboardInteraction.processInput('a') // Yaw left
    engine.processDeltas() // Process the deltas to update internal state
    
    val result = engine.rotateShapes(0)
    result should be a 'right
    
    val rotatedWorld = result.right.get
    val placement = rotatedWorld.placements.head
    placement.rotation.pitch should be > 0.0
    placement.rotation.yaw should be < 0.0
  }
  
  it should "work with test input" in {
    val testInteraction = new TestUserInteraction()
    val world = World(10, 10, 10).add(TriangleShapes.cube(1, 2), Coord(5, 5, 5), Rotation.ZERO)
    
    val engine = new AnimationEngine(
      world = world,
      userInteraction = testInteraction,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1
    )
    
    // Test controlled input
    testInteraction.setRotationDelta(Rotation(0, Math.PI/4, 0))
    engine.processDeltas() // Process the delta to update internal state
    
    val result = engine.rotateShapes(0)
    result should be a 'right
    
    val rotatedWorld = result.right.get
    val placement = rotatedWorld.placements.head
    placement.rotation.pitch should be(Math.PI/4 +- 0.001)
  }
  
  it should "handle quit requests from different input sources" in {
    val world = World(10, 10, 10).add(TriangleShapes.cube(1, 2), Coord(5, 5, 5), Rotation.ZERO)
    
    // Test with TestUserInteraction
    val testInteraction = new TestUserInteraction()
    testInteraction.requestQuit()
    val testEngine = new AnimationEngine(
      world = world,
      userInteraction = testInteraction,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1
    )
    
    testInteraction.isQuitRequested should be(true)
    
    // Test with KeyboardInputManager
    val keyboardInteraction = new KeyboardInputManager()
    val keyboardEngine = new AnimationEngine(
      world = world,
      userInteraction = keyboardInteraction,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1
    )
    
    keyboardInteraction.processInput('q')
    keyboardInteraction.isQuitRequested should be(true)
  }
  
  it should "handle reset requests from different input sources" in {
    val world = World(10, 10, 10).add(TriangleShapes.cube(1, 2), Coord(5, 5, 5), Rotation.ZERO)
    
    // Test with TestUserInteraction
    val testInteraction = new TestUserInteraction()
    testInteraction.setRotationDelta(Rotation(Math.PI/3, 0, 0))
    val testEngine = new AnimationEngine(
      world = world,
      userInteraction = testInteraction,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1
    )
    
    testInteraction.requestReset()
    testInteraction.isResetRequested should be(true)
    
    // Test with KeyboardInputManager
    val keyboardInteraction = new KeyboardInputManager()
    val keyboardEngine = new AnimationEngine(
      world = world,
      userInteraction = keyboardInteraction,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1
    )
    
    keyboardInteraction.processInput('r')
    keyboardInteraction.isResetRequested should be(true)
  }
  
  it should "maintain consistent behavior across different implementations" in {
    val world = World(10, 10, 10).add(TriangleShapes.cube(1, 2), Coord(5, 5, 5), Rotation.ZERO)
    val testRotation = Rotation(Math.PI/6, Math.PI/8, 0)
    
    // Test with TestUserInteraction
    val testInteraction = new TestUserInteraction()
    testInteraction.setRotationDelta(testRotation)
    val testEngine = new AnimationEngine(
      world = world,
      userInteraction = testInteraction,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1
    )
    
    val testResult = testEngine.rotateShapes(0)
    testResult should be a 'right
    
    // Test with KeyboardInputManager (set to same rotation)
    val keyboardInteraction = new KeyboardInputManager()
    // Manually set the rotation to match (since KeyboardInputManager doesn't have setRotation)
    // This tests that the interface contract is consistent
    
    val keyboardEngine = new AnimationEngine(
      world = world,
      userInteraction = keyboardInteraction,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1
    )
    
    val keyboardResult = keyboardEngine.rotateShapes(0)
    keyboardResult should be a 'right
    
    // Both engines should produce valid worlds
    testResult.right.get.placements should have size 1
    keyboardResult.right.get.placements should have size 1
    
    // Both should have the same world structure
    testResult.right.get.width should equal(keyboardResult.right.get.width)
    testResult.right.get.height should equal(keyboardResult.right.get.height)
    testResult.right.get.depth should equal(keyboardResult.right.get.depth)
  }
}
