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
  
  // Configuration constants
  private val RAY_DIRECTIONS = Seq(
    Coord(1, 0, 0),
    Coord(0, 1, 0),  
    Coord(0, 0, 1),
    Coord(1, 1, 0).normalize
  )
  
  def occupiesSpaceAt(coord: Coord): Boolean = {
    // For triangle meshes, we use ray-casting with multiple rays to handle edge cases
    val results = RAY_DIRECTIONS.map { rayDirection =>
      var intersectionCount = 0
      triangles.foreach { triangle =>
        if (triangle.intersectRay(coord, rayDirection).isDefined) {
          intersectionCount += 1
        }
      }
      intersectionCount % 2 == 1
    }
    
    // Return true if majority of rays indicate inside
    results.count(_ == true) > results.length / 2
  }
  
  override def surfaceNormalAt(local: Coord): Coord = {
    // Find closest triangle
    val directions = RAY_DIRECTIONS ++ RAY_DIRECTIONS.map(_ * -1)
    val hits = directions.flatMap { dir =>
      triangles.flatMap { triangle =>
        triangle.intersectRay(local, dir).map(dist => (dist, triangle))
      }
    }
    
    if (hits.isEmpty) {
      // Fallback if no intersections found
      Coord(0, 0, 1)
    } else {
      // Get normal of closest triangle
      val (_, triangle) = hits.minBy(_._1)
      val normal = triangle.normal
      
      // Determine if we're inside or outside by checking if the normal points towards us
      val toPoint = local - triangle.centroid
      val dot = toPoint.dot(normal)
      if (dot < 0) normal * -1 else normal
    }
  }
  
  // Ray-triangle intersection for the entire mesh
  def intersectRay(rayOrigin: Coord, rayDirection: Coord): Option[(Double, Triangle)] = {
    var closestHit: Option[(Double, Triangle)] = None
    var closestDist = Double.PositiveInfinity
    
    triangles.foreach { triangle =>
      triangle.intersectRay(rayOrigin, rayDirection) match {
        case Some(dist) if dist < closestDist =>
          closestDist = dist
          closestHit = Some((dist, triangle))
        case _ =>
      }
    }
    
    closestHit
  }
}