package com.Morph.logisticspipes.client.pipe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Minimal OBJ parser for PipeModel_moved.obj.
 *
 * Replicates LP1's loading pipeline:
 *   scale = 1/100  (OBJ units → block units)
 *   Z offset = +1  (mirrors LP1's LPTranslation(0,0,1) applied after parsing)
 *
 * Groups are stored by their full OBJ "g" line so callers can search with
 * the same contains/endsWith logic LP1 uses.
 */
public final class LPPipeModel {

    // One rendered face (always 4 verts — triangles have v3 == v2)
    public record Quad(
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            float nx,  float ny,  float nz) {}

    // Full "g" line → faces that belong to that group
    private final Map<String, List<Quad>> groups;

    private LPPipeModel(Map<String, List<Quad>> groups) {
        this.groups = Collections.unmodifiableMap(groups);
    }

    /**
     * Strict match — LP1 uses for sides, edges, corners, mounts:
     *   contains(" " + id + " ")  OR  endsWith(" " + id)
     */
    public List<Quad> get(String id) {
        String mid = " " + id + " ";
        String end = " " + id;
        List<Quad> result = new ArrayList<>();
        for (Map.Entry<String, List<Quad>> e : groups.entrySet()) {
            String key = e.getKey();
            if (key.contains(mid) || key.endsWith(end)) {
                result.addAll(e.getValue());
            }
        }
        return result;
    }

    /**
     * Loose match — LP1 uses for Texture_Plate_* and Inner_Plate_*:
     *   contains(" " + id)   → matches "Texture_Plate_N" AND "Texture_Plate_N1"
     */
    public List<Quad> getLoose(String id) {
        String prefix = " " + id;
        List<Quad> result = new ArrayList<>();
        for (Map.Entry<String, List<Quad>> e : groups.entrySet()) {
            if (e.getKey().contains(prefix)) {
                result.addAll(e.getValue());
            }
        }
        return result;
    }

    /** Count matching groups (for debugging). */
    public int groupCount(String id) {
        String midPattern = " " + id + " ";
        String endPattern = " " + id;
        return (int) groups.keySet().stream()
                .filter(k -> k.contains(midPattern) || k.endsWith(endPattern))
                .count();
    }

    // -------------------------------------------------------------------------

    public static LPPipeModel parse(InputStream is) throws IOException {
        List<float[]> verts = new ArrayList<>();  // [x, y, z]
        List<float[]> uvs   = new ArrayList<>();  // [u, v]

        Map<String, List<Quad>> groups = new LinkedHashMap<>();
        String currentGroup = "g default";
        groups.put(currentGroup, new ArrayList<>());

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) {
                    String[] p = line.substring(2).trim().split("\\s+");
                    verts.add(new float[]{ Float.parseFloat(p[0]),
                                           Float.parseFloat(p[1]),
                                           Float.parseFloat(p[2]) });
                } else if (line.startsWith("vt ")) {
                    String[] p = line.substring(3).trim().split("\\s+");
                    uvs.add(new float[]{ Float.parseFloat(p[0]),
                                         Float.parseFloat(p[1]) });
                } else if (line.startsWith("g ")) {
                    currentGroup = line;
                    groups.putIfAbsent(currentGroup, new ArrayList<>());
                } else if (line.startsWith("f ")) {
                    String[] tokens = line.substring(2).trim().split("\\s+");
                    // Build list of [vertIdx, uvIdx] for each token
                    int[][] indices = new int[tokens.length][2];
                    for (int i = 0; i < tokens.length; i++) {
                        String[] parts = tokens[i].split("/");
                        indices[i][0] = Integer.parseInt(parts[0]);  // 1-based
                        indices[i][1] = parts.length > 1 && !parts[1].isEmpty()
                                ? Integer.parseInt(parts[1]) : 1;  // 1-based
                    }
                    // Convert to quads (triangles → degenerate quad with v[2] repeated)
                    if (indices.length == 4) {
                        groups.get(currentGroup).add(buildQuad(verts, uvs, indices, 0, 1, 2, 3));
                    } else if (indices.length == 3) {
                        groups.get(currentGroup).add(buildQuad(verts, uvs, indices, 0, 1, 2, 2));
                    } else if (indices.length > 4) {
                        // Fan-triangulate and emit quads
                        for (int i = 1; i + 1 < indices.length; i++) {
                            groups.get(currentGroup).add(buildQuad(verts, uvs, indices, 0, i, i + 1, i + 1));
                        }
                    }
                }
            }
        }
        return new LPPipeModel(groups);
    }

    private static Quad buildQuad(List<float[]> verts, List<float[]> uvs,
                                   int[][] indices, int i0, int i1, int i2, int i3) {
        float[] p0 = transform(verts.get(indices[i0][0] - 1));
        float[] p1 = transform(verts.get(indices[i1][0] - 1));
        float[] p2 = transform(verts.get(indices[i2][0] - 1));
        float[] p3 = transform(verts.get(indices[i3][0] - 1));

        float[] t0 = uvs.get(indices[i0][1] - 1);
        float[] t1 = uvs.get(indices[i1][1] - 1);
        float[] t2 = uvs.get(indices[i2][1] - 1);
        float[] t3 = uvs.get(indices[i3][1] - 1);

        float[] n = computeNormal(p0, p1, p2);

        return new Quad(
                p0[0], p0[1], p0[2], t0[0], flipV(t0[1]),
                p1[0], p1[1], p1[2], t1[0], flipV(t1[1]),
                p2[0], p2[1], p2[2], t2[0], flipV(t2[1]),
                p3[0], p3[1], p3[2], t3[0], flipV(t3[1]),
                n[0], n[1], n[2]);
    }

    // LP1: scale 1/100, then translate Z by +1.0
    private static float[] transform(float[] v) {
        return new float[]{ v[0] / 100f, v[1] / 100f, v[2] / 100f + 1.0f };
    }

    // OBJ V=0 is bottom-left; MC PNG V=0 is top-left — flip V to match MC convention
    private static float flipV(float v) {
        return 1.0f - v;
    }

    private static float[] computeNormal(float[] p0, float[] p1, float[] p2) {
        float ax = p1[0] - p0[0], ay = p1[1] - p0[1], az = p1[2] - p0[2];
        float bx = p2[0] - p0[0], by = p2[1] - p0[1], bz = p2[2] - p0[2];
        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6f) return new float[]{ 0, 1, 0 };
        return new float[]{ nx / len, ny / len, nz / len };
    }
}
