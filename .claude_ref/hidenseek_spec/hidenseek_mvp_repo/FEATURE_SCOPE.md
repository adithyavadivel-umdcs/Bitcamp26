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
- hiders only see seeker when seeker falls inside the hider minimap window
- seeker does not see hiders by default
- shrinking square boundary
- elimination if outside square bounds after shrink resolution
- multiple hotspots
- hotspots are fixed 15ft by 15ft squares
- hotspot count is capped by strict sub-10-percent total map coverage
- dwell-based hotspot rewards
- per-player hotspot progress resets on exit
- per-player hotspot cooldowns
- hotspots can reward multiple players independently
- powerup duration based on map half-width
- seeker powerup doubles minimap width and height
- hider powerup reduces seeker-specific visibility for that hider to 0.6x
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

### Platform complexity
- background gameplay
- app-closed location tracking
- Health Connect
- Google Fit
- in-app navigation
- cosmetics backend
