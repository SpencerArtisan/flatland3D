import org.scalatest.flatspec._
import org.scalatest.matchers._
import org.scalatest.BeforeAndAfterEach

class AnimationEngineSpec extends AnyFlatSpec with should.Matchers with BeforeAndAfterEach {

  // Test data
  private val testWorld = World.infinite
    .add(TriangleShapes.cube(101, 5), Coord(10, 10, 10))
  
  private val testUserInteraction = new TestUserInteraction()
  
  private val animationEngine = new AnimationEngine(
    world = testWorld,
    userInteraction = testUserInteraction,
    worldSize = 22,
    cubeSize = 5,
    cubeCenter = Coord(10, 10, 10),
    shapeId = 101,
    frameDelayMs = 66
  )

  override def afterEach(): Unit = {
    testUserInteraction.clearRequests()
    testUserInteraction.clearDeltas()
    animationEngine.resetState()
  }

  "AnimationEngine" should "apply user rotation to world correctly" in {
    val rotationDelta = Rotation(yaw = Math.PI / 4, pitch = Math.PI / 6, roll = 0)
    testUserInteraction.setRotationDelta(rotationDelta)
    
    // Process the deltas
    animationEngine.processDeltas()
    
    // Check that AnimationEngine accumulated the rotation
    val currentRotation = animationEngine.getCurrentRotation
    currentRotation.yaw should be(Math.PI / 4 +- 0.001)
    currentRotation.pitch should be(Math.PI / 6 +- 0.001)
    currentRotation.roll should be(0.0 +- 0.001)
  }

  it should "handle quit requests correctly" in {
    testUserInteraction.requestQuit()
    
    testUserInteraction.isQuitRequested should be(true)
  }

  it should "handle reset requests correctly" in {
    // Apply some rotation first
    val initialRotationDelta = Rotation(yaw = Math.PI / 4, pitch = Math.PI / 6, roll = 0)
    testUserInteraction.setRotationDelta(initialRotationDelta)
    animationEngine.processDeltas()
    
    // Verify rotation was applied
    val rotationAfterDelta = animationEngine.getCurrentRotation
    rotationAfterDelta should not be(Rotation.ZERO)
    
    // Request reset and process deltas
    testUserInteraction.requestReset()
    animationEngine.processDeltas()
    
    // Verify rotation was reset to zero
    animationEngine.getCurrentRotation should be(Rotation.ZERO)
  }

  it should "build animation frames with user rotation" in {
    val rotationDelta = Rotation(yaw = Math.PI / 6, pitch = 0, roll = 0)
    testUserInteraction.setRotationDelta(rotationDelta)
    animationEngine.processDeltas()
    
    val frames = animationEngine.buildAnimationFrames()
    frames should not be empty
    
    // First frame should contain the rotated cube
    val firstFrame = frames.head
    firstFrame should not be empty
    
    // Verify the rotation was applied
    val currentRotation = animationEngine.getCurrentRotation
    currentRotation.yaw should be(Math.PI / 6 +- 0.001)
  }

  it should "handle multiple rotation changes correctly" in {
    val delta1 = Rotation(yaw = Math.PI / 4, pitch = 0, roll = 0)
    val delta2 = Rotation(yaw = Math.PI / 4, pitch = Math.PI / 6, roll = 0) // Additional delta
    
    // Apply first delta
    testUserInteraction.setRotationDelta(delta1)
    animationEngine.processDeltas()
    
    val rotationAfterFirst = animationEngine.getCurrentRotation
    rotationAfterFirst.yaw should be(Math.PI / 4 +- 0.001)
    
    // Clear deltas and apply second delta
    testUserInteraction.clearDeltas()
    testUserInteraction.setRotationDelta(delta2)
    animationEngine.processDeltas()
    
    val rotationAfterSecond = animationEngine.getCurrentRotation
    rotationAfterSecond.yaw should be(Math.PI / 2 +- 0.001) // π/4 + π/4
    rotationAfterSecond.pitch should be(Math.PI / 6 +- 0.001)
  }

  it should "work with different UserInteraction implementations" in {
    val keyboardInteraction = new KeyboardInputManager()
    
    val keyboardEngine = new AnimationEngine(
      world = testWorld,
      userInteraction = keyboardInteraction,
      worldSize = 22,
      cubeSize = 5,
      cubeCenter = Coord(10, 10, 10),
      shapeId = 101,
      frameDelayMs = 66
    )
    
    // Should not throw any exceptions
    noException should be thrownBy keyboardEngine.rotateShapes(0)
  }

