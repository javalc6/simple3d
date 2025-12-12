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
/* JSONArray holds a JSON array

array ::= '[' [ value ( ',' value )* ] ']'

int size() returns number of elements
JSONValue get(idx) returns element at index idx
Object toJava();//return Java value
String toString();//return JSON value
boolean equals(Object o)//check equal
*/
final public class JSONArray extends JSONValue {
	final ArrayList<JSONValue> value = new ArrayList<>();

	public JSONArray() {
	}

	public JSONArray(ArrayList<? extends Object> val) {//Java oriented constructor
		for (Object element: val) {
			if (element == null)
				value.add(null);
			else if (element instanceof Boolean) {
				value.add(new JSONBoolean((Boolean) element));
			} else if (element instanceof BigDecimal) {
				value.add(new JSONNumber((BigDecimal) element));
			} else if (element instanceof String) {
				try	{
					value.add(new JSONString((String) element, false));				
				} catch (JSONException je) {
//never happen
				}
			} else if (element instanceof ArrayList) {
				value.add(new JSONArray((ArrayList<Object>) element));
			} else if (element instanceof LinkedHashMap) {
				value.add(new JSONObject((LinkedHashMap<String, Object>) element));
			} else if (element instanceof JSONValue) {
				value.add((JSONValue) element);
			} else throw new RuntimeException("unexpected element of type " + element.getClass().getName());
		}
	}

	public int size() {
		return value.size();
	}

	public JSONValue get(int idx) {
		return value.get(idx);
	}

	public void add(JSONValue val) {
		value.add(val);
	}

	public JSONArray(String str) throws JSONException {//constructor parsing a string representing a JSON array
		Scanner scanner = new Scanner(str);
		_parse(scanner);
		if (!scanner.eos()) throw new JSONException("parsing error due to unexpected trailing characters");
	}

	public JSONArray(Scanner scanner) throws JSONException {//constructor using Scanner, note that parsing stops after a json value is found, even if extra characters remain in scanner
		_parse(scanner);
	}

	private void _parse(Scanner scanner) throws JSONException {
		Character ch = scanner.getChar("[", true, true);
		while (((ch = scanner.getChar(true, true)) != null) && ((ch = scanner.getChar("]", true, false)) == null)) {
			value.add(JSONValue.parse(scanner));
			ch = scanner.getChar(",]", true, true);
			if (ch == ']')
				break;
		}
		if (ch == null) throw new JSONException("parsing error, expecting ]");
	}

	public Object toJava() {
		ArrayList<Object> result = new ArrayList<>();
		for (JSONValue element: value) {
			result.add(element == null ? "null" : element.toJava());
		}
		return result;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder().append('[');
		boolean first = true;
		for (JSONValue element: value) {
			if (first)
				first = false;
			else sb.append(',');
			sb.append(element == null ? "null" : element.toString());
		}
		sb.append(']');
		return sb.toString();
	}

}