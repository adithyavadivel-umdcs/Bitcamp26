# Rules for all AI code generation

## Project constraints
- Android only
- Kotlin only
- Jetpack Compose only
- Firebase Realtime Database for live game state
- Firebase Authentication with anonymous auth
- Firebase Cloud Functions for server-authoritative game logic
- Google Maps SDK for Android + Maps Compose
- FusedLocationProviderClient for live location
- SensorManager step counter for local steps
- No background gameplay support in MVP
- No public lobby discovery
- No friend-of-friend feature
- No image system
- No voting system
- No computer vision
- No Health Connect
- No Google Fit

## Architecture rules
- All gameplay formulas must live in one file: GameBalance.kt
- All critical game rules must be decided by server-side code or server-owned state
- Client may send:
  - location updates
  - join or leave intents
  - ready state
  - host start request
  - hotspot claim request
  - code submission request
  - powerup use request
- Client must NOT decide:
  - eliminations
  - shrink resolution
  - hotspot grant resolution
  - winner
  - catch validity
- Never duplicate formulas across files
- Never place game-balance constants directly in UI files
- Use one canonical game state enum
- Use one canonical player role enum
- Use one canonical hotspot state model
- Use one canonical player visibility model

## UI rules
- Compose only
- Separate screens and viewmodels cleanly
- Keep screens simple and hackathon-friendly
- Avoid overengineering
- Do not add features outside the documented MVP

## Coding rules
- Prefer small, clear files
- Prefer explicit data classes
- Prefer sealed classes or enums for game states
- Comment tricky logic
- Do not invent extra mechanics
