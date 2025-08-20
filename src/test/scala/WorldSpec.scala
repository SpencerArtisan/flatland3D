import org.scalatest.flatspec._
import org.scalatest.matchers._

class WorldSpec extends AnyFlatSpec with should.Matchers {

  "A 3D renderer" should "render empty world as empty scene" in {
    val world = World(0, 0, 0)
    Renderer.renderWith(world, _ => '*', blankChar = '.') should be("")
  }

  it should "render a single voxel box orthographically" in {
    val world = World(3, 1, 3)
      .add(TriangleShapes.cube(100, 1), Coord(1, 0, 1))

    Renderer.renderWith(world, _ => '*', blankChar = '.') should be(".*.")
  }

  it should "occlude nearer z in front (higher z nearer)" in {
    val world = World(3, 1, 3)
      .add(TriangleShapes.cube(100, 1), Coord(1, 0, 0)) // far
      .add(TriangleShapes.cube(101, 1), Coord(1, 0, 2)) // near

    val rendered = Renderer.renderWith(world, p => if (p.shape.id == 100) 'A' else 'B', blankChar = '.')
    rendered should be(".B.")
  }

  it should "respect explicit near-to-far z scan order" in {
    val world = World(3, 1, 3)
      .add(TriangleShapes.cube(100, 1), Coord(1, 0, 0)) // far
      .add(TriangleShapes.cube(101, 1), Coord(1, 0, 2)) // near

    val rendered = Renderer.renderWith(world, p => if (p.shape.id == 100) 'A' else 'B', blankChar = '.', nearToFarZs = Seq(0, 1, 2))
    rendered should be(".A.")
  }
}


