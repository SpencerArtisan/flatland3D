import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class KeyboardInputManagerSpec extends AnyFlatSpec with Matchers {
  
  "KeyboardInputManager" should "implement UserInteraction interface" in {
    val keyboardInteraction = new KeyboardInputManager()
    keyboardInteraction shouldBe a[UserInteraction]
  }
  
  it should "start with default values" in {
    val keyboardInteraction = new KeyboardInputManager()
    
    keyboardInteraction.getCurrentRotation should equal(Rotation.ZERO)
    keyboardInteraction.isQuitRequested should be(false)
    keyboardInteraction.isResetRequested should be(false)
  }
  
  it should "process WASD keys correctly" in {
    val keyboardInteraction = new KeyboardInputManager()
    val initialRotation = keyboardInteraction.getCurrentRotation
    
    // Test W key (pitch up)
    keyboardInteraction.processInput('w')
    val afterW = keyboardInteraction.getCurrentRotation
    afterW.pitch should be > initialRotation.pitch
    
    // Test A key (yaw left)
    keyboardInteraction.processInput('a')
    val afterA = keyboardInteraction.getCurrentRotation
    afterA.yaw should be < afterW.yaw
    
    // Test S key (pitch down)
    keyboardInteraction.processInput('s')
    val afterS = keyboardInteraction.getCurrentRotation
    afterS.pitch should be < afterW.pitch
    
    // Test D key (yaw right)
    keyboardInteraction.processInput('d')
    val afterD = keyboardInteraction.getCurrentRotation
    afterD.yaw should be > afterA.yaw
  }
  
  it should "process ZX keys for roll" in {
    val keyboardInteraction = new KeyboardInputManager()
    val initialRotation = keyboardInteraction.getCurrentRotation
    
    // Test Z key (roll left)
    keyboardInteraction.processInput('z')
    val afterZ = keyboardInteraction.getCurrentRotation
    afterZ.roll should be < initialRotation.roll
    
    // Test X key (roll right)
    keyboardInteraction.processInput('x')
    val afterX = keyboardInteraction.getCurrentRotation
    afterX.roll should be > afterZ.roll
  }
  
  it should "handle reset key correctly" in {
    val keyboardInteraction = new KeyboardInputManager()
    
    // Rotate first
    keyboardInteraction.processInput('w')
    keyboardInteraction.processInput('a')
    keyboardInteraction.getCurrentRotation should not equal(Rotation.ZERO)
    
    // Reset
    keyboardInteraction.processInput('r')
    keyboardInteraction.isResetRequested should be(true)
  }
  
  it should "detect quit requests" in {
    val keyboardInteraction = new KeyboardInputManager()
    
    keyboardInteraction.processInput('q')
    keyboardInteraction.isQuitRequested should be(true)
    
    keyboardInteraction.processInput('Q')
    keyboardInteraction.isQuitRequested should be(true)
    
    keyboardInteraction.processInput(27) // ESC key
    keyboardInteraction.isQuitRequested should be(true)
  }
  
  it should "ignore unknown keys" in {
    val keyboardInteraction = new KeyboardInputManager()
    val initialRotation = keyboardInteraction.getCurrentRotation
    
    keyboardInteraction.processInput('?')
    keyboardInteraction.getCurrentRotation should equal(initialRotation)
    keyboardInteraction.isQuitRequested should be(false)
    keyboardInteraction.isResetRequested should be(false)
  }
  
  it should "handle case-insensitive keys" in {
    val keyboardInteraction = new KeyboardInputManager()
    
    // Test both upper and lower case
    keyboardInteraction.processInput('w')
    keyboardInteraction.processInput('W')
    keyboardInteraction.getCurrentRotation.pitch should be > 0.0
    
    keyboardInteraction.processInput('a')
    keyboardInteraction.processInput('A')
    keyboardInteraction.getCurrentRotation.yaw should be < 0.0
  }
  
  it should "process reset requests in update method" in {
    val keyboardInteraction = new KeyboardInputManager()
    
    // Request reset
    keyboardInteraction.processInput('r')
    keyboardInteraction.isResetRequested should be(true)
    
    // Update should process the reset
    keyboardInteraction.update()
    keyboardInteraction.getCurrentRotation should equal(Rotation.ZERO)
    keyboardInteraction.isResetRequested should be(false)
  }
  
  it should "have no-op cleanup when not started" in {
    val keyboardInteraction = new KeyboardInputManager()
    
    // Should not throw exception
    noException should be thrownBy keyboardInteraction.cleanup()
  }
}
