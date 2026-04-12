# Architecture

## Principle
Server-authoritative rules.
Client-driven rendering.

## Client responsibilities
- render all screens
- request location permission
- subscribe to lobby state
- send periodic location updates every 2 to 3 seconds
- render a square minimap centered on the local player
- use shared RTDB coordinates plus local minimap bounds to decide what to render
- if seeker has the seeker minimap boost active, double minimap width and height
- if a hider has hider vision reduction active, apply a 0.6x seeker-specific visibility window for that hider
- show shrink warnings
- show hotspot dwell progress from locally observed hotspot and player state
- allow seeker code submission UI
- show step count and level

## Server responsibilities
- create and validate lobby state
- assign roles
- assign unique hider codes
- verify ready/start proximity
- manage game phase transitions
- enforce seeker freeze
- compute shrink events
- resolve out-of-bounds elimination against square bounds
- evaluate hotspot dwell progress on per-player location writes
- validate catch code submissions
- grant and resolve powerups
- compute winner

## Key idea
Client sends facts and intents.
Server decides outcomes.
Client decides minimap rendering.
