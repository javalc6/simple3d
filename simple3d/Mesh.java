/*
License Information, 2025 Livio (javalc6)

Feel free to modify, re-use this software, please give appropriate
credit by referencing this Github repository.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

IMPORTANT NOTICE
Note that this software is freeware and it is not designed, licensed or
intended for use in mission critical, life support and military purposes.
The use of this software is at the risk of the user. 

DO NOT USE THIS SOFTWARE IF YOU DON'T AGREE WITH STATED CONDITIONS.
*/
package simple3d;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import json.*;
/**
 * Mesh is a collection of polygons. It is purely geometric data.
 * v1.0   12-12-2025: first release
 * v1.0.1 17-12-2025: new shape regularPolygon and new method extrudePolygonMesh to extrude polygons
 */
public class Mesh implements Dumpable {
	String id;
	final List<Polygon3D> polygons = new ArrayList<>();
	Vector3D[] vertices;

	public enum Shape {
		square,
		cube,
		pyramid,
		cone,
		cylinder,
		sphere,
		regularPolygon,
	}

	final static HashMap<Shape, Mesh> immutable_shapes = new HashMap<>();

	/**
     * Create a mesh from vertices and polygons
	 */
	public static Mesh createMesh(String id, Vector3D[] vertices, List<Polygon3D> polygons) {
		Mesh m = new Mesh(id, vertices); 
		m.polygons.addAll(polygons);
        for (Polygon3D polygon: m.polygons)
            polygon.mesh = m; // Polygon3D needs its mesh reference
		m.sanityCheck();//just in case
		return m;
	}

	/**
     * Create a mesh by extruding a mesh containing only one polygon
	 * The original polygon is extruded along its normal
	 * The extruded mesh is obtained using the flipped original polygon as a base and adding walls and a cap
	 */
	public static Mesh extrudePolygonMesh(String id, Mesh source, Double distance) {
		if (source.polygons.size() > 1) throw new IllegalArgumentException("Too many polygons, only one allowed");
		int n_source_vertices = source.vertices.length;
		Vector3D[] vertices = new Vector3D[n_source_vertices * 2];
		for (int i = 0; i < n_source_vertices; i++)
			vertices[i] = source.vertices[i].clone();
		
		ArrayList<Polygon3D> polygons = new ArrayList<>();
        Polygon3D polygon = source.polygons.get(0);
		Vector3D P0 = vertices[polygon.vertex_indexes[0]];
		Vector3D P1 = vertices[polygon.vertex_indexes[1]];
		Vector3D P2 = vertices[polygon.vertex_indexes[2]];

		// Edges E01 = P1 - P0 and E12 = P2 - P1
		Vector3D E01 = P1.subtract(P0, new Vector3D(0, 0, 0));
		Vector3D E12 = P2.subtract(P1, new Vector3D(0, 0, 0));
		// Normal multiplied by distance
		Vector3D N = E01.cross(E12).normalize().multiply(distance);

		// Add polygon displaced by distance as cap
		Integer[] vertex_indexes = new Integer[n_source_vertices];
		for (int i = 0; i < n_source_vertices; i++) {
			vertex_indexes[i] = polygon.vertex_indexes[i] + n_source_vertices;
			Vector3D vertex = source.vertices[i].clone();
			vertex.add(N);
			vertices[i + n_source_vertices] = vertex;
		}
		Polygon3D displaced = new Polygon3D(null, vertex_indexes);
		if (polygon.colorIndex != null)
			displaced.setColorIndex(polygon.colorIndex);
		polygons.add(displaced);

		// Add lateral rectangles as walls
		for (int i = 0; i < n_source_vertices; i++) {
			vertex_indexes = new Integer[4];
			vertex_indexes[0] = polygon.vertex_indexes[i];
			vertex_indexes[1] = polygon.vertex_indexes[(i + 1) % n_source_vertices];
			vertex_indexes[2] = displaced.vertex_indexes[(i + 1) % n_source_vertices];
			vertex_indexes[3] = displaced.vertex_indexes[i];
			Polygon3D poly = new Polygon3D(null, vertex_indexes);
			if (polygon.colorIndex != null)
				poly.setColorIndex(polygon.colorIndex);
			polygons.add(poly);
		}

		// Add flipped polygon
		vertex_indexes = new Integer[n_source_vertices];
		for (int i = 0; i < n_source_vertices; i++) {
			vertex_indexes[i] = polygon.vertex_indexes[n_source_vertices - i - 1];
		}
		Polygon3D poly = new Polygon3D(null, vertex_indexes);
		if (polygon.colorIndex != null)
			poly.setColorIndex(polygon.colorIndex);
		polygons.add(poly);

		return createMesh(id, vertices, polygons);
	}

