package com.game.map;

import java.util.List;
import java.util.Random;

public class VaultManager {

    public record VaultLocation(int col, int row, String description) {}

    private static final List<VaultLocation> LOCATIONS = List.of(
        new VaultLocation(22,  8, "in the northeast corner of the village, near the forest edge"),
        new VaultLocation( 2,  8, "in the northwest corner of the village, where the trees cast long shadows"),
        new VaultLocation(22, 18, "at the southern end of the village, in the far eastern corner"),
        new VaultLocation( 2, 18, "at the southern end of the village, in the far western corner"),
        new VaultLocation(12, 18, "in the heart of the southern meadow, past the old pine groves"),
        new VaultLocation( 8, 16, "west of the village square, near the ancient pine cluster"),
        new VaultLocation(20, 16, "east of the village square, in the open field near the pine grove"),
        new VaultLocation(22, 13, "on the far eastern side of the village, past the wildflower patch")
    );

    private static final Random RANDOM = new Random();

    private int locationIndex;
    public boolean playerNearby = false;

    public VaultManager() {
        locationIndex = RANDOM.nextInt(LOCATIONS.size());
    }

    public VaultLocation getCurrent() { return LOCATIONS.get(locationIndex); }
    public int           getIndex()   { return locationIndex; }

    public void setIndex(int idx) {
        if (idx >= 0 && idx < LOCATIONS.size()) locationIndex = idx;
    }

    public void randomize() {
        int prev = locationIndex;
        if (LOCATIONS.size() > 1) {
            do { locationIndex = RANDOM.nextInt(LOCATIONS.size()); } while (locationIndex == prev);
        }
    }

    public String getContext() {
        return getCurrent().description();
    }
}
