package simple3d;

import json.JSONValue;

interface Dumpable {
	public void load(JSONValue data);
	public JSONValue save();
}