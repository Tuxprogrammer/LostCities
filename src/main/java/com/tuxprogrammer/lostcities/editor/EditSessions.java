package com.tuxprogrammer.lostcities.editor;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * The part-editing sessions of one world, by player. The overlay of the world owns it, so every session is dropped
 * when the world unloads, and {@link EditorEvents} drops a player's session when the player logs out (avoids CORE-35;
 * CONTRIBUTING § Architecture, no mutable statics). A new session of a player replaces the old one, as upstream.
 *
 * <p>
 * Server thread only. Side: both (plain Java).
 */
public final class EditSessions {

    private final Map<UUID, EditSession> sessions = new TreeMap<>();

    /**
     * Returns a player's session.
     *
     * @param player the player's UUID
     * @return the session, or {@code null} when the player is not editing in this world
     */
    public EditSession get(UUID player) {
        return sessions.get(player);
    }

    /**
     * Starts or replaces a player's session.
     *
     * @param player  the player's UUID
     * @param session the session
     */
    public void start(UUID player, EditSession session) {
        sessions.put(player, session);
    }

    /**
     * Ends a player's session.
     *
     * @param player the player's UUID
     * @return whether there was one
     */
    public boolean end(UUID player) {
        return sessions.remove(player) != null;
    }

    /** Returns how many sessions are open. */
    public int size() {
        return sessions.size();
    }

    /** Ends every session; the world is unloading. */
    public void clear() {
        sessions.clear();
    }
}
