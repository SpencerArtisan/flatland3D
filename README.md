# Flatland3D

A 3D graphics rendering engine written in Scala that creates ASCII-based 3D visualizations directly in the terminal.

## Overview

Flatland3D demonstrates core computer graphics concepts by rendering 3D shapes as animated ASCII art. The engine implements fundamental 3D graphics features including transformations, lighting, shading, and backface culling - all rendered using Unicode block characters in your terminal.

## Features

- **3D Shape Rendering**: Renders 3D objects (boxes) as ASCII art using Unicode block characters
- **Real-time Animation**: Smooth rotation animations with configurable frame rates
- **Lighting System**: Directional lighting with ambient light support
- **Shading**: Surface normal calculations for realistic lighting effects  
- **Backface Culling**: Performance optimization by hiding non-visible faces
- **Boundary Checking**: Prevents shapes from extending beyond world boundaries
- **Frame Diagnostics**: Real-time display of rendering bounds and aspect ratios

## Quick Start

### Prerequisites

- Scala 2.13.14
- sbt (Scala Build Tool)

### Running the Demo

```bash
# Clone and navigate to the project
cd flatland3D

# Run the animated demo
sbt run
```

The demo will display a rotating 3D box (40×70×20 units) in a 300×180×60 world space, updating at ~15 FPS.

### Running Tests

```bash
# Run all tests
sbt test
```

## Architecture

### Core Components

- **World**: 3D coordinate system that manages shape placement and boundaries
- **Shape**: Abstract shapes with occupancy and surface normal calculations
- **Box**: Concrete 3D rectangular shape implementation
- **Renderer**: Converts 3D world state to ASCII art with lighting
- **Coord**: 3D coordinate system with vector operations
- **Rotation**: 3D rotation handling (yaw, pitch, roll)
- **Placement**: Combines shape, position, and rotation

### Rendering Pipeline

1. **World Setup**: Define 3D world boundaries and place shapes
2. **Transformation**: Apply rotations to shapes each frame
3. **Lighting Calculation**: Compute surface normals and apply directional/ambient lighting
4. **Rasterization**: Convert 3D coordinates to 2D terminal output
5. **Display**: Render ASCII art to terminal with frame diagnostics

## Configuration

The main demo can be customized by modifying `Main.scala`:

- **World size**: `World(width, height, depth)`
- **Shape dimensions**: `Box(id, width, height, depth)`
- **Animation speed**: `Thread.sleep(milliseconds)`
- **Lighting**: `lightDirection` and `ambient` parameters
- **Rendering**: `xScale` and `cullBackfaces` options

## Technical Details

- Uses Unicode block character (█) for solid pixels
- Implements gradient-based surface normal estimation
- Supports deterministic face shading for consistent lighting
- Includes boundary violation prevention during rotations
- Terminal clearing using ANSI escape sequences

## Project Structure

```
src/main/scala/
├── Main.scala          # Entry point and animation loop
├── World.scala         # 3D world management
├── Shape.scala         # Shape definitions (Box)
├── Renderer.scala      # 3D to ASCII conversion
├── Coord.scala         # 3D coordinate operations
├── Rotation.scala      # 3D rotation handling
└── Placement.scala     # Shape positioning

src/test/scala/         # Test suites
```

## License

This project is available under standard open source licensing terms.