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
import java.util.Arrays;

import json.*;

/**
 * Matrix4x4 is a 4x4 matrix, primarily used for 3D transformations.
 * v1.0 12-12-2025: first release
 */
public class Matrix4x4 implements Dumpable {
	final double[][] m = new double[4][4];

	protected Matrix4x4() {
		// Initialize to 0
	}

	public static Matrix4x4 createIdentity() {
		Matrix4x4 matrix = new Matrix4x4();
		matrix.m[0][0] = 1.0;
		matrix.m[1][1] = 1.0;
		matrix.m[2][2] = 1.0;
		matrix.m[3][3] = 1.0;
		return matrix;
	}

	public void setIdentity() {
		for (double[] row : m) {
			Arrays.fill(row, 0.0);
		}
		m[0][0] = 1.0;
		m[1][1] = 1.0;
		m[2][2] = 1.0;
		m[3][3] = 1.0;
	}

	public boolean isIdentity() {
		for (int c = 0; c < 4; c++)
			for (int r = 0; r < 4; r++)
				if (m[r][c] != (r == c ? 1 : 0))
					return false;
		return true;
	}

	public void applyTranslation(double x, double y, double z) {
		Matrix4x4 matrix = createIdentity();
		matrix.m[3][0] = x;
		matrix.m[3][1] = y;
		matrix.m[3][2] = z;
		multiply(matrix, this);
	}
	
	public void applyScale(double sx, double sy, double sz) {
		Matrix4x4 matrix = createIdentity();
		matrix.m[0][0] = sx;
		matrix.m[1][1] = sy;
		matrix.m[2][2] = sz;
		multiply(matrix, this);
	}

	public void applyRotationX(double angleRad) {
		Matrix4x4 matrix = createIdentity();
		double cos = Math.cos(angleRad);
		double sin = Math.sin(angleRad);
		matrix.m[1][1] = cos;
		matrix.m[1][2] = sin;
		matrix.m[2][1] = -sin;
		matrix.m[2][2] = cos;
		multiply(matrix, this);
	}
	
	public void applyRotationY(double angleRad) {
		Matrix4x4 matrix = createIdentity();
		double cos = Math.cos(angleRad);
		double sin = Math.sin(angleRad);
		matrix.m[0][0] = cos;
		matrix.m[0][2] = -sin;
		matrix.m[2][0] = sin;
		matrix.m[2][2] = cos;
		multiply(matrix, this);
	}
	

	public void applyRotationZ(double angleRad) {
		Matrix4x4 matrix = createIdentity();
		double cos = Math.cos(angleRad);
		double sin = Math.sin(angleRad);
		matrix.m[0][0] = cos;
		matrix.m[0][1] = -sin;
		matrix.m[1][0] = sin;
		matrix.m[1][1] = cos;
		multiply(matrix, this);
	}
	
	public static Matrix4x4 createRotationY(double angleRad) {
		Matrix4x4 matrix = createIdentity();
		matrix.applyRotationY(angleRad);
		return matrix;
	}

	public static Matrix4x4 createProjection(double fov, double aspectRatio) {
		Matrix4x4 matrix = new Matrix4x4();
		createProjection(fov, aspectRatio, matrix);
		return matrix;
	}

	/** In-place projection (re-initializes 'result' matrix). */
	public static void createProjection(double fov, double aspectRatio, Matrix4x4 result) {
		for (int r = 0; r < 4; r++) Arrays.fill(result.m[r], 0.0); // Clear matrix

		double fovRad = 1.0 / Math.tan(fov / 2.0);

		result.m[0][0] = aspectRatio * fovRad;
		result.m[1][1] = fovRad;
		result.m[2][2] = 1.0;
		result.m[3][2] = -1.0;
		result.m[2][3] = 1.0;
		result.m[3][3] = 0.0;
	}

	/** Result vector-matrix multiplication: result = mat * v */
	public Vector3D multiply(Vector3D v, Vector3D result) {
		double x = v.x * m[0][0] + v.y * m[1][0] + v.z * m[2][0] + v.w * m[3][0];
		double y = v.x * m[0][1] + v.y * m[1][1] + v.z * m[2][1] + v.w * m[3][1];
		double z = v.x * m[0][2] + v.y * m[1][2] + v.z * m[2][2] + v.w * m[3][2];
		double w = v.x * m[0][3] + v.y * m[1][3] + v.z * m[2][3] + v.w * m[3][3];

		result.x = x;
		result.y = y;
		result.z = z;
		result.w = w;
		return result;
	}

	/** Result matrix multiplication: result = this * other */
	public void multiply(Matrix4x4 other, Matrix4x4 result) {
		// Need temporary storage because 'result' might be 'this' or 'other'
		double[][] temp = new double[4][4];

		for (int c = 0; c < 4; c++) {
			for (int r = 0; r < 4; r++) {
				temp[r][c] = m[r][0] * other.m[0][c] + m[r][1] * other.m[1][c] + m[r][2] * other.m[2][c] + m[r][3] * other.m[3][c];
			}
		}
		for (int r = 0; r < 4; r++) {
			System.arraycopy(temp[r], 0, result.m[r], 0, 4);
		}
	}

