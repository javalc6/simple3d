/*
License Information, 2023 Livio (javalc6)

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
package json;

/* JSONValue is the base abstract class for a JSON value

static method parse(String str) can be used to parse a string representing a JSON value
*/
public abstract class JSONValue {
	public static JSONValue parse(String str) throws JSONException {//method to parse a string representing a JSON value
		Scanner scanner = new Scanner(str);
		JSONValue jv = parse(scanner);
		if (!scanner.eos()) throw new JSONException("parsing error due to unexpected trailing characters");
		return jv;
	}

	public static JSONValue parse(Scanner scanner) throws JSONException {//method to parse a string representing a JSON value using Scanner, note that parsing stops after a json value is found, even if extra characters remain in scanner
		Character ch = scanner.getChar(true, true);
		if (ch == null) throw new JSONException("parsing error");
		if (ch == '{') {
			return new JSONObject(scanner);
		} else if (ch == '[') {
			return new JSONArray(scanner);
		} else if (ch == '"') {
			return new JSONString(scanner);
		} else if ("-0123456789".indexOf(ch) != -1) {
			return new JSONNumber(scanner);
		} else if (Character.isLetter(ch)) {
			String id = scanner.getIdentifier();
			if ("null".equals(id))
				return null;
			else if ("false".equals(id))
				return new JSONBoolean(false);
			else if ("true".equals(id))
				return new JSONBoolean(true);
		}
		throw new JSONException("parsing error");
	}

	abstract public Object toJava();//return Java value

	abstract public String toString();//return JSON value

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof JSONValue))
			return false;
		return ((JSONValue) o).toJava().equals(toJava());
	}

}