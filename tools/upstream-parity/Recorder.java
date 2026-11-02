import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import mcjty.lostcities.varia.QualityRandom;
import mcjty.lostcities.worldgen.street.BridgeHashes;
import mcjty.lostcities.worldgen.street.HierarchicalStreetPlanner;
import mcjty.lostcities.worldgen.street.PlannedStreetInfo;
import mcjty.lostcities.worldgen.street.StreetPlannerSettings;

/**
 * Records reference values from upstream Lost Cities 1.20 (f18a094d) and from Minecraft 1.20.1 itself, for the unit
 * tests of Lost Cities: New Horizons to compare against (decision log, M03 parity row). Throwaway harness: it is run
 * by record.sh on a machine with JDK 17 or newer, never by the mod's build, and it contains no upstream or Mojang
 * code. Upstream's pure classes are compiled from the 1.20 branch beside it (stage.py); Minecraft's classes are reached
 * by reflection, with names resolved through Mojang's published mappings.
 *
 * <p>
 * Usage: {@code java Recorder <server-1.20.1.jar> <server.txt mappings> <libraries dir> <out dir>}.
 */
public final class Recorder {

    private static final long[] SEEDS = { 0L, 1L, -1L, 42L, 123456789L, -987654321012345L, Long.MIN_VALUE,
        Long.MAX_VALUE, 0x5DEECE66DL };

    private final Mappings mappings;
    private final ClassLoader minecraft;

    private Recorder(Mappings mappings, ClassLoader minecraft) {
        this.mappings = mappings;
        this.minecraft = minecraft;
    }

    public static void main(String[] args) throws Exception {
        Path jar = Paths.get(args[0]);
        Mappings mappings = Mappings.read(Paths.get(args[1]));
        List<URL> urls = new ArrayList<>();
        urls.add(jar.toUri().toURL());
        try (Stream<Path> libs = Files.walk(Paths.get(args[2]))) {
            for (Path lib : (Iterable<Path>) libs.filter(p -> p.toString().endsWith(".jar"))::iterator) {
                urls.add(lib.toUri().toURL());
            }
        }
        Path out = Paths.get(args[3]);
        Files.createDirectories(out);
        Recorder recorder = new Recorder(mappings, new URLClassLoader(urls.toArray(new URL[0]), null));
        recorder.qualityRandom(out.resolve("quality_random.txt"));
        recorder.legacyRandom(out.resolve("legacy_random.txt"));
        recorder.simplex(out.resolve("simplex_noise.txt"));
        recorder.rarity(out.resolve("city_rarity.txt"));
        recorder.resourceLocations(out.resolve("resource_location_hash.txt"));
        recorder.streets(out);
        recorder.bridges(out.resolve("bridge_hashes.txt"));
    }

    // ---------------------------------------------------------------- random streams

    /** The draw sequence every random recording uses, so the tests replay the same calls. */
    private interface Draws {
        long nextLong();

        int nextInt();

        int nextInt(int bound);

        float nextFloat();

        double nextDouble();

        boolean nextBoolean();
    }

    private static void drawSequence(PrintWriter w, long seed, Draws d) {
        for (int round = 0; round < 6; round++) {
            w.println(seed + " L " + d.nextLong());
            w.println(seed + " I " + d.nextInt());
            w.println(seed + " I7 " + d.nextInt(7));
            w.println(seed + " I1000 " + d.nextInt(1000));
            w.println(seed + " I256 " + d.nextInt(256));
            w.println(seed + " F " + Integer.toHexString(Float.floatToIntBits(d.nextFloat())));
            w.println(seed + " D " + Long.toHexString(Double.doubleToLongBits(d.nextDouble())));
            w.println(seed + " Z " + d.nextBoolean());
        }
    }

    private void qualityRandom(Path file) throws IOException {
        try (PrintWriter w = writer(file, "QualityRandom (upstream varia/QualityRandom.java), op sequence per seed")) {
            List<Long> seeds = new ArrayList<>();
            for (long s : SEEDS) {
                seeds.add(s);
            }
            // The building random of a few chunks (BuildingInfo.getBuildingRandom).
            for (int[] c : new int[][] { { 0, 0 }, { 1, 0 }, { 0, 1 }, { -1, -1 }, { 37, -112 }, { -400, 250 } }) {
                seeds.add(42L + c[1] * 341873128712L + c[0] * 132897987541L);
            }
            for (long seed : seeds) {
                QualityRandom r = new QualityRandom(seed);
                drawSequence(w, seed, new Draws() {

                    public long nextLong() {
                        return r.nextLong();
                    }

                    public int nextInt() {
                        return r.nextInt();
                    }

                    public int nextInt(int bound) {
                        return r.nextInt(bound);
                    }

                    public float nextFloat() {
                        return r.nextFloat();
                    }

                    public double nextDouble() {
                        return r.nextDouble();
                    }

                    public boolean nextBoolean() {
                        return r.nextBoolean();
                    }
                });
            }
        }
    }

