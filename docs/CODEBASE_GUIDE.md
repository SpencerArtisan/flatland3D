# Flatland3D Codebase Guide

## Project Overview

Flatland3D is a 3D graphics rendering engine written in Scala that creates ASCII-based 3D visualizations directly in the terminal. The project demonstrates core computer graphics concepts including 3D transformations, lighting, shading, and real-time animation.

## Architecture & Design Patterns

### Core Architecture

The project follows a **component-based architecture** with clear separation of concerns:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Main.scala    │───▶│AnimationEngine  │───▶│    Renderer     │
│  (Entry Point)  │    │  (Controller)   │    │  (View Layer)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   World.scala   │    │   Placement     │    │   TriangleMesh  │
│  (Data Model)   │    │  (Transform)    │    │   (Geometry)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Key Design Principles

1. **Immutability**: Core data structures (`World`, `Coord`, `Rotation`) are immutable case classes
2. **Functional Programming**: Heavy use of Scala's functional features (map, flatMap, Option, Either)
3. **Separation of Concerns**: Clear boundaries between rendering, animation, and data management
4. **Type Safety**: Strong typing with case classes and sealed traits

## Core Components Deep Dive

### 1. Coordinate System (`Coord.scala`)

**Purpose**: 3D vector mathematics and coordinate operations

**Key Features**:
- Vector operations: `+`, `-`, `*`, `dot`, `cross`
- Utility methods: `magnitude`, `normalize`, `distance`, `midpoint`, `lerp`
- Immutable case class design

**Usage Pattern**:
```scala
val point = Coord(1.0, 2.0, 3.0)
val direction = Coord(0.0, 0.0, -1.0).normalize
val distance = Coord.distance(point, Coord.ZERO)
```

### 2. World Management (`World.scala`)

**Purpose**: 3D world container that manages shape placement and boundaries

**Key Features**:
- Immutable world state with `add` and `rotate` operations
- Boundary validation during rotations
- Shape placement tracking via `Map[Int, Placement]`

**Critical Methods**:
```scala
def add(shape: Shape, origin: Coord, rotation: Rotation = Rotation.ZERO): World
def rotate(shapeId: Int, delta: Rotation): Either[NoSuchShape, World]
def placements: Iterable[Placement]
```

**Validation**: Prevents shapes from extending beyond world boundaries during rotation

### 3. Shape System (`Shape.scala`)

**Purpose**: Abstract shape definitions with triangle-based geometry

**Hierarchy**:
```
Shape (trait)
├── TriangleMesh (case class)
└── Triangle (case class)
```

**Key Features**:
- `occupiesSpaceAt(coord: Coord): Boolean` - Space occupancy testing
- `surfaceNormalAt(local: Coord): Coord` - Surface normal calculation
- Ray-triangle intersection using Möller-Trumbore algorithm

**Triangle Mesh Implementation**:
- Multiple ray casting for robust inside/outside testing
- Majority voting system for edge case handling
- Closest triangle normal calculation for lighting

### 4. Transformation System

#### Rotation (`Rotation.scala`)
**Purpose**: 3D rotation handling with Euler angles

**Implementation**: 
- Pre-computed trigonometric values for performance
- Order: Roll (X) → Pitch (Y) → Yaw (Z)
- Inverse rotation support

**Usage**:
```scala
val rotation = Rotation(yaw = Math.PI/4, pitch = 0, roll = 0)
val rotatedPoint = rotation.applyTo(originalPoint)
```

#### Placement (`Placement.scala`)
**Purpose**: Combines shape, position, and rotation

**Key Features**:
- World-to-local coordinate transformation
- Rotation composition
- Boundary violation prevention

**Transformation Pipeline**:
1. Translate by negative origin
2. Apply inverse rotation
3. Check shape occupancy in local space

### 5. Rendering System (`Renderer.scala`)

**Purpose**: Converts 3D world state to 2D ASCII art

**Rendering Modes**:
- `renderWith`: Basic rendering with custom character mapping
- `renderShaded`: Lambertian shading with lighting calculations

**Key Features**:
- Z-buffer-like depth ordering (far-to-near scan)
- Surface normal-based lighting
- Configurable shading characters and ambient light
- X-axis scaling for terminal compatibility

**Lighting Model**:
```scala
brightness = ambient + (1 - ambient) * max(0, normal · lightDirection)
```

### 6. Animation Engine (`AnimationEngine.scala`)

**Purpose**: Manages real-time animation loop and frame generation

**Key Features**:
- Lazy frame generation using `LazyList`
- Smooth rotation interpolation
- Terminal clearing and frame display
- Frame diagnostics (rotation angles, frame count)

**Animation Loop**:
1. Generate rotation for current frame
2. Apply rotation to world
3. Render frame with shading
4. Display with diagnostics
5. Sleep for frame delay

## Testing Strategy

### Test Framework
- **ScalaTest** with `AnyFlatSpec` and `should.Matchers`
- **Test-first approach** (as per workspace rules)
- Comprehensive coverage of core functionality

### Test Categories

#### 1. Unit Tests
- **Shape101Spec**: Complex shape behavior and edge cases
- **WorldSpec**: World management and rendering
- **TriangleSpec**: Triangle geometry and intersection
- **ShadingSpec**: Lighting and shading calculations

