// Cache for recently hit triangles to accelerate ray intersection tests
class TriangleCache(capacity: Int) {
  require(capacity > 0, "Cache capacity must be positive")
  
  // Cache entry with triangle and its hit count
  private case class Entry(
    triangle: Triangle,
    bounds: BoundingBox,
    var hits: Int = 0
  )
  
  // LRU cache with hit counting
  private var entries = Vector.empty[Entry]
  
  // Add or update a triangle in the cache
  def recordHit(triangle: Triangle): Unit = {
    val bounds = BoundingBox.fromTriangle(triangle)
    entries.indexWhere(_.triangle == triangle) match {
      case -1 =>
        // Not in cache, add it
        if (entries.size >= capacity) {
          // Remove least recently used entry
          entries = entries.sortBy(_.hits).drop(1)
        }
        entries = entries :+ Entry(triangle, bounds, 1)
      case i =>
        // Update existing entry
        entries(i).hits += 1
        // Move to end (most recently used)
        entries = entries.patch(i, Nil, 1) :+ entries(i)
    }
  }
  
  // Try to find a triangle intersection in the cache
  def findIntersection(origin: Coord, direction: Coord): Option[(Double, Triangle)] = {
    var closestHit: Option[(Double, Triangle)] = None
    var closestDist = Double.PositiveInfinity
    
    entries.foreach { entry =>
      // Quick reject with bounding box
      if (entry.bounds.intersectRay(origin, direction)) {
        entry.triangle.intersectRay(origin, direction) match {
          case Some(dist) if dist < closestDist =>
            closestDist = dist
            closestHit = Some((dist, entry.triangle))
            entry.hits += 1
          case _ =>
        }
      }
    }
    
    closestHit
  }
  
  // Get cache statistics
  def stats: (Int, Int, Double) = {
    val size = entries.size
    val totalHits = entries.map(_.hits).sum
    val hitRate = if (size > 0) entries.map(_.hits.toDouble).sum / size else 0.0
    (size, totalHits, hitRate)
  }
  
  // Clear the cache
  def clear(): Unit = {
    entries = Vector.empty
  }
}