    private Object legacyRandomSource(long seed) throws ReflectiveOperationException {
        Class<?> c = mcClass("net.minecraft.world.level.levelgen.LegacyRandomSource");
        return c.getConstructor(long.class).newInstance(seed);
    }

    private void legacyRandom(Path file) throws Exception {
        try (PrintWriter w = writer(file, "Minecraft 1.20.1 LegacyRandomSource(seed), op sequence per seed")) {
            String rs = "net.minecraft.util.RandomSource";
            for (long seed : SEEDS) {
                Object r = legacyRandomSource(seed);
                Method nextLong = mcMethod(rs, "nextLong");
                Method nextInt = mcMethod(rs, "nextInt");
                Method nextIntBound = mcMethod(rs, "nextInt", "int");
                Method nextFloat = mcMethod(rs, "nextFloat");
                Method nextDouble = mcMethod(rs, "nextDouble");
                Method nextBoolean = mcMethod(rs, "nextBoolean");
                drawSequence(w, seed, new Draws() {

                    public long nextLong() {
                        return (Long) call(nextLong, r);
                    }

                    public int nextInt() {
                        return (Integer) call(nextInt, r);
                    }

                    public int nextInt(int bound) {
                        return (Integer) call(nextIntBound, r, bound);
                    }

                    public float nextFloat() {
                        return (Float) call(nextFloat, r);
                    }

                    public double nextDouble() {
                        return (Double) call(nextDouble, r);
                    }

                    public boolean nextBoolean() {
                        return (Boolean) call(nextBoolean, r);
                    }
                });
            }
        }
    }

    // ---------------------------------------------------------------- noise

    private static final double[] SIMPLEX_COORDS = { -7.0, -3.0, -2.5, -1.0, -0.5, -0.25, 0.0, 0.25, 1.0 / 3.0, 0.5,
        1.0, 2.0, 2.75, 3.0, 10.0, 123.456, -1000.0 / 3.0 };

    private Object simplexNoise(long seed) throws ReflectiveOperationException {
        Class<?> simplex = mcClass("net.minecraft.world.level.levelgen.synth.SimplexNoise");
        Constructor<?> ctor = simplex.getConstructor(mcClass("net.minecraft.util.RandomSource"));
        return ctor.newInstance(legacyRandomSource(seed));
    }

    private void simplex(Path file) throws Exception {
        try (PrintWriter w = writer(
            file,
            "Minecraft 1.20.1 SimplexNoise(new LegacyRandomSource(seed)).getValue(x, y): seed x y value-bits")) {
            Method getValue = mcMethod("net.minecraft.world.level.levelgen.synth.SimplexNoise", "getValue", "double",
                "double");
            for (long seed : new long[] { 0L, 42L, -5L, 123456789L }) {
                Object noise = simplexNoise(seed);
                for (double x : SIMPLEX_COORDS) {
                    for (double y : SIMPLEX_COORDS) {
                        double v = (Double) call(getValue, noise, x, y);
                        w.println(seed + " " + Long.toHexString(Double.doubleToLongBits(x)) + " "
                            + Long.toHexString(Double.doubleToLongBits(y)) + " "
                            + Long.toHexString(Double.doubleToLongBits(v)));
                    }
                }
            }
        }
    }

