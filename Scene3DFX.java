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
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import json.JSONException;
import json.JSONObject;
import json.JSONValue;

import simple3d.Engine3D.Direction;

import simple3d.*;
/**
 * Scene3DFX
 * This class implements a demo of the simple3D software renderer using JavaFX.
 *
 * Keyboard controls for camera movement using arrows(up/down/left/right) and shift key are included.
 * Shift + up arrow: move camera higher; Shift + down arrow: move camera lower
 * Shift + right arrow: rotate camera right; Shift + left arrow: rotate camera left
 *
 * v1.0, 23-12-2025: Scene3DFX first release
 * v1.0.2 26-12-2025: added flag useGouraud, note that when this flag is true, anti-alias is not working due to PixelWriter
 */

public class Scene3DFX extends Application {

	private final static boolean useGouraud = false;

    private final static int WIDTH = 1000;
    private final static int HEIGHT = 500;

	private final static boolean print_statistics = true;

	private final static double FOV = Math.toRadians(60); // Field of View
    private final static double ASPECT_RATIO = (double) HEIGHT / WIDTH;

	private double cameraYaw = 0.0; // Rotation around Y-axis (left/right)
//        private double cameraPitch = 0.0; // Rotation around X-axis (up/down)
    private final double moveSpeed = 0.5;

	Color skyColor = Color.rgb(130, 210, 230);// Sky color
	Color groundColor = Color.rgb(140, 60, 20);// Ground level color

	private Engine3D engine;
    private Canvas canvas;

	// Statistics
	private final float[] timings = {-1, 0}; //min and max rendering time in ms
	private int n_frames = 0;//number of frames
	private float acc_render_time = 0;//cumulative render time

    @Override
    public void start(Stage primaryStage) {
		List<String> args = getParameters().getRaw();

		engine = new Engine3D(print_statistics);
		if (args.size() > 0) {
			try	{
				JSONObject userdata = engine.importFile(args.get(0));
				if (userdata != null) {
					JSONValue _skyColor = userdata.get("skycolor");
					if (_skyColor != null) {
						skyColor = Color.web((String)_skyColor.toJava());
					}
					JSONValue _groundColor = userdata.get("groundcolor");
					if (_groundColor != null) {
						groundColor = Color.web((String)_groundColor.toJava());
					}
				}
			} catch (json.JSONException | IOException ex) {
				System.out.println("Unable to load file: " + ex);
				System.exit(0);
			}
		} else {
			final Light3D light = new Light3D(new simple3d.Color(255, 255, 255), 10, 20, -10);
			engine.setLight(light);
			engine.setCameraPos(new Vector3D(0, 1.7, -10));
			buildWorld(engine);
		}

        engine.setupScene(FOV, ASPECT_RATIO);

        canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        BorderPane root = new BorderPane();
        root.setTop(buildMenuBar(primaryStage));
        root.setCenter(canvas);

        Scene scene = new Scene(root);
        
        handleInput(scene, gc);
/*TODO
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                render(gc);
            }
        }.start();
*/
        primaryStage.setTitle("3D Scene[JavaFX]");
        primaryStage.setScene(scene);
		primaryStage.sizeToScene();
        primaryStage.show();
		primaryStage.setResizable(false);

		render(gc);
    }

