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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import json.*;

/**
 * Engine3D
 * This class implements a simple and fast software 3D engine.
 * It features perspective projection, basic flat shading, only one light. Requires aspect ratio 2:1
 * Unsupported: reflections, textures, shadows, multiple lights, any other advanced 3d feature. 
 * BSP algorithm is not optimal in case of moving objects.
 * CameraPitch is not implemented. Far objects are always processed like any objects instead of being skipped.
 *
 * v1.0 12-12-2025: first release
 * v1.0.1 17-12-2025: added shapeArguments
 */

public class Engine3D {
	public final static String format = "simple3D.1";

	public enum Direction {
		UP,
		DOWN,
		LEFT,
		RIGHT;
	}
	private final boolean print_statistics;

	private final ArrayList<Node> sceneNodes = new ArrayList<>(); //Objects
	private final HashMap<String, Mesh> meshes = new HashMap<>(); //Meshes
	private Light3D light;// Light Source (used for flat shading)
    private Vector3D cameraPos;// Camera Position or POV

	private BSPNode sceneBspTree; //Binary Space Partitioning (BSP) Tree

	// Pre-allocated objects for performance optimization
	private final Vector3D lookDir = new Vector3D(0, 0, 1);
	private final Vector3D target = new Vector3D(0, 0, 0);
	private final Vector3D up = new Vector3D(0, 1, 0);

	private final Matrix4x4 matProj = new Matrix4x4();
	private final Matrix4x4 matCameraRot = new Matrix4x4();
	private final Matrix4x4 matView = new Matrix4x4();
	private final Matrix4x4 matViewProj = new Matrix4x4();

	private final Vector3D cameraRay = new Vector3D(0, 0, 0);
	private final Vector3D polyCenter = new Vector3D(0, 0, 0);
	private final Vector3D lightDir = new Vector3D(0, 0, 0);

	private final static double Z_NEAR = 0.1;

	private String filename = "notitle.json.gz";

	public Engine3D(boolean print_statistics) {
		this.print_statistics = print_statistics;
	}

	public void setLight(Light3D light) {
		this.light = light;
	}

	public void setCameraPos(Vector3D cameraPos) {
		this.cameraPos = cameraPos;
	}

	public Vector3D getCameraPos() {
		return cameraPos;
	}

	public ArrayList<Node> getSceneNodes() {
		return sceneNodes;
	}

	//return false if mesh id already present
	public boolean addMesh(Mesh mesh) {
		String id = mesh.id;
		if (meshes.get(id) == null)	{
			meshes.put(id, mesh);
			return true;
		} else return false;
	}

	/**
     * Performs preparation of scene
	 */
	public void setupScene(double FOV, double ASPECT_RATIO) {
		// Pre-calculate projection matrix once, as FOV and Aspect Ratio are constant
		Matrix4x4.createProjection(FOV, ASPECT_RATIO, matProj);

		if (print_statistics)
			System.out.println(getInfo(true));
// Consolidate all polygons into one list to build BSP tree
		List<Polyface3D> allPolygons = new ArrayList<>();
		for (Node node : sceneNodes) {
			Mesh mesh = node.meshID == null ? Mesh.getShapeInstance(node.shape, node.shapeArguments) : meshes.get(node.meshID);
			// Need to transform polygons to *World Space* before building the tree
			for (Polygon3D poly : mesh.polygons) {
				Vector3D[] vertices = new Vector3D[poly.vertex_indexes.length];
				for (int i = 0; i < poly.vertex_indexes.length; i++)
					vertices[i] = mesh.vertices[poly.vertex_indexes[i]].clone();
				node.worldMatrix.transformInPlace(vertices);

				Color color = Color.GRAY;//default color
				if (poly.colorIndex == null)
					color = node.color;
				else if (node.colorList != null && poly.colorIndex < node.colorList.length) {
					color = node.colorList[poly.colorIndex];
				}
				Polyface3D worldPoly = new Polyface3D(color, vertices); // Base color comes from the original node polygon
				if (!worldPoly.isConvex())
					System.out.println("Warning: found not convex polygon in mesh " + poly.mesh.id);
				allPolygons.add(worldPoly);
			}
		}
		long t0 = System.nanoTime();
		sceneBspTree = BSPNode.build(allPolygons);
		if (print_statistics) {
			float delta_ms = (System.nanoTime() - t0) / 1000000f;
			System.out.println("BSP building time: " + delta_ms + " ms");
			System.out.println("Size of BSP tree: " + sceneBspTree.size() + ", deepness: " + sceneBspTree.deepness());
		}
	}

