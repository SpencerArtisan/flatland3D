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

  // Scale factor tests
  "AnimationEngine scale factor" should "scale both size and positions" in {
    val testUserInteraction = new TestEasterEggUserInteraction()
    val engine = new AnimationEngine(
      world = testWorld,
      userInteraction = testUserInteraction,
      worldSize = 22,
      cubeSize = 5,
      cubeCenter = Coord(10, 10, 10),
      shapeId = 101,
      frameDelayMs = 66
    )
    
    // Initial positions and sizes
    val initialWorld = engine.buildCurrentWorld()
    val initialPlacements = initialWorld.placements.toSeq
    val initialPos = initialPlacements.head.origin
    val initialSize = initialPlacements.head.shape match {
      case mesh: TriangleMesh => mesh.triangles.flatMap(t => Seq(t.v0, t.v1, t.v2)).map(_.magnitude).max
      case _ => 0.0
    }
    
    // Request scale up
    testUserInteraction.requestScaleUp()
    engine.processDeltas()
    
    // Force a rebuild of the world with the new scale
    val rotated = engine.rotateShapes(0)
    val scaledWorld = rotated.getOrElse(testWorld)
    val scaledPlacements = scaledWorld.placements.toSeq
    val scaledPos = scaledPlacements.head.origin
    val scaledSize = scaledPlacements.head.shape match {
      case mesh: TriangleMesh => mesh.triangles.flatMap(t => Seq(t.v0, t.v1, t.v2)).map(_.magnitude).max
      case _ => 0.0
    }
    
    // Both position and size should be scaled by 1.1
    println(s"Initial pos: $initialPos (mag: ${initialPos.magnitude})")
    println(s"Scaled pos: $scaledPos (mag: ${scaledPos.magnitude})")
    println(s"Initial size: $initialSize")
    println(s"Scaled size: $scaledSize")
    println(s"Scale factor: ${testUserInteraction.getScaleFactor}")
    println(s"Current scale: ${engine.getCurrentScale}")
    scaledPos.magnitude should be (initialPos.magnitude * 1.1 +- 0.001)
    scaledSize should be (initialSize * 1.1 +- 0.001)
  }

  // Easter egg mode tests
  "AnimationEngine Easter egg" should "detect Easter egg toggle requests from user interaction" in {
    // Create a user interaction that can simulate Easter egg toggle
    val easterEggUserInteraction = new TestEasterEggUserInteraction()
    val easterEggEngine = new AnimationEngine(
      world = testWorld,
      userInteraction = easterEggUserInteraction,
      worldSize = 22,
      cubeSize = 5,
      cubeCenter = Coord(10, 10, 10),
      shapeId = 101,
      frameDelayMs = 66
    )
    
    // Initially not in Easter egg mode
    easterEggEngine.isEasterEggActive should be(false)
    
    // Simulate Easter egg toggle request
    easterEggUserInteraction.requestEasterEggToggle()
    easterEggEngine.processDeltas()
    
    // Should now be in Easter egg mode
    easterEggEngine.isEasterEggActive should be(true)
  }

  it should "toggle Easter egg mode on and off" in {
    val easterEggUserInteraction = new TestEasterEggUserInteraction()
    val easterEggEngine = new AnimationEngine(
      world = testWorld,
      userInteraction = easterEggUserInteraction,
      worldSize = 22,
      cubeSize = 5,
      cubeCenter = Coord(10, 10, 10),
      shapeId = 101,
      frameDelayMs = 66
    )
    
    // Toggle on
    easterEggUserInteraction.requestEasterEggToggle()
    easterEggEngine.processDeltas()
    easterEggEngine.isEasterEggActive should be(true)
    
    // Toggle off
    easterEggUserInteraction.clearRequests()
    easterEggUserInteraction.requestEasterEggToggle()
    easterEggEngine.processDeltas()
    easterEggEngine.isEasterEggActive should be(false)
  }

  it should "build Elite world with single large Cobra spaceship when Easter egg is active" in {
    val easterEggUserInteraction = new TestEasterEggUserInteraction()
    val easterEggEngine = new AnimationEngine(
      world = testWorld,
      userInteraction = easterEggUserInteraction,
      worldSize = 22,
      cubeSize = 5,
      cubeCenter = Coord(10, 10, 10),
      shapeId = 101,
      frameDelayMs = 66
    )
    
    // Activate Easter egg mode
    easterEggUserInteraction.requestEasterEggToggle()
    easterEggEngine.processDeltas()
    
    // Build current world
    val eliteWorld = easterEggEngine.buildCurrentWorld()
    
    // Should contain exactly one Cobra spaceship
    val placements = eliteWorld.placements.toSeq
    placements should have length 1
    
    // The single shape should be a triangle mesh (Cobra spaceship)
    val cobraPlacement = placements.head
    cobraPlacement.shape shouldBe a[TriangleMesh]
    
    // Should be larger than original cubeSize (5) - expecting 9x scale = 45
    val cobra = cobraPlacement.shape.asInstanceOf[TriangleMesh]
    val vertices = cobra.triangles.flatMap(t => Seq(t.v0, t.v1, t.v2)).distinct
    val maxDimension = math.max(
      vertices.map(_.x).max - vertices.map(_.x).min,
      math.max(
        vertices.map(_.y).max - vertices.map(_.y).min,
        vertices.map(_.z).max - vertices.map(_.z).min
      )
    )
    maxDimension should be > 35.0 // Should be massive (9x scale)
  }
}

// Test user interaction class that supports Easter egg functionality
class TestEasterEggUserInteraction extends UserInteraction {
  // Delta accumulation for testing
  private var rotationDelta: Rotation = Rotation.ZERO
  private var viewportDelta: ViewportDelta = ViewportDelta.IDENTITY
  private var quitRequested: Boolean = false
  private var resetRequested: Boolean = false
  private var viewportResetRequested: Boolean = false
  private var easterEggToggleRequested: Boolean = false
  private var scaleFactor: Double = 1.0
  
  // UserInteraction interface implementation
  override def getRotationDelta: Rotation = rotationDelta
  override def getViewportDelta: ViewportDelta = viewportDelta
  override def getScaleFactor: Double = scaleFactor
  override def isQuitRequested: Boolean = quitRequested
  override def isResetRequested: Boolean = resetRequested
  override def isViewportResetRequested: Boolean = viewportResetRequested
  override def isEasterEggToggleRequested: Boolean = easterEggToggleRequested
  override def update(): Unit = {}
  override def cleanup(): Unit = {}
  override def clearDeltas(): Unit = {
    rotationDelta = Rotation.ZERO
    viewportDelta = ViewportDelta.IDENTITY
    // Don't reset scaleFactor - it should persist until explicitly changed
    resetRequested = false
    viewportResetRequested = false
    easterEggToggleRequested = false
  }
  
  // Test helper methods
  def setRotationDelta(delta: Rotation): Unit = rotationDelta = delta
  def setViewportDelta(delta: ViewportDelta): Unit = viewportDelta = delta
  def requestScaleUp(): Unit = scaleFactor = 1.1
  def requestScaleDown(): Unit = scaleFactor = 0.9
  def requestQuit(): Unit = quitRequested = true
  def requestReset(): Unit = resetRequested = true
  def requestViewportReset(): Unit = viewportResetRequested = true
  def requestEasterEggToggle(): Unit = easterEggToggleRequested = true
  def clearRequests(): Unit = { 
    quitRequested = false
    resetRequested = false
    viewportResetRequested = false
    easterEggToggleRequested = false
    scaleFactor = 1.0
  }
}