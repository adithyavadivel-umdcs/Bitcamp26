# Feature Scope

## KEEP

### Core gameplay
- private lobby by invite code
- one seeker, multiple hiders
- ready check
- all players near start before match
- head start
- seeker freeze-at-spawn mechanic
- live map
- square play area
- square minimap centered on the local player
- hiders only see seeker when seeker is within visibility radius
- seeker does not see hiders by default
- shrinking square boundary
- elimination if outside square bounds after shrink resolution
- multiple hotspots
- tap-to-claim hotspots
- hotspot radius based on map half-width
- powerup duration based on map half-width
- local step counting
- player level display in lobby/loading screen

### Catch system
- each hider gets a unique 6-digit code
- seeker can only submit code while that hider is catch-eligible
- catch eligibility based on seeker radius to that hider
- correct eligible code eliminates hider
- no images
- no votes

### Postgame / eliminated flow
- rally point outside play area
- open in Google Maps button

## CUT

### Social
- open public lobbies
- stranger discovery
- friend-of-friend codes
- full friend graph
- contacts sync
- chat

### Gameplay complexity
- custom polygons
- complex multi-zone maps
- building detection
- same-building auto-catch
- image capture
- image review
- voting
- computer vision
- many powerups
- inventory system
- rarity system
- full anti-cheat system
- replay system
- hotspot dwell timers
- hotspot progress interruptions
- per-player hotspot cooldowns

### Platform complexity
- background gameplay
- app-closed location tracking
- Health Connect
- Google Fit
- in-app navigation
- cosmetics backend
