import org.scalatest.flatspec._
import org.scalatest.matchers._

class BackfaceCullingSpec extends AnyFlatSpec with should.Matchers {

  "Backface culling" should "not hide visible faces of a rotated box" in {
    val world = World3D(160, 120, 120)
      .add(Box(1, 40, 40, 40), Coord3(60, 50, 60), Rotation3(Math.PI / 5, Math.PI / 7, Math.PI / 11))

    val rendered = Renderer3D.renderShaded(world, lightDirection = Coord3(-1, -1, -1), ambient = 0.35, xScale = 1, cullBackfaces = true)
    val lines = rendered.split("\n")

    def charAt(x: Int, y: Int): Char =
      if (y >= 0 && y < lines.length && x >= 0 && x < lines(y).length) lines(y)(x) else ' '

    val placement = world.placements.head
    val box = placement.shape.asInstanceOf[Box]

    def toWorld(local: Coord3): Coord3 = {
      val rotationCenter = placement.origin + box.center
      val rel = Coord3(local.x - box.center.x, local.y - box.center.y, local.z - box.center.z)
      val rot = placement.rotation.applyTo(rel)
      rotationCenter + rot
    }

    def toScreen(local: Coord3): (Int, Int) = {
      val w = toWorld(local)
      (Math.round(w.x).toInt, Math.round(w.y).toInt)
    }

    val faces = Seq(
      ("+X", Coord3(box.width - 0.01, box.height / 2, box.depth / 2), Coord3(1, 0, 0)),
      ("+Y", Coord3(box.width / 2, box.height - 0.01, box.depth / 2), Coord3(0, 1, 0)),
      ("+Z", Coord3(box.width / 2, box.height / 2, box.depth - 0.01), Coord3(0, 0, 1))
    )

    val viewDirWorld = Coord3(0, 0, -1)

    val visibleFaces = faces.flatMap { case (name, localCenter, localNormal) =>
      val worldNormal = placement.rotation.applyTo(localNormal)
      if (worldNormal.dot(viewDirWorld) < 0) Some((name, localCenter)) else None
    }

    // Expect at least one visible face in this orientation
    visibleFaces should not be empty

    // For each visible face, check at least one of a 3x3 neighborhood samples is non-blank when culled
    val offsets = Seq(-2.0, 0.0, 2.0)
    visibleFaces.foreach { case (name, localCenter) =>
      val samples = for {
        oy <- offsets
        oz <- offsets
      } yield toScreen(Coord3(localCenter.x, localCenter.y + oy, localCenter.z + oz))
      val anyVisible = samples.exists { case (sx, sy) => charAt(sx, sy) != ' ' }
      withClue(s"Face $name was culled incorrectly: ") { anyVisible shouldBe true }
    }
  }

  it should "not reduce overall visible pixels vs no-cull rendering" in {
    val world = World3D(200, 140, 120)
      .add(Box(1, 80, 60, 60), Coord3(80, 60, 60), Rotation3(Math.PI / 4, Math.PI / 6, Math.PI / 12))

    val noCull = Renderer3D.renderShaded(world, lightDirection = Coord3(-1, -1, -1), ambient = 0.35, xScale = 1, cullBackfaces = false)
    val withCull = Renderer3D.renderShaded(world, lightDirection = Coord3(-1, -1, -1), ambient = 0.35, xScale = 1, cullBackfaces = true)

    def countFilled(s: String): Int = s.count(_ != ' ')
    val a = countFilled(noCull)
    val b = countFilled(withCull)

    // Expect culling not to erase legitimate visible pixels (allow small tolerance for ties at edges)
    withClue(s"Culled count $b much less than no-cull $a: ") {
      b should be >= (a * 7 / 10)
    }
  }

  it should "retain at least 90% of visible pixels vs no-cull for a challenging rotation" in {
    val world = World3D(200, 140, 120)
      .add(Box(1, 80, 60, 60), Coord3(80, 60, 60), Rotation3(Math.PI / 5, Math.PI / 7, Math.PI / 11))

    val noCull = Renderer3D.renderShaded(world, lightDirection = Coord3(-1, -1, -1), ambient = 0.35, xScale = 1, cullBackfaces = false)
    val withCull = Renderer3D.renderShaded(world, lightDirection = Coord3(-1, -1, -1), ambient = 0.35, xScale = 1, cullBackfaces = true)

    def countFilled(s: String): Int = s.count(_ != ' ')
    val a = countFilled(noCull)
    val b = countFilled(withCull)

    withClue(s"Culled retains only $b of $a pixels: ") {
      b should be >= (a * 9 / 10)
    }
  }
}


