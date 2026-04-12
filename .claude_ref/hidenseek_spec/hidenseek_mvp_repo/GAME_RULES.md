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
- All players must be within the start radius of the host-selected start area.
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
- The square shrinks every 120 seconds.
- Each shrink subtracts 10 percent of the original half-width from both map dimensions while staying centered on the same map center.
- Players are warned shrinkWarningSeconds before each shrink.
- After a shrink, players outside the new square bounds are eliminated after the grace evaluation window resolves.
- When the square reaches 20 percent of its original half-width and at least one hider is still alive, hiders win immediately.

## Elimination on shrink
- Elimination is server-authoritative.
- Use a short grace window after shrink.
- A player is eliminated only if their recent location samples remain outside the new square bounds through the grace evaluation logic.

## Visibility
- Minimap is square and always centered on the local player.
- Frontend renders from shared RTDB world state and local minimap bounds.
- Hiders see seeker location only when seeker falls inside that hider's minimap window.
- Seeker does not see hider locations by default.
- If seeker minimap boost is active, seeker minimap width and height are both multiplied by 2.0.
- If a hider uses vision reduction, seeker-specific visibility for that hider is multiplied by 0.6.

## Catch system
- Every hider is assigned a unique 6-digit code at match start.
- Seeker may submit a hider code only when the server currently marks that hider as catch-eligible.
- A hider is catch-eligible only when the seeker is within catchEligibilityRadiusMeters of that hider.
- If seeker submits the correct code for an eligible active hider, that hider is eliminated.
- If seeker submits an incorrect code, nothing happens except optional rate limiting or UI error.
- Seeker may submit codes at any time during RUNNING state, but only eligible codes can succeed.

## Hotspots
- There are multiple hotspots on the map.
- Hotspots are fixed 15ft by 15ft squares with a preset powerup type.
- The number of hotspots is the highest integer that keeps total hotspot coverage strictly below 10 percent of total map area.
- Hotspot placement is randomized by the backend at match start.
- No two hotspots may intersect.
- Hotspots are permanently active for the whole match.
- Multiple players may earn from the same hotspot at the same time.
- There is no hotspot lock, no queue, and no occupancy cap.
- Player must remain inside hotspot square bounds continuously for hotspotDwellSeconds to earn that hotspot's powerup.
- If a player leaves before dwell completes, that player's hotspot progress resets immediately.
- If a player re-enters after leaving, a new stay starts immediately.
- After a player earns a hotspot reward, that uninterrupted stay cannot grant another reward.
- To earn again, the player must leave and re-enter a hotspot.
- Every player has a personal hotspot cooldown of hotspotPersonalCooldownSeconds after a reward.
- During personal cooldown, that player cannot earn from any hotspot.
- Backend evaluates hotspot dwell progress only when that player's location is written to RTDB.

## MVP powerups
### Hider powerup
- Vision reduction
- Duration: hiderVisionReductionDurationSeconds
- Effect: seeker-specific visibility for that hider becomes 0.6x the base amount

### Seeker powerup
- Minimap boost
- Duration: seekerMinimapBoostDurationSeconds
- Effect: seeker minimap width and height are doubled during effect

## Post-elimination
- Eliminated players can no longer influence movement gameplay.

## Win conditions
- If seeker is disqualified, all surviving hiders win.
- If all hiders are eliminated, seeker wins.
- If the map reaches the 20 percent shrink floor before all hiders are eliminated, surviving hiders win.
- If time limit exists and expires before all hiders are eliminated, surviving hiders win.

## Disconnect behavior
- If a player disconnects briefly, show disconnected status.
- MVP simple rule:
  - if disconnected longer than disconnectTimeoutSeconds during active match, eliminate that player
- Host disconnect during active match does not end the game automatically if server state exists.
