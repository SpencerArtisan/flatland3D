trait Shape {
  def id: Int
  def center: Coord
  def occupiesSpaceAt(coord: Coord): Boolean

  // Default: estimate local-space normal via occupancy gradient around the local point
  def surfaceNormalAt(local: Coord): Coord = {
    val eps = 0.5
    val gx = (if (occupiesSpaceAt(Coord(local.x + eps, local.y, local.z))) 1 else 0) -
      (if (occupiesSpaceAt(Coord(local.x - eps, local.y, local.z))) 1 else 0)
    val gy = (if (occupiesSpaceAt(Coord(local.x, local.y + eps, local.z))) 1 else 0) -
      (if (occupiesSpaceAt(Coord(local.x, local.y - eps, local.z))) 1 else 0)
    val gz = (if (occupiesSpaceAt(Coord(local.x, local.y, local.z + eps))) 1 else 0) -
      (if (occupiesSpaceAt(Coord(local.x, local.y, local.z - eps))) 1 else 0)
    val grad = Coord(gx, gy, gz)
    if (grad.magnitude == 0) Coord(0, 0, 1) else grad.normalize
  }
}


// Triangle-based shapes for complex geometry
case class Triangle(v0: Coord, v1: Coord, v2: Coord) {
  // Calculate triangle normal using cross product
  lazy val normal: Coord = {
    val edge1 = v1 - v0
    val edge2 = v2 - v0
    edge1.cross(edge2).normalize
  }
  
  // Calculate triangle centroid
  lazy val centroid: Coord = (v0 + v1 + v2) * (1.0 / 3.0)
  
  // Calculate triangle area using cross product
  lazy val area: Double = {
    val edge1 = v1 - v0
    val edge2 = v2 - v0
    edge1.cross(edge2).magnitude / 2.0
  }
  
  // Ray-triangle intersection using Möller-Trumbore algorithm
  def intersect(rayOrigin: Coord, rayDirection: Coord): Option[Double] = {
    val edge1 = v1 - v0
    val edge2 = v2 - v0
    val h = rayDirection.cross(edge2)
    val a = edge1.dot(h)
    
    // Parallel ray check
    if (Math.abs(a) < 1e-10) return None
    
    val f = 1.0 / a
    val s = rayOrigin - v0
    val u = f * s.dot(h)
    
    if (u < 0.0 || u > 1.0) return None
    
    val q = s.cross(edge1)
    val v = f * rayDirection.dot(q)
    
    if (v < 0.0 || u + v > 1.0) return None
    
    val t = f * edge2.dot(q)
    if (t > 1e-10) Some(t) else None
  }
}

case class TriangleMesh(id: Int, triangles: Seq[Triangle]) extends Shape {
  val center: Coord = Coord(0, 0, 0)
  
  def occupiesSpaceAt(coord: Coord): Boolean = {
    // Generic ray-casting algorithm for any triangle mesh
    // Use a diagonal ray direction to avoid edge cases
    val rayDirection = Coord(1, 0.773, 0.577).normalize // Avoid axis alignment
    val intersectionCount = triangles.count { triangle =>
      triangle.intersect(coord, rayDirection) match {
        case Some(distance) => distance > 1e-10 // Only count forward intersections
        case None => false
      }
    }
    
    // Odd number of intersections means point is inside
    intersectionCount % 2 == 1
  }
  
  override def surfaceNormalAt(local: Coord): Coord = {
    // For true face consistency, use dominant axis approach like the old Box implementation
    // This ensures all points on the same logical face get the same normal
    
    // Find bounding box of the triangle mesh
    val allVertices = triangles.flatMap(t => Seq(t.v0, t.v1, t.v2))
    val minX = allVertices.map(_.x).min
    val maxX = allVertices.map(_.x).max
    val minY = allVertices.map(_.y).min  
    val maxY = allVertices.map(_.y).max
    val minZ = allVertices.map(_.z).min
    val maxZ = allVertices.map(_.z).max
    
    // Calculate distance from each face with tolerance for floating point precision
    val eps = 1e-10
    val xMinDistance = Math.abs(local.x - minX)
    val xMaxDistance = Math.abs(local.x - maxX)
    val yMinDistance = Math.abs(local.y - minY)
    val yMaxDistance = Math.abs(local.y - maxY)
    val zMinDistance = Math.abs(local.z - minZ)
    val zMaxDistance = Math.abs(local.z - maxZ)
    
    // Find the minimum distance to any face
    val minDistance = Seq(xMinDistance, xMaxDistance, yMinDistance, yMaxDistance, zMinDistance, zMaxDistance).min
    
    // Return normal for the closest face, with tolerance for floating point comparison
    if (Math.abs(xMinDistance - minDistance) < eps) {
      Coord(-1, 0, 0) // -X face
    } else if (Math.abs(xMaxDistance - minDistance) < eps) {
      Coord(1, 0, 0)  // +X face
    } else if (Math.abs(yMinDistance - minDistance) < eps) {
      Coord(0, -1, 0) // -Y face
    } else if (Math.abs(yMaxDistance - minDistance) < eps) {
      Coord(0, 1, 0)  // +Y face
    } else if (Math.abs(zMinDistance - minDistance) < eps) {
      Coord(0, 0, -1) // -Z face
    } else {
      Coord(0, 0, 1)  // +Z face
    }
  }
  
  // Ray-triangle intersection for the entire mesh
  def intersectRay(rayOrigin: Coord, rayDirection: Coord): Option[(Double, Triangle)] = {
    val intersections = triangles.flatMap { triangle =>
      triangle.intersect(rayOrigin, rayDirection).map(distance => (distance, triangle))
    }
    
    if (intersections.nonEmpty) {
      Some(intersections.minBy(_._1)) // Return closest intersection
    } else {
      None
    }
  }
}