    // --- Main Renderer ---
	private void render(GraphicsContext gc) {
		gc.setImageSmoothing(true);//anti-alias, works only if useGouraud=false

		long t0 = System.nanoTime();

		int height = (int) canvas.getHeight();
		int width = (int) canvas.getWidth();

		// Horizon line
		int horizonY = height / 2;

		// Draw Ground
		if (groundColor != null) {
			gc.setFill(groundColor);
			gc.fillRect(0, horizonY, width, height);
		}

        // Draw Sky with Gradient
		if (skyColor != null) {
			LinearGradient skyGradient = new LinearGradient(
				0, 0, 0, horizonY, false, CycleMethod.NO_CYCLE,
				new Stop(0, skyColor),
				new Stop(1, skyColor.deriveColor(0, 1, 1.2, 1)) // Equivalent to .brighter()
			);
			gc.setFill(skyGradient);
			gc.fillRect(0, 0, width, horizonY);
		}

        // Render 3D Scene via Engine
        engine.render3D(cameraYaw, (projectedVertices, poly) -> {
            int n = projectedVertices.size();
            double[] xPoints = new double[n];
            double[] yPoints = new double[n];

			int i = 0;
            for (Engine3D.ClippedVertex cv : projectedVertices) {
				Vector3D v = cv.clipped;
				// X/Y are now in NDC space (-1 to +1). Scale to screen size.
                xPoints[i] = (v.x + 1) * 0.5 * width;
				// Note the y-flip for screen coordinates (y-down in AWT/Swing)
                yPoints[i] = (1.0 - v.y) * 0.5 * height;
				i++;
            }

			if (useGouraud)	{
	// Draw filled polygon with Gouraud shaded color
				simple3d.Color[] colors = engine.getVertexShaderColor(projectedVertices, poly);
				if (colors == null) {
				// fall back: draw filled polygon with flat shaded color
					simple3d.Color color = engine.getFlatShaderColor(poly);
					gc.setFill(Color.rgb(color.getRed(), color.getGreen(), color.getBlue()));
					gc.fillPolygon(xPoints, yPoints, n);
				} else {
					Vertex[] vertices = new Vertex[n];
					for (int j = 0; j < n; j++)	{
						vertices[j] = new Vertex(xPoints[j], yPoints[j], Color.rgb(colors[j].getRed(), colors[j].getGreen(), colors[j].getBlue()));
					}
					gradientFillPolygon(canvas, vertices, height, width);
				}
			} else {
	// Draw filled polygon with flat shaded color
				simple3d.Color color = engine.getFlatShaderColor(poly);
				gc.setFill(Color.rgb(color.getRed(), color.getGreen(), color.getBlue()));
				gc.fillPolygon(xPoints, yPoints, n);
			}	
		});
		if (print_statistics) {
			float delta_ms = (System.nanoTime() - t0) / 1000000f;
			acc_render_time += delta_ms; n_frames++;
			if (timings[0] == -1) {//undef
				timings[0] = delta_ms;
				timings[1] = delta_ms;
			} else if (delta_ms > timings[1])
				timings[1] = delta_ms;
			else if (delta_ms < timings[0])
				timings[0] = delta_ms;

			System.out.println("rendering time, min = " + timings[0] + " ms, current = " + delta_ms + " ms, max = " + timings[1] + " ms");
			System.out.println("number of frames = " + n_frames + " average rendering time = " + acc_render_time / n_frames + " ms");
			Vector3D cameraPos = engine.getCameraPos();
			System.out.println("current camera position = " + cameraPos.x + ", "+ cameraPos.y + "," + cameraPos.z);
		}
    }

	private class Vertex {
		double x, y;
		Color color;

		Vertex(double x, double y, Color color) {
			this.x = x;
			this.y = y;
			this.color = color;
		}
	}

	private static void gradientFillPolygon(Canvas canvas, Vertex[] vertices, int height, int width) {
		PixelWriter pw = canvas.getGraphicsContext2D().getPixelWriter();
		Vertex pivot = vertices[0];
		for (int i = 1; i < vertices.length - 1; i++) {
			drawTriangle(pw, pivot, vertices[i], vertices[i + 1], height, width);
		}
	}