	/**
     * Performs BSP Traversal to render the current scene
	 */
	public void render3D(double cameraYaw, int width, int height, BiConsumer<Vector3D[], Color> render) {
		// View-Projection Transform

		// Camera Rotation Matrix (reused object)
		matCameraRot.setIdentity();
		matCameraRot.applyRotationY(cameraYaw);

		// Calculate Look Direction (reused objects)
		lookDir.set(0, 0, 1);
		matCameraRot.multiply(lookDir, lookDir);
		lookDir.normalize(); // In-place normalize
		target.set(cameraPos).add(lookDir); // In-place add

		// View Matrix (reused object)
		Matrix4x4.pointAt(cameraPos, target, up, matView);
		matView.quickInverse(matView); // In-place inverse

		// View-Projection Matrix (reused object)
		matView.multiply(matProj, matViewProj); // matView * matProj -> matViewProj


		// --- Rendering Loop using BSP Traversal ---
		if (sceneBspTree != null) {
			// 1. Traverse the World-Space BSP tree to get a back-to-front list of *World* polygons.
			List<Polyface3D> worldPolygons = new ArrayList<>();
			sceneBspTree.traverse(cameraPos, worldPolygons);

			// 2. Process, Project, and Shade each polygon in the correct order.
			for (Polyface3D worldPoly : worldPolygons) {

				// Backface Culling (Check against the camera, as the poly is in world space)
				Vector3D v1 = worldPoly.vertices[0];
				v1.subtract(cameraPos, cameraRay); // cameraRay = v1 - cameraPos (In-place)
				if (worldPoly.normal.dot(cameraRay) >= 0.0) {
					continue;
				}
				Vector3D v2 = worldPoly.vertices[1];
				Vector3D v3 = worldPoly.vertices[2];

				// Illumination (Flat Shading)
				// polyCenter = (v1 + v2 + v3) * (1/3) (Reuse polyCenter object)
				polyCenter.set(v1).add(v2).add(v3).multiply(1.0/3.0); // In-place operations
				// lightDir = lightPos - polyCenter
				light.lightPos.subtract(polyCenter, lightDir).normalize(); // In-place subtract and normalize

				double dp = Math.max(0.1, worldPoly.normal.dot(lightDir)); // Max between 0.1 (ambient) and the dot product

				Color lightColor = light.color;
				Color baseColor = worldPoly.color;
				int r = (int) (baseColor.getRed() * lightColor.getRed() * dp / 255);
				int g = (int) (baseColor.getGreen() * lightColor.getGreen() * dp / 255);
				int b = (int) (baseColor.getBlue() * lightColor.getBlue() * dp / 255);

				// Transform & clip
				Vector3D[] projectedVertices = transformAndScreenMap(worldPoly, matViewProj, width, height);

				// Apply the single-color flat shade to all resulting polygons
				if (projectedVertices != null) {
					render.accept(projectedVertices, new Color(r, g, b));
				}
			}
		}
	}