	/**
	 * Transforms all vertices of polygon in-place
	 */
	public void transformInPlace(Vector3D[] vertices) {
		for (int i = 0; i < vertices.length; i++) {
			multiply(vertices[i], vertices[i]); // In-place vector-matrix multiplication
		}
	}

	/**
	 * Creates a View Matrix (Point At).
	 * Writes the result to the 'result' matrix.
	 */
	public static void pointAt(Vector3D pos, Vector3D target, Vector3D up, Matrix4x4 result) {
		Vector3D tempUp = new Vector3D(up.x, up.y, up.z);
		Vector3D tempForward = new Vector3D(0, 0, 0);

		// 1. Calculate new Z axis (forward direction)
		target.subtract(pos, tempForward).normalize(); // tempForward = target - pos, then normalize

		// 2. Calculate Projection Vector 'a': a = tempForward * (up dot tempForward)
		Vector3D a = tempForward.clone(); 
		// a.multiply(up.dot(tempForward)) -> a is now the projection vector
		a.multiply(up.dot(tempForward)); 

		// 3. Calculate new Y axis (up direction)
		// tempUp = up - a, then normalize
		tempUp.subtract(a).normalize();

		// 4. Calculate new X axis (right direction)
		// right = tempUp x tempForward
		Vector3D right = tempUp.cross(tempForward);

		// Construct Dimensioning and Translation Matrix (M)
		result.m[0][0] = right.x;    result.m[0][1] = right.y;    result.m[0][2] = right.z;    result.m[0][3] = 0.0;
		result.m[1][0] = tempUp.x;       result.m[1][1] = tempUp.y;       result.m[1][2] = tempUp.z;       result.m[1][3] = 0.0;
		result.m[2][0] = tempForward.x;  result.m[2][1] = tempForward.y;  result.m[2][2] = tempForward.z;  result.m[2][3] = 0.0;
		result.m[3][0] = pos.x;         result.m[3][1] = pos.y;         result.m[3][2] = pos.z;         result.m[3][3] = 1.0;
	}

	/**
	 * Performs quick inverse in-place
	 */
	public void quickInverse(Matrix4x4 result) {
		double t00 = m[0][0], t01 = m[0][1], t02 = m[0][2];
		double t10 = m[1][0], t11 = m[1][1], t12 = m[1][2];
		double t20 = m[2][0], t21 = m[2][1], t22 = m[2][2];
		double t30 = m[3][0], t31 = m[3][1], t32 = m[3][2];

		// Transpose rotation part (and store in result)
		result.m[0][0] = t00; result.m[0][1] = t10; result.m[0][2] = t20; result.m[0][3] = 0.0;
		result.m[1][0] = t01; result.m[1][1] = t11; result.m[1][2] = t21; result.m[1][3] = 0.0;
		result.m[2][0] = t02; result.m[2][1] = t12; result.m[2][2] = t22; result.m[2][3] = 0.0;

		// Calculate the inverted translation components
		result.m[3][0] = -(t30 * result.m[0][0] + t31 * result.m[1][0] + t32 * result.m[2][0]);
		result.m[3][1] = -(t30 * result.m[0][1] + t31 * result.m[1][1] + t32 * result.m[2][1]);
		result.m[3][2] = -(t30 * result.m[0][2] + t31 * result.m[1][2] + t32 * result.m[2][2]);
		result.m[3][3] = 1.0;
	}

	// Helper for pointAt that returns a new matrix (used only in non-performance-critical setup)
	public Matrix4x4 quickInverse() {
		Matrix4x4 result = new Matrix4x4();
		quickInverse(result);
		return result;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int r = 0; r < 4; r++) {
			sb.append("(");
			for (int c = 0; c < 4; c++) {
				sb.append(m[r][c]);
				if (c < 3) sb.append(",");
			}
			sb.append(")");
		}
		return sb.toString();
	}

	public void load(JSONValue data) {
		JSONArray mat = (JSONArray) data;
		for (int r = 0; r < 4; r++) {
			JSONArray row = (JSONArray) mat.get(r);
			for (int c = 0; c < 4; c++)
				m[r][c] = ((BigDecimal) row.get(c).toJava()).doubleValue();
		}
	}

	public JSONValue save() {
		JSONArray mat = new JSONArray();
		for (int r = 0; r < 4; r++) {
			ArrayList<BigDecimal> row = new ArrayList<>();
			for (int c = 0; c < 4; c++)
				row.add(BigDecimal.valueOf(m[r][c]));
			mat.add(new JSONArray(row));
		}
		return mat;
	}
}
