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
  initialHalfWidthMeters: number
  currentHalfWidthMeters: number
  seekerId: string
  config:
    headStartSeconds: number
    shrinkIntervalSeconds: number
    shrinkWarningSeconds: number
    catchEligibilityRadiusMeters: number
    hotspotSideLengthMeters: number
    hotspotMaxCoverageFraction: number
    hotspotDwellSeconds: number
    hotspotPersonalCooldownSeconds: number
    seekerFreezeRadiusMeters: number
    hiderVisionReductionDurationSeconds: number
    seekerMinimapBoostDurationSeconds: number
    hiderVisionReductionMultiplier: number
    seekerMinimapBoostMultiplier: number
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
  storedPowerupType: "NONE" | "HIDER_VISION_REDUCTION" | "SEEKER_MINIMAP_BOOST"
  nextPowerupEligibleAt: number
  currentHotspotId: string | null
  hotspotEnteredAt: number | null
  hotspotRewardGrantedForCurrentStay: boolean
  activePowerup: "NONE" | "HIDER_VISION_REDUCTION" | "SEEKER_MINIMAP_BOOST"
  powerupEndsAt: number | null
  catchCode: string | null
  catchEligible: boolean

lobbies/{lobbyId}/hotspots/{hotspotId}
  id: string
  lat: number
  lng: number
  radiusMeters: number // stored hotspot half-width for a 15ft by 15ft square
  active: boolean // always true for the full match in MVP
  powerupType: string

lobbies/{lobbyId}/events/{eventId}
  type: string
  createdAt: number
  payload: object
