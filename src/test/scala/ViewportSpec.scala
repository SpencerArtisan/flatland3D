import org.scalatest.flatspec._
import org.scalatest.matchers._

class ViewportSpec extends AnyFlatSpec with should.Matchers {

  "A Viewport" should "be created with valid dimensions" in {
    val viewport = Viewport(Coord(0, 0, 0), 10, 8, 6)
    viewport.center should be(Coord(0, 0, 0))
    viewport.width should be(10)
    viewport.height should be(8)
    viewport.depth should be(6)
  }

  it should "reject negative dimensions" in {
    an [IllegalArgumentException] should be thrownBy Viewport(Coord(0, 0, 0), -1, 5, 5)
    an [IllegalArgumentException] should be thrownBy Viewport(Coord(0, 0, 0), 5, -1, 5)
    an [IllegalArgumentException] should be thrownBy Viewport(Coord(0, 0, 0), 5, 5, -1)
  }

  it should "calculate world bounds correctly" in {
    val viewport = Viewport(Coord(10, 10, 10), 20, 16, 12)
    viewport.worldBounds should be(
      WorldBounds(
        minX = 0, maxX = 19,    // 10 - 10 to 10 + 9
        minY = 2, maxY = 17,    // 10 - 8 to 10 + 7  
        minZ = 4, maxZ = 15     // 10 - 6 to 10 + 5
      )
    )
  }

  it should "transform world coordinates to viewport coordinates" in {
    val viewport = Viewport(Coord(10, 10, 10), 20, 16, 12)
    
    // World coordinate at viewport center
    viewport.worldToViewport(Coord(10, 10, 10)) should be(Coord(10, 8, 6))
    
    // World coordinate at viewport edge
    viewport.worldToViewport(Coord(0, 2, 4)) should be(Coord(0, 0, 0))
    
    // World coordinate outside viewport
    viewport.worldToViewport(Coord(30, 30, 30)) should be(Coord(20, 16, 12))
  }

  it should "check if world coordinate is within viewport bounds" in {
    val viewport = Viewport(Coord(10, 10, 10), 20, 16, 12)
    
    // Inside viewport
    viewport.containsWorldCoord(Coord(10, 10, 10)) should be(true)
    viewport.containsWorldCoord(Coord(5, 5, 5)) should be(true)
    viewport.containsWorldCoord(Coord(15, 15, 15)) should be(true)
    
    // Outside viewport
    viewport.containsWorldCoord(Coord(30, 30, 30)) should be(false)
    viewport.containsWorldCoord(Coord(-5, -5, -5)) should be(false)
  }

  it should "support zooming in and out" in {
    val viewport = Viewport(Coord(10, 10, 10), 20, 16, 12)
    
    val zoomedIn = viewport.zoom(2.0)  // Zoom in by 2x
    zoomedIn.width should be(10)
    zoomedIn.height should be(8)
    zoomedIn.depth should be(6)
    zoomedIn.center should be(Coord(10, 10, 10))  // Center stays the same
    
    val zoomedOut = viewport.zoom(0.5)  // Zoom out by 0.5x
    zoomedOut.width should be(40)
    zoomedOut.height should be(32)
    zoomedOut.depth should be(24)
    zoomedOut.center should be(Coord(10, 10, 10))  // Center stays the same
  }

  it should "support panning" in {
    val viewport = Viewport(Coord(10, 10, 10), 20, 16, 12)
    
    val panned = viewport.pan(Coord(5, 3, 2))
    panned.center should be(Coord(15, 13, 12))
    panned.width should be(20)   // Dimensions unchanged
    panned.height should be(16)
    panned.depth should be(12)
  }

  it should "support reset to default size" in {
    val viewport = Viewport(Coord(10, 10, 10), 20, 16, 12)
    val defaultViewport = Viewport.default
    
    val reset = viewport.reset
    reset.width should be(defaultViewport.width)
    reset.height should be(defaultViewport.height)
    reset.depth should be(defaultViewport.depth)
    reset.center should be(Coord(0, 0, 0))  // Reset to origin
  }

  it should "maintain aspect ratio when zooming" in {
    val viewport = Viewport(Coord(0, 0, 0), 20, 16, 12)
    val aspectRatio = viewport.width.toDouble / viewport.height
    
    val zoomed = viewport.zoom(2.0)
    val newAspectRatio = zoomed.width.toDouble / zoomed.height
    
    newAspectRatio should be(aspectRatio +- 0.01)
  }
}

case class WorldBounds(minX: Int, maxX: Int, minY: Int, maxY: Int, minZ: Int, maxZ: Int)
