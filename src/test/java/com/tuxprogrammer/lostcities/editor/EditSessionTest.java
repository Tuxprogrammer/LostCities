package com.tuxprogrammer.lostcities.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.transform.Transform;

/** {@link EditSession} and {@link EditSessions}: a player's session, kept by its world only. */
public class EditSessionTest {

    private static final UUID PLAYER = new UUID(1, 2);
    private static final UUID OTHER = new UUID(3, 4);

    private static EditSession session(String part) {
        Map<BlockRef, Character> reverse = new HashMap<>();
        reverse.put(BlockRef.of("minecraft:stone", 0), '#');
        return new EditSession(4, 5, new PartRecord(AssetId.parse(part), 70, Transform.ROTATE_NONE, 0, 0), reverse);
    }

    @Test
    public void doesNotReproduceCORE35_sessionsEndAtLogoutAndWithTheirWorld() {
        EditSessions overworld = new EditSessions();
        EditSessions nether = new EditSessions();
        overworld.start(PLAYER, session("building1_1"));
        overworld.start(OTHER, session("building1_2"));
        assertNull(nether.get(PLAYER), "a session belongs to the world it was started in");
        assertTrue(overworld.end(PLAYER), "logging out ends the session");
        assertNull(overworld.get(PLAYER));
        assertNotNull(overworld.get(OTHER));
        overworld.clear();
        assertEquals(0, overworld.size(), "an unloading world drops every session");
    }

    @Test
    public void aNewSessionReplacesTheOld() {
        EditSessions sessions = new EditSessions();
        sessions.start(PLAYER, session("building1_1"));
        EditSession second = session("building1_2");
        sessions.start(PLAYER, second);
        assertSame(second, sessions.get(PLAYER));
        assertEquals(1, sessions.size());
    }

    @Test
    public void aSessionIsImmutable() {
        EditSession session = session("building1_1");
        assertEquals(AssetId.parse("lostcities:building1_1"), session.part());
        assertEquals(70, session.bottom());
        assertEquals(4, session.chunkX());
        assertThrows(
            UnsupportedOperationException.class,
            () -> session.reverse()
                .put(BlockRef.AIR, ' '));
    }
}
