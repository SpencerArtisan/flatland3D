// Bounding Volume Hierarchy for accelerating ray-triangle intersection tests
class BVHMetrics {
  var nodesVisited: Int = 0
  var boxIntersectionTests: Int = 0
  var triangleIntersectionTests: Int = 0
  var triangleHits: Int = 0
  var maxDepthReached: Int = 0
  def merge(other: BVHMetrics): BVHMetrics = {
    val result = new BVHMetrics
    result.nodesVisited = nodesVisited + other.nodesVisited
    result.boxIntersectionTests = boxIntersectionTests + other.boxIntersectionTests
    result.triangleIntersectionTests = triangleIntersectionTests + other.triangleIntersectionTests
    result.triangleHits = triangleHits + other.triangleHits
    result.maxDepthReached = math.max(maxDepthReached, other.maxDepthReached)
    result
  }
  
  override def toString: String = {
    s"""BVH Traversal Metrics:
       |  Nodes Visited: $nodesVisited
       |  Box Tests: $boxIntersectionTests
       |  Triangle Tests: $triangleIntersectionTests
       |  Triangle Hits: $triangleHits
       |  Max Depth: $maxDepthReached
       |  Box Test Hit Rate: ${if (boxIntersectionTests > 0) f"${nodesVisited.toDouble / boxIntersectionTests * 100}%.1f%%" else "N/A"}
       |  Triangle Hit Rate: ${if (triangleIntersectionTests > 0) f"${triangleHits.toDouble / triangleIntersectionTests * 100}%.1f%%" else "N/A"}
       |""".stripMargin
  }
}

sealed trait BVHNode {
  def bounds: BoundingBox
  def intersectRay(origin: Coord, direction: Coord, metrics: BVHMetrics, depth: Int = 0): Option[(Double, Triangle)]
  def intersectRay(origin: Coord, direction: Coord): Option[(Double, Triangle)] = 
    intersectRay(origin, direction, new BVHMetrics(), 0)
}

case class BVHLeaf(triangles: Seq[Triangle], bounds: BoundingBox) extends BVHNode {
  def intersectRay(origin: Coord, direction: Coord, metrics: BVHMetrics, depth: Int): Option[(Double, Triangle)] = {
    metrics.maxDepthReached = math.max(metrics.maxDepthReached, depth)
    metrics.boxIntersectionTests += 1
    
    if (!bounds.intersectRay(origin, direction)) return None
    metrics.nodesVisited += 1
    
    var closestHit: Option[(Double, Triangle)] = None
    var closestDist = Double.PositiveInfinity
    
    triangles.foreach { triangle =>
      metrics.triangleIntersectionTests += 1
      triangle.intersectRay(origin, direction) match {
        case Some(dist) if dist < closestDist =>
          metrics.triangleHits += 1
          closestDist = dist
          closestHit = Some((dist, triangle))
        case _ =>
      }
    }
    
    closestHit
  }
}

case class BVHInterior(left: BVHNode, right: BVHNode, bounds: BoundingBox) extends BVHNode {
  def intersectRay(origin: Coord, direction: Coord, metrics: BVHMetrics, depth: Int): Option[(Double, Triangle)] = {
    metrics.maxDepthReached = math.max(metrics.maxDepthReached, depth)
    metrics.boxIntersectionTests += 1
    
    // Quick reject if ray misses bounding box
    if (!bounds.intersectRay(origin, direction)) return None
    metrics.nodesVisited += 1
    
    // Calculate distances to child bounding boxes
    var leftDist = Double.PositiveInfinity
    var rightDist = Double.PositiveInfinity
    
    // Test left child bounds
    metrics.boxIntersectionTests += 1
    if (left.bounds.intersectRay(origin, direction)) {
      // Approximate distance to box (using center point)
      val toCenter = left.bounds.center - origin
      leftDist = toCenter.dot(direction)
    }
    
    // Test right child bounds
    metrics.boxIntersectionTests += 1
    if (right.bounds.intersectRay(origin, direction)) {
      // Approximate distance to box
      val toCenter = right.bounds.center - origin
      rightDist = toCenter.dot(direction)
    }
    
    // Check closer child first, only check farther if needed
    var closestHit: Option[(Double, Triangle)] = None
    
    if (leftDist < rightDist) {
      // Check left first
      closestHit = left.intersectRay(origin, direction, metrics, depth + 1)
      if (closestHit.isEmpty || closestHit.get._1 > rightDist) {
        // Only check right if we haven't found a hit or if right might be closer
        val rightHit = right.intersectRay(origin, direction, metrics, depth + 1)
        if (rightHit.isDefined) {
          if (closestHit.isEmpty || rightHit.get._1 < closestHit.get._1) {
            closestHit = rightHit
          }
        }
      }
    } else {
      // Check right first
      closestHit = right.intersectRay(origin, direction, metrics, depth + 1)
      if (closestHit.isEmpty || closestHit.get._1 > leftDist) {
        // Only check left if we haven't found a hit or if left might be closer
        val leftHit = left.intersectRay(origin, direction, metrics, depth + 1)
        if (leftHit.isDefined) {
          if (closestHit.isEmpty || leftHit.get._1 < closestHit.get._1) {
            closestHit = leftHit
          }
        }
      }
    }
    
    closestHit
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
