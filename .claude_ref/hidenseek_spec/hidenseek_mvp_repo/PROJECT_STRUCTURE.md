# Suggested Android Project Structure

app/
  src/main/java/com/example/hidenseek/
    MainActivity.kt

    core/
      model/
        GameState.kt
        PlayerRole.kt
        PowerupType.kt
        HotspotState.kt
      balance/
        GameBalance.kt
      util/
        GeoUtils.kt
        TimeUtils.kt
        CodeUtils.kt

    data/
      auth/
        AuthRepository.kt
      lobby/
        LobbyRepository.kt
        LobbyRemoteDataSource.kt
      location/
        LocationRepository.kt
      steps/
        StepsRepository.kt
      maps/
        MapsIntentRepository.kt

    domain/
      usecase/
        JoinLobbyUseCase.kt
        StartGameUseCase.kt
        SubmitLocationUseCase.kt
        ClaimHotspotUseCase.kt
        SubmitCatchCodeUseCase.kt
        UsePowerupUseCase.kt

    feature/
      auth/
        AuthViewModel.kt
        AuthScreen.kt
      lobby/
        LobbyViewModel.kt
        LobbyScreen.kt
      ready/
        ReadyCheckViewModel.kt
        ReadyCheckScreen.kt
      match/
        MatchViewModel.kt
        MatchScreen.kt
        components/
          MapView.kt
          HotspotOverlay.kt
          PlayerMarkers.kt
          ShrinkBanner.kt
      results/
        ResultsViewModel.kt
        ResultsScreen.kt

    navigation/
      AppNavGraph.kt
      Routes.kt

functions/
  src/
    index.ts
    balance.ts
    geo.ts
    gameEngine.ts
