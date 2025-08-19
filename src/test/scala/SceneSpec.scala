import org.scalatest.flatspec._
import org.scalatest.matchers._

class SceneSpec extends AnyFlatSpec with should.Matchers {

  "A renderer" should "render a world if it has no size" in {
    val world = World(0, 0)
    Scene.from(world).render() should be("")
  }

  it should "render a single rectangle" in {
    val world = World(1, 1)
    Scene.from(world).render() should be(".")
  }

  it should "render a single row" in {
    val world = World(3, 1)
    Scene.from(world).render() should be("...")
  }

  it should "render a single column" in {
    val world = World(1, 3)
    Scene.from(world).render() should be(".\n.\n.")
  }

  it should "render multiple rows and columns" in {
    val world = World(3, 2)
    Scene.from(world).render() should be("...\n...")
  }

  it should "not render a dot outside the world" in {
    val world = World(2, 2).add(Dot(100), Coord(-1, -1))
    Scene.from(world).render() should be("..\n..")
  }

  it should "render a single dot in the top left" in {
    val world = World(2, 2)
    Scene.from(world.add(Dot(100), Coord(0, 0))).render() should be("*.\n..")
  }

  it should "render a single dot in the bottom right" in {
    val world = World(2, 2)
    Scene.from(world.add(new Dot(100), Coord(1, 1))).render() should be("..\n.*")
  }

  it should "render multiple dots" in {
    val world = World(2, 2)
    Scene.from(world.add(Dot(100), Coord(0, 0))
      .add(Dot(101), Coord(1, 1)))
      .render() should be("*.\n.*")
  }

  it should "render colocated dots as one dot" in {
    val world = World(2, 2)
    Scene.from(world.add(Dot(100), Coord(0, 0))
      .add(Dot(101), Coord(0, 0)))
      .render() should be("*.\n..")
  }

  it should "render a rectangle" in {
    val world = World(3, 3)
    Scene.from(world.add(Rectangle(100, 2, 1), Coord(1, 1)))
      .render() should be("...\n.**\n...")
  }

  it should "render a rectangle rotated through 90 degrees" in {
    val world = World(3, 3)
    Scene.from(world.add(Rectangle(100, 3, 1), Coord(0, 0), Math.PI / 2))
      .render() should be(".*.\n.*.\n.*.")
  }

  it should "rotate a rectangle after placement" in {
    val world = World(3, 3)

    val s = for {
      w <- world.add(Rectangle(100, 3, 1), Coord(0, 0)).rotate(100, Math.PI / 2)
      scene = Scene.from(w).render()
    } yield scene
    s should be(Right(".*.\n.*.\n.*."))
  }

  it should "render a rectangle which fills the entire world" in {
    val world = World(3, 3)
    Scene.from(world.add(Rectangle(100, 5, 5), Coord(-1, -1)))
      .render() should be("***\n***\n***")
  }

  it should "occlude with higher z in front by default" in {
    val world = World(3, 1)
      .add(Rectangle(100, 1, 1), Coord(1, 0), z = 0)
      .add(Rectangle(101, 1, 1), Coord(1, 0), z = 1)

    val render = Scene.renderWith(world, p => if (p.shape.id == 100) 'A' else 'B', blankChar = '.')
    render should be(".B.")
  }

  it should "allow explicit z-order override" in {
    val world = World(3, 1)
      .add(Rectangle(100, 1, 1), Coord(1, 0), z = 0)
      .add(Rectangle(101, 1, 1), Coord(1, 0), z = 1)

    val renderBackToFront = Scene.renderWith(world, p => if (p.shape.id == 100) 'A' else 'B', blankChar = '.', zOrder = Seq(0, 1))
    renderBackToFront should be(".A.")
  }
}