	/**
     * Return a mesh as instance of shape using String format
	 */
	public static Mesh getShapeInstance(String shape) {
		return getShapeInstance(Shape.valueOf(shape), null);		
	}

	public static Mesh getShapeInstance(String shape, String shapeArguments) {
		try {
			return getShapeInstance(Shape.valueOf(shape), new JSONObject(shapeArguments));		
		} catch (JSONException e) {
			throw new IllegalArgumentException("Incorrect arguments", e);
		}
	}

	/**
     * Return a mesh as instance of shape using native format
	 */
	public static Mesh getShapeInstance(Shape shape, JSONObject shapeArguments) {
		if (shape.equals(Shape.regularPolygon))
			return createRegularPolygon(shapeArguments);
		if (immutable_shapes.isEmpty())	{
			immutable_shapes.put(Shape.cube, createCube());
			immutable_shapes.put(Shape.pyramid, createPyramid());
			immutable_shapes.put(Shape.sphere, createSphere(4));//N4 = N / 4
			immutable_shapes.put(Shape.cylinder, createCylinder(16));
			immutable_shapes.put(Shape.cone, createCone(16));
			immutable_shapes.put(Shape.square, createSquare());
		}
		return immutable_shapes.get(shape);
	}

//simple test to verify that all polygons in the mesh have same orientation
	public boolean checkCorrectness() {
		HashSet<Integer> segments = new HashSet<>();
		int n_vertices = vertices.length;
		for (Polygon3D polygon: polygons) {
			int prev = polygon.vertex_indexes[polygon.vertex_indexes.length - 1];//use last vertex as previous vertex of first one
			for (Integer vidx: polygon.vertex_indexes) {
				int segment = prev + vidx * n_vertices;
				if (segments.contains(segment))
					return false;
				segments.add(segment);
				prev = vidx;
			}
		}
		return true;
	}

//test to verify that the mesh is a closed surface without holes
	public boolean checkManifold() {
		HashSet<Integer> segments = new HashSet<>();
		int n_vertices = vertices.length;
		for (Polygon3D polygon: polygons) {
			int prev = polygon.vertex_indexes[polygon.vertex_indexes.length - 1];//use last vertex as previous vertex of first one
			for (Integer vidx: polygon.vertex_indexes) {
				int anti_segment = vidx + prev * n_vertices;
				if (segments.contains(anti_segment))
					segments.remove(anti_segment);
				else {
					int segment = prev + vidx * n_vertices;
					segments.add(segment);
				}
				prev = vidx;
			}
		}
		return segments.isEmpty();
	}

//get number of unconnected parts
	public int getNumberUnconnectedParts() {
		int n_vertices = vertices.length;
		if (n_vertices < 2)
			return 0;
		HashMap<Integer, Integer> groups = new HashMap<>();
		for (int i = 0; i < n_vertices; i++)
			groups.put(i, i);

		for (Polygon3D polygon: polygons) {
			int prev = polygon.vertex_indexes[polygon.vertex_indexes.length - 1];//use last vertex as previous vertex of first one
			for (Integer vidx: polygon.vertex_indexes) {
				final int group_prev = groups.get(prev);
				final int group_vidx = groups.get(vidx);
				if (group_prev < group_vidx) {
					if (vidx == group_vidx)
						groups.put(vidx, group_prev);
					else {
						final int prevf = prev;
						groups.replaceAll((key, value) -> value == group_vidx ? group_prev : value);
					}
				} else {
					if (prev == group_prev)
						groups.put(prev, group_vidx);
					else {
						groups.replaceAll((key, value) -> value == group_prev ? group_vidx : value);
					}
				}
				prev = vidx;
			}
		}
		int n_groups = 0;
		int prev = groups.get(0);
		for (int i = 1; i < n_vertices; i++) {
			int group = groups.get(i);
			if (group != prev)
				n_groups++;
			prev = group;
		}
		return n_groups;
	}

