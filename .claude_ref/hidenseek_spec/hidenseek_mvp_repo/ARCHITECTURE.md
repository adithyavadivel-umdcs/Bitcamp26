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
- show seeker and allowed markers on map
- show shrink warnings
- show hotspot claim buttons when locally in range
- allow seeker code submission UI
- show step count and level
- open Google Maps for rally point

## Server responsibilities
- create and validate lobby state
- assign roles
- assign unique hider codes
- verify ready/start proximity
- manage game phase transitions
- enforce seeker freeze
- compute shrink events
- resolve out-of-bounds elimination against square bounds
- determine whether seeker is visible to each hider based on visibility radius
- validate hotspot claims
- validate catch code submissions
- grant and resolve powerups
- compute winner

## Key idea
Client sends facts and intents.
Server decides outcomes.
