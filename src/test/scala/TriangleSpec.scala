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
}