	private final static double EPSILON = 1e-5; // Tolerance for floating point comparisons
	protected Mesh(String id, Vector3D[] vertices) {
		this.id = id;
		this.vertices = vertices;
	}
	protected Mesh(String id, int n_vertices) {
		vertices = new Vector3D[n_vertices];
		this.id = id;
	}
	private void sanityCheck() {
		for (Polygon3D polygon: polygons) {
			if (polygon.vertex_indexes.length < 3)
				throw new IllegalArgumentException("Incorrect polygon with only " + polygon.vertex_indexes.length + " vertices found in mesh " + id);
			int prev = polygon.vertex_indexes[polygon.vertex_indexes.length - 1];//use last vertex as previous vertex of first one
			for (Integer vidx: polygon.vertex_indexes) {
				if (vidx >= vertices.length)
					throw new IllegalArgumentException("Incorrect polygon with too high vertex index " + vidx + " found in mesh " + id);
				Vector3D difference = vertices[prev].subtract(vertices[vidx], new Vector3D(0, 0, 0));
				double distance = difference.squaredMagnitude();
				if (distance < EPSILON) throw new IllegalArgumentException("Incorrect polygon with too near consecutive vertices found in mesh " + id);
				prev = vidx;
			}

			Vector3D pivot = null;
			Vector3D normal = null;
			int n = polygon.vertex_indexes.length;
			for (int i = 0; i < n; i++) {
				Vector3D P0 = vertices[polygon.vertex_indexes[i]];
				Vector3D P1 = vertices[polygon.vertex_indexes[(i + 1) % n]];
				Vector3D P2 = vertices[polygon.vertex_indexes[(i + 2) % n]];

				// Edges E01 = P1 - P0 and E12 = P2 - P1
				Vector3D E01 = P1.subtract(P0, new Vector3D(0, 0, 0));
				Vector3D E12 = P2.subtract(P1, new Vector3D(0, 0, 0));

				// Calculate the potential normal N = E01 x E12
				Vector3D N = E01.cross(E12);

				// Check if the magnitude of the cross product is non-zero (i.e., not collinear)
				if (N.squaredMagnitude() > EPSILON) {
					// Found three non-collinear points. This defines our reference plane.
					pivot = P0;
					normal = N;
					break;
				}
			}
			if (normal == null)
				throw new IllegalArgumentException("Degenerate polygon with only collinear vertices, found in mesh " + id);
			for (int i = 0; i < n; i++) {
				// Vector from the reference point (pivot) to the current point (P).
				Vector3D Pivot_to_P = pivot.subtract(vertices[polygon.vertex_indexes[i]], new Vector3D(0, 0, 0));

				// The dot product of this vector and the normal must be zero (within EPSILON).
				if (Math.abs(Pivot_to_P.dot(normal)) > EPSILON)
					throw new IllegalArgumentException("Not planar polygon found in mesh " + id);
			}
		}
	}

	private static Mesh createPyramid() {
		Mesh m = new Mesh("native:pyramid", 5); 

		// Base vertices (on y=0)
		m.vertices[0] = new Vector3D(-0.5, 0, -0.5);
		m.vertices[1] = new Vector3D(-0.5, 0, 0.5);
		m.vertices[2] = new Vector3D(0.5, 0, 0.5);
		m.vertices[3] = new Vector3D(0.5, 0, -0.5);
		// Apex vertex
		m.vertices[4] = new Vector3D(0, 1, 0);

		// Base (v0, v3, v2, v1)
		m.polygons.add(new Polygon3D(m, 0, 3, 2, 1)); 

		// Front face (v1, v2, vA)
		m.polygons.add(new Polygon3D(m, 1, 2, 4));
		// Right face (v2, v3, vA)
		m.polygons.add(new Polygon3D(m, 2, 3, 4));
		// Back face (v3, v0, vA)
		m.polygons.add(new Polygon3D(m, 3, 0, 4));
		// Left face (v0, v1, vA)
		m.polygons.add(new Polygon3D(m, 0, 1, 4));

		m.sanityCheck();//just in case
		return m;
	}

	private static Mesh createCube() {
		Mesh m = new Mesh("native:cube", 8);

		// Vertices
		m.vertices[0] = new Vector3D(-0.5, 0, -0.5);
		m.vertices[1] = new Vector3D(-0.5, 0, 0.5);
		m.vertices[2] = new Vector3D(0.5, 0, 0.5);
		m.vertices[3] = new Vector3D(0.5, 0, -0.5);
		m.vertices[4] = new Vector3D(-0.5, 1, -0.5);
		m.vertices[5] = new Vector3D(-0.5, 1, 0.5);
		m.vertices[6] = new Vector3D(0.5, 1, 0.5);
		m.vertices[7] = new Vector3D(0.5, 1, -0.5);

		m.polygons.add(new Polygon3D(m, 3, 2, 1, 0)); // Square
		m.polygons.add(new Polygon3D(m, 4, 5, 6, 7)); // Square
		m.polygons.add(new Polygon3D(m, 0, 1, 5, 4)); // Square
		m.polygons.add(new Polygon3D(m, 1, 2, 6, 5)); // Square
		m.polygons.add(new Polygon3D(m, 2, 3, 7, 6)); // Square
		m.polygons.add(new Polygon3D(m, 0, 4, 7, 3)); // Square

		m.sanityCheck();//just in case
		return m;
	}

