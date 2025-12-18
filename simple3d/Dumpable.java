package simple3d;

import json.JSONValue;

interface Dumpable {
	void load(JSONValue data);
	JSONValue save();
}