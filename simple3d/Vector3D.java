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

import json.*;

/**
 * 3D vector or point (x, y, z) with a fourth component (w) for homogeneous coordinates.
 * v1.0 12-12-2025: first release
 */
public class Vector3D implements Dumpable {
	public double x, y, z;//public for fast access
	double w = 1.0;

	public Vector3D(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = 1.0; // Point in homogeneous coordinates
	}

	public Vector3D(double x, double y, double z, double w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	public Vector3D set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = 1.0;
		return this;
	}

	public Vector3D set(Vector3D other) {
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
		this.w = other.w;
		return this;
	}

	/** In-place addition: this = this + other */
	public Vector3D add(Vector3D other) {
		this.x += other.x;
		this.y += other.y;
		this.z += other.z;
		return this;
	}

	/** In-place subtraction: this = this - other */
	public Vector3D subtract(Vector3D other) {
		this.x -= other.x;
		this.y -= other.y;
		this.z -= other.z;
		return this;
	}

	/** Result subtraction: result = this - other */
	public Vector3D subtract(Vector3D other, Vector3D result) {
		result.x = this.x - other.x;
		result.y = this.y - other.y;
		result.z = this.z - other.z;
		result.w = this.w;
		return result;
	}

	/** In-place scalar multiplication: this = this * scalar */
	public Vector3D multiply(double scalar) {
		this.x *= scalar;
		this.y *= scalar;
		this.z *= scalar;
		// w is usually left as 1.0 for points, but if it was 0 for a vector, it remains 0.
		return this;
	}

	/** In-place scalar division: this = this / scalar */
	public Vector3D divide(double scalar) {
		this.x /= scalar;
		this.y /= scalar;
		this.z /= scalar;
		// w is usually left as 1.0 for points, but if it was 0 for a vector, it remains 0.
		return this;
	}

	public double length() {
		return Math.sqrt(x * x + y * y + z * z);
	}

	/** In-place normalization: this = normalize(this) */
	public Vector3D normalize() {
		double len = length();
		if (len != 0) {
			this.x /= len;
			this.y /= len;
			this.z /= len;
		}
		return this;
	}

	public double dot(Vector3D other) {
		return x * other.x + y * other.y + z * other.z;
	}

	public double squaredMagnitude() {
		return x * x + y * y + z * z;
	}

    public Vector3D cross(Vector3D other) {
        double newX = this.y * other.z - this.z * other.y;
        double newY = this.z * other.x - this.x * other.z;
        double newZ = this.x * other.y - this.y * other.x;
        return new Vector3D(newX, newY, newZ);
    }

	@Override
	public Vector3D clone() {
		return new Vector3D(this.x, this.y, this.z, this.w);
	}

	@Override
	public String toString() {
		return "[" + this.x + ", " + this.y + ", " + this.z + ", " + this.w + "]";
	}

	protected Vector3D() {}
	public void load(JSONValue data) {
		JSONArray vector = (JSONArray) data;
		x = ((BigDecimal) vector.get(0).toJava()).doubleValue();
		y = ((BigDecimal) vector.get(1).toJava()).doubleValue();
		z = ((BigDecimal) vector.get(2).toJava()).doubleValue();
		w = 1;
	}

	public JSONValue save() {
		ArrayList<BigDecimal> vector = new ArrayList<>();
		vector.add(BigDecimal.valueOf(x));
		vector.add(BigDecimal.valueOf(y));
		vector.add(BigDecimal.valueOf(z));
		return new JSONArray(vector);
	}
}