case class Viewport(center: Coord, width: Int, height: Int, depth: Int) {
  // Validation
  require(width > 0, "Viewport width must be positive")
  require(height > 0, "Viewport height must be positive")
  require(depth > 0, "Viewport depth must be positive")

  // Calculate the world bounds that this viewport covers
  def worldBounds: WorldBounds = {
    val halfWidth = width / 2
    val halfHeight = height / 2
    val halfDepth = depth / 2
    
    WorldBounds(
      minX = center.x.toInt - halfWidth,
      maxX = center.x.toInt + halfWidth - 1,
      minY = center.y.toInt - halfHeight,
      maxY = center.y.toInt + halfHeight - 1,
      minZ = center.z.toInt - halfDepth,
      maxZ = center.z.toInt + halfDepth - 1
    )
  }

  // Transform world coordinates to viewport coordinates
  def worldToViewport(worldCoord: Coord): Coord = {
    val bounds = worldBounds
    Coord(
      Math.max(0, Math.min(width, (worldCoord.x - bounds.minX).toInt)),
      Math.max(0, Math.min(height, (worldCoord.y - bounds.minY).toInt)),
      Math.max(0, Math.min(depth, (worldCoord.z - bounds.minZ).toInt))
    )
  }

  // Check if a world coordinate is within this viewport's bounds
  def containsWorldCoord(worldCoord: Coord): Boolean = {
    val bounds = worldBounds
    worldCoord.x >= bounds.minX && worldCoord.x <= bounds.maxX &&
    worldCoord.y >= bounds.minY && worldCoord.y <= bounds.maxY &&
    worldCoord.z >= bounds.minZ && worldCoord.z <= bounds.maxZ
  }

  // Zoom in/out while keeping the same center
  def zoom(factor: Double): Viewport = {
    require(factor > 0, "Zoom factor must be positive")
    val newWidth = Math.max(1, (width / factor).toInt)
    val newHeight = Math.max(1, (height / factor).toInt)
    val newDepth = Math.max(1, (depth / factor).toInt)
    
    this.copy(width = newWidth, height = newHeight, depth = newDepth)
  }

  // Pan the viewport by moving the center
  def pan(offset: Coord): Viewport = {
    this.copy(center = center + offset)
  }

  // Reset to default viewport
  def reset: Viewport = Viewport.default

  // Get the aspect ratio
  def aspectRatio: Double = width.toDouble / height
}

object Viewport {
  // Default viewport size (increased to accommodate multiple shapes)
  val default: Viewport = Viewport(Coord(0, 0, 0), 60, 60, 60)
  
  // Create viewport with default size centered at specified point
  def centeredAt(center: Coord): Viewport = default.copy(center = center)
  
  // Create viewport with specified dimensions centered at origin
  def withDimensions(width: Int, height: Int, depth: Int): Viewport = 
    Viewport(Coord(0, 0, 0), width, height, depth)
}

case class WorldBounds(minX: Int, maxX: Int, minY: Int, maxY: Int, minZ: Int, maxZ: Int)
