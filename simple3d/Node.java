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
 * Node is a structural object that holds the transformation, color,
 * and reference to a Mesh object.
 * v1.0 12-12-2025: first release
 * v1.0.1 17-12-2025: added shapeArguments
 */
public class Node implements Dumpable {
	String id;
	String meshID;
	Mesh.Shape shape; JSONObject shapeArguments;
	Color color;
	Color[] colorList;
	final Matrix4x4 worldMatrix = Matrix4x4.createIdentity();

	public Node(String id, String meshID, Color color) {
		this.id = id;
		this.meshID = meshID;
		this.color = color;
		this.colorList = colorList;
	}
	//Node constructor for immutable shapes
	public Node(String id, Mesh.Shape shape, Color color) {
		this.id = id;
		this.shape = shape;
		this.color = color;
	}

	//Node constructor for shapes with arguments
	public Node(String id, Mesh.Shape shape, JSONObject shapeArguments, Color color) {
		this.id = id;
		this.shape = shape;
		this.color = color;
		this.shapeArguments = shapeArguments;
	}

	public void setColorList(Color[] colorList) {
		this.colorList = colorList;
	}

	public Color[] getColorList() {
		return colorList;
	}

	public void applyTranslation(double x, double y, double z) {
		worldMatrix.applyTranslation(x, y, z);
	}

	public void applyScale(double sx, double sy, double sz) {
		worldMatrix.applyScale(sx, sy, sz);
	}

	public void applyRotationX(double angleRad) {
		worldMatrix.applyRotationX(angleRad);
	}

	public void applyRotationY(double angleRad) {
		worldMatrix.applyRotationY(angleRad);
	}

	public void applyRotationZ(double angleRad) {
		worldMatrix.applyRotationZ(angleRad);
	}
	
	protected Node() {}
	@Override
	public void load(JSONValue data) {
		JSONObject node = (JSONObject) data;
		id = (String) node.get("id").toJava();
		JSONValue _meshID = node.get("meshID");
		if (_meshID != null)
			meshID = (String) node.get("meshID").toJava();
		else {
			JSONValue _shape = node.get("shape");
			if (_shape == null)
				throw new RuntimeException("invalid data: either meshID or shape shall be defined");
			shape = Mesh.Shape.valueOf((String)_shape.toJava());
			shapeArguments = (JSONObject) node.get("shapeArguments");
		}

		JSONValue colorVal = node.get("color");
		if (colorVal != null) {
			color = Color.parse((String) colorVal.toJava());
		} else color = null;
		JSONArray colors = (JSONArray) node.get("colorList");
		if (colors != null) {
			int n = colors.size();
			colorList = new Color[n];
			for (int i = 0; i < n; i++) {
				colorList[i] = Color.parse((String) colors.get(i).toJava());
			}
		}

		// Load 'transformationMatrix' if present
		JSONValue mat = node.get("transformationMatrix");
		if (mat != null) {
			worldMatrix.load(mat);
		} else {
			// If no 'transformationMatrix', compose it from rotation/translation/scale
			worldMatrix.setIdentity();

			// Handle optional translation
			JSONValue translationVal = node.get("translation");
			if (translationVal != null) {
				JSONArray translation = (JSONArray) translationVal;
				double x = ((BigDecimal) translation.get(0).toJava()).doubleValue();
				double y = ((BigDecimal) translation.get(1).toJava()).doubleValue();
				double z = ((BigDecimal) translation.get(2).toJava()).doubleValue();
				applyTranslation(x, y, z);
			}

			// Handle optional rotations (in X, Y, Z order)
			JSONValue rotXVal = node.get("rotationX");
			if (rotXVal != null) {
				applyRotationX(((BigDecimal) rotXVal.toJava()).doubleValue());
			}
			JSONValue rotYVal = node.get("rotationY");
			if (rotYVal != null) {
				applyRotationY(((BigDecimal) rotYVal.toJava()).doubleValue());
			}
			JSONValue rotZVal = node.get("rotationZ");
			if (rotZVal != null) {
				applyRotationZ(((BigDecimal) rotZVal.toJava()).doubleValue());
			}

			// Handle optional scale
			JSONValue scaleVal = node.get("scale");
			if (scaleVal != null) {
				JSONArray scale = (JSONArray) scaleVal;
				double sx = ((BigDecimal) scale.get(0).toJava()).doubleValue();
				double sy = ((BigDecimal) scale.get(1).toJava()).doubleValue();
				double sz = ((BigDecimal) scale.get(2).toJava()).doubleValue();
				applyScale(sx, sy, sz);
			}
		}
	}

	@Override
	public JSONValue save() {
		LinkedHashMap<String, Object> node = new LinkedHashMap<>();
		node.put("type", "node");
		node.put("id", id);
		if (meshID != null)
			node.put("meshID", meshID);
		else if (shape != null) {
			node.put("shape", shape.toString());
			if (shapeArguments != null)
				node.put("shapeArguments", shapeArguments);
		} else throw new RuntimeException("invalid data: either meshID or shape shall be defined");
		
		if (color != null) {
			node.put("color", color.toString());
		}
		
		if (colorList != null) {
			JSONArray colors = new JSONArray();
			for (int i = 0; i < colorList.length; i++) {
				colors.add(colorList[i].save());
			}
			node.put("colorList", colors);
		}
		
		if (!worldMatrix.isIdentity()) {
			node.put("transformationMatrix", worldMatrix.save());
		}
		
		return new JSONObject(node);
	}
}