package com.tuxprogrammer.lostcities.worldgen.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.worldgen.overlay.LinkScenarios.RailLeg.Boundary;

/**
 * The {@code network.railway} links branch's boundary verdict (D-p4v-5): a boundary where track does not meet is broken
 * unless an explosion reaches either chunk, which the highway walk and {@code city.walk} already count without judging,
 * since upstream's damage breaks rails as it breaks decks.
 */
public class LinkScenariosTest {

    private static List<int[]> rails(int... acrossAndY) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < acrossAndY.length; i += 2) {
            out.add(new int[] { acrossAndY[i], acrossAndY[i + 1] });
        }
        return out;
    }

    @Test
    public void trackThatMeetsIsJoinedWhetherDamagedOrNot() {
        assertEquals(Boundary.JOINED, LinkScenarios.RailLeg.judge(rails(7, 54), rails(7, 54), false));
        assertEquals(Boundary.JOINED, LinkScenarios.RailLeg.judge(rails(7, 54), rails(7, 55), true));
    }

    @Test
    public void noTrackOnEitherSideIsBrokenUnlessAnExplosionReaches() {
        List<int[]> none = Collections.emptyList();
        assertEquals(Boundary.DAMAGED, LinkScenarios.RailLeg.judge(none, none, true));
        assertEquals(Boundary.BROKEN, LinkScenarios.RailLeg.judge(none, none, false));
    }

    @Test
    public void trackThatDoesNotMeetIsBroken() {
        assertEquals(Boundary.BROKEN, LinkScenarios.RailLeg.judge(rails(7, 54), rails(7, 57), false));
        assertEquals(Boundary.BROKEN, LinkScenarios.RailLeg.judge(rails(7, 54), rails(3, 54), false));
        assertEquals(Boundary.BROKEN, LinkScenarios.RailLeg.judge(rails(7, 54), Collections.<int[]>emptyList(), false));
        assertEquals(
            Boundary.BROKEN,
            LinkScenarios.RailLeg.judge(Arrays.asList(new int[] { 7, 54 }, new int[] { 8, 54 }), rails(7, 54), false));
    }
}