	private static void drawTriangle(PixelWriter pw, Vertex v0, Vertex v1, Vertex v2, int height, int width) {

		int minX = (int) Math.floor(Math.min(v0.x, Math.min(v1.x, v2.x)));
		int maxX = (int) Math.ceil(Math.max(v0.x, Math.max(v1.x, v2.x)));
		int minY = (int) Math.floor(Math.min(v0.y, Math.min(v1.y, v2.y)));
		int maxY = (int) Math.ceil(Math.max(v0.y, Math.max(v1.y, v2.y)));

		double denom = (v1.y - v2.y) * (v0.x - v2.x) + (v2.x - v1.x) * (v0.y - v2.y);

		for (int y = Math.max(0, minY); y <= Math.min(maxY, height); y++) {
			for (int x = Math.max(0, minX); x <= Math.min(maxX, width); x++) {

				double w1 = ((v1.y - v2.y) * (x - v2.x) + (v2.x - v1.x) * (y - v2.y)) / denom;
				double w2 = ((v2.y - v0.y) * (x - v2.x) + (v0.x - v2.x) * (y - v2.y)) / denom;
				double w3 = 1 - w1 - w2;

				if (w1 >= 0 && w2 >= 0 && w3 >= 0) {
					Color interpolatedColor = v0.color.interpolate(v1.color, w2 / (w1 + w2)).interpolate(v2.color, w3);
					pw.setColor(x, y, interpolatedColor);
				}
			}
		}
	}


// -- setup Menu Bar --
    private MenuBar buildMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();
		menuBar.getMenus().add(buildFileMenu(stage));
		menuBar.getMenus().add(buildHelpMenu(stage));
        return menuBar;
    }

	private File create_fc(Stage stage, boolean save) {
        String userDirLocation = System.getProperty("user.dir");
        File userDir = new File(userDirLocation);
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(userDir);
		fileChooser.getExtensionFilters().addAll(
			new FileChooser.ExtensionFilter("Scene3D Files", "*.gz")
		);
		File selectedFile = save ? fileChooser.showSaveDialog(stage) : fileChooser.showOpenDialog(stage);
		return selectedFile;
	}

// -- File Menu --
    protected Menu buildFileMenu(Stage stage) {
		Menu fileMenu = new Menu("File");
		MenuItem open = new MenuItem("Open...");
		MenuItem save = new MenuItem("Save");
		MenuItem saveas = new MenuItem("Save As...");
		MenuItem quit = new MenuItem("Quit");
// Begin "Open"
		open.setOnAction(e -> {
			File file = create_fc(stage, false);
			if (file != null) {
				try {
					String fname = file.getPath();
					skyColor = Color.rgb(130, 210, 230);// Sky color
					groundColor = Color.rgb(140, 60, 20);// Ground level color
					Engine3D new_engine = new Engine3D(print_statistics);
					JSONObject userdata = new_engine.importFile(fname);
					if (userdata != null) {
						JSONValue _skyColor = userdata.get("skycolor");
						if (_skyColor != null) {
							skyColor = Color.web((String)_skyColor.toJava());
						}
						JSONValue _groundColor = userdata.get("groundcolor");
						if (_groundColor != null) {
							groundColor = Color.web((String)_groundColor.toJava());
						}
					}
					engine = new_engine;
			        engine.setupScene(FOV, ASPECT_RATIO);
					render(canvas.getGraphicsContext2D());
				}	catch (json.JSONException | IOException ex) {
					showAlert("Open failed", ex.getMessage(), Alert.AlertType.ERROR);
				}   catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
// End "Open"

// Begin "Save"
		save.setOnAction(e -> {
		   try {
				savefile(engine.getFileName());
		   } catch (IOException ex) {
				showAlert("Save failed", ex.getMessage(), Alert.AlertType.ERROR);
		   }
		});
// End "Save"

// Begin "SaveAs"
		saveas.setOnAction(e -> {
			File file = create_fc(stage, true);
			if (file != null) {
				try {
					String fname = file.getPath();
					if (!fname.endsWith(".gz"))
						fname += ".gz";
					savefile(fname);
				} catch (IOException ex) {    
					showAlert("Save failed", ex.getMessage(), Alert.AlertType.ERROR);
				}
			}
		});
// End "SaveAs"

// Begin "Quit"
		quit.setOnAction(e -> {
			Platform.exit();
		});
// End "Quit"

		SeparatorMenuItem separator = new SeparatorMenuItem();
		fileMenu.getItems().addAll(open, save, saveas, separator, quit);
		return fileMenu;
    }

	protected void savefile(String filename) throws IOException {
		if (skyColor != null || groundColor != null) {
			LinkedHashMap<String, Object> userdata = new LinkedHashMap<>();
			if (skyColor != null)
				userdata.put("skycolor", colorToString(skyColor));
			if (groundColor != null)
				userdata.put("groundcolor", colorToString(groundColor));
			engine.exportFile(filename, new JSONObject(userdata));					
		} else engine.exportFile(filename);
	}

	private static String colorToString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255)
        );
    }


