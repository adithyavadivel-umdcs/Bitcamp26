# Rules to Avoid Stupid Bugs

## Radius logic
- Never resolve elimination off a single location sample
- Use generous radii
- Respect accuracyMeters when possible

## Freeze logic
- For seeker freeze, require sustained violation before DQ

## Hotspot logic
- Client may show local claim affordance
- Server is final authority on claim success
- First valid claim wins
- Hotspot deactivates after successful claim
- Do not use dwell timers
- Do not use per-player cooldowns

## Catch logic
- Seeker code submission only succeeds if target hider is currently catch-eligible
- Eligibility is server-owned
- Server validates code against target hider only
- Wrong code does not eliminate anyone

## Shrink logic
- Use grace window after shrink
- Do not eliminate based on stale location
- Evaluate recent samples, not one sample

## Client/server separation
- Client never decides elimination
- Client never decides winner
- Client never decides hotspot award
