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
import java.util.LinkedHashMap;

import json.*;
/**
 * 3D polygon with optional colorIndex. All vertices shall be coplanar.
 * v1.0 12-12-2025: first release
 */
public class Polygon3D implements Dumpable {
	Integer[] vertex_indexes;
	Integer colorIndex;
	Mesh mesh;

	public Polygon3D(Mesh mesh, Integer... vertex_indexes) {
		this.mesh = mesh;
		this.vertex_indexes = vertex_indexes;
	}

	public void setColorIndex(Integer colorIndex) {
		this.colorIndex = colorIndex;
	}

	public Integer getColorIndex() {
		return colorIndex;
	}

	protected Polygon3D() {}
	public void load(JSONValue data) {
		JSONObject poly = (JSONObject) data;
		JSONValue _colorIndex = poly.get("colorIndex");
		colorIndex = _colorIndex == null ? null : ((BigDecimal)_colorIndex.toJava()).intValue();
		JSONArray idxs = (JSONArray) poly.get("indexes");
		int n = idxs.size();
		vertex_indexes = new Integer[n];
		for (int i = 0; i < n; i++) {
			vertex_indexes[i] = ((BigDecimal) idxs.get(i).toJava()).intValue();
		}
	}

	public JSONValue save() {
		LinkedHashMap<String, Object> poly = new LinkedHashMap<>();
		JSONArray idxs = new JSONArray();
        for (Integer vertexIndex : vertex_indexes) {
            idxs.add(new JSONNumber(BigDecimal.valueOf(vertexIndex)));
        }
		poly.put("indexes", idxs);
		if (colorIndex != null)
			poly.put("colorIndex", new JSONNumber(BigDecimal.valueOf(colorIndex)));
		return new JSONObject(poly);
	}
}