// -- Help Menu --
    private Menu buildHelpMenu(Stage stage) {
        Menu helpMenu = new Menu("Help");

        MenuItem helpTopics = new MenuItem("Help Topics...");
        helpTopics.setOnAction(e -> showAlert("Help", 
            "Use Keyboard controls to move camera.\nUp arrow: move camera forward; Down arrow: move camera backward.\nLeft arrow: move camera strafe left; Right arrow: move camera strafe right.\nShift + up arrow: move camera higher; Shift + down arrow: move camera lower\nShift + right arrow: rotate camera right; Shift + left arrow: rotate camera left", Alert.AlertType.INFORMATION));

        MenuItem info = new MenuItem("Info...");
        info.setOnAction(e -> showAlert("Information", engine.getInfo(false), Alert.AlertType.INFORMATION));

        helpMenu.getItems().addAll(helpTopics, info);
		return helpMenu;
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void handleInput(Scene scene, GraphicsContext gc) {
        scene.setOnKeyPressed(e -> {
			KeyCode ecode = e.getCode();
            if (ecode == KeyCode.UP) {
                // Move Forward / Up
				engine.updateCamera(Direction.UP, e.isShiftDown(), cameraYaw, moveSpeed);
			} else if (ecode == KeyCode.DOWN) {
                // Move Backward / Down
				engine.updateCamera(Direction.DOWN, e.isShiftDown(), cameraYaw, moveSpeed);
			} else if (ecode == KeyCode.LEFT) {
                // Strafe/Rotate Left
				if (e.isShiftDown())
					cameraYaw -= Math.toRadians(5); // Rotate 5 degrees
				else engine.updateCamera(Direction.LEFT, false, cameraYaw, moveSpeed);
			} else if (ecode == KeyCode.RIGHT) {
                // Strafe/Rotate Right
				if (e.isShiftDown())
					cameraYaw += Math.toRadians(5); // Rotate 5 degrees
				else engine.updateCamera(Direction.RIGHT, false, cameraYaw, moveSpeed);
            }
			render(gc);
        });
    }

	private static Polygon3D addColorIndex(Polygon3D face, Integer colorIndex) {
		face.setColorIndex(colorIndex);
		return face;
	}

	private static void buildWorld(Engine3D engine) {
		List<Node> sceneNodes = engine.getSceneNodes();

		// Define meshes

		// Vertices
		Vector3D[] vertices = new Vector3D[8];
		vertices[0] = new Vector3D(-0.5, 0, -0.5);
		vertices[1] = new Vector3D(-0.5, 0, 0.5);
		vertices[2] = new Vector3D(0.5, 0, 0.5);
		vertices[3] = new Vector3D(0.5, 0, -0.5);
		vertices[4] = new Vector3D(-0.5, 1, -0.5);
		vertices[5] = new Vector3D(-0.5, 1, 0.5);
		vertices[6] = new Vector3D(0.5, 1, 0.5);
		vertices[7] = new Vector3D(0.5, 1, -0.5);

		//Polygons
		List<Polygon3D> polygons = new ArrayList<>();
		polygons.add(addColorIndex(new Polygon3D(null, 3, 2, 1, 0), 0)); // Square
		polygons.add(new Polygon3D(null, 4, 5, 6, 7)); // Square
		polygons.add(addColorIndex(new Polygon3D(null, 0, 1, 5, 4), 0)); // Square
		polygons.add(new Polygon3D(null, 1, 2, 6, 5)); // Square
		polygons.add(addColorIndex(new Polygon3D(null, 2, 3, 7, 6), 0)); // Square
		polygons.add(new Polygon3D(null, 0, 4, 7, 3)); // Square

		engine.addMesh(Mesh.createMesh("mycube", vertices, polygons));

		//Extruded Mesh
		engine.addMesh(Mesh.extrudePolygonMesh("extruded", Mesh.getShapeInstance("regularPolygon", "{\"N\":6}"), 1.0));


		// Plane
		Node base = new Node("plane", Mesh.Shape.square, simple3d.Color.GRAY);
		base.applyScale(20, 1, 20);
		base.applyTranslation(0, 0, 5);
		sceneNodes.add(base);

		// regular polygons, example related to shapeArguments
		try {
			for (int k = 3; k < 9; k++) {
				simple3d.Color[] colors = {simple3d.Color.MAGENTA, simple3d.Color.RED, simple3d.Color.YELLOW, simple3d.Color.GREEN, simple3d.Color.CYAN, simple3d.Color.BLUE};
				Node poly = new Node("poly" + k, Mesh.Shape.regularPolygon, new JSONObject("{\"N\":"+ k + "}"), colors[k - 3]);
				poly.applyRotationX(Math.toRadians(-90));
				poly.applyScale(8.0 / k, 8.0 / k, 1);
				poly.applyTranslation(-16.5 + k * 3, 3, 5);
				sceneNodes.add(poly);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		// Pyramids
		Node pyr1 = new Node("pyr1", Mesh.Shape.pyramid, simple3d.Color.parse("#009090")); // Teal
		pyr1.applyScale(1, 2, 1);
		pyr1.applyTranslation(0, 0, 5);
		sceneNodes.add(pyr1);

		Node pyr2 = new Node("pyr2", Mesh.Shape.pyramid, simple3d.Color.ORANGE);
		pyr2.applyScale(1, 2, 1);
		pyr2.applyTranslation(5, 0, 0);
		sceneNodes.add(pyr2);

		// Cubes
		Node cube = new Node("cube", Mesh.Shape.cube, simple3d.Color.RED);
		cube.applyTranslation(2, 0, 0);
		cube.applyScale(1, 3, 1);
		sceneNodes.add(cube);

		Node cube2 = new Node("cube2", "mycube", simple3d.Color.GREEN);
		cube2.setColorList(new simple3d.Color[]{simple3d.Color.BLUE});
		cube2.applyTranslation(-2, 1, 0);
		cube2.applyRotationX(Math.toRadians(30));
		cube2.applyRotationY(Math.toRadians(45));
		cube2.applyRotationZ(Math.toRadians(60));
		sceneNodes.add(cube2);

		// cones
		Node cone = new Node("cone", Mesh.Shape.cone, simple3d.Color.BLUE);
		cone.applyScale(1, 2, 1);
		cone.applyTranslation(-5, 0, 0);
		sceneNodes.add(cone);
		Node cone2 = new Node("cone2", Mesh.Shape.cone, simple3d.Color.CYAN);
		cone2.applyRotationX(Math.toRadians(180));
		cone2.applyScale(1, 2, 1);
		cone2.applyTranslation(-5, 4, 0);
		sceneNodes.add(cone2);

		// cylinder
		Node cylinder = new Node("cylinder", Mesh.Shape.cylinder, simple3d.Color.RED);
		cylinder.applyTranslation(-5, 0, 5);
		sceneNodes.add(cylinder);
		

		// sphere
		Node sphere = new Node("sphere", Mesh.Shape.sphere, simple3d.Color.MAGENTA);
		sphere.applyTranslation(5, 0, 5);
		sceneNodes.add(sphere);
		

		// Extruded
		Node extruded = new Node("extruded1", "extruded", simple3d.Color.RED);
		extruded.applyScale(0.5, 0.5, 0.5);
		extruded.applyTranslation(-2, 1, 0);
		sceneNodes.add(extruded);

		Node extruded2 = new Node("extruded2", "extruded", simple3d.Color.GREEN);
		extruded2.applyScale(0.5, 0.5, 0.5);
		extruded2.applyTranslation(-2, 0.5, 0);
		sceneNodes.add(extruded2);

		Node extruded3 = new Node("extruded3", "extruded", simple3d.Color.BLUE);
		extruded3.applyScale(0.5, 0.5, 0.5);
		extruded3.applyTranslation(-2, 0, 0);
		sceneNodes.add(extruded3);

	}

}