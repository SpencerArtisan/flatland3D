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
  private val NUM_BINS = 12  // Number of bins for SAH calculation
  
  // Cost constants for SAH
  private val INTERSECTION_COST = 1.0  // Cost of ray-triangle intersection
  private val TRAVERSAL_COST = 0.125   // Cost of traversing a node (typically 1/8th of intersection)
  
  // Bin structure for SAH calculation
  private case class Bin(
    bounds: BoundingBox,
    triangleCount: Int
  ) {
    def extendWith(triangle: Triangle): Bin = {
      val triangleBounds = BoundingBox.fromTriangle(triangle)
      Bin(
        bounds.union(triangleBounds),
        triangleCount + 1
      )
    }
  }
  
  private object Bin {
    def empty: Bin = Bin(
      bounds = BoundingBox(Coord(0,0,0), Coord(0,0,0)),
      triangleCount = 0
    )
  }
  
  // Calculate SAH cost for a potential split
  private def calculateSAHCost(
    leftBounds: BoundingBox,
    leftCount: Int,
    rightBounds: BoundingBox,
    rightCount: Int,
    parentArea: Double
  ): Double = {
    val leftProb = leftBounds.surfaceArea / parentArea
    val rightProb = rightBounds.surfaceArea / parentArea
    
    TRAVERSAL_COST + (
      leftProb * leftCount * INTERSECTION_COST +
      rightProb * rightCount * INTERSECTION_COST
    )
  }
  
  // Find best split using Surface Area Heuristic
  private def findBestSplit(triangles: Seq[Triangle], parentBounds: BoundingBox): (Int, Double) = {
    if (triangles.length <= 1) return (0, Double.PositiveInfinity)
    
    // Find axis with maximum extent
    val size = parentBounds.size
    val axis = if (size.x > size.y && size.x > size.z) 0
               else if (size.y > size.z) 1
               else 2
               
    // Initialize bins
    val bins = Array.fill(NUM_BINS)(Bin.empty)
    
    // Calculate bin width
    val min = parentBounds.min.get(axis)
    val max = parentBounds.max.get(axis)
    val extent = max - min
    val binWidth = extent / NUM_BINS
    
    // Sort triangles into bins
    triangles.foreach { triangle =>
      val centroid = triangle.centroid
      val binIndex = {
        val normalizedPos = (centroid.get(axis) - min) / extent
        val bin = (normalizedPos * NUM_BINS).toInt
        math.min(NUM_BINS - 1, math.max(0, bin))  // Clamp to valid range
      }
      bins(binIndex) = bins(binIndex).extendWith(triangle)
    }
    
    // Sweep from left to right, accumulating bounds
    val leftBounds = Array.fill(NUM_BINS)(Bin.empty)
    var running = Bin.empty
    for (i <- bins.indices) {
      running = Bin(
        running.bounds.union(bins(i).bounds),
        running.triangleCount + bins(i).triangleCount
      )
      leftBounds(i) = running
    }
    
    // Sweep from right to left, finding lowest SAH cost
    running = Bin.empty
    var minCost = Double.PositiveInfinity
    var bestSplit = 0
    val parentArea = parentBounds.surfaceArea
    
    for (i <- (0 until NUM_BINS - 1).reverse) {
      running = Bin(
        running.bounds.union(bins(i + 1).bounds),
        running.triangleCount + bins(i + 1).triangleCount
      )
      
      val cost = calculateSAHCost(
        leftBounds(i).bounds,
        leftBounds(i).triangleCount,
        running.bounds,
        running.triangleCount,
        parentArea
      )
      
      if (cost < minCost) {
        minCost = cost
        bestSplit = i
      }
    }
    
    (bestSplit, minCost)
  }
  
  def build(triangles: Seq[Triangle]): BVHNode = {
    buildRecursive(triangles, 0)
  }
  
  private def buildRecursive(triangles: Seq[Triangle], depth: Int): BVHNode = {
    val bounds = BoundingBox.fromTriangles(triangles)
    
    // Create leaf node if few enough triangles or max depth reached
    if (triangles.length <= MAX_TRIANGLES_PER_LEAF || depth >= MAX_DEPTH) {
      return BVHLeaf(triangles, bounds)
    }
    
    // Find best split using SAH
    val (splitBin, splitCost) = findBestSplit(triangles, bounds)
    
    // Find axis with maximum extent
    val size = bounds.size
    val axis = if (size.x > size.y && size.x > size.z) 0
               else if (size.y > size.z) 1
               else 2
    
    // Calculate split position
    val min = bounds.min.get(axis)
    val max = bounds.max.get(axis)
    val extent = max - min
    val splitPos = min + (extent * (splitBin + 1).toDouble / NUM_BINS)
    
    // If split cost is worse than just making a leaf, create leaf
    val leafCost = triangles.length * INTERSECTION_COST
    if (splitCost >= leafCost && triangles.length <= MAX_TRIANGLES_PER_LEAF * 2) {
      return BVHLeaf(triangles, bounds)
    }
    
    // Split triangles based on SAH split position
    val (leftTris, rightTris) = triangles.partition { triangle =>
      triangle.centroid.get(axis) <= splitPos
    }
    
    // Handle degenerate splits
    if (leftTris.isEmpty || rightTris.isEmpty) {
      val mid = triangles.length / 2
      val (left, right) = triangles.splitAt(mid)
      return BVHInterior(
        buildRecursive(left, depth + 1),
        buildRecursive(right, depth + 1),
        bounds
      )
    }
    
    // Recursively build children
    val left = buildRecursive(leftTris, depth + 1)
    val right = buildRecursive(rightTris, depth + 1)
    
    BVHInterior(left, right, bounds)
  }
}