#### 2. Test Patterns
- **Property-based testing** for mathematical operations
- **Edge case coverage** (boundary conditions, extreme rotations)
- **Regression testing** for known issues (e.g., frame 190 truncation)
- **Integration testing** between components

#### 3. Test Data
- **Deterministic test cases** with known expected outputs
- **Boundary value testing** (world edges, shape boundaries)
- **Rotation edge cases** (90°, 180°, extreme angles)

### Running Tests
```bash
# Run all tests
sbt test

# Run specific test class
sbt "testOnly Shape101Spec"

# Run with verbose output
sbt "testOnly *Spec -- -v"
```

## Development Workflow

### 1. Code Quality Standards

**Immutability**: Prefer immutable data structures
**Functional Style**: Use `map`, `flatMap`, `Option`, `Either`
**Type Safety**: Leverage Scala's type system
**Performance**: Pre-compute values where possible (e.g., trigonometric functions)

### 2. Adding New Features

**Before coding** (per workspace rules):
1. Discuss design approach
2. Write tests first
3. Ensure agreement on best design

**Implementation Steps**:
1. Add tests for new functionality
2. Implement core logic
3. Add necessary validation
4. Update related components
5. Run full test suite

### 3. Common Patterns

#### Adding New Shapes
```scala
// 1. Define in TriangleShapes.scala
def newShape(id: Int, params: Double*): TriangleMesh = {
  // Create vertices and triangles
  // Ensure proper winding order
  TriangleMesh(id, triangles)
}

// 2. Add tests
// 3. Update Main.scala if needed
```

#### Adding New Rendering Features
```scala
// 1. Extend Renderer object
def renderNewFeature(world: World, params: Any*): String = {
  // Implementation
}

// 2. Add tests
// 3. Integrate with AnimationEngine if needed
```

### 4. Debugging Tips

**Frame Analysis**:
- Use frame diagnostics in animation
- Check rotation values and world state
- Verify boundary conditions

**Rendering Issues**:
- Check Z-scan order
- Verify shape occupancy calculations
- Test with simple cases first

**Performance Issues**:
- Profile with large worlds
- Check for unnecessary recalculations
- Optimize trigonometric computations

## Configuration & Constants

### Key Configuration Points

**Main.scala**:
```scala
private val SHAPE_ID = 101
private val WORLD_SIZE = 22
private val CUBE_SIZE = 10
private val FRAME_DELAY_MS = 66
private val YAW_ROTATION_RATE = Math.PI / -36
private val ROLL_ROTATION_RATE = Math.PI / 72
```

**Renderer.scala**:
```scala
private val DEFAULT_EPSILON = 1e-10
private val DEFAULT_AMBIENT = 0.2
private val QUANTIZATION_LEVELS = 8
```

**Shape.scala**:
```scala
private val NORMAL_EPSILON = 0.5
private val PARALLEL_THRESHOLD = 1e-10
private val INTERSECTION_THRESHOLD = 1e-10
```

## Performance Considerations

### Optimization Strategies

1. **Pre-computation**: Trigonometric values in `Rotation`
2. **Lazy Evaluation**: `LazyList` for frame generation
3. **Efficient Data Structures**: `Map` for shape lookup
4. **Minimal Allocations**: Reuse objects where possible

### Performance Bottlenecks

1. **Ray-triangle intersection**: O(n) per pixel for complex meshes
2. **Surface normal calculation**: Gradient-based estimation
3. **Boundary checking**: Vertex transformation for all triangles

### Scalability Limits

- **World Size**: Memory usage scales with width × height × depth
- **Shape Complexity**: Triangle count affects rendering performance
- **Animation Speed**: Frame rate limited by rendering complexity

## Common Issues & Solutions

### 1. Shape Truncation
**Problem**: Shapes appear cut off at world boundaries
**Solution**: Check `wouldExtendBeyondBounds` in `Placement`

### 2. Rendering Artifacts
**Problem**: Strange characters or gaps in output
**Solution**: Verify Z-scan order and shape occupancy

### 3. Performance Issues
**Problem**: Slow animation with complex shapes
**Solution**: Reduce triangle count or world size

### 4. Lighting Inconsistencies
**Problem**: Uneven shading across surfaces
**Solution**: Check surface normal calculations and light direction

## Future Development Areas

### Potential Enhancements

1. **More Shape Types**: Spheres, cylinders, custom meshes
2. **Advanced Lighting**: Multiple light sources, shadows
3. **Texture Support**: ASCII-based texturing
4. **Animation Improvements**: Keyframe animation, easing
5. **Performance**: Spatial partitioning, GPU acceleration

### Architecture Improvements

1. **Plugin System**: Extensible shape and renderer plugins
2. **Configuration Management**: External configuration files
3. **Logging & Monitoring**: Performance metrics and debugging
4. **Error Handling**: More robust error recovery

## Conclusion

This codebase demonstrates solid software engineering principles with a clear separation of concerns, comprehensive testing, and functional programming patterns. The modular design makes it easy to extend and modify while maintaining code quality and performance.

When working with this codebase:
1. **Always run tests** after changes
2. **Follow the established patterns** for consistency
3. **Discuss design changes** before implementation
4. **Test edge cases** thoroughly
5. **Consider performance implications** of changes

