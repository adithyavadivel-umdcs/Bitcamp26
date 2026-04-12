# Game Rules

## Lobby
- Host creates a private lobby with an invite code.
- Players join using invite code.
- No public discovery exists.
- No friend-of-friend feature exists in MVP.

## Roles
- One seeker
- Multiple hiders

## Start condition
- All players must be within the start radius of the host-selected start point.
- Host starts the game only when all players are marked ready and in range.

## Head start
- On game start, seeker is frozen at spawn.
- Seeker must remain within seekerFreezeRadiusMeters for headStartSeconds.
- If seeker leaves the freeze radius for more than seekerFreezeViolationSeconds continuously, seeker is disqualified and hiders win.
- Hiders can move immediately.

## Map boundary
- Play area is one square with:
  - center latitude
  - center longitude
  - current half-width
- The square shrinks every shrinkIntervalSeconds.
- Players are warned shrinkWarningSeconds before each shrink.
- After a shrink, players outside the new square bounds are eliminated after the grace evaluation window resolves.

## Elimination on shrink
- Elimination is server-authoritative.
- Use a short grace window after shrink.
- A player is eliminated only if their recent location samples remain outside the new square bounds through the grace evaluation logic.

## Visibility
- Minimap is square and always centered on the local player.
- Hiders see seeker location only when seeker is within seekerVisibilityRadiusMeters of that hider.
- Seeker does not see hider locations by default.
- If seeker uses the reveal-all powerup, seeker temporarily sees all active hiders.
- If a hider uses invisibility, that hider is hidden from seeker reveal systems during the powerup duration.

## Catch system
- Every hider is assigned a unique 6-digit code at match start.
- Seeker may submit a hider code only when the server currently marks that hider as catch-eligible.
- A hider is catch-eligible only when the seeker is within catchEligibilityRadiusMeters of that hider.
- If seeker submits the correct code for an eligible active hider, that hider is eliminated.
- If seeker submits an incorrect code, nothing happens except optional rate limiting or UI error.
- Seeker may submit codes at any time during RUNNING state, but only eligible codes can succeed.

## Hotspots
- There are multiple hotspots on the map.
- Hotspots are fixed points with a radius and a preset powerup type.
- Player must be inside hotspot radius to claim.
- Hotspot claim is tap-to-claim, not dwell-to-capture.
- Client may show claim availability locally.
- Server validates claim using latest known position and hotspot state.
- First valid claim wins.
- After a successful claim, hotspot becomes inactive.
- MVP default: hotspot is single-use for the whole game.

## MVP powerups
### Hider powerup
- Invisibility
- Duration: hiderInvisibilityDurationSeconds
- Effect: seeker cannot see this hider through seeker reveal systems during effect

### Seeker powerup
- Reveal all
- Duration: seekerRevealAllDurationSeconds
- Effect: seeker sees all active hider locations during effect

## Post-elimination
- Eliminated players can no longer influence movement gameplay.
- Eliminated players are directed to a rally point outside the play area.
- MVP routing: button opens Google Maps to rally point.

## Win conditions
- If seeker is disqualified, all surviving hiders win.
- If all hiders are eliminated, seeker wins.
- If time limit exists and expires before all hiders are eliminated, surviving hiders win.

## Disconnect behavior
- If a player disconnects briefly, show disconnected status.
- MVP simple rule:
  - if disconnected longer than disconnectTimeoutSeconds during active match, eliminate that player
- Host disconnect during active match does not end the game automatically if server state exists.
