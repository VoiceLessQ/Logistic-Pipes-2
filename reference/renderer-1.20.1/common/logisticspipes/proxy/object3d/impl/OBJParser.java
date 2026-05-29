package logisticspipes.proxy.object3d.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal Wavefront OBJ parser sufficient for LP's pipe models.
 *
 * <p>Supports: {@code v}, {@code vt}, {@code vn}, {@code g}, {@code f} with
 * triangle (3 indices) or quad (4 indices) faces in the {@code v/vt/vn} form.
 * Triangles are expanded to degenerate quads by duplicating the last vertex.
 * Groups (from {@code g <name>}) become map keys; an empty leading group
 * encountered before any {@code g} directive is stored under {@code "default"}.</p>
 *
 * <p>All positions, UVs, and normals are 1-indexed in the OBJ format — this
 * parser normalizes them to 0-indexed internally.</p>
 */
public final class OBJParser {

	private OBJParser() {}

	/**
	 * Parse an OBJ stream and return a map from group name → list of quads.
	 * The input stream is closed on return.
	 */
	public static Map<String, List<LPQuadData>> parse(InputStream in) throws IOException {
		List<float[]> positions = new ArrayList<>();
		List<float[]> uvs = new ArrayList<>();
		List<float[]> normals = new ArrayList<>();
		Map<String, List<LPQuadData>> groups = new LinkedHashMap<>();
		String currentGroup = "default";

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) continue;
				String[] tok = line.split("\\s+");
				switch (tok[0]) {
					case "v":
						positions.add(new float[]{
							Float.parseFloat(tok[1]),
							Float.parseFloat(tok[2]),
							Float.parseFloat(tok[3])
						});
						break;
					case "vt":
						// OBJ V axis is bottom-up; Minecraft textures are top-down. Flip V.
						uvs.add(new float[]{
							Float.parseFloat(tok[1]),
							1.0f - Float.parseFloat(tok[2])
						});
						break;
					case "vn":
						normals.add(new float[]{
							Float.parseFloat(tok[1]),
							Float.parseFloat(tok[2]),
							Float.parseFloat(tok[3])
						});
						break;
					case "g":
						// LP's OBJ files put multiple group names on a single `g` line
						// (e.g. `g Mesh332 Side_Texture_Plate_Side3 Texture_Side_N`), and
						// LogisticsNewRenderPipe.loadModels() does substring lookups of the
						// form `key.contains(" " + grp + " ")` / `key.endsWith(" " + grp)`.
						// Store the entire rest of the line as the key so those lookups hit.
						currentGroup = line.length() > 2 ? line.substring(2).trim() : "default";
						if (currentGroup.isEmpty()) currentGroup = "default";
						groups.computeIfAbsent(currentGroup, k -> new ArrayList<>());
						break;
					case "f":
						parseFace(tok, positions, uvs, normals,
							groups.computeIfAbsent(currentGroup, k -> new ArrayList<>()));
						break;
					default:
						// s, mtllib, usemtl, o: ignored
						break;
				}
			}
		}
		return groups;
	}

	private static void parseFace(String[] tok, List<float[]> positions, List<float[]> uvs, List<float[]> normals, List<LPQuadData> out) {
		int n = tok.length - 1;
		if (n < 3 || n > 4) return; // only triangles or quads supported
		int[] pi = new int[4];
		int[] ti = new int[4];
		int[] ni = new int[4];
		for (int i = 0; i < n; i++) {
			String[] parts = tok[i + 1].split("/");
			pi[i] = Integer.parseInt(parts[0]) - 1;
			ti[i] = (parts.length > 1 && !parts[1].isEmpty()) ? Integer.parseInt(parts[1]) - 1 : -1;
			ni[i] = (parts.length > 2 && !parts[2].isEmpty()) ? Integer.parseInt(parts[2]) - 1 : -1;
		}
		if (n == 3) {
			// Duplicate the last vertex to form a degenerate quad.
			pi[3] = pi[2];
			ti[3] = ti[2];
			ni[3] = ni[2];
		}

		LPQuadData q = new LPQuadData();
		for (int i = 0; i < 4; i++) {
			float[] p = positions.get(pi[i]);
			q.x[i] = p[0];
			q.y[i] = p[1];
			q.z[i] = p[2];
			if (ti[i] >= 0 && ti[i] < uvs.size()) {
				float[] uv = uvs.get(ti[i]);
				q.u[i] = uv[0];
				q.v[i] = uv[1];
			}
			if (ni[i] >= 0 && ni[i] < normals.size()) {
				float[] nrm = normals.get(ni[i]);
				q.nx[i] = nrm[0];
				q.ny[i] = nrm[1];
				q.nz[i] = nrm[2];
			}
		}
		out.add(q);
	}
}
