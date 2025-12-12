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

/* JSONString holds a JSON string

string ::= '"' char* '"'
char ::= unescaped | '\' ('"' | '\' | '/' | 'b' | 'f' | 'n' | 'r' | 't' | 'u' <hex><hex><hex><hex>)
unescaped ::= %x20-21 | %x23-5B | %x5D-10FFFF

Object toJava();//return Java value
String toString();//return JSON value
boolean equals(Object o)//check equal
*/
final public class JSONString extends JSONValue {
	final String value;

	public JSONString(String str) {//Java oriented constructor
		value = _process(str);
	}

	public JSONString(String str, boolean json_format) throws JSONException {//constructor parsing a string representing a JSON String if json_format is true or a standard java string if json_format is false
		if (json_format) {
			Scanner scanner = new Scanner(str);
			value = _parse(scanner);
			if (!scanner.eos()) throw new JSONException("parsing error due to unexpected trailing characters");
		} else {//java format
			value = _process(str);
		}
	}

	public JSONString(Scanner scanner) throws JSONException {//constructor using Scanner, note that parsing stops after a json value is found, even if extra characters remain in scanner
		value = _parse(scanner);
	}

	private String _parse(Scanner scanner) throws JSONException {
		StringBuilder sb = new StringBuilder();
		Character ch = scanner.getChar("\"", true, true);
		while (((ch = scanner.getChar(false, false)) != null) && (ch != '\"')) {
			if (ch == '\\') {
				sb.append(ch);
				ch = scanner.getChar("\"\\/bfnrtu", false, true);
				if (ch == 'u') {
					int n_hex = 0;
					do {
						sb.append(ch);
						ch = scanner.getChar("0123456789abcdefABCDEF", false, true);
						n_hex++;
					} while (n_hex < 4);
				}
			} else if (ch < 0x20)
				throw new JSONException("parsing error, unexpected control character found");
			sb.append(ch);
		}
		if (ch == null) throw new JSONException("parsing error, expecting \"");
		return sb.toString();
	}

	private String _process(String str) {
		int length = str.length();
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			char ch = str.charAt(i);
			switch(ch) {
				case '"':
					sb.append("\\\"");
					break;
				case '\\':
					sb.append("\\\\");
					break;
/*					case '/': it is not required to escape /
					sb.append("\\/");
					break;*/
				case '\b':
					sb.append("\\b");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '\t':
					sb.append("\\t");
					break;
				default:
					if (ch >= 0x20)
						sb.append(ch);
					else {
						sb.append("\\u").append(String.format("%04x", (int) ch));
					}
			}
		}
		return sb.toString();
	}

	public Object toJava() {
		int length = value.length();
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			char ch = value.charAt(i);
			if (ch == '\\')	{
				i++;
				ch = value.charAt(i);
				switch(ch) {
					case '"':
						sb.append('"');
						break;
					case '\\':
						sb.append('\\');
						break;
					case '/':
						sb.append('/');
						break;
					case 'b':
						sb.append('\b');
						break;
					case 'f':
						sb.append('\f');
						break;
					case 'n':
						sb.append('\n');
						break;
					case 'r':
						sb.append('\r');
						break;
					case 't':
						sb.append('\t');
						break;
					case 'u':
						sb.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
						i += 4;
						break;
					default:
						throw new RuntimeException("found illegal escape character: " + ch);
				}
			} else sb.append(ch);
		}
		return sb.toString();
	}

	public String toString() {
		return "\"" + value + "\"";
	}
}