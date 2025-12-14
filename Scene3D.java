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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.*;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.UIManager;

import json.JSONObject;
import json.JSONValue;

import simple3d.Engine3D.Direction;

import simple3d.*;
/**
 * Scene3D
 * This class implements a demo of the simple3D software renderer using Java Swing.
 *
 * Keyboard controls for camera movement using arrows(up/down/left/right) and shift key are included.
 * Shift + up arrow: move camera higher; Shift + down arrow: move camera lower
 * Shift + right arrow: rotate camera right; Shift + left arrow: rotate camera left
 *
 * To run: Save as Scene3D.java, compile, and run:
 * javac Scene3D.java
 * java Scene3D
 *
 * v1.0, 12-12-2025: first release
 * v1.0.1, 14-12-2025: added menu bar for file handling and help
 */

public class Scene3D extends JFrame {

    private final static int WIDTH = 1000;
    private final static int HEIGHT = 500;

	private final static boolean print_statistics = true;

	private Engine3D engine;
    private final Renderer canvas;

	private final static double FOV = Math.toRadians(60); // Field of View

    public Scene3D(String title, Engine3D engine, Color skyColor, Color groundColor) {
		this.engine = engine;

		setTitle(title);
		setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
		setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setupMenuBar(); fc = create_fc();

        // Setup the rendering canvas
        canvas = new Renderer(WIDTH, HEIGHT, this, skyColor, groundColor);
        add(canvas, BorderLayout.CENTER);
        pack(); 

        // Set focus to the canvas for keyboard input
        canvas.setFocusable(true);
        canvas.requestFocusInWindow();

        setVisible(true);
    }

// -- setup Menu Bar --
	private void setupMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(buildFileMenu());
		menuBar.add(buildHelpMenu());
		setJMenuBar(menuBar);	
	}

	private static JFileChooser fc = null;
	private JFileChooser create_fc() {
        String userDirLocation = System.getProperty("user.dir");
        File userDir = new File(userDirLocation);
		JFileChooser fc = new JFileChooser(userDir);
		fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
			public String getDescription() { return "gz files"; }
			public boolean accept(File f) {
				return f.getName().toLowerCase().endsWith (".gz") || f.isDirectory ();
			}
		});
		return fc;
	}

// -- File Menu --
    protected JMenu buildFileMenu() {
		JMenu file = new JMenu("File");
		JMenuItem open = new JMenuItem("Open...");
		JMenuItem save = new JMenuItem("Save");
		JMenuItem saveas = new JMenuItem("Save As...");
		JMenuItem quit = new JMenuItem("Quit");

// Begin "Open"
		open.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showOpenDialog(Scene3D.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
					try	{ 	
						File file = fc.getSelectedFile();
						String fname = file.getPath();
						Color skyColor = new Color(130, 210, 230);// Sky color
						Color groundColor = new Color(140, 60, 20);// Ground level color
						Engine3D new_engine = new Engine3D(print_statistics);
						JSONObject userdata = new_engine.importFile(fname);
						if (userdata != null) {
							JSONValue _skyColor = userdata.get("skycolor");
							if (_skyColor != null) {
								skyColor = colorParser((String)_skyColor.toJava());
							}
							JSONValue _groundColor = userdata.get("groundcolor");
							if (_groundColor != null) {
								groundColor = colorParser((String)_groundColor.toJava());
							}
						}
						engine = new_engine;
						canvas.setup(skyColor, groundColor);
						canvas.repaint();
					}	catch (json.JSONException | IOException ex) {
						JOptionPane.showMessageDialog(Scene3D.this, ex.getMessage(),"Open failed",JOptionPane.ERROR_MESSAGE);
					}   catch (Exception ex) {
						ex.printStackTrace();
					}
                }
		   }
		});
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
// End "Open"

