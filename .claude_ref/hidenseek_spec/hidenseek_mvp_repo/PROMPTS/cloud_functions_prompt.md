Generate Firebase Cloud Functions for the server-authoritative game rules of an Android hackathon MVP.

Use TypeScript.

Requirements:
- lobby state machine with WAITING, READY_CHECK, HEAD_START, RUNNING, ENDED
- function to start game after validating all players are near start point
- function or scheduled logic to shrink circle
- function to evaluate out-of-bounds players using grace window
- function to enforce seeker freeze during head start
- function to assign unique six-digit hider codes at match start
- function to set catch eligibility based on seeker proximity to hiders
- function to validate hotspot claim with first-valid-claim-wins
- function to validate code submission and eliminate eligible hider on correct code
- function to resolve win conditions
- function to grant hotspot powerups
- client must not be able to directly set alive=false or winner values

Do not add public matchmaking, chat, image capture, or background tracking.
