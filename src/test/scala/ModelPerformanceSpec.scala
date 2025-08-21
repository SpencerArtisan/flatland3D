import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ModelPerformanceSpec extends AnyFlatSpec with Matchers {
  "Car model" should "load and transform correctly" in {
    // Load and parse model
    val modelContent = scala.io.Source.fromResource("models/car.obj").mkString
    val parseResult = ObjParser.parse(modelContent)
    parseResult.isRight shouldBe true
    
    // Verify model properties
    val mesh = parseResult.right.get
    mesh.triangles.size should be > 0
    
    // Test scaling
    val scaledMesh = ObjParser.scaleMesh(mesh.copy(id = 1), 10.0)
    scaledMesh.triangles.size shouldBe mesh.triangles.size
    
    // Test world transform
    val world = World.infinite
    val worldWithMesh = world.add(scaledMesh, Coord(0, 0, 0), Rotation(Math.PI/4, Math.PI/4, 0))
    worldWithMesh.placements.size shouldBe 1
    
    // Test rendering
    val viewport = Viewport.centeredAt(Coord(0, 0, 0))
    val rendered = Renderer.renderShadedForward(
      worldWithMesh,
      lightDirection = Coord(-1, -1, -1),
      ambient = 0.35,
      xScale = 2,
      viewport = Some(viewport)
    )
    rendered should not be empty
  }
}