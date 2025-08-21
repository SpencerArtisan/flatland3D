import org.scalatest.flatspec._
import org.scalatest.matchers._
import org.scalatest.BeforeAndAfterEach
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, PrintStream}

class KeyboardInputManagerSpec extends AnyFlatSpec with should.Matchers with BeforeAndAfterEach {

  private var keyboardInput: KeyboardInputManager = _
  private var originalIn: java.io.InputStream = _
  private var originalOut: java.io.PrintStream = _

  override def beforeEach(): Unit = {
    keyboardInput = new KeyboardInputManager()
    originalIn = System.in
    originalOut = System.out
  }

  override def afterEach(): Unit = {
    System.setIn(originalIn)
    System.setOut(originalOut)
    keyboardInput.cleanup()
  }

  "KeyboardInputManager" should "implement UserInteraction interface" in {
    keyboardInput shouldBe a[UserInteraction]
  }

  it should "start with default values" in {
    keyboardInput.getRotationDelta should be(Rotation.ZERO)
    keyboardInput.getViewportDelta should be(ViewportDelta.IDENTITY)
    keyboardInput.isQuitRequested should be(false)
    keyboardInput.isResetRequested should be(false)
  }

  it should "process WASD keys correctly" in {
    // Test individual key processing directly
    keyboardInput.processInput(119) // 'w' - Pitch up
    keyboardInput.processInput(97)  // 'a' - Yaw left
    keyboardInput.processInput(115) // 's' - Pitch down
    keyboardInput.processInput(100) // 'd' - Yaw right
    
    val rotationDelta = keyboardInput.getRotationDelta
    rotationDelta.yaw should not be(0.0)
    rotationDelta.pitch should not be(0.0)
  }

  it should "process ZX keys for roll" in {
    keyboardInput.processInput(122) // 'z' - Roll left
    keyboardInput.processInput(120) // 'x' - Roll right
    
    val rotationDelta = keyboardInput.getRotationDelta
    rotationDelta.roll should not be(0.0)
  }

  it should "handle reset key correctly" in {
    keyboardInput.processInput(114) // 'r' - Reset
    keyboardInput.isResetRequested should be(true)
  }

  it should "detect quit requests" in {
    keyboardInput.processInput(113) // 'q' - Quit
    keyboardInput.isQuitRequested should be(true)
  }

  it should "ignore unknown keys" in {
    val input = "1\n2\n3\n"
    System.setIn(new ByteArrayInputStream(input.getBytes))
    
    keyboardInput.start()
    
    // Should not throw exceptions
    noException should be thrownBy {
      keyboardInput.update()
      keyboardInput.update()
      keyboardInput.update()
    }
  }

  it should "handle case-insensitive keys" in {
    val input = "W\nA\nS\nD\n"
    System.setIn(new ByteArrayInputStream(input.getBytes))
    
    keyboardInput.start()
    
    // Should not throw exceptions
    noException should be thrownBy {
      keyboardInput.update()
      keyboardInput.update()
      keyboardInput.update()
      keyboardInput.update()
    }
  }

  it should "process reset requests in update method" in {
    keyboardInput.processInput(114) // 'r' - Reset
    keyboardInput.isResetRequested should be(true)
  }

  it should "have no-op cleanup when not started" in {
    // Should not throw exceptions
    noException should be thrownBy keyboardInput.cleanup()
  }

  // New tests for viewport controls
  "KeyboardInputManager with viewport controls" should "process plus/minus keys for zoom" in {
    // Test zoom in (+)
    keyboardInput.processInput(43) // '+'
    val deltaAfterZoomIn = keyboardInput.getViewportDelta
    deltaAfterZoomIn.zoomFactor should be > 1.0
    
    // Test zoom out (-)
    keyboardInput.processInput(45) // '-'
    val deltaAfterZoomOut = keyboardInput.getViewportDelta
    deltaAfterZoomOut.zoomFactor should be < deltaAfterZoomIn.zoomFactor
  }

  it should "process arrow keys for panning" in {
    // Test panning in different directions
    keyboardInput.processInput(38) // Up arrow equivalent
    val deltaAfterUp = keyboardInput.getViewportDelta
    deltaAfterUp.panOffset.y should be < 0.0 // Pan up = negative Y
    
    keyboardInput.processInput(40) // Down arrow equivalent
    val deltaAfterDown = keyboardInput.getViewportDelta
    deltaAfterDown.panOffset.y should be(0.0) // Up + Down cancel out
    
    keyboardInput.processInput(60) // Left arrow equivalent
    val deltaAfterLeft = keyboardInput.getViewportDelta
    deltaAfterLeft.panOffset.x should be < 0.0 // Pan left = negative X
  }

  it should "handle viewport reset key" in {
    keyboardInput.processInput(118) // 'v'
    keyboardInput.isViewportResetRequested should be(true)
  }

  it should "combine rotation and viewport controls" in {
    // Test rotation
    keyboardInput.processInput(119) // 'w'
    val rotationDelta = keyboardInput.getRotationDelta
    rotationDelta.pitch should be > 0.0
    
    // Test zoom
    keyboardInput.processInput(43) // '+'
    val viewportDelta = keyboardInput.getViewportDelta
    viewportDelta.zoomFactor should be > 1.0
    
    // Test pan
    keyboardInput.processInput(38) // Up arrow equivalent
    val finalViewportDelta = keyboardInput.getViewportDelta
    finalViewportDelta.panOffset.y should be < 0.0
  }
}