	/**
     * Updates camera position, return true if camera position is changed
	 */
	public boolean updateCamera(Direction direction, boolean shift, double cameraYaw, double moveSpeed) {
		Vector3D forwardv = new Vector3D(0, 0, 1);
		Vector3D rightv = new Vector3D(1, 0, 0); // Side vector

		// Create the rotation matrix from camera yaw (horizontal rotation)
		Matrix4x4 matCameraRot = Matrix4x4.createRotationY(cameraYaw); // This still creates a new matrix, but only on key press
		matCameraRot.multiply(forwardv, forwardv).normalize(); // In-place rotation and normalize
		matCameraRot.multiply(rightv, rightv).normalize(); // In-place rotation and normalize

		if (direction == Direction.UP) {
			if (shift)
				cameraPos.y += moveSpeed;
			else cameraPos.add(forwardv.multiply(moveSpeed)); // In-place add
			return true;
		} else if (direction == Direction.DOWN) {
			if (shift)
				cameraPos.y -= moveSpeed;
			else cameraPos.subtract(forwardv.multiply(moveSpeed)); // In-place subtract
			return true;
		} else if (direction == Direction.LEFT && !shift) {
			cameraPos.subtract(rightv.multiply(moveSpeed)); // In-place subtract
			return true;
		} else if (direction == Direction.RIGHT && !shift) {
			cameraPos.add(rightv.multiply(moveSpeed)); // In-place add
			return true;
		}
		return false;
	}

	/**
     * Import json compressed scene3D file, returns optional userdata JSONObject
	 */
	public JSONObject importFile(String filename) throws JSONException, IOException {
        try (
            FileInputStream fis = new FileInputStream(filename);
            GZIPInputStream gis = new GZIPInputStream(fis);
            InputStreamReader isr = new InputStreamReader(gis, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr)) {

            String fileContent = br.lines().collect(Collectors.joining("\n"));
			JSONValue content = JSONValue.parse(fileContent);
			JSONObject world = (JSONObject) content;
			JSONValue _type = world.get("type");
			if (_type == null)
				throw new IOException("missing element 'type'");
			String type = (String) _type.toJava();
			if (!type.equals("world"))
				throw new IOException("unsupported type: " + type);

			JSONValue _format = world.get("format");
			if ((_format == null) || !((String) _format.toJava()).equals("simple3D.1"))
				System.out.println("Warning: unsupported format");

			JSONObject _light = (JSONObject) world.get("light");
			Color color = new Color(); color.load(_light.get("color"));
			Vector3D vet = new Vector3D(); vet.load(_light.get("position"));			
			light = new Light3D(color, vet.x, vet.y, vet.z);

			JSONObject _camera = (JSONObject) world.get("camera");
			cameraPos = new Vector3D(); cameraPos.load(_camera.get("position"));	
			
			JSONArray nodes = (JSONArray) world.get("nodes");
			int n_nodes = nodes.size();
			sceneNodes.clear(); 
			for (int i = 0; i < n_nodes; i++) {
				Node node = new Node();
				node.load(nodes.get(i));
				sceneNodes.add(node);
			}

			meshes.clear(); 
			JSONArray _meshes = (JSONArray) world.get("meshes");
			if (_meshes != null && _meshes.size() > 0) {
				int n_meshes = _meshes.size();
				for (int i = 0; i < n_meshes; i++) {
					Mesh mesh = new Mesh();
					mesh.load(_meshes.get(i));
					meshes.put(mesh.id, mesh);
				}
			}
			this.filename = filename;
			return (JSONObject) world.get("userdata");
        }
	}

	public String getFileName() {
		return filename;
	}

	/**
     * Export to json compressed scene3D file
	 */
	public void exportFile(String filename) throws IOException {
		exportFile(filename, null);
	}

