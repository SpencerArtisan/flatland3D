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

  it should "process KLIO keys correctly" in {
    // Test individual key processing directly
    keyboardInput.processInput(107) // 'k' - Pitch up
    keyboardInput.processInput(105) // 'i' - Yaw left
    
    val rotationDelta = keyboardInput.getRotationDelta
    rotationDelta.yaw should be < 0.0  // 'i' = yaw left (negative)
    rotationDelta.pitch should be > 0.0 // 'k' = pitch up (positive)
  }

  it should "process MJ keys for roll" in {
    keyboardInput.processInput(109) // 'm' - Roll left
    
    val rotationDelta = keyboardInput.getRotationDelta
    rotationDelta.roll should be < 0.0 // 'm' = roll left (negative)
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
    val input = "K\nL\nI\nO\n"
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
    // Test arrow key escape sequences: ESC[A = Up arrow
    keyboardInput.processInput(27) // ESC
    keyboardInput.processInput(91) // [
    keyboardInput.processInput(65) // A
    
    val deltaAfterUp = keyboardInput.getViewportDelta
    deltaAfterUp.panOffset.y should be < 0.0 // Pan up = negative Y
  }

  it should "handle viewport reset key" in {
    keyboardInput.processInput(118) // 'v'
    keyboardInput.isViewportResetRequested should be(true)
  }

  it should "combine rotation and viewport controls" in {
    // Test rotation
    keyboardInput.processInput(107) // 'k'
    val rotationDelta = keyboardInput.getRotationDelta
    rotationDelta.pitch should be > 0.0
    
    // Test zoom
    keyboardInput.processInput(43) // '+'
    val viewportDelta = keyboardInput.getViewportDelta
    viewportDelta.zoomFactor should be > 1.0
    
    // Test pan with proper escape sequence
    keyboardInput.processInput(27) // ESC
    keyboardInput.processInput(91) // [
    keyboardInput.processInput(65) // A (Up arrow)
    val finalViewportDelta = keyboardInput.getViewportDelta
    finalViewportDelta.panOffset.y should be < 0.0
  }
}
