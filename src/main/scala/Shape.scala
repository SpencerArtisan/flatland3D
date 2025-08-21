trait Shape {
  def id: Int
  def center: Coord
  def occupiesSpaceAt(coord: Coord): Boolean

  // Configuration constants
  private val NORMAL_EPSILON = 0.5
  private val FALLBACK_NORMAL = Coord(0, 0, 1)

  // Default: estimate local-space normal via occupancy gradient around the local point
  def surfaceNormalAt(local: Coord): Coord = {
    val eps = NORMAL_EPSILON
    val gx = (if (occupiesSpaceAt(Coord(local.x + eps, local.y, local.z))) 1 else 0) -
      (if (occupiesSpaceAt(Coord(local.x - eps, local.y, local.z))) 1 else 0)
    val gy = (if (occupiesSpaceAt(Coord(local.x, local.y + eps, local.z))) 1 else 0) -
      (if (occupiesSpaceAt(Coord(local.x, local.y - eps, local.z))) 1 else 0)
    val gz = (if (occupiesSpaceAt(Coord(local.x, local.y, local.z + eps))) 1 else 0) -
      (if (occupiesSpaceAt(Coord(local.x, local.y, local.z - eps))) 1 else 0)
    val grad = Coord(gx, gy, gz)
    if (grad.magnitude == 0) FALLBACK_NORMAL else grad.normalize
  }
}


// Triangle-based shapes for complex geometry
case class Triangle(v0: Coord, v1: Coord, v2: Coord) {
  // Configuration constants
  private val PARALLEL_THRESHOLD = 1e-10
  private val INTERSECTION_THRESHOLD = 1e-10
  
  // Calculate triangle normal using cross product (always normalized)
  lazy val normal: Coord = {
    val edge1 = v1 - v0
    val edge2 = v2 - v0
    val n = edge1.cross(edge2)
    val m = n.magnitude
    if (m == 0) Coord(0, 0, 1) else n * (1.0 / m)
  }
  
  // Calculate triangle centroid
  lazy val centroid: Coord = (v0 + v1 + v2) * (1.0 / 3.0)
  
  // Ray-triangle intersection using Möller-Trumbore algorithm
  def intersectRay(rayOrigin: Coord, rayDirection: Coord): Option[Double] = {
    val edge1 = v1 - v0
    val edge2 = v2 - v0
    val h = rayDirection.cross(edge2)
    val a = edge1.dot(h)
    
    // Parallel ray check
    if (Math.abs(a) < PARALLEL_THRESHOLD) return None
    
    val f = 1.0 / a
    val s = rayOrigin - v0
    val u = f * s.dot(h)
    
    if (u < 0.0 || u > 1.0) return None
    
    val q = s.cross(edge1)
    val v = f * rayDirection.dot(q)
    
    if (v < 0.0 || u + v > 1.0) return None
    
    val t = f * edge2.dot(q)
    if (t > INTERSECTION_THRESHOLD) Some(t) else None
  }
}

case class TriangleMesh(id: Int, triangles: Seq[Triangle]) extends Shape {
  val center: Coord = Coord(0, 0, 0)
  
  // Build BVH acceleration structure
  private lazy val bvh = BVH.build(triangles)
  private lazy val bounds = BoundingBox.fromTriangles(triangles)
  
  // Configuration constants
  private val RAY_DIRECTIONS = Seq(
    Coord(1, 0, 0),
    Coord(0, 1, 0),  
    Coord(0, 0, 1),
    Coord(1, 1, 0).normalize
  )
  
  def occupiesSpaceAt(coord: Coord): Boolean = {
    // Quick reject using bounding box
    if (!bounds.intersectRay(coord, RAY_DIRECTIONS.head)) return false
    
    // For triangle meshes, we use ray-casting with multiple rays to handle edge cases
    val results = RAY_DIRECTIONS.map { rayDirection =>
      // Use BVH for intersection testing
      var intersectionCount = 0
      var currentHit = bvh.intersectRay(coord, rayDirection)
      while (currentHit.isDefined) {
        intersectionCount += 1
        // Move origin slightly past the hit point and continue
        val (hitDist, _) = currentHit.get
        val newOrigin = coord + rayDirection * (hitDist + 1e-4)
        currentHit = bvh.intersectRay(newOrigin, rayDirection)
      }
      intersectionCount % 2 == 1
    }
    
    // Return true if majority of rays indicate inside
    results.count(_ == true) > results.length / 2
  }
  
  override def surfaceNormalAt(local: Coord): Coord = {
    // Use BVH to find closest intersection in multiple directions
    val directions = RAY_DIRECTIONS ++ RAY_DIRECTIONS.map(_ * -1)
    val hits = directions.flatMap { dir =>
      bvh.intersectRay(local, dir)
    }
    
    if (hits.isEmpty) {
      // Fallback if no intersections found
      Coord(0, 0, 1)
    } else {
      // Use normal of closest triangle
      hits.minBy(_._1)._2.normal
    }
  }
  
  // Ray-triangle intersection for the entire mesh using BVH
  def intersectRay(rayOrigin: Coord, rayDirection: Coord): Option[(Double, Triangle)] = {
    bvh.intersectRay(rayOrigin, rayDirection)
  }
}
