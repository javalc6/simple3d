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

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
/* 
TestSuite to perform automatic tests of json parser

compile: javac -encoding UTF-8 json\TestSuite.java

run: java json.TestSuite [json file]

*/

final public class TestSuite {

	interface TestCase {
		JSONValue run(String str) throws JSONException;
	}

	static class testReport {
		int n_tests;
		int n_failed;
	}

	public static void main(String[] args) throws JSONException, IOException {
		TestSuite ts = new TestSuite();
		ts.run(args);
	}

	private void run(String[] args) throws JSONException, IOException {
		if (args.length > 0) {//json file
			String json = readfile(args[0]);
			JSONValue result = JSONValue.parse(json);
			System.out.println(result);
		} else {//set of test cases
			testReport tr = new testReport();

	//testing JSONNumber
			check_test(1, tr, "0", JSONNumber::new);
			check_test(2, tr, "1234", JSONNumber::new);
			check_test(3, tr, "1234.5678", JSONNumber::new);
			check_test(4, tr, "-1234.5678", JSONNumber::new);
			check_test(5, tr, "1234e56", "1.234E+59", JSONNumber::new);
			check_test(6, tr, "1234E-56", "1.234E-53", JSONNumber::new);
			check_test(7, tr, "1234.56e56", "1.23456E+59", JSONNumber::new);

			check_test_toJava(10, tr, "1234.5678", new BigDecimal("1234.5678"), JSONNumber::new);
			check_test_toJava(11, tr, "1234.5678", new BigDecimal("1234.5678"), (s) -> new JSONNumber(new BigDecimal(s)));

			check_negative_test(50, tr, "alfa", "parsing error, expecting one of the following characters: -0123456789", JSONNumber::new);
			check_negative_test(51, tr, "- 1234", "parsing error, expecting one of the following characters: 0123456789", JSONNumber::new);
			check_negative_test(52, tr, "1234a", "parsing error due to unexpected trailing characters", JSONNumber::new);
			check_negative_test(53, tr, "1234.", "parsing error", JSONNumber::new);
			check_negative_test(54, tr, ".1234", "parsing error, expecting one of the following characters: -0123456789", JSONNumber::new);
			check_negative_test(55, tr, "1234E", "parsing error, expecting one of the following characters: 0123456789", JSONNumber::new);

	//testing JSONBoolean
			check_test(100, tr, "true", (s) -> new JSONBoolean(true));
			check_test(101, tr, "false", (s) -> new JSONBoolean(false));

			check_test_toJava(110, tr, "false", Boolean.FALSE, (s) -> new JSONBoolean(false));

	//testing JSONString
			check_test(200, tr, "\"\"", (s) -> new JSONString(s, true));
			check_test(201, tr, "\"abcd efgh\"", (s) -> new JSONString(s, true));
			check_test(202, tr, "\"abcd\\tefgh\"", (s) -> new JSONString(s, true));
			check_test(203, tr, "\"abcd\\u12efefgh\"", (s) -> new JSONString(s, true));
			check_test(204, tr, "abcd\"\\\b\f\n\r\tefgh", "\"abcd\\\"\\\\\\b\\f\\n\\r\\tefgh\"", (s) -> new JSONString(s, false));

			check_test_toJava(210, tr, "\"abcd efgh\"", "abcd efgh", (s) -> new JSONString(s, true));
			check_test_toJava(211, tr, "\"abcd\\\"\\\\\\/\\b\\f\\n\\r\\tefgh\"", "abcd\"\\/\b\f\n\r\tefgh", (s) -> new JSONString(s, true));
			check_test_toJava(212, tr, "\"abcd\\u0040efgh\"", "abcd@efgh", (s) -> new JSONString(s, true));
			check_test_toJava(213, tr, "abcd\u0040efgh", "abcd@efgh", (s) -> new JSONString(s, false));

			check_negative_test(250, tr, "\"abcdefgh", "parsing error, expecting \"", (s) -> new JSONString(s, true));
			check_negative_test(251, tr, "\"abcd\tefgh\"", "parsing error, unexpected control character found", (s) -> new JSONString(s, true));
			check_negative_test(252, tr, "\"abcd\\u1zefgh\"", "parsing error, expecting one of the following characters: 0123456789abcdefABCDEF", (s) -> new JSONString(s, true));
			check_negative_test(253, tr, "\"abcd\\u", "parsing error, expecting one of the following characters: 0123456789abcdefABCDEF", (s) -> new JSONString(s, true));

	//testing JSONArray
			check_test(300, tr, "[]", JSONArray::new);
			check_test(301, tr, "[1,2.1,3e2]", "[1,2.1,3E+2]", JSONArray::new);
			check_test(302, tr, "    [   1   ,  true  ,  null  ]    ", "[1,true,null]", JSONArray::new);
			check_test(310, tr, "[[]]", JSONArray::new);
			check_test(311, tr, "[[],[[1,2,3],[false,true],[null]],[[]]]", JSONArray::new);
			check_test(312, tr, "[[],[[1,2,3],[false,true],[null]],{\"a\":{}}]", JSONArray::new);

			ArrayList<Object> al = new ArrayList<>(); al.add("123"); al.add(Boolean.TRUE);
			check_test_toJava(330, tr, "[\"123\" , true]", al, JSONArray::new);
			check_test_toJava(331, tr, "[\"123\" , true]", al, (s) -> new JSONArray(al));

			check_negative_test(350, tr, "[[]", "parsing error, expecting one of the following characters: ,]", JSONArray::new);
			check_negative_test(351, tr, "[,]", "parsing error", JSONArray::new);
			check_negative_test(352, tr, "[", "parsing error, expecting ]", JSONArray::new);

	//testing JSONObject
			check_test(400, tr, "{}", JSONObject::new);
			check_test(401, tr, "{\"a\":1,\"b\":2.1,\"c\":3e2}", "{\"a\":1,\"b\":2.1,\"c\":3E+2}", JSONObject::new);
			check_test(402, tr, "    {   \"a\":1   , \"b\": true  , \"c\": null  }    ", "{\"a\":1,\"b\":true,\"c\":null}", JSONObject::new);
			check_test(403, tr, "{\"a\u0040\":{}}", JSONObject::new);
			check_test(410, tr, "{\"a\":{}}", JSONObject::new);
			check_test(411, tr, "{\"a\":{},\"aa\":{\"a\":{\"a\":1,\"b\":2,\"c\":3},\"b\":{\"a\":false,\"b\":true},\"c\":{\"a\":null}},\"d\":{\"a\":{}}}", JSONObject::new);
			check_test(412, tr, "{\"a\":[[],[[1,2,3],[false,true],[null]],[[]]]}", JSONObject::new);

			LinkedHashMap<String, Object> lhm = new LinkedHashMap<>(); lhm.put("a", "123"); lhm.put("b", Boolean.TRUE);
			check_test_toJava(430, tr, "{a=123, b=true}", lhm, (s) -> new JSONObject("{\"a\":\"123\",\"b\":true}"));
			check_test_toJava(431, tr, "{a=123, b=true}", lhm, (s) -> new JSONObject(lhm));

			check_negative_test(450, tr, "{\"a\":{}", "parsing error, expecting one of the following characters: ,}", JSONObject::new);
			check_negative_test(451, tr, "{,}", "parsing error, expecting one of the following characters: \"", JSONObject::new);
			check_negative_test(452, tr, "{\"a\"}", "parsing error, expecting one of the following characters: :", JSONObject::new);
			check_negative_test(453, tr, "{\"a\":1,\"a\":2.1,\"c\":3e2}", "parsing error due duplicate key: \"a\"", JSONObject::new);
			check_negative_test(454, tr, "{", "parsing error, expecting }", JSONObject::new);

	//testing JSON parser
			check_test(1000, tr, "-1234.5678", JSONValue::parse);
			check_test(1001, tr, "1234", new JSONNumber("1234"), JSONValue::parse);
			check_test(1002, tr, "1234.5678", new JSONNumber("1234.5678"), JSONValue::parse);
			check_test(1003, tr, "true", JSONValue::parse);
			check_test(1004, tr, "true", new JSONBoolean(true), JSONValue::parse);
			check_test(1005, tr, "null", null, JSONValue::parse);
			check_test(1006, tr, "\"abcdefgh\"", JSONValue::parse);
			check_test(1007, tr, "\"abcd efgh\"", new JSONString("\"abcd efgh\"", true), JSONValue::parse);
			check_test(1008, tr, "[[],[[1,2,3],[false,true,\"alfa\"],[null]],[[]]]", JSONValue::parse);
			check_test(1009, tr, "[123]", new JSONArray("[123]"), JSONValue::parse);
			check_test(1010, tr, "{\"a\":1,\"b\":true,\"c\":null}", JSONValue::parse);
			check_test(1011, tr, "{\"a\":123}", new JSONObject("{\"a\":123}"), JSONValue::parse);
			check_test(1012, tr, "{\"a\":[[[1,2,3],[false,true],[null]],{\"a\":{}}]}", new JSONObject("{\"a\":[[[1,2,3],[false,true],[null]],{\"a\":{}}]}"), JSONValue::parse);
			check_test(1013, tr, "[[[[[[[[[1],2],3],4],5],6],7],8],9]", JSONValue::parse);
			check_test(1013, tr, "{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":1},\"b\":2},\"b\":3},\"b\":4},\"b\":5},\"b\":6},\"b\":7},\"b\":8},\"b\":9}", JSONValue::parse);

			check_negative_test(1050, tr, "", "parsing error", JSONValue::parse);
			check_negative_test(1051, tr, "alfa", "parsing error", JSONValue::parse);
			check_negative_test(1052, tr, "null true", "parsing error due to unexpected trailing characters", JSONValue::parse);
			check_negative_test(1053, tr, "1234 1234", "parsing error due to unexpected trailing characters", JSONValue::parse);
			check_negative_test(1054, tr, "1234 \"abcdefgh\"", "parsing error due to unexpected trailing characters", JSONValue::parse);
			check_negative_test(1055, tr, "[] []", "parsing error due to unexpected trailing characters", JSONValue::parse);
			check_negative_test(1056, tr, "{} {}", "parsing error due to unexpected trailing characters", JSONValue::parse);

			if (tr.n_failed > 0) System.out.println(tr.n_tests + " test performed with " + tr.n_failed + " test failed");
			else System.out.println(tr.n_tests + " test performed without any failures");
		}
    }

	public static void check_test(int num, testReport tr, String expected, TestCase testcase) {
		check_test(num, tr, expected, expected, testcase);
	}

