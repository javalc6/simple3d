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

/* JSONNumber holds a JSON number

number ::= [ '-' ] int [ frac ] [ exp ]
int ::= '0' | ( digit1-9 digit* )
frac ::= '.' digit+
digit ::= '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'
exp ::= ('e' | 'E') [ '+' | '-' ] digit+

Object toJava();//return Java value
String toString();//return JSON value
boolean equals(Object o)//check equal
*/
final public class JSONNumber extends JSONValue {
	BigDecimal value;

	public JSONNumber(BigDecimal val) {//Java oriented constructor
		value = val;
	}

	public JSONNumber(String str) throws JSONException {//constructor parsing a string representing a JSON number
		Scanner scanner = new Scanner(str);
		_parse(scanner);
		if (!scanner.eos()) throw new JSONException("parsing error due to unexpected trailing characters");
	}

	public JSONNumber(Scanner scanner) throws JSONException {//constructor using Scanner, note that parsing stops after a json value is found, even if extra characters remain in scanner
		_parse(scanner);
	}

	private void _parse(Scanner scanner) throws JSONException {
		StringBuilder sb = new StringBuilder();
		Character ch = scanner.getChar("-0123456789", true, true);
		if (ch == '-') {
			sb.append(ch);
			ch = scanner.getChar("0123456789", false, true);
		}
		if (ch == '0') {
			sb.append(ch);
		} else if (ch > '0' && ch <= '9') {
			sb.append(ch);
			while ((ch = scanner.getChar("0123456789", false, false)) != null) {
				sb.append(ch);
			}
		}

		ch = scanner.getChar(".eE", false, false);
		if ((ch != null) && ch == '.') {
			int decimals = 0;
			sb.append(ch);
			while ((ch = scanner.getChar("0123456789", false, false)) != null) {
				decimals++;
				sb.append(ch);
			}
			if (decimals == 0) throw new JSONException("parsing error");
			ch = scanner.getChar("eE", false, false);
		}
		if (ch != null) {//if ch != null then ch is either 'e' or 'E'
			sb.append(ch);
			ch = scanner.getChar("+-", false, false);
			if (ch != null)
				sb.append(ch);
			boolean first = true;
			while ((ch = scanner.getChar("0123456789", false, first)) != null) {
				sb.append(ch);
				first = false;
			}
		}
		value = new BigDecimal(sb.toString());
	}

	public Object toJava() {
		return value;
	}

	public String toString() {
		return value.toString();
	}
}