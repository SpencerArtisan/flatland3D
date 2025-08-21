import org.scalatest.flatspec._
import org.scalatest.matchers._

class MainSpec extends AnyFlatSpec with should.Matchers {

  "Main" should "create an infinite world by default" in {
    val world = Main.buildWorld
    world.isInfinite should be(true)
  }

  it should "place all shapes at the specified center coordinates" in {
    val world = Main.buildWorld
    world.placements should have size 3
    
    val placements = world.placements.toSeq
    val cubeMovement = placements.find(_.shape.id == Main.CUBE_ID).get
    val tetraMovement = placements.find(_.shape.id == Main.TETRAHEDRON_ID).get
    val pyramidMovement = placements.find(_.shape.id == Main.PYRAMID_ID).get
    
    cubeMovement.origin should be(Main.CUBE_CENTER)
    tetraMovement.origin should be(Main.TETRAHEDRON_CENTER)
    pyramidMovement.origin should be(Main.PYRAMID_CENTER)
  }

  it should "use proportional shape size based on world size" in {
    val shapeSize = Main.SHAPE_SIZE
    val worldSize = Main.WORLD_SIZE
    
    // Shape size should be reasonable relative to world size
    shapeSize should be < worldSize
    shapeSize should be > 0
  }

  it should "create AnimationEngine with viewport support" in {
    val world = Main.buildWorld
    val userInteraction = new TestUserInteraction()
    
    val animationEngine = new AnimationEngine(
      world = world,
      userInteraction = userInteraction,
      worldSize = Main.WORLD_SIZE,
      cubeSize = Main.SHAPE_SIZE,
      cubeCenter = Main.SHAPES_CENTROID,
      shapeId = Main.CUBE_ID,
      frameDelayMs = Main.FRAME_DELAY_MS
    )
    
    // Should have viewport support
    animationEngine.getCurrentViewport should not be(None)
    
    // Default viewport should be centered on the shapes' centroid  
    val viewport = animationEngine.getCurrentViewport.get
    viewport.center should be(Main.SHAPES_CENTROID)
  }

  it should "support viewport manipulation through user interaction" in {
    val world = Main.buildWorld
    val userInteraction = new TestUserInteraction()
    
    val animationEngine = new AnimationEngine(
      world = world,
      userInteraction = userInteraction,
      worldSize = Main.WORLD_SIZE,
      cubeSize = Main.SHAPE_SIZE,
      cubeCenter = Main.SHAPES_CENTROID,
      shapeId = Main.CUBE_ID,
      frameDelayMs = Main.FRAME_DELAY_MS
    )
    
    // Test zoom functionality
    val zoomResult = animationEngine.zoomViewport(2.0)
    zoomResult should be('right)
    
    // Test pan functionality
    val panResult = animationEngine.panViewport(Coord(5, 3, 2))
    panResult should be('right)
    
    // Test reset functionality
    val resetResult = animationEngine.resetViewport()
    resetResult should be('right)
  }
}

// Mock TestUserInteraction for testing
class TestUserInteraction extends UserInteraction {
  // Delta accumulation for testing
  private var rotationDelta: Rotation = Rotation.ZERO
  private var viewportDelta: ViewportDelta = ViewportDelta.IDENTITY
  private var quitRequested: Boolean = false
  private var resetRequested: Boolean = false
  private var viewportResetRequested: Boolean = false
  
  // UserInteraction interface implementation
  override def getRotationDelta: Rotation = rotationDelta
  override def getViewportDelta: ViewportDelta = viewportDelta
  override def isQuitRequested: Boolean = quitRequested
  override def isResetRequested: Boolean = resetRequested
  override def isViewportResetRequested: Boolean = viewportResetRequested
  override def update(): Unit = {}
  override def cleanup(): Unit = {}
  override def clearDeltas(): Unit = {
    rotationDelta = Rotation.ZERO
    viewportDelta = ViewportDelta.IDENTITY
    resetRequested = false
    viewportResetRequested = false
  }
  
  // Test helper methods
  def setRotationDelta(delta: Rotation): Unit = rotationDelta = delta
  def setViewportDelta(delta: ViewportDelta): Unit = viewportDelta = delta
  def requestQuit(): Unit = quitRequested = true
  def requestReset(): Unit = resetRequested = true
  def requestViewportReset(): Unit = viewportResetRequested = true
  def clearRequests(): Unit = { 
    quitRequested = false
    resetRequested = false
    viewportResetRequested = false
  }
  
  // Additional test helper methods for backward compatibility
  def requestZoom(): Unit = {
    viewportDelta = viewportDelta.copy(zoomFactor = viewportDelta.zoomFactor * 1.2)
  }
  def requestPan(): Unit = {
    viewportDelta = viewportDelta.copy(panOffset = viewportDelta.panOffset + Coord(1, 1, 0))
  }
}