    /**
     * Upstream's {@code CityRarityMap} over {@code PerlinNoiseGenerator14}, evaluated with Minecraft's own
     * {@code SimplexNoise}: four octaves, each from a fresh {@code LegacyRandomSource(seed)}, summed as
     * {@code value(x * f, z * f) / f} with {@code f} halving; then {@code max(0, v * innerScale - offset)} as a float.
     */
    private void rarity(Path file) throws Exception {
        try (PrintWriter w = writer(
            file,
            "upstream CityRarityMap.getCityFactor(cx, cz) over Minecraft 1.20.1 SimplexNoise:"
                + " seed scale offset innerScale cx cz float-bits")) {
            Method getValue = mcMethod("net.minecraft.world.level.levelgen.synth.SimplexNoise", "getValue", "double",
                "double");
            double[][] params = { { 3, 0.1, 0.1 }, { 5, 0.0, 1.0 }, { 2.5, 0.05, 0.3 } };
            for (long seed : new long[] { 0L, 42L, -77777777777L }) {
                Object[] octaves = new Object[4];
                for (int i = 0; i < 4; i++) {
                    octaves[i] = simplexNoise(seed);
                }
                for (double[] p : params) {
                    double scale = p[0];
                    double offset = p[1];
                    double inner = p[2];
                    for (int cx = -30; cx <= 30; cx += 3) {
                        for (int cz = -30; cz <= 30; cz += 5) {
                            double x = cx / scale;
                            double z = cz / scale;
                            double sum = 0;
                            double f = 1;
                            for (int i = 0; i < 4; i++) {
                                sum += (Double) call(getValue, octaves[i], x * f, z * f) / f;
                                f /= 2.0;
                            }
                            double factor = sum * inner - offset;
                            if (factor < 0) {
                                factor = 0;
                            }
                            w.println(seed + " " + scale + " " + offset + " " + inner + " " + cx + " " + cz + " "
                                + Integer.toHexString(Float.floatToIntBits((float) factor)));
                        }
                    }
                }
            }
        }
    }

    private void resourceLocations(Path file) throws Exception {
        try (PrintWriter w = writer(file, "Minecraft 1.20.1 new ResourceLocation(ns, path).hashCode(): id hash")) {
            Class<?> rl = mcClass("net.minecraft.resources.ResourceLocation");
            Constructor<?> ctor = rl.getConstructor(String.class, String.class);
            String[][] ids = { { "minecraft", "overworld" }, { "minecraft", "the_nether" }, { "minecraft", "the_end" },
                { "lostcities", "lostcity" }, { "lostcitiesnh", "dim0" }, { "lostcitiesnh", "dim7" },
                { "lostcitiesnh", "dim-1" } };
            for (String[] id : ids) {
                Object loc = ctor.newInstance(id[0], id[1]);
                w.println(id[0] + ":" + id[1] + " " + loc.hashCode());
            }
        }
    }

    // ---------------------------------------------------------------- streets and bridges

    private static StreetPlannerSettings defaults() {
        return new StreetPlannerSettings(8, 8, .45f, 4, 0, 2, 0, 2, 4, 3, .40f, 2, 5);
    }

    private static StreetPlannerSettings variant() {
        return new StreetPlannerSettings(12, 9, .3f, 3, 1, 3, 0, 1, 3, 2, .9f, 1, 7);
    }

    private void streets(Path out) throws IOException {
        Object[][] cases = { { "default_seed0", 0L, "minecraft:overworld", defaults() },
            { "default_seed42", 42L, "minecraft:overworld", defaults() },
            { "default_seedneg", -6543210987654321L, "minecraft:overworld", defaults() },
            { "default_seed42_dim7", 42L, "lostcitiesnh:dim7", defaults() },
            { "variant_seed42", 42L, "minecraft:overworld", variant() } };
        for (Object[] c : cases) {
            String name = (String) c[0];
            long seed = (Long) c[1];
            String dim = (String) c[2];
            StreetPlannerSettings settings = (StreetPlannerSettings) c[3];
            HierarchicalStreetPlanner planner = new HierarchicalStreetPlanner(seed, dim, settings);
            try (PrintWriter w = writer(
                out.resolve("streets_" + name + ".txt"),
                "upstream HierarchicalStreetPlanner, seed " + seed + ", dimension " + dim + ", settings " + settings)) {
                w.println("offsets " + planner.primaryOffsetX() + " " + planner.primaryOffsetZ());
                StringBuilder vertical = new StringBuilder("vertical");
                StringBuilder horizontal = new StringBuilder("horizontal");
                for (int i = -100; i <= 100; i++) {
                    if (planner.isVerticalPrimary(i)) {
                        vertical.append(' ').append(i);
                    }
                    if (planner.isHorizontalPrimary(i)) {
                        horizontal.append(' ').append(i);
                    }
                }
                w.println(vertical);
                w.println(horizontal);
                // Raw road type map, rows of constant chunk Z from -48 to 47, columns chunk X -48 to 47.
                for (int z = -48; z < 48; z++) {
                    StringBuilder row = new StringBuilder("row " + z + " ");
                    for (int x = -48; x < 48; x++) {
                        row.append(planner.getRoadType(x, z).name().charAt(0));
                    }
                    w.println(row);
                }
                for (int x = -48; x < 48; x += 7) {
                    for (int z = -48; z < 48; z += 11) {
                        PlannedStreetInfo s = planner.getStreetInfo(x, z);
                        w.println("info " + x + " " + z + " " + s.roadType() + " " + s.north() + " " + s.south() + " "
                            + s.west() + " " + s.east() + " " + s.primaryBlockX() + " " + s.primaryBlockZ() + " "
                            + s.primaryWestX() + " " + s.primaryNorthZ() + " " + s.primaryEastX() + " "
                            + s.primarySouthZ() + " " + Long.toHexString(Double.doubleToLongBits(s.density())) + " "
                            + s.secondaryRoadsX() + " " + s.secondaryRoadsZ() + " "
                            + (s.tertiarySegment() == null ? "-"
                                : s.tertiarySegment().id() + "," + s.tertiarySegment().originX() + ","
                                    + s.tertiarySegment().originZ() + "," + s.tertiarySegment().direction() + ","
                                    + s.tertiarySegment().length()));
                    }
                }
            }
        }
    }

