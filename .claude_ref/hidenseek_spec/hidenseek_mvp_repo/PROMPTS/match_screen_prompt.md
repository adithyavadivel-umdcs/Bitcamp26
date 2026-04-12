Create a Jetpack Compose MatchScreen for an Android-only hide-and-seek hackathon MVP.

Requirements:
- Google Maps via Maps Compose
- show play-area circle
- show current shrinking radius
- show multiple hotspots
- show rally point if current player is eliminated
- if current player is a hider:
  - always show seeker marker
  - do not show other hider markers
- if current player is the seeker:
  - show self marker
  - show hiders only when server marks them visible
- top overlay for timers:
  - head start timer
  - next shrink timer
- bottom controls:
  - use powerup button if available
  - claim hotspot button when inside an active hotspot
  - seeker code entry panel when current player is seeker
- keep UI clean and minimal
