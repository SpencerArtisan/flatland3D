import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class KeyboardInputManagerSpec extends AnyFlatSpec with Matchers {
  
  "KeyboardInputManager" should "start with zero rotation" in {
    val manager = new KeyboardInputManager()
    manager.getCurrentRotation should equal(Rotation.ZERO)
  }
  
  it should "increase yaw when 'd' key is pressed" in {
    val manager = new KeyboardInputManager()
    val initialRotation = manager.getCurrentRotation
    
    manager.processInput(100) // 'd' key ASCII code
    val afterRight = manager.getCurrentRotation
    
    afterRight.yaw should be > initialRotation.yaw
  }
  
  it should "decrease yaw when 'a' key is pressed" in {
    val manager = new KeyboardInputManager()
    val initialRotation = manager.getCurrentRotation
    
    manager.processInput(97) // 'a' key ASCII code
    val afterLeft = manager.getCurrentRotation
    
    afterLeft.yaw should be < initialRotation.yaw
  }
  
  it should "increase pitch when 'w' key is pressed" in {
    val manager = new KeyboardInputManager()
    val initialRotation = manager.getCurrentRotation
    
    manager.processInput(119) // 'w' key ASCII code
    val afterUp = manager.getCurrentRotation
    
    afterUp.pitch should be > initialRotation.pitch
  }
  
  it should "decrease pitch when 's' key is pressed" in {
    val manager = new KeyboardInputManager()
    val initialRotation = manager.getCurrentRotation
    
    manager.processInput(115) // 's' key ASCII code
    val afterDown = manager.getCurrentRotation
    
    afterDown.pitch should be < initialRotation.pitch
  }

  it should "decrease roll when 'z' key is pressed" in {
    val manager = new KeyboardInputManager()
    val initialRotation = manager.getCurrentRotation
    
    manager.processInput(122) // 'z' key ASCII code
    val afterRollLeft = manager.getCurrentRotation
    
    afterRollLeft.roll should be < initialRotation.roll
  }

  it should "increase roll when 'x' key is pressed" in {
    val manager = new KeyboardInputManager()
    val initialRotation = manager.getCurrentRotation
    
    manager.processInput(120) // 'x' key ASCII code
    val afterRollRight = manager.getCurrentRotation
    
    afterRollRight.roll should be > initialRotation.roll
  }
  
  it should "reset rotation when 'r' key is pressed" in {
    val manager = new KeyboardInputManager()
    
    // First, change the rotation
    manager.processInput(100) // 'd' key
    manager.processInput(119) // 'w' key
    manager.getCurrentRotation should not equal(Rotation.ZERO)
    
    // Then reset
    manager.processInput(114) // 'r' key ASCII code
    manager.getCurrentRotation should equal(Rotation.ZERO)
  }
  
  it should "handle uppercase keys correctly" in {
    val manager = new KeyboardInputManager()
    val initialRotation = manager.getCurrentRotation
    
    manager.processInput(68) // 'D' key ASCII code
    val afterRight = manager.getCurrentRotation
    
    afterRight.yaw should be > initialRotation.yaw
    
    // Test uppercase Z and X as well
    val initialRoll = manager.getCurrentRotation.roll
    
    manager.processInput(90) // 'Z' key ASCII code
    val afterRollLeft = manager.getCurrentRotation
    afterRollLeft.roll should be < initialRoll
    
    manager.processInput(88) // 'X' key ASCII code
    val afterRollRight = manager.getCurrentRotation
    afterRollRight.roll should be > afterRollLeft.roll
  }
  
  it should "ignore unknown keys" in {
    val manager = new KeyboardInputManager()
    val initialRotation = manager.getCurrentRotation
    
    manager.processInput(121) // 'y' key ASCII code (not mapped)
    val afterUnknown = manager.getCurrentRotation
    
    afterUnknown should equal(initialRotation)
  }
  
  it should "apply correct rotation step size" in {
    val manager = new KeyboardInputManager()
    val expectedStep = Math.PI / 18 // 10 degrees
    
    manager.processInput(100) // 'd' key
    val rotation = manager.getCurrentRotation
    
    rotation.yaw shouldBe expectedStep +- 0.001
    
    // Test roll step size as well
    manager.processInput(122) // 'z' key
    val rollRotation = manager.getCurrentRotation
    
    rollRotation.roll shouldBe -expectedStep +- 0.001 // Negative because Z decreases roll
  }
}
