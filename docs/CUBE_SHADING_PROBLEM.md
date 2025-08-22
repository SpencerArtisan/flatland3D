# Cube Face Shading Problem

## The Problem
When rendering a cube in our ASCII 3D engine, each face should look identical when viewed straight-on (orthogonally). However, the test reveals that the top face is rendered with '*' characters (brighter) while all other faces are rendered with ':' characters.

This suggests a bug in how we handle lighting calculations when the cube is rotated.

## Core Hypothesis
When we rotate the cube to look at any face straight-on, that face should have exactly the same relationship to:
1. Our viewing direction (perpendicular)
2. The fixed light source at `Coord(-1, -1, -1)`

Therefore, all faces should have identical shading when viewed straight-on.

## Attempted Solutions

1. **Transform Light to Shape Space**
   - Tried transforming the light direction into shape space using `transformLightToShapeSpace`
   - Used `shapeRotation.inverse.applyTo(worldLight)`
   - Failed: Still got different shading for top face

2. **Transform Normal to World Space**
   - Tried transforming the triangle normal into world space using `placement.rotation.applyTo(triangle.normal)`
   - Failed: Still got different shading for top face

3. **Fix Rotation Order**
   - Tried changing the order of rotations in `Rotation.applyTo` from (roll, pitch, yaw) to (yaw, pitch, roll)
   - Failed: Still got different shading for top face

4. **Fix Inverse Rotation**
   - Tried fixing `Placement.worldToLocal` to use proper inverse rotation
   - Failed: Still got different shading for top face

## Analysis of Current Approach
I agree that I've been:
1. Making changes without a clear understanding of the underlying math
2. Not validating each change with proper geometric reasoning
3. Going in circles between different approaches without making real progress

## Suggested Path Forward

1. **Research Existing Solutions**
   - 3D graphics and lighting calculations are well-understood problems
   - We should research how standard 3D engines handle:
     - Coordinate space transformations (model → world → view)
     - Normal vector transformations
     - Light direction calculations
   - Key topics to research:
     - OpenGL/DirectX lighting models
     - Normal transformation matrices
     - View space vs world space lighting

2. **Validate Core Assumptions**
   - Are we correctly defining "straight-on" views?
   - Should we transform normals by the inverse transpose of the rotation matrix?
   - Are we handling coordinate spaces consistently?

3. **Create Visualization Tools**
   - Add debug output showing:
     - Original and transformed normal vectors
     - Light direction in different coordinate spaces
     - Dot product calculations at each step

4. **Consider Alternative Designs**
   - Maybe transform everything to view space first?
   - Consider using a standard lighting model (e.g., Phong) as reference
   - Look at how other ASCII art 3D engines handle this

## Next Steps
1. Research standard 3D graphics lighting implementations
2. Document the mathematical theory behind normal transformations
3. Create diagrams showing our coordinate space transformations
4. Implement proper debugging/visualization tools
5. Consider a clean-sheet implementation based on proven approaches

## Questions to Answer
1. How do standard 3D engines handle normal transformations during rotation?
2. What's the correct order of operations for transforming between coordinate spaces?
3. Should lighting calculations be done in world space, view space, or model space?
4. Are we handling the inverse rotation correctly for normal vectors?

