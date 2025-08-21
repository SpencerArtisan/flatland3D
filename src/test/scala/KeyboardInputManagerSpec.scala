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
    keyboardInput.getCurrentRotation should be(Rotation.ZERO)
    keyboardInput.isQuitRequested should be(false)
    keyboardInput.isResetRequested should be(false)
  }

  it should "process WASD keys correctly" in {
    // Simulate key presses
    val input = "w\na\ns\nd\n"
    System.setIn(new ByteArrayInputStream(input.getBytes))
    
    keyboardInput.start()
    
    // Process keys
    keyboardInput.update()
    keyboardInput.update()
    keyboardInput.update()
    keyboardInput.update()
    
    val rotation = keyboardInput.getCurrentRotation
    rotation.yaw should not be(0.0)
    rotation.pitch should not be(0.0)
  }

  it should "process ZX keys for roll" in {
    val input = "z\nx\n"
    System.setIn(new ByteArrayInputStream(input.getBytes))
    
    keyboardInput.start()
    
    keyboardInput.update()
    keyboardInput.update()
    
    val rotation = keyboardInput.getCurrentRotation
    rotation.roll should not be(0.0)
  }

  it should "handle reset key correctly" in {
    // Set some rotation first
    val input = "w\na\nr\n"
    System.setIn(new ByteArrayInputStream(input.getBytes))
    
    keyboardInput.start()
    
    keyboardInput.update()
    keyboardInput.update()
    keyboardInput.update()
    
    keyboardInput.isResetRequested should be(true)
  }

  it should "detect quit requests" in {
    val input = "q\n"
    System.setIn(new ByteArrayInputStream(input.getBytes))
    
    keyboardInput.start()
    keyboardInput.update()
    
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
    val input = "r\n"
    System.setIn(new ByteArrayInputStream(input.getBytes))
    
    keyboardInput.start()
    keyboardInput.update()
    
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
    keyboardInput.getZoomRequests should be(1)
    
    // Test zoom out (-)
    keyboardInput.processInput(45) // '-'
    keyboardInput.getZoomRequests should be(2)
  }

  it should "process arrow keys for panning" in {
    // Test panning in different directions
    keyboardInput.processInput(38) // Up arrow equivalent
    keyboardInput.getPanRequests should be(1)
    
    keyboardInput.processInput(40) // Down arrow equivalent
    keyboardInput.getPanRequests should be(2)
    
    keyboardInput.processInput(60) // Left arrow equivalent
    keyboardInput.getPanRequests should be(3)
    
    keyboardInput.processInput(62) // Right arrow equivalent
    keyboardInput.getPanRequests should be(4)
  }

  it should "handle viewport reset key" in {
    keyboardInput.processInput(118) // 'v'
    keyboardInput.isViewportResetRequested should be(true)
  }

  it should "combine rotation and viewport controls" in {
    // Test rotation
    keyboardInput.processInput(119) // 'w'
    val rotation = keyboardInput.getCurrentRotation
    rotation.pitch should be > 0.0
    
    // Test zoom
    keyboardInput.processInput(43) // '+'
    keyboardInput.getZoomRequests should be(1)
    
    // Test pan
    keyboardInput.processInput(38) // Up arrow equivalent
    keyboardInput.getPanRequests should be(1)
  }
}
