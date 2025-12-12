package json;

public final class JSONException extends Exception {
    public JSONException(Exception e) {
        super(e);
    }

    public JSONException(String s) {
        super(s);
    }
}
