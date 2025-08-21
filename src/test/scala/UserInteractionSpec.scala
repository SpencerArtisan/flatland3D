import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UserInteractionSpec extends AnyFlatSpec with Matchers {
  
  "UserInteraction trait" should "be implemented by TestUserInteraction" in {
    val testInteraction = new TestUserInteraction()
    testInteraction shouldBe a[UserInteraction]
  }
  
  it should "be implemented by KeyboardInputManager" in {
    val keyboardInteraction = new KeyboardInputManager()
    keyboardInteraction shouldBe a[UserInteraction]
  }
}
