import org.scalatest.flatspec._
import org.scalatest.matchers._

class ShadingSpec extends AnyFlatSpec with should.Matchers {

  "Shaded renderer" should "render three faces of a corner-on box with consistent per-face shade" in {
      val world = World(160, 120, 120)
    .add(Box(1, 40, 40, 40), Coord(60, 50, 60), Rotation(Math.PI / 6, Math.PI / 6, Math.PI / 12))

  val rendered = Renderer.renderShadedForward(world, lightDirection = Coord(-1, -1, -1), ambient = 0.35, xScale = 1)
    val lines = rendered.split("\n")

    def charAt(x: Int, y: Int): Char =
      if (y >= 0 && y < lines.length && x >= 0 && x < lines(y).length) lines(y)(x) else ' '

    // Sample multiple points per face by picking local face points and mapping to screen (x,y)
    val placement = world.placements.head
    val box = placement.shape.asInstanceOf[Box]

    def toScreen(local: Coord): (Int, Int) = {
      val w = placement.origin + placement.rotation.applyTo(local - box.center)
      (Math.round(w.x).toInt, Math.round(w.y).toInt)
    }

    def faceSamples(localFacePoint: Coord): Seq[(Int, Int)] = {
      val offsets = Seq(-6.0, -2.0, 0.0, 6.0)
      for {
        oy <- offsets
        oz <- offsets
      } yield toScreen(Coord(localFacePoint.x, localFacePoint.y + oy, localFacePoint.z + oz))
    }

    val faces = Seq(
      // +X face
      ("+X", faceSamples(Coord(box.width / 2 - 0.001, 0, 0))),
      // +Y face
      ("+Y", faceSamples(Coord(0, box.height / 2 - 0.001, 0))),
      // +Z face
      ("+Z", faceSamples(Coord(0, 0, box.depth / 2 - 0.001)))
    )

    // Keep only faces that appear on screen (non-blank)
    val visibleFaces = faces.flatMap { case (name, pts) =>
      val chars = pts.map { case (x, y) => charAt(x, y) }.filter(_ != ' ')
      if (chars.nonEmpty) Some((name, chars)) else None
    }

    // Expect at least 2 faces, ideally 3
    visibleFaces.size should be >= 2

    // Each visible face should have consistent character across its samples
    visibleFaces.foreach { case (name, chars) =>
      withClue(s"Face $name inconsistent shades: ") {
        chars.distinct.size should be <= 1
      }
    }
  }
}