  // New tests for viewport functionality
  "AnimationEngine with viewport" should "support viewport zoom in/out" in {
    val viewportEngine = new AnimationEngine(
      world = testWorld,
      userInteraction = testUserInteraction,
      worldSize = 22,
      cubeSize = 5,
      cubeCenter = Coord(10, 10, 10),
      shapeId = 101,
      frameDelayMs = 66
    )
    
    // Test zoom in
    val zoomedIn = viewportEngine.zoomViewport(2.0)
    zoomedIn should be('right)
    
    // Test zoom out
    val zoomedOut = viewportEngine.zoomViewport(0.5)
    zoomedOut should be('right)
  }

  it should "support viewport panning" in {
    val viewportEngine = new AnimationEngine(
      world = testWorld,
      userInteraction = testUserInteraction,
      worldSize = 22,
      cubeSize = 5,
      cubeCenter = Coord(10, 10, 10),
      shapeId = 101,
      frameDelayMs = 66
    )
    
    val panned = viewportEngine.panViewport(Coord(5, 3, 2))
    panned should be('right)
  }

  it should "support viewport reset" in {
    val viewportEngine = new AnimationEngine(
      world = testWorld,
      userInteraction = testUserInteraction,
      worldSize = 22,
      cubeSize = 5,
      cubeCenter = Coord(10, 10, 10),
      shapeId = 101,
      frameDelayMs = 66
    )
    
    val reset = viewportEngine.resetViewport()
    reset should be('right)
  }

  it should "maintain viewport state across frames" in {
    val viewportEngine = new AnimationEngine(
      world = testWorld,
      userInteraction = testUserInteraction,
      worldSize = 22,
      cubeSize = 5,
      cubeCenter = Coord(10, 10, 10),
      shapeId = 101,
      frameDelayMs = 66
    )
    
    // Set a custom viewport
    val customViewport = Viewport(Coord(5, 5, 5), 15, 15, 15)
    viewportEngine.setViewport(customViewport)
    
    // Build frames - should use the custom viewport
    val frames = viewportEngine.buildAnimationFrames()
    frames should not be empty
    
    // Verify viewport was maintained
    viewportEngine.getCurrentViewport should be(Some(customViewport))
  }

  it should "process viewport deltas from UserInteraction" in {
    val testInteraction = new TestUserInteraction()
    val viewportEngine = new AnimationEngine(
      world = testWorld,
      userInteraction = testInteraction,
      worldSize = 22,
      cubeSize = 5,
      cubeCenter = Coord(10, 10, 10),
      shapeId = 101,
      frameDelayMs = 66
    )
    
    val initialViewport = viewportEngine.getCurrentViewport.get
    
    // Set viewport delta via user interaction
    val zoomDelta = ViewportDelta.zoomIn(1.5)
    testInteraction.setViewportDelta(zoomDelta)
    
    // Process the deltas
    viewportEngine.processDeltas()
    
    // Verify viewport was changed
    val newViewport = viewportEngine.getCurrentViewport.get
    newViewport.width should be < initialViewport.width // Zoomed in = smaller viewport
  }

  it should "handle zoom deltas from UserInteraction" in {
    val testInteraction = new TestUserInteraction()
    val viewportEngine = new AnimationEngine(
      world = testWorld,
      userInteraction = testInteraction,
      worldSize = 22,
      cubeSize = 5,
      cubeCenter = Coord(10, 10, 10),
      shapeId = 101,
      frameDelayMs = 66
    )
    
    // Set zoom delta
    val zoomDelta = ViewportDelta.zoomIn(1.5)
    testInteraction.setViewportDelta(zoomDelta)
    
    testInteraction.getViewportDelta.zoomFactor should be(1.5)
    
    // Clear deltas
    testInteraction.clearDeltas()
    testInteraction.getViewportDelta should be(ViewportDelta.IDENTITY)
  }

  it should "handle pan deltas from UserInteraction" in {
    val testInteraction = new TestUserInteraction()
    val viewportEngine = new AnimationEngine(
      world = testWorld,
      userInteraction = testInteraction,
      worldSize = 22,
      cubeSize = 5,
      cubeCenter = Coord(10, 10, 10),
      shapeId = 101,
      frameDelayMs = 66
    )
    
    // Set pan delta
    val panDelta = ViewportDelta.pan(Coord(5, 3, 2))
    testInteraction.setViewportDelta(panDelta)
    
    testInteraction.getViewportDelta.panOffset should be(Coord(5, 3, 2))
    
    // Clear deltas
    testInteraction.clearDeltas()
    testInteraction.getViewportDelta.panOffset should be(Coord.ZERO)
  }

  it should "handle viewport reset requests from UserInteraction" in {
    val testInteraction = new TestUserInteraction()
    val viewportEngine = new AnimationEngine(
      world = testWorld,
      userInteraction = testInteraction,
      worldSize = 22,
      cubeSize = 5,
      cubeCenter = Coord(10, 10, 10),
      shapeId = 101,
      frameDelayMs = 66
    )
    
    // Request viewport reset
    testInteraction.requestViewportReset()
    
    testInteraction.isViewportResetRequested should be(true)
    
    // Clear deltas
    testInteraction.clearDeltas()
    testInteraction.isViewportResetRequested should be(false)
  }

  it should "combine rotation and viewport deltas from UserInteraction" in {
    val testInteraction = new TestUserInteraction()
    val viewportEngine = new AnimationEngine(
      world = testWorld,
      userInteraction = testInteraction,
      worldSize = 22,
      cubeSize = 5,
      cubeCenter = Coord(10, 10, 10),
      shapeId = 101,
      frameDelayMs = 66
    )
    
    // Set rotation delta
    val rotationDelta = Rotation(Math.PI / 4, Math.PI / 6, 0)
    testInteraction.setRotationDelta(rotationDelta)
    
    // Set viewport delta
    val viewportDelta = ViewportDelta(zoomFactor = 1.5, panOffset = Coord(2, 3, 1))
    testInteraction.setViewportDelta(viewportDelta)
    
    // Verify both deltas are available
    testInteraction.getRotationDelta should be(rotationDelta)
    testInteraction.getViewportDelta should be(viewportDelta)
  }
}