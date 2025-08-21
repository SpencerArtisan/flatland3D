# Flatland3D Quick Reference

## Essential Commands

### Build & Test
```bash
# Run all tests
sbt test

# Run specific test class
sbt "testOnly Shape101Spec"

# Run with verbose output
sbt "testOnly *Spec -- -v"

# Clean and rebuild
sbt clean compile

# Run the demo
sbt run
```

### Development
```bash
# Start sbt console
sbt

# Continuous compilation
sbt ~compile

# Continuous testing
sbt ~test
```

## Common Patterns

### Creating a New Shape
```scala
// 1. Add to TriangleShapes.scala
def newShape(id: Int, size: Double): TriangleMesh = {
  require(size > 0, "Size must be positive")
  val vertices = Seq(/* define vertices */)
  val triangles = Seq(/* define triangles with proper winding */)
  TriangleMesh(id, triangles)
}

// 2. Add tests
// 3. Update Main.scala if needed
```

### Adding New Rendering Features
```scala
// 1. Extend Renderer object
def renderNewFeature(world: World, params: Any*): String = {
  // Implementation
}

// 2. Add tests
// 3. Integrate with AnimationEngine if needed
```

### World Manipulation
```scala
// Create world
val world = World(width, height, depth)

// Add shape
val worldWithShape = world.add(shape, origin, rotation)

// Rotate shape
val rotatedWorld = world.rotate(shapeId, deltaRotation)

// Check result
rotatedWorld match {
  case Right(w) => // Success
  case Left(NoSuchShape(id)) => // Shape not found
}
```

## Key Constants

### Main Configuration
```scala
SHAPE_ID = 101
WORLD_SIZE = 22
CUBE_SIZE = 10
FRAME_DELAY_MS = 66          // ~15 FPS
YAW_ROTATION_RATE = Math.PI / -36   // -5°/frame
ROLL_ROTATION_RATE = Math.PI / 72   // 2.5°/frame
```

### Rendering Parameters
```scala
DEFAULT_AMBIENT = 0.2        // Base light level
DEFAULT_X_SCALE = 1          // Horizontal scaling
QUANTIZATION_LEVELS = 8      // Shading granularity
```

### Mathematical Constants
```scala
NORMAL_EPSILON = 0.5         // Normal calculation precision
PARALLEL_THRESHOLD = 1e-10   // Ray-triangle parallel threshold
INTERSECTION_THRESHOLD = 1e-10 // Ray-triangle intersection threshold
```

## Coordinate System

### 3D Space
- **Origin**: Top-left corner (0, 0, 0)
- **X**: Right (increasing)
- **Y**: Down (increasing) 
- **Z**: Forward (increasing)

### Vector Operations
```scala
val a = Coord(1.0, 2.0, 3.0)
val b = Coord(4.0, 5.0, 6.0)

a + b                    // Addition
a - b                    // Subtraction
a * 2.0                  // Scalar multiplication
a.dot(b)                 // Dot product
a.cross(b)               // Cross product
a.magnitude              // Vector length
a.normalize              // Unit vector
Coord.distance(a, b)     // Distance between points
```

## Rotation System

### Euler Angles
- **Yaw**: Z-axis rotation (left/right)
- **Pitch**: Y-axis rotation (up/down)
- **Roll**: X-axis rotation (forward/backward)

### Rotation Operations
```scala
val rotation = Rotation(yaw = Math.PI/4, pitch = 0, roll = 0)
val rotatedPoint = rotation.applyTo(originalPoint)
val inverseRotation = rotation.inverse
```

### Common Rotations
```scala
Rotation.ZERO                    // No rotation
Rotation(Math.PI/2, 0, 0)       // 90° yaw
Rotation(Math.PI, 0, 0)         // 180° yaw
Rotation(0, Math.PI/2, 0)       // 90° pitch
Rotation(0, 0, Math.PI/2)       // 90° roll
```

## Shape System

### Triangle Properties
```scala
case class Triangle(v0: Coord, v1: Coord, v2: Coord)

// Calculated properties
triangle.normal                  // Surface normal
triangle.centroid               // Triangle center
triangle.intersect(rayOrigin, rayDirection)  // Ray intersection
```

### Triangle Mesh
```scala
case class TriangleMesh(id: Int, triangles: Seq[Triangle])

// Key methods
mesh.occupiesSpaceAt(coord)     // Space occupancy test
mesh.surfaceNormalAt(local)     // Surface normal at point
mesh.intersectRay(origin, dir)  // Ray intersection
```

### Pre-built Shapes
```scala
TriangleShapes.cube(id, size)           // Cube
TriangleShapes.tetrahedron(id, size)    // Tetrahedron
TriangleShapes.pyramid(id, base, height) // Pyramid
```

## Rendering Pipeline

### Basic Rendering
```scala
// Simple rendering
val output = Renderer.renderWith(world, charFor)

// Shaded rendering
val shaded = Renderer.renderShaded(
  world, 
  lightDirection = Coord(0, 0, -1),
  ambient = 0.2,
  xScale = 2
)
```

### Custom Character Mapping
```scala
def charFor(placement: Placement): Char = {
  placement.shape match {
    case _: TriangleMesh => '#'
    case _ => '*'
  }
}
```

