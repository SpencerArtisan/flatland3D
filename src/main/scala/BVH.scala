// Bounding Volume Hierarchy for accelerating ray-triangle intersection tests
sealed trait BVHNode {
  def bounds: BoundingBox
  def intersectRay(origin: Coord, direction: Coord): Option[(Double, Triangle)]
}

case class BVHLeaf(triangles: Seq[Triangle], bounds: BoundingBox) extends BVHNode {
  def intersectRay(origin: Coord, direction: Coord): Option[(Double, Triangle)] = {
    if (!bounds.intersectRay(origin, direction)) return None
    
    var closestHit: Option[(Double, Triangle)] = None
    var closestDist = Double.PositiveInfinity
    
    triangles.foreach { triangle =>
      triangle.intersectRay(origin, direction) match {
        case Some(dist) if dist < closestDist =>
          closestDist = dist
          closestHit = Some((dist, triangle))
        case _ =>
      }
    }
    
    closestHit
  }
}

case class BVHInterior(left: BVHNode, right: BVHNode, bounds: BoundingBox) extends BVHNode {
  def intersectRay(origin: Coord, direction: Coord): Option[(Double, Triangle)] = {
    if (!bounds.intersectRay(origin, direction)) return None
    
    // Check both children and return closest hit
    val leftHit = left.intersectRay(origin, direction)
    val rightHit = right.intersectRay(origin, direction)
    
    (leftHit, rightHit) match {
      case (Some((d1, t1)), Some((d2, t2))) => if (d1 < d2) leftHit else rightHit
      case (Some(_), None) => leftHit
      case (None, Some(_)) => rightHit
      case (None, None) => None
    }
  }
}

object BVH {
  private val MAX_TRIANGLES_PER_LEAF = 4
  private val MAX_DEPTH = 20
  
  def build(triangles: Seq[Triangle]): BVHNode = {
    buildRecursive(triangles, 0)
  }
  
  private def buildRecursive(triangles: Seq[Triangle], depth: Int): BVHNode = {
    val bounds = BoundingBox.fromTriangles(triangles)
    
    // Create leaf node if few enough triangles or max depth reached
    if (triangles.length <= MAX_TRIANGLES_PER_LEAF || depth >= MAX_DEPTH) {
      return BVHLeaf(triangles, bounds)
    }
    
    // Find longest axis of bounding box
    val size = bounds.size
    val axis = if (size.x > size.y && size.x > size.z) 0
               else if (size.y > size.z) 1
               else 2
    
    // Sort triangles by centroid along longest axis
    val sortedTriangles = triangles.sortBy(_.centroid.get(axis))
    
    // Split triangles into two roughly equal groups
    val mid = sortedTriangles.length / 2
    val (leftTris, rightTris) = sortedTriangles.splitAt(mid)
    
    // Recursively build children
    val left = buildRecursive(leftTris, depth + 1)
    val right = buildRecursive(rightTris, depth + 1)
    
    BVHInterior(left, right, bounds)
  }
}
