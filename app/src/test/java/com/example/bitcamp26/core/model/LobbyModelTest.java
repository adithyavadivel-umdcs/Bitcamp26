package com.example.bitcamp26.core.model;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LobbyModelTest {

    @Test
    public void codeAndInviteCodeStayInSync() {
        Lobby lobby = new Lobby();

        lobby.setCode("ABC123");
        assertEquals("ABC123", lobby.getCode());
        assertEquals("ABC123", lobby.getInviteCode());

        lobby.setInviteCode("ROOM99");
        assertEquals("ROOM99", lobby.getCode());
        assertEquals("ROOM99", lobby.getInviteCode());
    }

    @Test
    public void runningOrEndedStateMarksLobbyStarted() {
        Lobby lobby = new Lobby();

        lobby.setState("WAITING");
        assertFalse(lobby.isStarted());

        lobby.setState("RUNNING");
        assertTrue(lobby.isStarted());

        lobby.setState("ENDED");
        assertTrue(lobby.isStarted());
    }

    @Test
    public void setPlayersUpdatesDerivedPlayerCount() {
        Player one = new Player();
        one.setId("1");
        Player two = new Player();
        two.setId("2");

        Lobby lobby = new Lobby();
        lobby.setPlayers(Arrays.asList(one, two));

        assertEquals(2, lobby.getPlayerCount());
    }
}
