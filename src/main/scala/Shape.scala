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

case class Box(id: Int, width: Double, height: Double, depth: Double) extends Shape {
  val center: Coord = Coord(0, 0, 0)

  def occupiesSpaceAt(coord: Coord): Boolean = {
    val halfWidth = width / 2
    val halfHeight = height / 2
    val halfDepth = depth / 2
    
    val roundedCoord = Coord(
      Math.round(coord.x).toDouble,
      Math.round(coord.y).toDouble,
      Math.round(coord.z).toDouble
    )
    
    val xWithin = roundedCoord.x >= -halfWidth && roundedCoord.x <= halfWidth
    val yWithin = roundedCoord.y >= -halfHeight && roundedCoord.y <= halfHeight
    val zWithin = roundedCoord.z >= -halfDepth && roundedCoord.z <= halfDepth
    
    xWithin && yWithin && zWithin
  }

  override def surfaceNormalAt(local: Coord): Coord = {
    val halfWidth = width / 2
    val halfHeight = height / 2
    val halfDepth = depth / 2
    
    findClosestFaceNormal(local, halfWidth, halfHeight, halfDepth)
  }
  
  private def findClosestFaceNormal(point: Coord, halfWidth: Double, halfHeight: Double, halfDepth: Double): Coord = {
    // Use dominant axis approach: whichever coordinate is closest to its maximum extent determines the face
    val xDistance = Math.abs(Math.abs(point.x) - halfWidth)
    val yDistance = Math.abs(Math.abs(point.y) - halfHeight)  
    val zDistance = Math.abs(Math.abs(point.z) - halfDepth)
    
    val minDistance = Math.min(xDistance, Math.min(yDistance, zDistance))
    
    if (xDistance == minDistance) {
      if (point.x > 0) Coord(1, 0, 0) else Coord(-1, 0, 0)
    } else if (yDistance == minDistance) {
      if (point.y > 0) Coord(0, 1, 0) else Coord(0, -1, 0)  
    } else {
      if (point.z > 0) Coord(0, 0, 1) else Coord(0, 0, -1)
    }
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
    // For triangle meshes, we use ray-casting with multiple rays to handle edge cases
    val rayDirections = Seq(
      Coord(1, 0, 0),
      Coord(0, 1, 0),  
      Coord(0, 0, 1),
      Coord(1, 1, 0).normalize
    )
    
    // Test with multiple rays and use majority vote
    val results = rayDirections.map { rayDirection =>
      val intersectionCount = triangles.count(_.intersect(coord, rayDirection).isDefined)
      intersectionCount % 2 == 1
    }
    
    // Return true if majority of rays indicate inside
    results.count(_ == true) > results.length / 2
  }
  
  override def surfaceNormalAt(local: Coord): Coord = {
    // Find the closest triangle and return its normal
    val closestTriangle = triangles.minBy(triangle => {
      val toTriangle = triangle.centroid - local
      toTriangle.magnitude
    })
    closestTriangle.normal
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