// Begin "Save"
		save.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
				   try {
						savefile(engine.getFileName());
				   } catch (IOException ex) {    
						JOptionPane.showMessageDialog(Scene3D.this, ex.getMessage(),"Save failed",JOptionPane.ERROR_MESSAGE);
				   }
		   }
		});
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
// End "Save"

// Begin "SaveAs"
		saveas.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showSaveDialog(Scene3D.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
					try {
						String fname = fc.getSelectedFile().getPath();
						if (!fname.endsWith(".gz"))
							fname += ".gz";
						savefile(fname);
					} catch (IOException ex) {    
						JOptionPane.showMessageDialog(Scene3D.this, ex.getMessage(),"Save failed",JOptionPane.ERROR_MESSAGE);
					}
				}
		   }
		});
// End "SaveAs"

// Begin "Quit"
		quit.addActionListener(new ActionListener() {
		   public void actionPerformed(ActionEvent e) {
			   Scene3D.this.dispose();
		   }
		});
// End "Quit"

		file.add(open);
		file.addSeparator();
		file.add(save);
		file.add(saveas);
		file.addSeparator();
		file.add(quit);
		return file;
    }

	protected void savefile(String filename) throws IOException {
		Color skyColor = canvas.getSkyColor();
		Color groundColor = canvas.getGroundColor();
		if (skyColor != null || groundColor != null) {
			LinkedHashMap<String, Object> userdata = new LinkedHashMap<>();
			if (skyColor != null)
				userdata.put("skycolor", colorToString(skyColor));
			if (groundColor != null)
				userdata.put("groundcolor", colorToString(groundColor));
			engine.exportFile(filename, new JSONObject(userdata));					
		} else engine.exportFile(filename);
	}

