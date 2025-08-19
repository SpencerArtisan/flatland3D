import org.scalatest.flatspec._
import org.scalatest.matchers._

class Shape101Spec extends AnyFlatSpec with should.Matchers {

  "Shape 101" should "reproduce frame 190 truncation and bulge issue" in {
    // Reproduce exact conditions from frame 190
    val world = World3D(300, 180, 60)
      .add(Box(101, 40.0, 70.0, 20.0), Coord3(20.0, 90.0, 40.0), Rotation3.ZERO)

    // Apply the rotation that would occur at frame 190
    val frame190Rotation = Rotation3(
      yaw = 190 * Math.PI / -36,
      pitch = 0,
      roll = 190 * Math.PI / 72
    )
    val rotatedWorld = world.rotate(101, frame190Rotation)

    rotatedWorld match {
      case Right(w) =>
        val rendered = Renderer3D.renderShaded(w, lightDirection = Coord3(-1, -1, -1), ambient = 0.35, xScale = 2, cullBackfaces = true)
        val lines = rendered.split("\n")

        // Print the actual rendered output
        println("=== SHAPE 101 FRAME 190 RENDERED OUTPUT ===")
        lines.zipWithIndex.foreach { case (line, i) =>
          if (line.trim.nonEmpty) {
            println(f"$i%3d: $line")
          }
        }
        println("=== END SHAPE 101 OUTPUT ===")

        // Find bounding box of rendered content
        var minX = Int.MaxValue
        var maxX = Int.MinValue
        var minY = Int.MaxValue
        var maxY = Int.MinValue

        for (y <- lines.indices; x <- lines(y).indices) {
          if (lines(y)(x) != ' ') {
            minX = Math.min(minX, x)
            maxX = Math.max(maxX, x)
            minY = Math.min(minY, y)
            maxY = Math.max(maxY, y)
          }
        }

        if (minX != Int.MaxValue) {
          val width = maxX - minX + 1
          val height = maxY - minY + 1
          val aspectRatio = width.toDouble / height

          println(s"Shape 101 frame 190 rendered bounds: ${width}x${height} at ($minX,$minY), aspect ratio: $aspectRatio")
          
          // This should reproduce the issue: very wide but severely truncated height
          withClue(s"Frame 190 should show truncation issue: ") {
            // The issue is that height is only 41 pixels when it should be much taller
            height should be < 100  // This should fail and show the truncation
            aspectRatio should be > 3.0  // This should pass and show the bulge
          }
        } else {
          fail("No rendered content found for shape 101")
        }

      case Left(error) =>
        fail(s"Error rotating shape: $error")
    }
  }
}
