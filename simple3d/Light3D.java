package simple3d;

public class Light3D {
	final Vector3D lightPos;
	final Color color;

	public Light3D(Color color, double x, double y, double z) {
		this.color = color;
		this.lightPos = new Vector3D(x, y, z);
	}
}