/*
check_test(): check test case, note that 'expected' may be String or JSONValue
*/
	public static void check_test(int num, testReport tr, String test, Object expected, TestCase testcase) {
		try {
			tr.n_tests++;
			Object result = testcase.run(test);
			if (expected instanceof String)
				result = result.toString();
			if ((result != null || expected != null) && (result == null || !result.equals(expected))) {
				tr.n_failed++;
				System.out.println("test " + num + " failed, mismatch: " + result + " <-> " + expected);
			}
		} catch (JSONException ex) {
			tr.n_failed++;
			System.out.println("test " + num + " failed, exception: " + ex);			
		}
	}

/*
check_test_toJava(): check test case, note that 'expected' shall be one of the following Java objects: Boolean, String, ArrayList<Object>, LinkedHashMap<String, Object>
*/
	public static void check_test_toJava(int num, testReport tr, String test, Object expected, TestCase testcase) {
		try {
			tr.n_tests++;
			JSONValue result = testcase.run(test);
			if ((result != null || expected != null) && (result == null || !result.toJava().equals(expected))) {
				tr.n_failed++;
				System.out.println("test " + num + " failed, mismatch: " + result.toJava() + " <-> " + expected);
			}
		} catch (JSONException ex) {
			tr.n_failed++;
			System.out.println("test " + num + " failed, exception: " + ex);			
		}
	}

/*
check_negative_test(): check negative test case with JSONException
*/
	public static void check_negative_test(int num, testReport tr, String test, String expected_exception, TestCase testcase) {
		try {
			tr.n_tests++;
			String result = testcase.run(test).toString();
			System.out.println("test " + num + " failed, no exception, result: " + result);
		} catch (JSONException ex) {
			String result = ex.getMessage();
			if (!result.equals(expected_exception)) {
				tr.n_failed++;
				System.out.println("test " + num + " failed, mismatch: " + result + " <-> " + expected_exception);			
			}
		}
	}

/*
readfile(): read utf-8 file into string
*/
	public static String readfile(String filename) throws IOException {
		if (filename == null)
			return null;
		LineNumberReader in = new LineNumberReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
		String st;
		StringBuilder sb = new StringBuilder();
		while ((st = in.readLine()) != null) {
			sb.append(st).append('\n');
		}
		in.close();
		return sb.toString();
	}

/*
writefile(): write string into utf-8 file
*/
	public static void writefile(String filename, String str) throws IOException {
		PrintWriter output = new PrintWriter(filename, "UTF-8");
		output.print(str);
		output.close();
	}

}
