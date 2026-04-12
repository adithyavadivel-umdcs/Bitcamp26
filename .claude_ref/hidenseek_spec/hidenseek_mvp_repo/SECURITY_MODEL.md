# Security Model

## Client may write
- own user profile basics
- own location under current lobby player node
- own ready state
- host start request through callable function or command path
- hotspot claim request through callable function or command path
- catch code submission request through callable function or command path
- powerup use request through callable function or command path

## Client may NOT write directly
- lobby winner
- eliminations
- seeker assignment after game start
- shrink timing
- hotspot grant resolution
- catch eligibility
- other players' states
- other players' catch codes

## Server-owned fields
- state transitions
- alive status
- currentRadiusMeters
- timing values after match start
- hotspot state resolution
- code validation resolution
- events log
