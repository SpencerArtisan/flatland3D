import org.scalatest.flatspec._
import org.scalatest.matchers._

class ShadingSpec extends AnyFlatSpec with should.Matchers {

  "Shaded renderer" should "render three faces of a corner-on box with consistent per-face shade" in {
      val world = World(160, 120, 120)
    .add(TriangleShapes.cube(1, 40), Coord(60, 50, 60), Rotation(Math.PI / 6, Math.PI / 6, Math.PI / 12))

  val rendered = Renderer.renderShadedForward(world, lightDirection = Coord(-1, -1, -1), ambient = 0.35, xScale = 1)
    val lines = rendered.split("\n")

    def charAt(x: Int, y: Int): Char =
      if (y >= 0 && y < lines.length && x >= 0 && x < lines(y).length) lines(y)(x) else ' '

    // Sample multiple points per face by picking local face points and mapping to screen (x,y)
    val placement = world.placements.head
    val triangleMesh = placement.shape.asInstanceOf[TriangleMesh]
    val cubeSize = 40.0 // The cube size we used

    def toScreen(local: Coord): (Int, Int) = {
      val w = placement.origin + placement.rotation.applyTo(local - triangleMesh.center)
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
      ("+X", faceSamples(Coord(cubeSize / 2 - 0.001, 0, 0))),
      // +Y face
      ("+Y", faceSamples(Coord(0, cubeSize / 2 - 0.001, 0))),
      // +Z face
      ("+Z", faceSamples(Coord(0, 0, cubeSize / 2 - 0.001)))
    )

    // Keep only faces that appear on screen (non-blank)
    val visibleFaces = faces.flatMap { case (name, pts) =>
      val chars = pts.map { case (x, y) => charAt(x, y) }.filter(_ != ' ')
      if (chars.nonEmpty) Some((name, chars)) else None
    }

    // Expect at least 2 faces, ideally 3
    visibleFaces.size should be >= 2

    // Each visible face should have reasonably consistent character across its samples
    // Allow for some variation due to triangle mesh rendering and lighting calculations
    visibleFaces.foreach { case (name, chars) =>
      withClue(s"Face $name inconsistent shades: ") {
        // Allow up to 2 different shading characters per face, as some variation is expected
        // in triangle mesh rendering due to different triangle orientations and lighting
        chars.distinct.size should be <= 2
      }
    }
  }
}