	/**
     * Export to json compressed scene3D file, including optional userdata JSONObject
	 */
	public void exportFile(String filename, JSONObject userdata) throws IOException {
		LinkedHashMap<String, Object> world = new LinkedHashMap<>();
		world.put("type", "world");
		world.put("format", "simple3D.1");
		LinkedHashMap<String, Object> _light = new LinkedHashMap<>();
		_light.put("color", light.color.toString());
		_light.put("position", light.lightPos.save());
		world.put("light", new JSONObject(_light));
		LinkedHashMap<String, Object> _camera = new LinkedHashMap<>();
		_camera.put("position", cameraPos.save());
		world.put("camera", new JSONObject(_camera));
		ArrayList<JSONValue> nodes = new ArrayList<>();
		for (Node node : sceneNodes) {
			nodes.add(node.save());
		}
		world.put("nodes", new JSONArray(nodes));
		ArrayList<JSONValue> _meshes = new ArrayList<>();
		for (Map.Entry<String, Mesh> entry : meshes.entrySet()) {
//			String key = entry.getKey();
			Mesh mesh = entry.getValue();
			_meshes.add(mesh.save());

		}
		if (_meshes != null && _meshes.size() > 0)
			world.put("meshes", new JSONArray(_meshes));

		if (userdata != null)//transparent user data
			world.put("userdata", userdata);

//Write <world> to file with filename
		JSONValue out = new JSONObject(world);
		try (FileOutputStream fos = new FileOutputStream(filename); GZIPOutputStream gos = new GZIPOutputStream(fos)) {
            gos.write(out.toString().getBytes(StandardCharsets.UTF_8));
            gos.finish();
        }
		this.filename = filename;
	}

	/**
	 * Applies the View-Projection transform and scales vertices to screen coordinates.
	 * The input worldPoly is copied and transformed to a new projectedPoly,
	 * which is necessary since the resulting vertices are in screen space
	 * and must be kept for the 'polygonsToDraw' list.
	 */
	protected Vector3D[] transformAndScreenMap(Polyface3D worldPoly, Matrix4x4 matViewProj, int width, int height) {

		Vector3D[] vertices = new Vector3D[worldPoly.vertices.length];
		for (int i = 0; i < vertices.length; i++)
			vertices[i] = worldPoly.vertices[i].clone();
		// 1. Transform directly by the combined View-Projection matrix

		// Clip-space coordinates (before perspective divide)
		for (int i = 0; i < vertices.length; i++)
			matViewProj.multiply(vertices[i], vertices[i]); // In-place vector-matrix multiplication

		// 2. Near-Plane Clipping (w = Z_NEAR)
		Vector3D[] clippedVertices = clipPolygonAgainstPlane(vertices);

		// If clipping resulted in no polygons, return null
		if (clippedVertices == null)
			return null;

		// 3. Perspective Divide and Screen Mapping
		boolean valid = true;
		for (Vector3D v : clippedVertices) {
			// This check should now only happen if Z_NEAR was missed in clipping, or for the new vertices
			// that should now be exactly on w = Z_NEAR.
			if (v.w <= 0) { // Check for w=0 (division by zero) or w<0 (behind the camera)
				valid = false;
				break;
			}

			// Normalize by W (Perspective Divide)
			v.x /= v.w;
			v.y /= v.w;
			v.z /= v.w;

			// X/Y are now in NDC space (-1 to +1). Scale to screen size.
			v.x = (v.x + 1) * 0.5 * width;
			// Note the y-flip for screen coordinates (y-down in AWT/Swing)
			v.y = (1.0 - v.y) * 0.5 * height;
		}
		
		if (valid)
			return clippedVertices;

		return null;
	}

// --- CLIPPING UTILITIES ---

