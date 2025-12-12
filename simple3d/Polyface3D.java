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

import java.util.ArrayList;
import java.util.List;

/**
 * 3D polygon for internal processing. All vertices shall be coplanar.
 * v1.0 12-12-2025: first release
 */
public class Polyface3D {
	final Vector3D[] vertices;//vertices shall be considered as immutable, in case value is changed, normal shall be re-computed again
	Color color;
	final Vector3D normal = new Vector3D();//value shall be updated in case of vertices are changed

	public Polyface3D(Color color, Vector3D... vertices) {
		this.vertices = vertices;
		this.color = color;
		calculateNormal(normal);
	}

	// Constants for classification relative to the splitter
	private final static double EPSILON = 1e-5; // Tolerance for floating point comparisons
	public final static int COPLANAR = 0;
	public final static int FRONT = 1;
	public final static int BACK = 2;
	public final static int SPANNING = 3;

	/**
	 * Classifies the polygon relative to the plane of the splitter.
	 * @param splitter A polygon representing the plane (first three vertices define the plane).
	 * @return COPLANAR, FRONT, BACK, or SPANNING.
	 */
	public int classify(Polyface3D splitter) {
		Vector3D splitterPoint = splitter.vertices[0];

		int numFront = 0;
		int numBack = 0;

		for (Vector3D v : this.vertices) {
			// Dot product of (Point - PlanePoint) and PlaneNormal
			// Gives the distance from the point to the plane.
			Vector3D ray = v.subtract(splitterPoint, new Vector3D(0, 0, 0)); // ray = v - splitterPoint
			double distance = ray.dot(splitter.normal);

			if (distance > EPSILON) {
				numFront++;
			} else if (distance < -EPSILON) {
				numBack++;
			}
		}

		if (numFront > 0 && numBack == 0) return FRONT;
		if (numFront == 0 && numBack > 0) return BACK;
		if (numFront == 0 && numBack == 0) return COPLANAR;
		return SPANNING;
	}

	/**
	 * Calculates the polygon's normal vector. Assumes vertices are V0, V1, V2.
	 */
	public void calculateNormal(Vector3D resultNormal) {
		double x1 = vertices[1].x - vertices[0].x;
		double y1 = vertices[1].y - vertices[0].y;
		double z1 = vertices[1].z - vertices[0].z;

		double x2 = vertices[2].x - vertices[0].x;
		double y2 = vertices[2].y - vertices[0].y;
		double z2 = vertices[2].z - vertices[0].z;

		resultNormal.x = y1 * z2 - z1 * y2;
		resultNormal.y = z1 * x2 - x1 * z2;
		resultNormal.z = x1 * y2 - y1 * x2;
		resultNormal.w = 1.0;

		resultNormal.normalize();
	}

   public boolean isConvex() {
		if (vertices.length < 3) return true;

		Double initialSign = null; // Store the sign of the first cross product's dot product

		for (int i = 0; i < vertices.length; i++) {
			// Get the three vertices that form the internal angle at v_i+1
			Vector3D v_prev = vertices[i];
			Vector3D v_curr = vertices[(i + 1) % vertices.length];
			Vector3D v_next = vertices[(i + 2) % vertices.length];

			// Edge vector 1: E1 = v_curr - v_prev
			Vector3D e1 = v_curr.subtract(v_prev, new Vector3D(0, 0, 0));
			// Edge vector 2: E2 = v_next - v_curr
			Vector3D e2 = v_next.subtract(v_curr, new Vector3D(0, 0, 0));

			// Cross product: CP = E1 x E2
			Vector3D cp = e1.cross(e2);

			double dp = cp.dot(normal); //DP = CP . normal

			// Check the sign of the dot product
			if (Math.abs(dp) > EPSILON) { // Not collinear (non-zero angle)
				double currentSign = Math.signum(dp);
				if (initialSign == null) {
					initialSign = currentSign;
				} else if (Math.abs(initialSign - currentSign) > EPSILON) {
					// The sign changed, indicating a concave angle (internal angle > 180 deg)
					return false;
				}
			}
		}
		return true;
	}


