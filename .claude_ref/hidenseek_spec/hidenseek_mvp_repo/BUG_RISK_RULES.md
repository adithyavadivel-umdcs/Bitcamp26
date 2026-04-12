# Rules to Avoid Stupid Bugs

## Radius logic
- Never resolve elimination off a single location sample
- Use generous radii
- Respect accuracyMeters when possible

## Freeze logic
- For seeker freeze, require sustained violation before DQ

## Hotspot logic
- Client may show local dwell progress affordance
- Server is final authority on hotspot reward success
- Multiple players may earn from the same hotspot independently
- Hotspot progress resets immediately on exit
- Use per-player dwell state, not hotspot locks
- Use per-player cooldowns only after a reward is granted

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
