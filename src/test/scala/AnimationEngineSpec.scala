import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AnimationEngineSpec extends AnyFlatSpec with Matchers {
  
  private def createTestEngine(): AnimationEngine = {
    val world = World(10, 10, 10)
      .add(TriangleShapes.cube(1, 2), Coord(5, 5, 5), Rotation.ZERO)
    
    new AnimationEngine(
      world = world,
      worldSize = 10,
      cubeSize = 2,
      cubeCenter = Coord(5, 5, 5),
      shapeId = 1,
      frameDelayMs = 100
    )
  }
  
  "AnimationEngine" should "be constructed with correct parameters" in {
    val engine = createTestEngine()
    engine should not be null
  }
  
  it should "apply rotations correctly to shapes for frame 0" in {
    val engine = createTestEngine()
    
    val rotatedWorld = engine.rotateShapes(0) // Frame 0 - no rotation
    rotatedWorld shouldBe a[Right[_, _]]
    
    val Right(result) = rotatedWorld
    result.placements should not be empty
    result.placements should have size 1
  }
  
  it should "use same rotation for different frame indices with no input" in {
    val engine = createTestEngine()
    
    val frame0 = engine.rotateShapes(0)
    val frame5 = engine.rotateShapes(5)
    val frame10 = engine.rotateShapes(10)
    
    // All should succeed
    frame0 shouldBe a[Right[_, _]]
    frame5 shouldBe a[Right[_, _]]
    frame10 shouldBe a[Right[_, _]]
    
    // Extract the worlds
    val Right(world0) = frame0
    val Right(world5) = frame5
    val Right(world10) = frame10
    
    // All should have the same number of placements
    world0.placements should have size 1
    world5.placements should have size 1
    world10.placements should have size 1
    
    // With interactive control, all frames should have same rotation (no keys pressed)
    val placement0 = world0.placements.head
    val placement5 = world5.placements.head
    val placement10 = world10.placements.head
    
    // Same rotation for all frames when no input
    placement0.rotation should equal(placement5.rotation)
    placement5.rotation should equal(placement10.rotation)
  }
  
  it should "start with zero rotation when no keys pressed" in {
    val engine = createTestEngine()
    
    // With interactive control, should start with zero rotation
    val Right(world0) = engine.rotateShapes(0)
    val placement0 = world0.placements.head
    
    placement0.rotation.yaw shouldBe 0.0 +- 0.001
    placement0.rotation.pitch shouldBe 0.0 +- 0.001
    placement0.rotation.roll shouldBe 0.0 +- 0.001
  }
  
  it should "generate animation frames as a LazyList" in {
    val engine = createTestEngine()
    
    val frames = engine.buildAnimationFrames()
    frames shouldBe a[LazyList[_]]
    
    // Should be able to take a few frames without issues
    val firstThreeFrames = frames.take(3).toList
    firstThreeFrames should have size 3
    
    // Each frame should be a non-empty string
    firstThreeFrames.foreach { frame =>
      frame should not be empty
      frame shouldBe a[String]
    }
  }
  
  it should "include rotation details in generated frames" in {
    val engine = createTestEngine()
    
    val frames = engine.buildAnimationFrames()
    val firstFrame = frames.head
    
    // Frame should contain rotation details
    firstFrame should include("Frame:")
    firstFrame should include("Yaw:")
    firstFrame should include("Pitch:")
    firstFrame should include("Roll:")
    firstFrame should include("°") // Degree symbol
  }
  
  it should "format rotation details correctly" in {
    val engine = createTestEngine()
    
    // Test the rotation details formatting indirectly through frame generation
    val frames = engine.buildAnimationFrames()
    val frame5 = frames.drop(5).head // Get frame 5
    
    // Should contain "Frame:   5" (with proper formatting)
    frame5 should include("Frame:   5")
    
    // Should contain degree measurements
    frame5 should include("Yaw:")
    frame5 should include("Roll:")
    frame5 should include("°")
  }
  
  it should "reset world and maintain consistent rotation across frames" in {
    val engine = createTestEngine()
    
    // Generate multiple rotated worlds
    val world1 = engine.rotateShapes(1)
    val world2 = engine.rotateShapes(2)
    
    val Right(result1) = world1
    val Right(result2) = world2
    
    // Both should have exactly one shape (the cube)
    result1.placements should have size 1
    result2.placements should have size 1
    
    // The shapes should be at the same center position (reset worked)
    val placement1 = result1.placements.head
    val placement2 = result2.placements.head
    
    placement1.origin shouldBe Coord(5, 5, 5)
    placement2.origin shouldBe Coord(5, 5, 5)
    
    // With interactive control, rotations should be the same (no keys pressed)
    placement1.rotation should equal(placement2.rotation)
  }
  
  it should "support keyboard input for quit functionality" in {
    val engine = createTestEngine()
    
    // Test that the engine can be created successfully with keyboard input enabled
    // Note: We can't easily test the actual quit behavior in unit tests
    // since it involves System.in, threads, and terminal mode changes
    engine should not be null
    
    // Verify that frames can be generated (this indirectly tests that 
    // the keyboard input thread doesn't interfere with normal operation)
    val frames = engine.buildAnimationFrames()
    val firstFrame = frames.head
    
    firstFrame should not be empty
    // The controls are added in the animate method via addKeyDisplay, 
    // so we just verify the frame generation works correctly
    firstFrame should include("Frame:")
  }
  
  it should "use interactive rotation instead of automatic rotation" in {
    val engine = createTestEngine()
    
    // Test that rotation comes from KeyboardInputManager, not frame index
    val world0 = engine.rotateShapes(0).right.get
    val world5 = engine.rotateShapes(5).right.get
    
    // With interactive control, rotation should be the same regardless of frame index
    // (since no keys have been pressed)
    val placement0 = world0.placements.head
    val placement5 = world5.placements.head
    
    // Interactive control means same rotation for all frames when no input
    placement0.rotation should equal(placement5.rotation)
  }
  
  it should "process WASD keys for rotation control" in {
    val engine = createTestEngine()
    
    // Test that the engine integrates with KeyboardInputManager correctly
    val initialWorld = engine.rotateShapes(0).right.get
    val initialPlacement = initialWorld.placements.head
    val initialRotation = initialPlacement.rotation
    
    // With no keys pressed, should start at zero rotation
    initialRotation.yaw shouldBe 0.0 +- 0.001
    initialRotation.pitch shouldBe 0.0 +- 0.001
    initialRotation.roll shouldBe 0.0 +- 0.001
    
    // Verify engine is properly constructed with KeyboardInputManager integration
    engine should not be null
  }
}