	/**
	 * Clips a Polyface3D against the plane defined by the splitter polygon (Sutherland-Hodgman for two outputs).
	 */
	public void split(Polyface3D splitter, List<Polyface3D> frontList, List<Polyface3D> backList) {
		List<Vector3D> frontVertices = new ArrayList<>();
		List<Vector3D> backVertices = new ArrayList<>();

		// The core of Sutherland-Hodgman clipping for two outputs
		for (int i = 0; i < this.vertices.length; i++) {
			Vector3D v1 = this.vertices[i];
			Vector3D v2 = this.vertices[(i + 1) % this.vertices.length];
			
			// Calculate signed distance of v1 and v2 from the splitter plane
			Vector3D v1ToPlanePoint = v1.subtract(splitter.vertices[0], new Vector3D(0, 0, 0));
			double dist1 = v1ToPlanePoint.dot(splitter.normal);
			
			Vector3D v2ToPlanePoint = v2.subtract(splitter.vertices[0], new Vector3D(0, 0, 0));
			double dist2 = v2ToPlanePoint.dot(splitter.normal);

			// Classify points: (>= -EPSILON) is Front/Coplanar, (<= EPSILON) is Back/Coplanar
			boolean v1InsideFront = dist1 >= -EPSILON;
			boolean v1InsideBack = dist1 <= EPSILON;
			boolean v2InsideFront = dist2 >= -EPSILON;
			boolean v2InsideBack = dist2 <= EPSILON;


			if (v1InsideFront) { 
				frontVertices.add(v1.clone()); 
			}
			if (v1InsideBack) { 
				backVertices.add(v1.clone()); 
			}
			
			// Edge crosses the plane: find intersection
			// This happens if one point is strictly front (> EPSILON) and the other strictly back (< -EPSILON)
			if ((dist1 > EPSILON && dist2 < -EPSILON) || (dist1 < -EPSILON && dist2 > EPSILON)) {
				Vector3D intersection = intersectPlane(v1, v2, splitter);
				
				// Intersection point is on the plane, so it belongs to both new polygons.
				if (intersection != null) {
					frontVertices.add(intersection);
					backVertices.add(intersection.clone()); // Must clone for the back list
				}
			}
		}

		// Create the new Polyface3D objects
		if (frontVertices.size() >= 3) {
			frontList.add(new Polyface3D(this.color, frontVertices.toArray(new Vector3D[0])));
		}
		if (backVertices.size() >= 3) {
			backList.add(new Polyface3D(this.color, backVertices.toArray(new Vector3D[0])));
		}
	}

	/**
	 * Calculates the intersection point of a line segment (v1, v2) with the plane defined by 'splitter'.
	 * This uses linear interpolation based on the signed distance (dot product) from the plane.
	 * @param v1 Start point of the segment.
	 * @param v2 End point of the segment.
	 * @param splitter The polygon defining the plane.
	 * @return The intersection point, or null if intersection is not on the segment.
	 */
	public static Vector3D intersectPlane(Vector3D v1, Vector3D v2, Polyface3D splitter) {
		Vector3D planeNormal = splitter.normal;
		Vector3D planePoint = splitter.vertices[0];
		
		// D(P) = (P - planePoint) . planeNormal
		Vector3D v1ToPlanePoint = v1.subtract(planePoint, new Vector3D(0, 0, 0));
		Vector3D v2ToPlanePoint = v2.subtract(planePoint, new Vector3D(0, 0, 0));
		double dist1 = v1ToPlanePoint.dot(planeNormal);
		double dist2 = v2ToPlanePoint.dot(planeNormal);
		
		double t = 0.0;
		double distDelta = dist2 - dist1;

		// Check if segment is parallel to the plane (distDelta is small)
		if (Math.abs(distDelta) > EPSILON) {
			// Find t where dist1 + t * distDelta = 0 (on the plane)
			t = -dist1 / distDelta;
		} else {
			return null;
		}

		// Intersection must be on the segment, ignoring endpoints which are handled by classification
		if (t <= EPSILON || t >= 1.0 - EPSILON) {
			return null; 
		}

		// Linear interpolation for all components (x, y, z, w)
		double x = v1.x + t * (v2.x - v1.x);
		double y = v1.y + t * (v2.y - v1.y);
		double z = v1.z + t * (v2.z - v1.z);
		double w = v1.w + t * (v2.w - v1.w);

		// Return the new vertex which lies on the splitter plane
		return new Vector3D(x, y, z, w);
	}


}
