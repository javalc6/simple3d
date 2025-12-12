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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
/* JSONObject holds a JSON object

object ::= '{' [ member ( ',' member )* ] '}'
member ::= string ':' value

int size() returns number of elements
JSONValue get(key) returns element with given key
Object toJava();//return Java value
String toString();//return JSON value
boolean equals(Object o)//check equal
*/
final public class JSONObject extends JSONValue {
	final LinkedHashMap<String, JSONValue> value = new LinkedHashMap<>();

	public JSONObject(LinkedHashMap<String, Object> val) {//Java oriented constructor
        val.forEach((key, element) -> {
			if (element == null)
				value.put(key, null);
			else if (element instanceof Boolean) {
				value.put(key, new JSONBoolean((Boolean) element));
			} else if (element instanceof BigDecimal) {
				value.put(key, new JSONNumber((BigDecimal) element));
			} else if (element instanceof String) {
				try	{
					value.put(key, new JSONString((String) element, false));				
				} catch (JSONException je) {
//never happen
				}
			} else if (element instanceof ArrayList) {
				value.put(key, new JSONArray((ArrayList<Object>) element));
			} else if (element instanceof LinkedHashMap) {
				value.put(key, new JSONObject((LinkedHashMap<String, Object>) element));
			} else if (element instanceof JSONValue) {
				value.put(key, (JSONValue) element);
			} else throw new RuntimeException("unexpected element of type " + element.getClass().getName() + " with key " + key);
        });
	}

	public int size() {
		return value.size();
	}

	public JSONValue get(String key) {
		return value.get(key);
	}

	public JSONObject(String str) throws JSONException {//constructor parsing a string representing a JSON array
		Scanner scanner = new Scanner(str);
		_parse(scanner);
		if (!scanner.eos()) throw new JSONException("parsing error due to unexpected trailing characters");
	}

	public JSONObject(Scanner scanner) throws JSONException {//constructor using Scanner, note that parsing stops after a json value is found, even if extra characters remain in scanner
		_parse(scanner);
	}

	private void _parse(Scanner scanner) throws JSONException {
		Character ch = scanner.getChar("{", true, true);
		while (((ch = scanner.getChar(true, true)) != null) && ((ch = scanner.getChar("}", true, false)) == null)) {
			String key = new JSONString(scanner).toString();
			key = key.substring(1, key.length() - 1); //remove leading and trailing "
			ch = scanner.getChar(":", true, true);
			if (value.putIfAbsent(key, JSONValue.parse(scanner)) != null)
				throw new JSONException("parsing error due duplicate key: \"" + key + "\"");
			ch = scanner.getChar(",}", true, true);
			if (ch == '}')
				break;
		}
		if (ch == null) throw new JSONException("parsing error, expecting }");
	}

	public Object toJava() {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		value.forEach((key, element) -> result.put(key, element == null ? "null" : element.toJava()));
		return result;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder().append('{');
		boolean first = true;
		for (Map.Entry<String, JSONValue> entry : value.entrySet()) {
			if (first)
				first = false;
			else sb.append(',');
			sb.append('\"').append(entry.getKey()).append("\":");
			JSONValue element = entry.getValue();
			sb.append(element == null ? "null" : element.toString());
        }
		sb.append('}');
		return sb.toString();
	}

}