// -- Help Menu --
	protected JMenu buildHelpMenu() {
		JMenu help = new JMenu("Help");
		JMenuItem openHelp = new JMenuItem("Help Topics...");
		JMenuItem openInfo = new JMenuItem("Info...");

		openHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(
					Scene3D.this,
					"Use Keyboard controls to move camera.\nUp arrow: move camera forward; Down arrow: move camera backward.\nLeft arrow: move camera strafe left; Right arrow: move camera strafe right.\nShift + up arrow: move camera higher; Shift + down arrow: move camera lower\nShift + right arrow: rotate camera right; Shift + left arrow: rotate camera left",
					"Help",
					JOptionPane.INFORMATION_MESSAGE
				);
			}
		});
		openInfo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(
					Scene3D.this,
					engine.getInfo(false),
					"Information",
					JOptionPane.INFORMATION_MESSAGE
				);
			}
		});

		help.add(openHelp);
		help.add(openInfo);
		return help;
	}

    // --- Main Renderer Panel Class ---
    private class Renderer extends JPanel implements KeyListener {
		final Scene3D scene3D;
        // Scene constants
        private final static double ASPECT_RATIO = (double) WIDTH / HEIGHT;

        private double cameraYaw = 0.0; // Rotation around Y-axis (left/right)
//        private double cameraPitch = 0.0; // Rotation around X-axis (up/down)
        private final double moveSpeed = 0.5;

		private final Polygon screenPoly = new Polygon(); // Reusable AWT Polygon to offload paint()
		private Color skyColor;
		private Color groundColor;

		// Statistics
		private final float[] timings = {-1, 0}; //min and max rendering time in ms
		private int n_frames = 0;//number of frames
		private float acc_render_time = 0;//cumulative render time

        public Renderer(int width, int height, Scene3D scene3D, Color skyColor, Color groundColor) {
            setPreferredSize(new Dimension(width, height));
			addKeyListener(this);
			this.scene3D = scene3D;
			setup(skyColor, groundColor);
        }

		protected void setup(Color skyColor, Color groundColor) {
			if (groundColor != null) {
				setBackground(groundColor);
			}
			this.groundColor = groundColor;
			this.skyColor = skyColor;
			engine.setupScene(FOV, ASPECT_RATIO);
		}

		protected Color getSkyColor() {
			return skyColor;
		}
		protected Color getGroundColor() {
			return groundColor;
		}

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // This clears the background and handles the automatic double buffering swap

			long t0 = System.nanoTime();

			// Horizon line
			int horizonY = getHeight() / 2;

			// Draw the Sky
			if (skyColor != null) {
				g.setColor(skyColor);
				g.fillRect(0, 0, getWidth(), horizonY);
			}

			// Render scene
			engine.render3D(cameraYaw, getWidth(), getHeight(), (projectedVertices, color) -> {
				screenPoly.reset(); // Reset and reuse the pre-allocated AWT Polygon
				for (Vector3D v : projectedVertices)
					screenPoly.addPoint((int) v.x, (int) v.y);

				// Draw filled polygon with flat shaded color
				g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
				g.fillPolygon(screenPoly);
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
        
        // --- KeyListener Implementation for Camera Controls ---
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                // Move Forward / Up
                case KeyEvent.VK_UP:
					engine.updateCamera(Direction.UP, e.isShiftDown(), cameraYaw, moveSpeed);
                    break;
                // Move Backward / Down
                case KeyEvent.VK_DOWN:
					engine.updateCamera(Direction.DOWN, e.isShiftDown(), cameraYaw, moveSpeed);
                    break;
                // Strafe/Rotate Left
                case KeyEvent.VK_LEFT:
					if (e.isShiftDown())
	                    cameraYaw -= Math.toRadians(5); // Rotate 5 degrees
	                else engine.updateCamera(Direction.LEFT, false, cameraYaw, moveSpeed);
                    break;
                // Strafe/Rotate Right
                case KeyEvent.VK_RIGHT:
					if (e.isShiftDown())
	                    cameraYaw += Math.toRadians(5); // Rotate 5 degrees
                    else engine.updateCamera(Direction.RIGHT, false, cameraYaw, moveSpeed);
                    break;
            }
            // Trigger repaint on movement
            repaint();
        }

        @Override public void keyTyped(KeyEvent e) {}
        @Override public void keyReleased(KeyEvent e) {}
    }

    private static Color colorParser(String string) {
		int rgb = Integer.parseInt(string.trim().substring(1), 16);//remove leading #
		return new Color(rgb);
	}

    private static String colorToString(Color color) {
        return String.format("#%06X", color.getRGB() & 0xFFFFFF);
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
		

		// Plane
		Node base = new Node("plane", Mesh.Shape.square, simple3d.Color.GRAY);
		base.applyScale(20, 1, 20);
		base.applyTranslation(0, 0, 5);
		sceneNodes.add(base);
	}

	public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Set system look and feel for a more native appearance (optional)
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.out.println("Could not set system look and feel.");
            }

			Color skyColor = new Color(130, 210, 230);// Sky color
			Color groundColor = new Color(140, 60, 20);// Ground level color

            final Light3D light = new Light3D(new simple3d.Color(255, 255, 255), 10, 20, -10);
			Engine3D engine = new Engine3D(print_statistics);
			if (args.length > 0) {
				try	{
					JSONObject userdata = engine.importFile(args[0]);
					if (userdata != null) {
						JSONValue _skyColor = userdata.get("skycolor");
						if (_skyColor != null) {
							skyColor = colorParser((String)_skyColor.toJava());
						}
						JSONValue _groundColor = userdata.get("groundcolor");
						if (_groundColor != null) {
							groundColor = colorParser((String)_groundColor.toJava());
						}
					}
				} catch (json.JSONException | IOException ex) {
					System.out.println("Unable to load file: " + ex);
					System.exit(1);
				}
			} else {
				engine.setLight(light);
				engine.setCameraPos(new Vector3D(0, 1.7, -10));
				buildWorld(engine);
			}
			new Scene3D("3D scene", engine, skyColor, groundColor);
        });
    }
}