	/**
	 * Clips a Polyface3D against the near plane (w >= Z_NEAR).
	 * Returns a list of new polygons (0, 1, or 2) after clipping.
	 * This is a simplified Sutherland-Hodgman for a single plane.
	 */
	protected Vector3D[] clipPolygonAgainstPlane(Vector3D[] inputVertices) {
		List<Vector3D> outputVertices = new ArrayList<>();
		
		// Loop over all edges of the input polygon
		for (int i = 0; i < inputVertices.length; i++) {
			Vector3D v1 = inputVertices[i];
			Vector3D v2 = inputVertices[(i + 1) % inputVertices.length];
			
			// Inside means in front of the near plane (w >= Z_NEAR)
			boolean v1Inside = v1.w >= Z_NEAR;
			boolean v2Inside = v2.w >= Z_NEAR;

			if (v1Inside && v2Inside) {
				// Case 1: Both vertices are inside (keep v2)
				outputVertices.add(v2);
			} else if (v1Inside && !v2Inside) {
				// Case 2: Going from inside to outside (keep intersection)
				Vector3D intersection = intersectNearPlane(v1, v2);
				if (intersection != null) {
					outputVertices.add(intersection);
				}
			} else if (!v1Inside && v2Inside) {
				// Case 3: Going from outside to inside (keep intersection and v2)
				Vector3D intersection = intersectNearPlane(v1, v2);
				if (intersection != null) {
					outputVertices.add(intersection);
				}
				outputVertices.add(v2);
			}
			// Case 4: Both outside (keep nothing)
		}

		if (outputVertices.size() < 3)
			return null; // Clipped away completely
		
		//return clipped vertices
		return outputVertices.toArray(new Vector3D[0]);
	}

	private final static double EPSILON = 1e-5;

	/**
	 * Calculates the intersection point of a line segment (v1, v2) with the near plane (w=Z_NEAR).
	 * This uses linear interpolation in homogeneous space (clip space).
	 */
	protected static Vector3D intersectNearPlane(Vector3D v1, Vector3D v2) {
		double t; // Intersection parameter
		
		// We clip against w = Z_NEAR.
		double dw = v2.w - v1.w;
		// Check if the difference is non-zero to avoid division by zero
		if (Math.abs(dw) > EPSILON) { 
			t = (Z_NEAR - v1.w) / dw;
		} else {
			// Line is almost parallel to the clip plane (or both points are on it)
			return null; 
		}

		// Clamp t for safety, although it should be in (0, 1) when clipping against Z_NEAR
		t = Math.max(0.0, Math.min(1.0, t));

		// Linear interpolation for all components (x, y, z, w)
		double x = v1.x + t * (v2.x - v1.x);
		double y = v1.y + t * (v2.y - v1.y);
		double z = v1.z + t * (v2.z - v1.z);

		return new Vector3D(x, y, z, Z_NEAR);
	}

	private static void printOnce(String message, HashSet<String> messages, StringBuilder sb) {
		if (!messages.contains(message)) {
			sb.append(message).append('\n');
			messages.add(message);
		}
	}

	/**
     * Get information about current scene
	 */
	public String getInfo(boolean fullchecks) {
		int n_nodes = sceneNodes.size();
		int n_polygons = 0;
		int n_vertices = 0;
		int n_unconnectedParts = 0;
		StringBuilder sb = new StringBuilder(64);
		HashSet<String> messages = new HashSet<>();
		for (Node node : sceneNodes) {
			Mesh mesh = meshes.get(node.meshID);
			if (mesh == null) {
				if (node.shape == null) {
					printOnce("Error: unknown mesh id " + node.meshID, messages, sb);
					continue;
				} else mesh = Mesh.getShapeInstance(node.shape, node.shapeArguments);
			}
			n_polygons += mesh.polygons.size();
			n_vertices += mesh.vertices.length;
			int n_unconnected = mesh.getNumberUnconnectedParts();
			n_unconnectedParts += n_unconnected;
			if (fullchecks) {		
				if (n_unconnected > 0)
					printOnce("Warning: unconnected part found in mesh " + mesh.id, messages, sb);

				if (!mesh.checkCorrectness())
					printOnce("Error found in mesh " + mesh.id, messages, sb);

				if (!mesh.checkManifold())
					printOnce("Warning: mesh " + mesh.id + " is not manifold", messages, sb);			
			}
		}
		sb.append("Number of nodes: ").append(n_nodes).append('\n');
		sb.append("Number of user defined meshes: ").append(meshes.size()).append('\n');
		sb.append("Number of polygons: ").append(n_polygons).append('\n');
		sb.append("Number of vertices: ").append(n_vertices).append('\n');
		if (n_unconnectedParts > 0)
			sb.append("Number of unconnected parts: ").append(n_unconnectedParts);
		return sb.toString();
	}

}