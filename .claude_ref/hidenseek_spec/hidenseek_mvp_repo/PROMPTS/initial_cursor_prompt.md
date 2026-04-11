We are building an Android-only hackathon MVP in Kotlin with Jetpack Compose.

Stack:
- Android Studio
- Kotlin
- Jetpack Compose
- Google Maps SDK for Android
- Maps Compose
- FusedLocationProviderClient
- Firebase Anonymous Auth
- Firebase Realtime Database
- Firebase Cloud Functions
- SensorManager step counter

Important constraints:
- No public lobbies
- Private invite-code lobby only
- No friend-of-friend feature
- No background gameplay
- No image system
- No voting
- No computer vision
- No Health Connect
- No Google Fit
- No in-app navigation
- All formulas in GameBalance.kt
- All critical game logic server-authoritative

Game loop:
- one seeker, multiple hiders
- all players start near each other
- seeker frozen during head start
- hiders always see seeker
- seeker does not see hiders by default
- circular map shrinks over time
- multiple hotspots
- tap-to-claim hotspots
- hotspots are single-use
- two powerups only:
  - hider invisibility
  - seeker reveal all
- each hider gets a unique 6-digit catch code
- seeker can submit a code only when that hider is catch-eligible
- correct eligible code eliminates the hider
- eliminated players get rally-point directions via Google Maps intent

Please first generate:
1. the Kotlin enums and data classes for game state, player role, powerup type, and hotspot state
2. GameBalance.kt with formulas derived from mapRadiusMeters
3. a high-level Realtime Database repository interface
4. a clean suggested navigation route list
Do not generate extra features.