	private static Mesh createCone(int N) {
		if (N < 4) throw new IllegalArgumentException("N shall be greater than 3");
		final double HEIGHT = 1;
		final double RADIUS = 0.5;
		Mesh m = new Mesh("native:cone", N + 1); 

		double angleIncrement = 2.0 * Math.PI / N;
		//vertices
		for (int i = 0; i < N; i++) {
			double angle = i * angleIncrement;
			double x = RADIUS * Math.cos(angle);
			double z = RADIUS * Math.sin(angle);
			m.vertices[i] = new Vector3D(x, 0.0, z);
		}
		m.vertices[N] = new Vector3D(0.0, HEIGHT, 0.0);

		//bottom side polygon
		Integer[] bottom = new Integer[N];
		for (int i = 0; i < N; i++)
			bottom[i] = i;
		m.polygons.add(new Polygon3D(m, bottom));

		//lateral side polygons
		for (int i = 0; i < N; i++) {
			int v0 = i;
			int v2 = (i + 1) % N;
			int v1 = N;
			Integer[] triangle = new Integer[]{v0, v1, v2};
			m.polygons.add(new Polygon3D(m, triangle));
		}
		m.sanityCheck();//just in case
		return m;
	}

	private static Mesh createCylinder(int N) {
		if (N < 4) throw new IllegalArgumentException("N shall be greater than 3");
		final double HEIGHT = 1;
		final double RADIUS = 0.5;
		Mesh m = new Mesh("native:cylinder", 2 * N); 

		double angleIncrement = 2.0 * Math.PI / N;
		//vertices
		for (int i = 0; i < N; i++) {
			double angle = i * angleIncrement;
			double x = RADIUS * Math.cos(angle);
			double z = RADIUS * Math.sin(angle);
			m.vertices[2 * i] = new Vector3D(x, 0.0, z);
			m.vertices[2 * i + 1] = new Vector3D(x, HEIGHT, z);
		}

		//bottom side polygon
		Integer[] bottom = new Integer[N];
		for (int i = 0; i < N; i++)
			bottom[i] = 2 * i;
		m.polygons.add(new Polygon3D(m, bottom));

		//top side polygon
		Integer[] top = new Integer[N];
		for (int i = 0; i < N; i++)
			top[i] = 2 * (N - i) - 1;
		m.polygons.add(new Polygon3D(m, top));

		int N2 = 2 * N;
		//lateral side polygons
		for (int i = 0; i < N; i++) {
			int v0 = 2 * i;
			int v1 = v0 + 1;
			int v2 = (2 * i + 3) % N2;
			int v3 = (2 * i + 2) % N2;				
			Integer[] quad = new Integer[]{v0, v1, v2, v3};
			m.polygons.add(new Polygon3D(m, quad));
		}
		m.sanityCheck();//just in case
		return m;
	}

