import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TestUserInteractionSpec extends AnyFlatSpec with Matchers {
  
  "TestUserInteraction" should "start with default values" in {
    val testInteraction = new TestUserInteraction()
    
    testInteraction.getRotationDelta should equal(Rotation.ZERO)
    testInteraction.getViewportDelta should equal(ViewportDelta.IDENTITY)
    testInteraction.isQuitRequested should be(false)
    testInteraction.isResetRequested should be(false)
  }
  
  it should "allow setting custom initial values" in {
    val testInteraction = new TestUserInteraction()
    testInteraction.requestQuit()
    testInteraction.requestReset()
    
    testInteraction.isQuitRequested should be(true)
    testInteraction.isResetRequested should be(true)
  }
  
  it should "allow changing rotation delta" in {
    val testInteraction = new TestUserInteraction()
    val rotationDelta = Rotation(Math.PI/2, 0, 0)
    
    testInteraction.setRotationDelta(rotationDelta)
    testInteraction.getRotationDelta should equal(rotationDelta)
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
    val testInteraction = new TestUserInteraction()
    testInteraction.requestQuit()
    testInteraction.requestReset()
    
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