    private void bridges(Path file) throws IOException {
        try (PrintWriter w = writer(
            file,
            "upstream HierarchicalBridgePlanner span hashes (seed dim orientation minX minZ maxX maxZ hash) and"
                + " preferred intersection orientations (seed dim orientation)")) {
            String[] dims = { "minecraft:overworld", "lostcitiesnh:dim7" };
            for (long seed : SEEDS) {
                for (String dim : dims) {
                    w.println("preferred " + seed + " " + dim + " " + BridgeHashes.preferred(seed, dim));
                    for (int[] span : new int[][] { { 0, 0, 5, 0 }, { -3, 17, 9, 17 }, { 40, -8, 40, 3 },
                        { -1000, -1000, -1000, -990 } }) {
                        for (String o : new String[] { "X", "Z" }) {
                            long h = BridgeHashes.span(seed, dim, o, span[0], span[1], span[2], span[3]);
                            w.println("span " + seed + " " + dim + " " + o + " " + span[0] + " " + span[1] + " "
                                + span[2] + " " + span[3] + " " + h);
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private static PrintWriter writer(Path file, String what) throws IOException {
        PrintWriter w = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8));
        w.println("# " + what);
        w.println("# Recorded by tools/upstream-parity (Recorder.java); do not edit by hand.");
        return w;
    }

    private Class<?> mcClass(String named) throws ClassNotFoundException {
        return Class.forName(mappings.obfClass(named), true, minecraft);
    }

    private Method mcMethod(String owner, String name, String... params) throws ReflectiveOperationException {
        Class<?> c = mcClass(owner);
        String obf = mappings.obfMethod(owner, name, params);
        Class<?>[] types = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            types[i] = primitive(params[i]);
        }
        Method m = c.getMethod(obf, types);
        m.setAccessible(true);
        return m;
    }

    private static Class<?> primitive(String name) {
        switch (name) {
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "double":
                return double.class;
            case "float":
                return float.class;
            default:
                throw new IllegalArgumentException(name);
        }
    }

    private static Object call(Method m, Object target, Object... args) {
        try {
            return m.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Mojang's ProGuard-format mappings: named class and member to obfuscated name. */
    private static final class Mappings {

        private final Map<String, String> classes = new HashMap<>();
        private final Map<String, String> methods = new HashMap<>();

        static Mappings read(Path file) throws IOException {
            Mappings m = new Mappings();
            String current = null;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                if (!line.startsWith(" ")) {
                    String[] parts = line.substring(0, line.length() - 1).split(" -> ");
                    current = parts[0];
                    m.classes.put(parts[0], parts[1]);
                    continue;
                }
                String body = line.trim();
                int arrow = body.indexOf(" -> ");
                String left = body.substring(0, arrow);
                String obf = body.substring(arrow + 4);
                if (!left.contains("(")) {
                    continue;
                }
                left = left.replaceFirst("^[0-9]+:[0-9]+:", "");
                String sig = left.substring(left.indexOf(' ') + 1);
                m.methods.put(current + "#" + sig, obf);
            }
            return m;
        }

        String obfClass(String named) {
            String obf = classes.get(named);
            if (obf == null) {
                throw new IllegalArgumentException("no mapping for " + named);
            }
            return obf;
        }

        String obfMethod(String owner, String name, String... params) {
            String key = owner + "#" + name + "(" + String.join(",", params) + ")";
            String obf = methods.get(key);
            if (obf == null) {
                throw new IllegalArgumentException("no mapping for " + key);
            }
            return obf;
        }
    }
}