	private static Mesh createSphere(int N4) {//N4 = N / 4
		if (N4 < 1) throw new IllegalArgumentException("N4 shall be greater than 0");
		final double RADIUS = 0.5;
		int N = N4 * 4;
		Mesh m = new Mesh("native:sphere", N * N / 2 - N + 2); 

		double angleIncrement = 2.0 * Math.PI / N;
		//vertices
		int vertex_idx = 0;
//upper pole
		m.vertices[vertex_idx++] = new Vector3D(0, 2 * RADIUS, 0);
//from upper latitudes to lower altitudes
		for (int k = N4 - 1; k > - N4; k--) {
			double h = RADIUS * Math.sin(k * angleIncrement);
			double r = RADIUS * Math.cos(k * angleIncrement);
			int baseline_idx = vertex_idx;
			for (int i = 0; i < N; i++) {
				double angle = i * angleIncrement;
				double x = r * Math.cos(angle);
				double z = r * Math.sin(angle);
				m.vertices[vertex_idx++] = new Vector3D(x, h + RADIUS, z);
			}
			if (k == N4 - 1) {
				for (int i = 0; i < N; i++) {
					Integer[] triangle = new Integer[3];
					triangle[0] = 0;//upper pole
					triangle[1] = 1 + (i + 1) % N;
					triangle[2] = 1 + i;
					m.polygons.add(new Polygon3D(m, triangle));
				}
			} else {
				for (int i = 0; i < N; i++) {
					Integer[] quad = new Integer[4];
					quad[0] = baseline_idx - N + i;
					quad[1] = baseline_idx - N + (i + 1) % N;
					quad[2] = baseline_idx + (i + 1) % N;
					quad[3] = baseline_idx + i;
					m.polygons.add(new Polygon3D(m, quad));
				}
			}
		}
//lower pole
		m.vertices[vertex_idx++] = new Vector3D(0, 0, 0);
		for (int i = 0; i < N; i++) {
			Integer[] triangle = new Integer[3];
			triangle[0] = m.vertices.length - 1;//lower pole
			triangle[1] = m.vertices.length - (2 + (i + 1) % N);
			triangle[2] = m.vertices.length - (2 + i);
			m.polygons.add(new Polygon3D(m, triangle));
		}
		m.sanityCheck();//just in case
		return m;
	}

	private static Mesh createSquare() {
		Mesh m = new Mesh("native:square", 4);

		// Vertices
		m.vertices[0] = new Vector3D(-0.5, 0, -0.5);
		m.vertices[1] = new Vector3D(-0.5, 0, 0.5);
		m.vertices[2] = new Vector3D(0.5, 0, 0.5);
		m.vertices[3] = new Vector3D(0.5, 0, -0.5);

		//Polygons
		m.polygons.add(new Polygon3D(m, 0, 1, 2, 3));

		m.sanityCheck();//just in case
		return m;
	}

	private static Mesh createRegularPolygon(JSONObject shapeArguments) {
		if (shapeArguments == null) throw new IllegalArgumentException("shapeArguments is required");
		JSONValue _N = shapeArguments.get("N");
		if (_N == null) throw new IllegalArgumentException("shapeArguments shall include N");
		int N = ((BigDecimal) _N.toJava()).intValueExact();
		if (N < 3) throw new IllegalArgumentException("N shall be greater or equal to 3");

		Mesh m = new Mesh("native:regularPolygon", N);
		Integer[] vertexIndexes = new Integer[N];

		double angle0 = Math.PI * (1.0 / N - 0.5);
		double angle_step = -2.0 * Math.PI / N;
		// radius is calculated in order to have unitary side
		double radius = 0.5 / Math.sin(Math.PI / N);
		// Vertices
		for (int i = 0; i < N; i++) {
			m.vertices[i] = new Vector3D(radius * Math.cos(angle0 + angle_step * i), 0, radius * Math.sin(angle0 + angle_step * i));
			vertexIndexes[i] = i;
		}

		//Polygons
		m.polygons.add(new Polygon3D(m, vertexIndexes));

		m.sanityCheck();//just in case
		return m;
	}

	protected Mesh() {}
	public void load(JSONValue data) {
		JSONObject mesh = (JSONObject) data;
		id = (String) mesh.get("id").toJava();
		
		JSONArray vert = (JSONArray) mesh.get("vertices");
		int n = vert.size();
		vertices = new Vector3D[n];
		for (int i = 0; i < n; i++) {
			Vector3D vet = new Vector3D();
			vet.load(vert.get(i));
			vertices[i] = vet;
		}
		JSONArray polys = (JSONArray) mesh.get("polygons");
		n = polys.size();
		polygons.clear();
		for (int i = 0; i < n; i++) {
			Polygon3D poly = new Polygon3D();
			poly.load(polys.get(i));
			poly.mesh = this; // Polygon3D needs its mesh reference
			polygons.add(poly);
		}
		sanityCheck();//just in case
	}

	public JSONValue save() {
		LinkedHashMap<String, Object> mesh = new LinkedHashMap<>();
		mesh.put("type", "mesh");
		mesh.put("id", id);
		
		JSONArray vert = new JSONArray();
        for (Vector3D vertex : vertices) {
            vert.add(vertex.save());
        }
		mesh.put("vertices", vert);
		JSONArray polys = new JSONArray();
		for (Polygon3D polygon: polygons) {
			polys.add(polygon.save());
		}
		mesh.put("polygons", polys);

		return new JSONObject(mesh);
	}
}