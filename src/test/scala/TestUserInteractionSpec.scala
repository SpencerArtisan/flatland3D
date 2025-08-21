import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TestUserInteractionSpec extends AnyFlatSpec with Matchers {
  
  "TestUserInteraction" should "start with default values" in {
    val testInteraction = new TestUserInteraction()
    
    testInteraction.getCurrentRotation should equal(Rotation.ZERO)
    testInteraction.isQuitRequested should be(false)
    testInteraction.isResetRequested should be(false)
  }
  
  it should "allow setting custom initial values" in {
    val customRotation = Rotation(Math.PI/4, Math.PI/6, 0)
    val testInteraction = new TestUserInteraction(
      rotation = customRotation,
      quitRequested = true,
      resetRequested = true
    )
    
    testInteraction.getCurrentRotation should equal(customRotation)
    testInteraction.isQuitRequested should be(true)
    testInteraction.isResetRequested should be(true)
  }
  
  it should "allow changing rotation" in {
    val testInteraction = new TestUserInteraction()
    val newRotation = Rotation(Math.PI/2, 0, 0)
    
    testInteraction.setRotation(newRotation)
    testInteraction.getCurrentRotation should equal(newRotation)
  }
  
  it should "allow requesting quit" in {
    val testInteraction = new TestUserInteraction()
    
    testInteraction.requestQuit()
    testInteraction.isQuitRequested should be(true)
  }
  
  it should "allow requesting reset" in {
    val testInteraction = new TestUserInteraction()
    
    testInteraction.requestReset()
    testInteraction.isResetRequested should be(true)
  }
  
  it should "allow clearing requests" in {
    val testInteraction = new TestUserInteraction(quitRequested = true, resetRequested = true)
    
    testInteraction.clearRequests()
    testInteraction.isQuitRequested should be(false)
    testInteraction.isResetRequested should be(false)
  }
  
  it should "have no-op update and cleanup methods" in {
    val testInteraction = new TestUserInteraction()
    
    // These should not throw exceptions
    noException should be thrownBy testInteraction.update()
    noException should be thrownBy testInteraction.cleanup()
  }
}
