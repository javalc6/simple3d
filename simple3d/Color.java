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

import json.JSONValue;
import json.JSONString;
import json.JSONException;
/**
 * Color (r,g,b)
 */
public class Color implements Dumpable {
	int r, g, b;

	public Color(int r, int g, int b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}

    public final static Color WHITE = new Color(255, 255, 255);
    public final static Color LIGHT_GRAY = new Color(192, 192, 192);
    public final static Color GRAY = new Color(128, 128, 128);
    public final static Color DARK_GRAY = new Color(64, 64, 64);
    public final static Color BLACK = new Color(0, 0, 0);
    public final static Color RED = new Color(255, 0, 0);
    public final static Color PINK = new Color(255, 175, 175);
    public final static Color ORANGE = new Color(255, 200, 0);
    public final static Color YELLOW = new Color(255, 255, 0);
	public final static Color GREEN = new Color(0, 255, 0);
    public final static Color MAGENTA = new Color(255, 0, 255);
    public final static Color CYAN = new Color(0, 255, 255);
    public final static Color BLUE = new Color(0, 0, 255);

    public int getRed() {
		return r;
    }

    public int getGreen() {
		return g;
    }

    public int getBlue() {
		return b;
    }

    public static Color parse(String string) {
		Color color = new Color();
		color.parser(string);
		return color;
	}

    private void parser(String string) {
		int rgb = Integer.parseInt(string.trim().substring(1), 16);//remove leading #

		r = (rgb >> 16) & 0xFF;
		g = (rgb >> 8) & 0xFF;
		b = rgb & 0xFF;
	}

    public String toString() {
		int rgb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

	protected Color() {}
	public void load(JSONValue data) {
		parser((String) data.toJava());
	}

	public JSONValue save() {
		return new JSONString(toString());
	}
}
