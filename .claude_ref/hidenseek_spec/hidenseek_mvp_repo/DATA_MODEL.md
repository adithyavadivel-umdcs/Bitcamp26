# Firebase Realtime Database Shape

users/{userId}
  displayName: string
  totalSteps: number
  level: number
  createdAt: number

lobbies/{lobbyId}
  hostId: string
  inviteCode: string
  state: string
  createdAt: number
  mapCenterLat: number
  mapCenterLng: number
  initialRadiusMeters: number
  currentRadiusMeters: number
  rallyPointLat: number
  rallyPointLng: number
  seekerId: string
  config:
    headStartSeconds: number
    shrinkIntervalSeconds: number
    shrinkWarningSeconds: number
    catchEligibilityRadiusMeters: number
    seekerFreezeRadiusMeters: number
    hiderInvisibilityDurationSeconds: number
    seekerRevealAllDurationSeconds: number
    startClusterRadiusMeters: number
  timing:
    gameStartAt: number
    nextShrinkAt: number
  results:
    winner: string
    endedAt: number

lobbies/{lobbyId}/players/{userId}
  userId: string
  displayName: string
  role: "SEEKER" | "HIDER"
  alive: boolean
  connected: boolean
  ready: boolean
  lat: number
  lng: number
  accuracyMeters: number
  lastLocationAt: number
  stepsAtGameStart: number
  currentMatchSteps: number
  activePowerup: "NONE" | "HIDER_INVISIBILITY" | "SEEKER_REVEAL_ALL"
  powerupEndsAt: number | null
  catchCode: string | null
  catchEligible: boolean

lobbies/{lobbyId}/hotspots/{hotspotId}
  id: string
  lat: number
  lng: number
  radiusMeters: number
  active: boolean
  claimedBy: string | null
  claimedAt: number | null
  powerupType: string

lobbies/{lobbyId}/events/{eventId}
  type: string
  createdAt: number
  payload: object
