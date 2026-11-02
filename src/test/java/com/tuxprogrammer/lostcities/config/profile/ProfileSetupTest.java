package com.tuxprogrammer.lostcities.config.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.config.ProfilesConfig;

/** How a dimension gets its profile without a world-preset screen (pinned-profile.md, "Lifecycle"). */
public class ProfileSetupTest {

    @Test
    public void dimensionProfilesMapsDimensionsToProfiles() {
        String[] saved = ProfilesConfig.dimensionProfiles;
        try {
            ProfilesConfig.dimensionProfiles = new String[] { "0=rarecities", " 7 = ", "x=default", "nosuch" };
            assertEquals("rarecities", ProfileSetup.profileFor(0));
            assertEquals("default", ProfileSetup.profileFor(7), "an entry with no profile means default");
            assertNull(ProfileSetup.profileFor(1));
            List<String> lines = ProfileSetup.describeDimensions();
            assertEquals(4, lines.size(), lines::toString);
            assertTrue(
                lines.get(0)
                    .contains("pin profile 'rarecities'"),
                lines::toString);
            assertTrue(
                lines.get(2)
                    .contains("ignored"),
                lines::toString);
        } finally {
            ProfilesConfig.dimensionProfiles = saved;
        }
    }

    @Test
    public void anUnknownProfileIsReportedAtStart() {
        String[] saved = ProfilesConfig.dimensionProfiles;
        try {
            ProfilesConfig.dimensionProfiles = new String[] { "0=nosuch" };
            assertTrue(
                ProfileSetup.describeDimensions()
                    .get(0)
                    .contains("does not exist"));
        } finally {
            ProfilesConfig.dimensionProfiles = saved;
        }
    }
}