### Z-Scan Control
```scala
// Custom Z-scan order (near-to-far)
val nearToFarZs = 0 until world.depth
val output = Renderer.renderShaded(world, nearToFarZs = nearToFarZs)
```

## Testing Patterns

### Basic Test Structure
```scala
import org.scalatest.flatspec._
import org.scalatest.matchers._

class MySpec extends AnyFlatSpec with should.Matchers {
  "MyComponent" should "do something" in {
    // Test implementation
    result should be(expected)
  }
}
```

### Common Assertions
```scala
result should be(expected)           // Exact equality
result should not be(unexpected)    // Inequality
result should have length 5         // Collection length
result should contain("item")       // Collection contains
result should be > 0                // Numeric comparison
```

### Test Data Setup
```scala
// World setup
val world = World(10, 10, 10)
  .add(TriangleShapes.cube(1, 2), Coord(5, 5, 5))

// Rotation testing
val testRotations = Seq(
  Rotation.ZERO,
  Rotation(Math.PI/2, 0, 0),
  Rotation(Math.PI, 0, 0)
)
```

## Debugging Tools

### Frame Diagnostics
```scala
// Add to rendered output
val details = Seq(
  f"Frame: $frameIndex%3d",
  f"Yaw: ${yawDegrees}%6.1f°",
  f"Pitch: ${pitchDegrees}%6.1f°", 
  f"Roll: ${rollDegrees}%6.1f°"
).mkString("  ")

rendered + "\n\n" + details
```

### Performance Monitoring
```scala
val startTime = System.currentTimeMillis()
val result = expensiveOperation()
val duration = System.currentTimeMillis() - startTime
println(s"Operation took ${duration}ms")
```

### Boundary Checking
```scala
// Check if shape extends beyond world
val wouldExtend = placement.wouldExtendBeyondBounds(
  worldWidth, worldHeight, worldDepth
)

if (wouldExtend) {
  println("Shape would extend beyond bounds")
}
```

## Common Issues & Solutions

### Shape Truncation
**Problem**: Shapes appear cut off at world boundaries
**Solution**: 
```scala
// Check boundary violation
if (placement.wouldExtendBeyondBounds(width, height, depth)) {
  // Handle boundary case
}
```

### Rendering Artifacts
**Problem**: Strange characters or gaps in output
**Solution**:
```scala
// Verify Z-scan order
val zScan = (world.depth - 1) to 0 by -1

// Check shape occupancy
val isOccupied = world.placements.exists(_.occupiesSpaceAt(coord))
```

### Performance Issues
**Problem**: Slow animation with complex shapes
**Solution**:
```scala
// Reduce world size
val world = World(50, 50, 50)  // Instead of 100³

// Reduce triangle count
val simpleCube = TriangleShapes.cube(id, size)  // 12 triangles
```

### Lighting Inconsistencies
**Problem**: Uneven shading across surfaces
**Solution**:
```scala
// Check light direction
val lightDirection = Coord(0, 0, -1).normalize

// Verify surface normals
val normal = shape.surfaceNormalAt(localPoint)
```

## Performance Tips

### Optimization Strategies
1. **Pre-compute** trigonometric values
2. **Use lazy evaluation** for frame generation
3. **Early termination** in loops
4. **Minimize allocations** during animation

### Scalability Limits
- **World Size**: ~100³ for interactive performance
- **Triangle Count**: < 1000 for real-time rendering
- **Frame Rate**: Limited by rendering complexity

### Memory Management
- **Immutable data** structures
- **Lazy evaluation** prevents memory buildup
- **Object reuse** where possible

## Development Workflow

### 1. Design Discussion
- Discuss approach before coding
- Agree on best design
- Consider performance implications

### 2. Test-First Development
- Write tests for new functionality
- Ensure tests pass
- Add necessary validation

### 3. Code Review
- Follow established patterns
- Maintain immutability
- Use functional programming style

### 4. Testing
- Run full test suite
- Test edge cases
- Verify performance impact

## File Structure
```
src/main/scala/
├── Main.scala              # Entry point
├── World.scala             # World management
├── Shape.scala             # Shape definitions
├── TriangleShapes.scala    # Pre-built shapes
├── Renderer.scala          # Rendering engine
├── AnimationEngine.scala   # Animation controller
├── Coord.scala             # 3D coordinates
├── Rotation.scala          # 3D rotations
├── Placement.scala         # Shape positioning
└── NoSuchShape.scala       # Error handling

src/test/scala/
├── Shape101Spec.scala      # Complex shape tests
├── WorldSpec.scala         # World tests
├── TriangleSpec.scala      # Triangle tests
├── ShadingSpec.scala       # Lighting tests
└── AnimationEngineSpec.scala # Animation tests
```

## Quick Troubleshooting

| Issue | Check | Solution |
|-------|-------|----------|
| Tests fail | Run `sbt test` | Fix failing assertions |
| Shape invisible | Check Z-scan order | Verify far-to-near scan |
| Performance slow | Check world size | Reduce dimensions |
| Lighting wrong | Verify light direction | Check surface normals |
| Boundary errors | Check `wouldExtendBeyondBounds` | Handle boundary cases |
| Memory issues | Check lazy evaluation | Verify LazyList usage |

