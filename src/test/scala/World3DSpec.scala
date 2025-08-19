import org.scalatest.flatspec._
import org.scalatest.matchers._

class World3DSpec extends AnyFlatSpec with should.Matchers {

  "A 3D renderer" should "render empty world as empty scene" in {
    val world = World3D(0, 0, 0)
    Renderer3D.renderWith(world, _ => '*', blankChar = '.') should be("")
  }

  it should "render a single voxel box orthographically" in {
    val world = World3D(3, 1, 3)
      .add(Box(100, 1, 1, 1), Coord3(1, 0, 1))

    Renderer3D.renderWith(world, _ => '*', blankChar = '.') should be(".*.")
  }

  it should "occlude nearer z in front (higher z nearer)" in {
    val world = World3D(3, 1, 3)
      .add(Box(100, 1, 1, 1), Coord3(1, 0, 0)) // far
      .add(Box(101, 1, 1, 1), Coord3(1, 0, 2)) // near

    val rendered = Renderer3D.renderWith(world, p => if (p.shape.id == 100) 'A' else 'B', blankChar = '.')
    rendered should be(".B.")
  }

  it should "respect explicit near-to-far z scan order" in {
    val world = World3D(3, 1, 3)
      .add(Box(100, 1, 1, 1), Coord3(1, 0, 0)) // far
      .add(Box(101, 1, 1, 1), Coord3(1, 0, 2)) // near

    val rendered = Renderer3D.renderWith(world, p => if (p.shape.id == 100) 'A' else 'B', blankChar = '.', nearToFarZs = Seq(0, 1, 2))
    rendered should be(".A.")
  }
}


