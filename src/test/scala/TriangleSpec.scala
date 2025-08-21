import org.scalatest.flatspec._
import org.scalatest.matchers._

class TriangleSpec extends AnyFlatSpec with should.Matchers {

  "Triangle" should "calculate normal correctly" in {
    val triangle = Triangle(
      Coord(0, 0, 0),
      Coord(1, 0, 0), 
      Coord(0, 1, 0)
    )
    
    // Normal should point in +Z direction for counter-clockwise vertices
    triangle.normal should be (Coord(0, 0, 1))
  }

  "Triangle" should "calculate centroid correctly" in {
    val triangle = Triangle(
      Coord(0, 0, 0),
      Coord(3, 0, 0),
      Coord(0, 3, 0)
    )
    
    triangle.centroid should be (Coord(1, 1, 0))
  }

  "Triangle" should "detect ray intersection" in {
    val triangle = Triangle(
      Coord(0, 0, 0),
      Coord(1, 0, 0),
      Coord(0, 1, 0)
    )
    
    // Ray from below triangle pointing up should intersect
    val rayOrigin = Coord(0.25, 0.25, -1)
    val rayDirection = Coord(0, 0, 1)
    
    val intersection = triangle.intersect(rayOrigin, rayDirection)
    intersection should be (defined)
    intersection.get should be (1.0 +- 0.001)
  }

  "Triangle" should "reject ray that misses" in {
    val triangle = Triangle(
      Coord(0, 0, 0),
      Coord(1, 0, 0),
      Coord(0, 1, 0)
    )
    
    // Ray that misses the triangle
    val rayOrigin = Coord(2, 2, -1)  // Outside triangle bounds
    val rayDirection = Coord(0, 0, 1)
    
    triangle.intersect(rayOrigin, rayDirection) should be (None)
  }

  "TriangleCube" should "be constructed with correct triangle count" in {
    val cube = TriangleShapes.cube(1, 2.0) // 2x2x2 cube centered at origin
    
    // Cube should have 12 triangles (2 per face, 6 faces)
    cube.triangles.length should be (12)
    
    // All triangles should have valid normals
    cube.triangles.foreach { triangle =>
      triangle.normal.magnitude should be (1.0 +- 0.001)
    }
  }
  
  "TriangleCube" should "provide surface normals" in {
    val cube = TriangleShapes.cube(1, 2.0)
    
    // Test normals at different face centers
    val rightFaceNormal = cube.surfaceNormalAt(Coord(1, 0, 0))
    val topFaceNormal = cube.surfaceNormalAt(Coord(0, 1, 0))
    val frontFaceNormal = cube.surfaceNormalAt(Coord(0, 0, 1))
    
    // Normals should point outward from cube faces
    rightFaceNormal.x should be > 0.0
    topFaceNormal.y should be > 0.0  
    frontFaceNormal.z should be > 0.0
  }

  // Elite Cobra spaceship tests
  "TriangleCobra" should "be constructed with correct triangle count" in {
    val cobra = TriangleShapes.cobra(1, 8.0) // Size 8.0 to match existing shapes
    
    // Authentic Elite Cobra should have specific triangle count for classic wireframe
    // Based on original Elite wireframe: sharp nose, diamond body, swept wings
    cobra.triangles.length should be >= 20
    cobra.triangles.length should be <= 40  // Increased to allow for more detailed engine pods
    
    // All triangles should have valid normals
    cobra.triangles.foreach { triangle =>
      triangle.normal.magnitude should be (1.0 +- 0.001)
    }
  }

  "TriangleCobra" should "have distinctive spaceship proportions" in {
    val cobra = TriangleShapes.cobra(1, 8.0)
    
    // Calculate bounding box to verify spaceship proportions
    val allVertices = cobra.triangles.flatMap(t => Seq(t.v0, t.v1, t.v2))
    val minX = allVertices.map(_.x).min
    val maxX = allVertices.map(_.x).max
    val minY = allVertices.map(_.y).min
    val maxY = allVertices.map(_.y).max
    val minZ = allVertices.map(_.z).min
    val maxZ = allVertices.map(_.z).max
    
    val width = maxX - minX
    val height = maxY - minY
    val length = maxZ - minZ
    
    // Cobra should have authentic Elite proportions (length > width)
    val lengthToWidthRatio = length / width
    lengthToWidthRatio should be > 1.2  // Authentic Elite Cobra is longer than it is wide
    
    // Should fit within the specified size bounds
    width should be <= 8.0
    height should be <= 8.0
    length should be <= 8.0
  }

  "TriangleCobra" should "provide surface normals" in {
    val cobra = TriangleShapes.cobra(1, 8.0)
    
    // Test that we can get normals from different parts of the spaceship
    val frontNormal = cobra.surfaceNormalAt(Coord(0, 0, 4))  // Front nose area
    val rearNormal = cobra.surfaceNormalAt(Coord(0, 0, -4))  // Rear engine area
    
    // Normals should be valid unit vectors
    frontNormal.magnitude should be (1.0 +- 0.001)
    rearNormal.magnitude should be (1.0 +- 0.001)
  }
}