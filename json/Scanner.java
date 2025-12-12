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

/* Scanner is utility class used to parse string

*/
public class Scanner {
	protected final String str;
	protected int pointer;

	public Scanner(String str) {
		this.str = str.trim();//remove leading and traling ws
		pointer = 0;
	}

	protected Character getChar(boolean skip_ws, boolean test) {//returns next char, optionally skip leading whitespaces and/or test mode
		Character ch = null;
		int i = pointer;
		if (skip_ws) {
			while ((i < str.length()) && Character.isWhitespace(ch = str.charAt(i++)))
				;
		} else if (i < str.length())
			ch = str.charAt(i++);
		if (!test)
			pointer = i;
		return ch;
	}

	protected Character getChar(String charset, boolean skip_ws, boolean mandatory) throws JSONException {//returns next char only if it is in charset, optionally skip leading whitespaces, in case 'mandatory' is specified JSONException() is raised in case of no match
		int i = pointer;
		if (skip_ws) {
			while (i < str.length() && Character.isWhitespace(str.charAt(i)))
				i++;
		}
		if (i < str.length()) {
			char ch = str.charAt(i);
			if (charset.indexOf(ch) != -1) {
				pointer = i + 1;
				return ch;
			}
		}
		if (mandatory)
			throw new JSONException("parsing error, expecting one of the following characters: " + charset);   
		return null;
	}

	protected String getIdentifier() {//returns identifier
		Character ch = null;
		int i = pointer;
		while ((i < str.length()) && Character.isWhitespace(ch = str.charAt(i++)))
			;
		if (ch != null) {
			StringBuilder sb = new StringBuilder().append(ch);
			while ((i < str.length()) && Character.isLetter(ch = str.charAt(i++))) {
				sb.append(ch);
				pointer = i;
			}
			return sb.toString();
		} else return null;		
	}

	protected boolean eos() {//end of scanning ?
		int i = pointer;
		while (i < str.length() && Character.isWhitespace(str.charAt(i))) {
			i++;
		}
		return i >= str.length();
	}
}