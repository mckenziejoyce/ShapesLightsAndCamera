//****************************************************************************
//       Main Program for CS480 PA4
//****************************************************************************
// Description:
//
//   This is a template program for the sketching tool.
//
//     LEFTMOUSE: draw line segments
//     RIGHTMOUSE: draw triangles
//
//     The following keys control the program:
//
//		Q,q: quit
//		C,c: clear polygon (set vertex count=0)
//		R,r: randomly change the color
//		S,s: toggle the smooth shading for triangle
//			 (no smooth shading by default)
//		T,t: show testing examples
//		>:	 increase the step number for examples
//		<:   decrease the step number for examples
//
//****************************************************************************
// History :
//   Aug 2004 Created by Jianming Zhang based on the C
//   code by Stan Sclaroff
//   Nov 2014 modified to include test cases
//   Nov 5, 2019 Updated by Zezhou Sun
//   Dec 2, 2019 Updated and modified by McKenzie Joyce to use as main program for PA4
//


import javax.swing.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.*;
import java.awt.image.*;
//import java.io.File;
//import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

//import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.awt.GLCanvas;//for new version of gl
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;

import com.jogamp.opengl.util.FPSAnimator;//for new version of gl


public class PA4 extends JFrame
	implements GLEventListener, KeyListener, MouseListener, MouseMotionListener
{

	private static final long serialVersionUID = 1L;
	private final int DEFAULT_WINDOW_WIDTH=512;
	private final int DEFAULT_WINDOW_HEIGHT=512;
	private final float DEFAULT_LINE_WIDTH=1.0f;

	private GLCapabilities capabilities;
	private GLCanvas canvas;
	private FPSAnimator animator;

	final private int numTestCase;
	private int testCase;
	private BufferedImage buff;
	//@SuppressWarnings("unused")
	private ColorType color;
	private Random rng;

	// specular exponent for materials
	private int n_s=5;

	private ArrayList<Point2D> lineSegs;
	private ArrayList<Point2D> triangles;
	private boolean gouraudRender;
	private boolean phongRender;
	private boolean flatRender;
	private int Nsteps;

	/** The quaternion which controls the rotation of the world. */
    private Quaternion viewing_quaternion = new Quaternion();
    private Point3D viewing_center = new Point3D((float)(DEFAULT_WINDOW_WIDTH/2),(float)(DEFAULT_WINDOW_HEIGHT/2),(float)0.0);
    /** The last x and y coordinates of the mouse press. */
    private int last_x = 0, last_y = 0;
    /** Whether the world is being rotated. */
    private boolean rotate_world = false;

    /** Random colors **/
    private ColorType[] colorMap = new ColorType[100];
    private Random rand = new Random();



    private boolean[] matTerms;
	private List<Light> lights;
	private boolean lightMade;
	private boolean lPressed;

	private float ambientFactor = 1;
	private float specularFactor = 1;
	private float diffuseFactor = 1;

	private float[][] depthBuffer;

	public PA4()
	{
	    capabilities = new GLCapabilities(null);
	    capabilities.setDoubleBuffered(true);  // Enable Double buffering

	    canvas  = new GLCanvas(capabilities);
	    canvas.addGLEventListener(this);
	    canvas.addMouseListener(this);
	    canvas.addMouseMotionListener(this);
	    canvas.addKeyListener(this);
	    canvas.setAutoSwapBufferMode(true); // true by default. Just to be explicit
	    canvas.setFocusable(true);
	    getContentPane().add(canvas);

	    animator = new FPSAnimator(canvas, 60); // drive the display loop @ 60 FPS

	    numTestCase = 3;
	    testCase = 0;
	    Nsteps = 12;

	    setTitle("CS480/680 Programming Assignment 4");
	    setSize( DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    setVisible(true);
	    setResizable(false);

	    rng = new Random();
	    color = new ColorType(1.0f,0.0f,0.0f);
	    lineSegs = new ArrayList<Point2D>();
	    triangles = new ArrayList<Point2D>();
	    gouraudRender = false;
	    phongRender = false;
	    flatRender = true;

	    for (int i=0; i<100; i++) {
	    	this.colorMap[i] = new ColorType(i*0.005f+0.5f, i*-0.005f+1f, i*0.0025f+0.75f);
	    }
	    matTerms = new boolean[3];
	}

	public void run()
	{
		animator.start();
	}

	public static void main( String[] args )
	{
	    PA4 P = new PA4();
	    P.run();
	}

	//***********************************************
	//  GLEventListener Interfaces
	//***********************************************
	public void init( GLAutoDrawable drawable)
	{
		lightMade = false;
		lPressed = false;


	    GL gl = drawable.getGL();
	    gl.glClearColor( 0.0f, 0.0f, 0.0f, 0.0f);
	    gl.glLineWidth( DEFAULT_LINE_WIDTH );
	    Dimension sz = this.getContentPane().getSize();
	    buff = new BufferedImage(sz.width,sz.height,BufferedImage.TYPE_3BYTE_BGR);
	    clearPixelBuffer();

	    Arrays.fill(matTerms, true);
	}

	// Redisplaying graphics
	public void display(GLAutoDrawable drawable)
	{
		setDepthBuffer();

	    GL2 gl = drawable.getGL().getGL2();
	    WritableRaster wr = buff.getRaster();
	    DataBufferByte dbb = (DataBufferByte) wr.getDataBuffer();
	    byte[] data = dbb.getData();

	    gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);
	    gl.glDrawPixels (buff.getWidth(), buff.getHeight(),
                GL2.GL_BGR, GL2.GL_UNSIGNED_BYTE,
                ByteBuffer.wrap(data));
        drawTestCase();
	}

	public void setDepthBuffer(){
		depthBuffer = new float[DEFAULT_WINDOW_WIDTH][DEFAULT_WINDOW_HEIGHT];
		for(int i = 0; i < depthBuffer.length; i++){
			for(int j = 0; j < depthBuffer[0].length; j++){
				depthBuffer[i][j] = -99999;
			}
		}
	}

	// Window size change
	public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
	{
		// deliberately left blank
	}
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged,
	      boolean deviceChanged)
	{
		// deliberately left blank
	}

	void clearPixelBuffer()
	{
		lineSegs.clear();
    	triangles.clear();
		Graphics2D g = buff.createGraphics();
	    g.setColor(Color.BLACK);
	    g.fillRect(0, 0, buff.getWidth(), buff.getHeight());
	    g.dispose();
	}

	// drawTest
	void drawTestCase()
	{
		/* clear the window and vertex state */
		clearPixelBuffer();

		//System.out.printf("Test case = %d\n",testCase);

		switch (testCase){
		case 0:
			sceneOne();
			break;
		case 1:
			sceneTwo();
			break;
		case 2:
			sceneThree();
			break;
		}
	}


	//***********************************************
	//          KeyListener Interfaces
	//***********************************************
	public void keyTyped(KeyEvent key)
	{
	//      Q,q: quit
	//      C,c: clear polygon (set vertex count=0)
	//		R,r: randomly change the color
	//		S,s: toggle the specular
    //      D,d: toggle the diffuser
	//      A,a: toggle the ambient
	//		T,t: show testing examples (toggles between smooth shading and flat shading test cases)
	//		>:	 increase the step number for examples
	//		<:   decrease the step number for examples
	//     +,-:  increase or decrease spectral exponent
	//     F,f: Flat surface rendering
	//     G,g: Gouraud rendering
	//	   L: Light Toggle Mode
	//			1 - Infinite Light On/Off
	//			2 - Ambient Light On/Off
	//          3 - Point Light On/Off
	//
	//

	    switch ( key.getKeyChar() )
	    {
	    case 'Q' :
	    case 'q' :
	    	new Thread()
	    	{
	          	public void run() { animator.stop(); }
	        }.start();
	        System.exit(0);
	        break;
	    case 'C' :
	    case 'c' :
	    	clearPixelBuffer();
	    	break;
	    case 'R' :
	    case 'r' :
	    	color = new ColorType(rng.nextFloat(),rng.nextFloat(),
	    			rng.nextFloat());
	    	break;
	    case 'S' :
		case 's' :
			matTerms[0] = !matTerms[0];
			break;
		case 'D' :
		case 'd' :
			matTerms[1] = !matTerms[1];
			break;
		case 'A' :
		case 'a' :
			matTerms[2] = !matTerms[2];
			break;
	    case 'T' :
	    case 't' :
	    	testCase = (testCase+1)%numTestCase;
	    	drawTestCase();
	        break;
	    case '<':
	        Nsteps = Nsteps < 4 ? Nsteps: Nsteps / 2;
	        System.out.printf( "Nsteps = %d \n", Nsteps);
	        drawTestCase();
	        break;
	    case '>':
	        Nsteps = Nsteps > 190 ? Nsteps: Nsteps * 2;
	        System.out.printf( "Nsteps = %d \n", Nsteps);
	        drawTestCase();
	        break;
	    case '+':
	    	n_s++;
	        drawTestCase();
	    	break;
	    case '-':
	    	if(n_s>0)
	    		n_s--;
	        drawTestCase();
	    	break;
	    case 'F':
		case 'f':
			if(flatRender == true) {
				break;
			}
			else {
				flatRender = true;
				gouraudRender = false;
				phongRender = false;
				drawTestCase();
				break;
			}
		case 'G':
		case 'g':
			if(gouraudRender == true) {
				break;
			}
			else {
				gouraudRender = true;
				flatRender = false;
				phongRender = false;
				drawTestCase();
				break;
			}
		case 'P':
		case 'p':
			if(phongRender == true) {
				break;
			}
			else {
				gouraudRender = false;
				flatRender = false;
				phongRender = true;
				drawTestCase();
				break;
			}
		case 'L':
			lPressed = !lPressed;
			break;
		case '1':
			if(lPressed) lights.get(0).flickerLight();
			break;
		case '2':
			if(lPressed) lights.get(1).flickerLight();
			break;
		case '3':
			if(lPressed) lights.get(2).flickerLight();
			break;
	    default :
	        break;
	    }
	}

	public void keyPressed(KeyEvent key)
	{
	    switch (key.getKeyCode())
	    {
	    case KeyEvent.VK_ESCAPE:
	    	new Thread()
	        {
	    		public void run()
	    		{
	    			animator.stop();
	    		}
	        }.start();
	        System.exit(0);
	        break;
	      default:
	        break;

	    }
	}

	public void keyReleased(KeyEvent key)
	{
		// deliberately left blank
	}

	//**************************************************
	// MouseListener and MouseMotionListener Interfaces
	//**************************************************
	public void mouseClicked(MouseEvent mouse)
	{
		// deliberately left blank
	}
	  public void mousePressed(MouseEvent mouse)
	  {
	    int button = mouse.getButton();
	    if ( button == MouseEvent.BUTTON1 )
	    {
	      last_x = mouse.getX();
	      last_y = mouse.getY();
	      rotate_world = true;
	    }
	  }

	  public void mouseReleased(MouseEvent mouse)
	  {
	    int button = mouse.getButton();
	    if ( button == MouseEvent.BUTTON1 )
	    {
	      rotate_world = false;
	    }
	  }

	public void mouseMoved( MouseEvent mouse)
	{
		// Deliberately left blank
	}

	/**
	   * Updates the rotation quaternion as the mouse is dragged.
	   *
	   * @param mouse
	   *          The mouse drag event object.
	   */
	  public void mouseDragged(final MouseEvent mouse) {
	    if (this.rotate_world) {
	      // get the current position of the mouse
	      final int x = mouse.getX();
	      final int y = mouse.getY();

	      // get the change in position from the previous one
	      final int dx = x - this.last_x;
	      final int dy = y - this.last_y;

	      // create a unit vector in the direction of the vector (dy, dx, 0)
	      final float magnitude = (float)Math.sqrt(dx * dx + dy * dy);
	      if(magnitude > 0.0001)
	      {
	    	  // define axis perpendicular to (dx,-dy,0)
	    	  // use -y because origin is in upper lefthand corner of the window
	    	  final float[] axis = new float[] { -(float) (dy / magnitude),
	    			  (float) (dx / magnitude), 0 };

	    	  // calculate appropriate quaternion
	    	  final float viewing_delta = 3.1415927f / 360.0f * magnitude;
	    	  final float s = (float) Math.sin(0.5f * viewing_delta);
	    	  final float c = (float) Math.cos(0.5f * viewing_delta);
	    	  final Quaternion Q = new Quaternion(c, s * axis[0], s * axis[1], s * axis[2]);
	    	  this.viewing_quaternion = Q.multiply(this.viewing_quaternion);

	    	  // normalize to counteract acccumulating round-off error
	    	  this.viewing_quaternion.normalize();

	    	  // save x, y as last x, y
	    	  this.last_x = x;
	    	  this.last_y = y;
	          drawTestCase();
	      }
	    }

	  }

	public void mouseEntered( MouseEvent mouse)
	{
		// Deliberately left blank
	}

	public void mouseExited( MouseEvent mouse)
	{
		// Deliberately left blank
	}


	public void dispose(GLAutoDrawable drawable) {
		// TODO Auto-generated method stub

	}

	//**************************************************
	// Test Cases
	// Nov 9, 2014 Stan Sclaroff -- removed line and triangle test cases
	//**************************************************

	//Function to apply all the lights in the scene
	public ColorType applyAllLights(List<Light> lights, Material mat, Point3D v, Point3D n, Point3D ps){
		Light l;
		ColorType res = new ColorType();
		// Temporarily holds RGB values
		ColorType temp = new ColorType();

		// Loop through all lights in the scene and if the light is on apply it to the rgb value
		for(int i = 0; i < lights.size(); i++){
			l = lights.get(i);

			if(l.lightOn){
				//Makes a call to apply light in the light class
				temp = l.applyLight(mat, v, n, ps);

				// adds up all color for each channel
				res.r += temp.r;
				res.g += temp.g;
				res.b += temp.b;
			}
		}

		// clamp so that allowable maximum illumination level is not exceeded
		res.clamp();

		return res;

	}


	// Creates all the lights in the scene - an infinite, ambient, and point returns a list of all the lights in the scene
	// which applyAllLights() uses
	public List<Light> makeLights() {
		lightMade = true;

		lights = new ArrayList<Light>();

		// Infinite light source, with color = white
		ColorType light_color = new ColorType(1.0f,1.0f,1.0f);
		Point3D light_direction = new Point3D((float)0.0,(float)(-1.0/Math.sqrt(2.0)),(float)(1.0/Math.sqrt(2.0)));
		Light light = new Light(light_color,light_direction, false, true);

		lights.add(light);

		// Ambient light source, with color = red
		light_color = new ColorType(1.0f,0.0f,0.0f);
		light_direction = new Point3D((float)0.0,(float)(-1.0/Math.sqrt(2.0)),(float)(1.0/Math.sqrt(2.0)));
		Light ambient = new Light(light_color,light_direction, true, false);

		lights.add(ambient);

		// Point light source, with color = blue
		light_color = new ColorType(0.0f,0.0f,1.0f);
		light_direction = new Point3D(0f, -1f, 1f);
		Point3D light_position = new Point3D(0f, 400f, 200f);
		Light spot = new Light(light_color,light_direction, light_position);

		lights.add(spot);

		return lights;
	}

	//Scene One : Sphere, torus, and ellipsoid
	void sceneOne() {
		float radius = (float)50.0;

		//Shapes for this scene
		Sphere3D sphere = new Sphere3D((float)128.0, (float)128.0, (float)128.0, (float)1.5*radius, Nsteps, Nsteps);
        Torus3D torus = new Torus3D(250.0f, 250.0f, 250.0f, 0.8f*radius, 1.25f*radius, Nsteps, Nsteps);
        Ellipsoid3D ellipsoid = new Ellipsoid3D(350f, 100f, 50f, 1.5f*radius, radius, radius, Nsteps, Nsteps);

        //View Vector for this scene
        Point3D view_vector = new Point3D((float)0.0,(float)0.0,(float)1.0);

        // Create material properties for the shapes

        // Ambient coefficients of reflection
        ColorType torus_k_a = new ColorType(0.3f*ambientFactor,0.3f*ambientFactor,0.3f*ambientFactor);
        ColorType sphere_k_a = new ColorType(1.0f*ambientFactor,1.0f*ambientFactor,1.0f*ambientFactor);
//        ColorType torus_k_a = new ColorType(1.0f*ambientFactor,1.0f*ambientFactor,1.0f*ambientFactor);
//        ColorType sphere_k_a = new ColorType(1.0f*ambientFactor,1.0f*ambientFactor,1.0f*ambientFactor);
		ColorType ellipsoid_k_a = new ColorType(1.0f*ambientFactor,1.0f*ambientFactor,1.0f*ambientFactor);

		// Diffuse coefficients of reflection
		ColorType torus_k_d = new ColorType(0.0f*diffuseFactor,0.5f*diffuseFactor,0.9f*diffuseFactor);
		ColorType sphere_k_d = new ColorType(0.9f*diffuseFactor,0.3f*diffuseFactor,0.1f*diffuseFactor);
//		ColorType torus_k_d = new ColorType(0.1f*diffuseFactor,0.9f*diffuseFactor,0.01f*diffuseFactor);
//		ColorType sphere_k_d = new ColorType(0.1f*diffuseFactor,0.9f*diffuseFactor,0.01f*diffuseFactor);
		ColorType ellipsoid_k_d = new ColorType(0.1f*diffuseFactor,0.9f*diffuseFactor,0.01f*diffuseFactor);

		// Specular coefficients of reflection
//		ColorType torus_k_s = new ColorType(1.0f*specularFactor,1.0f*specularFactor,1.0f*specularFactor);
//		ColorType sphere_k_s = new ColorType(1.0f*specularFactor,1.0f*specularFactor,1.0f*specularFactor);
		ColorType torus_k_s = new ColorType(1.0f*specularFactor,1.0f*specularFactor,1.0f*specularFactor);
		ColorType sphere_k_s = new ColorType(1.0f*specularFactor,1.0f*specularFactor,1.0f*specularFactor);
		ColorType ellipsoid_k_s = new ColorType(1.0f*specularFactor,1.0f*specularFactor,1.0f*specularFactor);

		// Store material properties in array
		Material[] mats = {
			new Material(matTerms, sphere_k_a, sphere_k_d, sphere_k_s,n_s),
			new Material(matTerms,torus_k_a, torus_k_d, torus_k_s,n_s),
			new Material(matTerms, ellipsoid_k_a, ellipsoid_k_d, ellipsoid_k_s,n_s),
		};

		// If no lights have been made for this scene make them
		if(lightMade == false) {
			lights = makeLights();
		}

		// Normal to the plane of a triangle : to be used in backface culling / backface rejection
        Point3D triangle_normal = new Point3D();

        // Define the triangle mesh
        Mesh3D mesh;

        // Define the variables used in loops
		int i, j;

		//Define variables used for stacks (m) and slices (n)
		int slices, stacks;

		// Create temp variables for triangles 3D vertices (v0,v1,v2) and normals (n0,n1,n2)
		Point3D v0,v1, v2, n0, n1, n2;

		// Projected triangle
		Point3D[] tri = {new Point3D(), new Point3D(), new Point3D()};
		// Loop through all the shapes
		for(int k=0;k<3;++k) {
			if(k == 0)
			{
				mesh=sphere.mesh;
				slices=sphere.get_n();
				stacks=sphere.get_m();
			}
			else if(k == 1)
			{
				mesh=torus.mesh;
				slices=torus.get_slices();
				stacks=torus.get_stacks();
			}
			else
			{
				mesh = ellipsoid.mesh;
				slices = ellipsoid.get_slices();
				stacks = ellipsoid.get_stacks();
			}

			// Rotate the surface's 3D mesh using quaternion
			mesh.rotateMesh(viewing_quaternion, viewing_center);

			// Phong Rendering
			if(phongRender) {
				Point3D[] tri_p = {new Point3D(), new Point3D(), new Point3D()};
				for(i=0; i < stacks-1; ++i){
					for(j=0; j < slices-1; ++j){

						//Get triangles 3D vertices
						v0 = mesh.v[i][j];
						v1 = mesh.v[i][j+1];
						v2 = mesh.v[i+1][j+1];
						// Get the triangle normal
						triangle_normal = computeTriangleNormal(v0,v1,v2);

						// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
						// triangle
						if(view_vector.dotProduct(triangle_normal) > 0.0)  {

							// Get triangles 3D normals
							n0 = mesh.n[i][j];
							n1 = mesh.n[i][j+1];
							n2 = mesh.n[i+1][j+1];

							// Set the vertex coordinates using the temp vertices
							tri_p[0].x = (int)v0.x;
							tri_p[0].y = (int)v0.y;
							tri_p[0].z = (int)v0.z;
							tri_p[1].x = (int)v1.x;
							tri_p[1].y = (int)v1.y;
							tri_p[1].z = (int)v0.z;
							tri_p[2].x = (int)v2.x;
							tri_p[2].y = (int)v2.y;
							tri_p[2].z = (int)v2.z;

							//Set the normal coordinates with nx,ny, and nz using the temp normals
							tri_p[0].nx = (int) n0.x;
							tri_p[0].ny = (int) n0.y;
							tri_p[0].nz = (int) n0.z;
							tri_p[1].nx = (int) n1.x;
							tri_p[1].ny = (int) n1.y;
							tri_p[1].nz = (int) n1.z;
							tri_p[2].nx = (int) n2.x;
							tri_p[2].ny = (int) n2.y;
							tri_p[2].nz = (int) n2.z;

							// Draw the triangle mesh using Phong Rendering (function in SketchBase.java)
							SketchBase.drawTriangleWithPhong(buff, depthBuffer, tri_p[0], tri_p[1], tri_p[2], n0, n1, n2, lights, mats[k], view_vector);
						}

						// Get the temporary triangles 3D vertices
						v0 = mesh.v[i][j];
						v1 = mesh.v[i+1][j+1];
						v2 = mesh.v[i+1][j];

						//Compute the Triangle Normal
						triangle_normal = computeTriangleNormal(v0,v1,v2);

						// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
						// triangle
						if(view_vector.dotProduct(triangle_normal) > 0.0)
						{
							// Get triangles 3D normals
							n0 = mesh.n[i][j];
							n1 = mesh.n[i+1][j+1];
							n2 = mesh.n[i+1][j];

							// Set the vertex coordinates using the temp vertices
							tri_p[0].x = (int)v0.x;
							tri_p[0].y = (int)v0.y;
							tri_p[0].z = (int)v0.z;
							tri_p[1].x = (int)v1.x;
							tri_p[1].y = (int)v1.y;
							tri_p[1].z = (int)v0.z;
							tri_p[2].x = (int)v2.x;
							tri_p[2].y = (int)v2.y;
							tri_p[2].z = (int)v2.z;

							//Set the normal coordinates with nx,ny, and nz using the temp normals
							tri_p[0].nx = (int) n0.x;
							tri_p[0].ny = (int) n0.y;
							tri_p[0].nz = (int) n0.z;
							tri_p[1].nx = (int) n1.x;
							tri_p[1].ny = (int) n1.y;
							tri_p[1].nz = (int) n1.z;
							tri_p[2].nx = (int) n2.x;
							tri_p[2].ny = (int) n2.y;
							tri_p[2].nz = (int) n2.z;

							// Draw the triangle mesh using Phong Rendering (function in SketchBase.java)
							SketchBase.drawTriangleWithPhong(buff, depthBuffer, tri_p[0], tri_p[1], tri_p[2], n0, n1, n2, lights, mats[k], view_vector);
						}
					}
			    }
			}

			// Flat Surface Rendering/ Gouraud rendering
			else {
				for(i=0; i < stacks-1; ++i){
					for(j=0; j < slices-1; ++j){
						// Get the temporary triangles 3D vertices
						v0 = mesh.v[i][j];
						v1 = mesh.v[i][j+1];
						v2 = mesh.v[i+1][j+1];

						//Compute the Triangle Plane Normal
						triangle_normal = computeTriangleNormal(v0,v1,v2);

						// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
						// triangle
						if(view_vector.dotProduct(triangle_normal) > 0.0){

							// Gouraud rendering
							if(gouraudRender){
								// Get the temp 3D normals
								n0 = mesh.n[i][j];
								n1 = mesh.n[i][j+1];
								n2 = mesh.n[i+1][j+1];

								// Call applyAllLights function each vertex to get the color determined by lighting equation
								tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
								tri[1].c = applyAllLights(lights, mats[k], view_vector, n1, v1);
								tri[2].c = applyAllLights(lights, mats[k], view_vector, n2, v2);
							}

							// Flat Surface Rendering
							else {
								// Use the Triangle Plane Normal as the temp 3D normals
								n2 = n1 = n0 =  triangle_normal;

								// Call applyAllLights function all have the same normal so can be done with same call for all three
								tri[2].c = tri[1].c = tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
							}

							// Set the vertex coordinates using the temp vertices
							tri[0].x = (int)v0.x;
							tri[0].y = (int)v0.y;
							tri[0].z = (int)v0.z;
							tri[1].x = (int)v1.x;
							tri[1].y = (int)v1.y;
							tri[1].z = (int)v1.z;
							tri[2].x = (int)v2.x;
							tri[2].y = (int)v2.y;
							tri[2].z = (int)v2.z;

							// Draw the triangle mesh works for flat surface and Gouraud rendering (function in SketchBase.java)
							SketchBase.drawTriangle(buff,tri[0],tri[1],tri[2],gouraudRender, flatRender, depthBuffer);
						}

						// Get the temporary triangles 3D vertices
						v0 = mesh.v[i][j];
						v1 = mesh.v[i+1][j+1];
						v2 = mesh.v[i+1][j];

						//Compute the Triangle Plane Normal
						triangle_normal = computeTriangleNormal(v0,v1,v2);

						// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
						// triangle
						if(view_vector.dotProduct(triangle_normal) > 0.0) {

							// Gouraud rendering
							if(gouraudRender){
								// Get the temp 3D normals
								n0 = mesh.n[i][j];
								n1 = mesh.n[i+1][j+1];
								n2 = mesh.n[i+1][j];

								// Call applyAllLights function each vertex to get the color determined by lighting equation
								tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
								tri[1].c = applyAllLights(lights, mats[k], view_vector, n1, v1);
								tri[2].c = applyAllLights(lights, mats[k], view_vector, n2, v2);
							}

							// Flat Surface Rendering
							else{
								// Use the Triangle Plane Normal as the temp 3D normals
								n2 = n1 = n0 =  triangle_normal;

								// Call applyAllLights function all have the same normal so can be done with same call for all three
								tri[2].c = tri[1].c = tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
							}

							// Set the vertex coordinates using the temp vertices
							tri[0].x = (int)v0.x;
							tri[0].y = (int)v0.y;
							tri[1].x = (int)v1.x;
							tri[1].y = (int)v1.y;
							tri[2].x = (int)v2.x;
							tri[2].y = (int)v2.y;

							// Draw the triangle mesh works for flat surface and Gouraud rendering (function in SketchBase.java)
							SketchBase.drawTriangle(buff,tri[0],tri[1],tri[2],gouraudRender, flatRender, depthBuffer);
						}
					}
				}
			}
		}
	}

	//Scene Two : Cylinder, sphere, and ellipsoid
	void sceneTwo() {
		float radius = (float)50.0;

		//Shapes for this scene
		Cylinder3D cylinder = new Cylinder3D(350f, radius, 50f,radius, 200f,  1.5f*radius, Nsteps, Nsteps);
		Sphere3D sphere = new Sphere3D(128.0f, 128.0f, 128.0f, 1.5f*radius, Nsteps, Nsteps);
		Ellipsoid3D ellipsoid = new Ellipsoid3D(350f, 380f, 130f, 1.5f*radius, radius, radius, Nsteps, Nsteps);

		//View Vector for this scene
        Point3D view_vector = new Point3D((float)0.0,(float)0.0,(float)1.0);

        // Create material properties for the shapes

        // Ambient coefficients of reflection
		ColorType cylinder_k_a = new ColorType(1.0f*ambientFactor,1.0f*ambientFactor,1.0f*ambientFactor);
		ColorType sphere_k_a = new ColorType(1.0f*ambientFactor,1.0f*ambientFactor,1.0f*ambientFactor);
		ColorType ellipsoid_k_a = new ColorType(1.0f*ambientFactor,1.0f*ambientFactor,1.0f*ambientFactor);

		// Diffuse coefficients of reflection
		ColorType cylinder_k_d = new ColorType(1.9f*diffuseFactor,1.0f*diffuseFactor,0.0f*diffuseFactor);
		ColorType sphere_k_d = new ColorType(0.9f*diffuseFactor,0.3f*diffuseFactor,0.1f*diffuseFactor);
		ColorType ellipsoid_k_d = new ColorType(0.1f*diffuseFactor,0.9f*diffuseFactor,0.01f*diffuseFactor);

		// Specular coefficients of reflection
		ColorType cylinder_k_s = new ColorType(0.0f*specularFactor,0.0f*specularFactor,0.0f*specularFactor);
		ColorType sphere_k_s = new ColorType(1.0f*specularFactor,1.0f*specularFactor,1.0f*specularFactor);
		ColorType ellipsoid_k_s = new ColorType(1.0f*specularFactor,1.0f*specularFactor,1.0f*specularFactor);

		// Store material properties in array
		Material[] mats = {
			new Material(matTerms,cylinder_k_a, cylinder_k_d, cylinder_k_s,n_s),
			new Material(matTerms, sphere_k_a, sphere_k_d, sphere_k_s,n_s),
			new Material(matTerms, ellipsoid_k_a, ellipsoid_k_d, ellipsoid_k_s,n_s),
		};

		// If no lights have been made for this scene make them
		if(lightMade == false) {
			lights = makeLights();
		}

		// Normal to the plane of a triangle : to be used in backface culling / backface rejection
        Point3D triangle_normal = new Point3D();

        // Define the triangle mesh
        Mesh3D mesh;

        // Define the variables used in loops
		int i, j;

		//Define variables used for stacks (m) and slices (n)
		int slices, stacks;

		// Create temp variables for triangles 3D vertices (v0,v1,v2) and normals (n0,n1,n2)
		Point3D v0,v1, v2, n0, n1, n2;


		// Projected triangle
		Point3D[] tri = {new Point3D(), new Point3D(), new Point3D()};
		// Loop through all the shapes
		for(int k=0;k<3;++k) {
			if(k == 0)
			{
				mesh=cylinder.mesh;
				slices=cylinder.get_slices();
				stacks=cylinder.get_stacks();
			}
			else if(k == 1)
			{
				mesh=sphere.mesh;
				slices=sphere.get_n();
				stacks=sphere.get_m();
			}
			else
			{
				mesh = ellipsoid.mesh;
				slices = ellipsoid.get_slices();
				stacks = ellipsoid.get_stacks();
			}


			// Rotate the surface's 3D mesh using quaternion
			mesh.rotateMesh(viewing_quaternion, viewing_center);

			// Phong Rendering
			if(phongRender) {
				Point3D[] tri_p = {new Point3D(), new Point3D(), new Point3D()};
				for(i=0; i < stacks-1; ++i){
					for(j=0; j < slices-1; ++j){

						//Get triangles 3D vertices
						v0 = mesh.v[i][j];
						v1 = mesh.v[i][j+1];
						v2 = mesh.v[i+1][j+1];
						// Get the triangle normal
						triangle_normal = computeTriangleNormal(v0,v1,v2);

						// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
						// triangle
						if(view_vector.dotProduct(triangle_normal) > 0.0)  {

							// Get triangles 3D normals
							n0 = mesh.n[i][j];
							n1 = mesh.n[i][j+1];
							n2 = mesh.n[i+1][j+1];

							// Set the vertex coordinates using the temp vertices
							tri_p[0].x = (int)v0.x;
							tri_p[0].y = (int)v0.y;
							tri_p[0].z = (int)v0.z;
							tri_p[1].x = (int)v1.x;
							tri_p[1].y = (int)v1.y;
							tri_p[1].z = (int)v0.z;
							tri_p[2].x = (int)v2.x;
							tri_p[2].y = (int)v2.y;
							tri_p[2].z = (int)v2.z;

							//Set the normal coordinates with nx,ny, and nz using the temp normals
							tri_p[0].nx = (int) n0.x;
							tri_p[0].ny = (int) n0.y;
							tri_p[0].nz = (int) n0.z;
							tri_p[1].nx = (int) n1.x;
							tri_p[1].ny = (int) n1.y;
							tri_p[1].nz = (int) n1.z;
							tri_p[2].nx = (int) n2.x;
							tri_p[2].ny = (int) n2.y;
							tri_p[2].nz = (int) n2.z;

							// Draw the triangle mesh using Phong Rendering (function in SketchBase.java)
							SketchBase.drawTriangleWithPhong(buff, depthBuffer, tri_p[0], tri_p[1], tri_p[2], n0, n1, n2, lights, mats[k], view_vector);
						}

						// Get the temporary triangles 3D vertices
						v0 = mesh.v[i][j];
						v1 = mesh.v[i+1][j+1];
						v2 = mesh.v[i+1][j];

						//Compute the Triangle Normal
						triangle_normal = computeTriangleNormal(v0,v1,v2);

						// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
						// triangle
						if(view_vector.dotProduct(triangle_normal) > 0.0) {
							// Get triangles 3D normals
							n0 = mesh.n[i][j];
							n1 = mesh.n[i+1][j+1];
							n2 = mesh.n[i+1][j];

							// Set the vertex coordinates using the temp vertices
							tri_p[0].x = (int)v0.x;
							tri_p[0].y = (int)v0.y;
							tri_p[0].z = (int)v0.z;
							tri_p[1].x = (int)v1.x;
							tri_p[1].y = (int)v1.y;
							tri_p[1].z = (int)v0.z;
							tri_p[2].x = (int)v2.x;
							tri_p[2].y = (int)v2.y;
							tri_p[2].z = (int)v2.z;

							//Set the normal coordinates with nx,ny, and nz using the temp normals
							tri_p[0].nx = (int) n0.x;
							tri_p[0].ny = (int) n0.y;
							tri_p[0].nz = (int) n0.z;
							tri_p[1].nx = (int) n1.x;
							tri_p[1].ny = (int) n1.y;
							tri_p[1].nz = (int) n1.z;
							tri_p[2].nx = (int) n2.x;
							tri_p[2].ny = (int) n2.y;
							tri_p[2].nz = (int) n2.z;

							// Draw the triangle mesh using Phong Rendering (function in SketchBase.java)
							SketchBase.drawTriangleWithPhong(buff, depthBuffer, tri_p[0], tri_p[1], tri_p[2], n0, n1, n2, lights, mats[k], view_vector);
						}
					}
				}
			}

			// Flat Surface Rendering/ Gouraud rendering
			else {
				for(i=0; i < stacks-1; ++i){
					for(j=0; j < slices-1; ++j){
						// Get the temporary triangles 3D vertices
						v0 = mesh.v[i][j];
						v1 = mesh.v[i][j+1];
						v2 = mesh.v[i+1][j+1];

						//Compute the Triangle Plane Normal
						triangle_normal = computeTriangleNormal(v0,v1,v2);

						// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
						// triangle
						if(view_vector.dotProduct(triangle_normal) > 0.0){

							// Gouraud rendering
							if(gouraudRender){
								// Get the temp 3D normals
								n0 = mesh.n[i][j];
								n1 = mesh.n[i][j+1];
								n2 = mesh.n[i+1][j+1];

								// Call applyAllLights function each vertex to get the color determined by lighting equation
								tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
								tri[1].c = applyAllLights(lights, mats[k], view_vector, n1, v1);
								tri[2].c = applyAllLights(lights, mats[k], view_vector, n2, v2);
							}

							// Flat Surface Rendering
							else {
								// Use the Triangle Plane Normal as the temp 3D normals
								n2 = n1 = n0 =  triangle_normal;

								// Call applyAllLights function all have the same normal so can be done with same call for all three
								tri[2].c = tri[1].c = tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
							}

							// Set the vertex coordinates using the temp vertices
							tri[0].x = (int)v0.x;
							tri[0].y = (int)v0.y;
							tri[0].z = (int)v0.z;
							tri[1].x = (int)v1.x;
							tri[1].y = (int)v1.y;
							tri[1].z = (int)v1.z;
							tri[2].x = (int)v2.x;
							tri[2].y = (int)v2.y;
							tri[2].z = (int)v2.z;

							// Draw the triangle mesh works for flat surface and Gouraud rendering (function in SketchBase.java)
							SketchBase.drawTriangle(buff,tri[0],tri[1],tri[2],gouraudRender, flatRender, depthBuffer);
						}

						// Get the temporary triangles 3D vertices
						v0 = mesh.v[i][j];
						v1 = mesh.v[i+1][j+1];
						v2 = mesh.v[i+1][j];

						//Compute the Triangle Plane Normal
						triangle_normal = computeTriangleNormal(v0,v1,v2);

						// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
						// triangle
						if(view_vector.dotProduct(triangle_normal) > 0.0) {

							// Gouraud rendering
							if(gouraudRender){
								// Get the temp 3D normals
								n0 = mesh.n[i][j];
								n1 = mesh.n[i+1][j+1];
								n2 = mesh.n[i+1][j];

								// Call applyAllLights function each vertex to get the color determined by lighting equation
								tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
								tri[1].c = applyAllLights(lights, mats[k], view_vector, n1, v1);
								tri[2].c = applyAllLights(lights, mats[k], view_vector, n2, v2);
							}

							// Flat Surface Rendering
							else{
								// Use the Triangle Plane Normal as the temp 3D normals
								n2 = n1 = n0 =  triangle_normal;

								// Call applyAllLights function all have the same normal so can be done with same call for all three
								tri[2].c = tri[1].c = tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
							}

							// Set the vertex coordinates using the temp vertices
							tri[0].x = (int)v0.x;
							tri[0].y = (int)v0.y;
							tri[1].x = (int)v1.x;
							tri[1].y = (int)v1.y;
							tri[2].x = (int)v2.x;
							tri[2].y = (int)v2.y;

							// Draw the triangle mesh works for flat surface and Gouraud rendering (function in SketchBase.java)
							SketchBase.drawTriangle(buff,tri[0],tri[1],tri[2],gouraudRender, flatRender, depthBuffer);
						}
					}
				}
			}
		}
	}

	void sceneThree() {
		float radius = (float)50.0;

		//Shapes for this scene
		Box3D box = new Box3D(128.0f, 128.0f, 128.0f, 1.5f*radius, Nsteps, Nsteps);
		Torus3D torus = new Torus3D(300.0f, 250.0f, 250.0f, 0.8f*radius, 1.25f*radius, Nsteps, Nsteps);
		Ellipsoid3D ellipsoid = new Ellipsoid3D(350f, 380f, 130f, 1.5f*radius, radius, radius, Nsteps, Nsteps);

		//View Vector for this scene
        Point3D view_vector = new Point3D((float)0.0,(float)0.0,(float)1.0);

        // Create material properties for the shapes

        // Ambient coefficients of reflection
        ColorType ellipsoid_k_a = new ColorType(1.0f*ambientFactor,1.0f*ambientFactor,1.0f*ambientFactor);
		ColorType torus_k_a = new ColorType(0.3f*ambientFactor,0.3f*ambientFactor,0.3f*ambientFactor);
		ColorType box_k_a = new ColorType(0.5f*ambientFactor,0.5f*ambientFactor,0.5f*ambientFactor);

		// Diffuse coefficients of reflection
		ColorType ellipsoid_k_d = new ColorType(0.1f*diffuseFactor,0.9f*diffuseFactor,0.01f*diffuseFactor);
		ColorType torus_k_d = new ColorType(0.0f*diffuseFactor,0.5f*diffuseFactor,0.9f*diffuseFactor);
		ColorType box_k_d = new ColorType(0.0f*diffuseFactor,0.4f*diffuseFactor,0.7f*diffuseFactor);

		// Specular coefficients of reflection
		ColorType ellipsoid_k_s = new ColorType(1.0f*specularFactor,1.0f*specularFactor,1.0f*specularFactor);
		ColorType torus_k_s = new ColorType(1.0f*specularFactor,1.0f*specularFactor,1.0f*specularFactor);
		ColorType box_k_s = new ColorType(1.0f*specularFactor,1.0f*specularFactor,1.0f*specularFactor);

		// Store material properties in array
		Material[] mats = {
			new Material(matTerms, box_k_a, box_k_d, box_k_s,n_s),
			new Material(matTerms, torus_k_a, torus_k_d, torus_k_s,n_s),
			new Material(matTerms,ellipsoid_k_a, ellipsoid_k_d, ellipsoid_k_s,n_s)
		};

		// If no lights have been made for this scene make them
		if(lightMade == false) {
			lights = makeLights();
		}

		// Normal to the plane of a triangle : to be used in backface culling / backface rejection
        Point3D triangle_normal = new Point3D();

        // Define the triangle mesh
        Mesh3D mesh;

        //Define an array of triangle meshes for the box
        Mesh3D[] meshes;

        // Define the variables used in loops
		int i, j;

		//Define variables used for stacks (m) and slices (n)
		int slices, stacks;

		// Create temp variables for triangles 3D vertices (v0,v1,v2) and normals (n0,n1,n2)
		Point3D v0,v1, v2, n0, n1, n2;


		// Projected triangle
		Point3D[] tri = {new Point3D(), new Point3D(), new Point3D()};
		//Loop through shapes
		for(int k=0;k<3;++k) {
			if(k == 0)
			{
				mesh = null;
				meshes = box.meshes;
				slices=box.get_slices();
				stacks=box.get_stacks();
				for(int w=0; w<meshes.length;w++) {
					drawBox(meshes[w], triangle_normal, mats, k,slices, stacks);
				}
				continue;
			}
			else if(k == 1)
			{
				mesh=torus.mesh;
				slices=torus.get_slices();
				stacks=torus.get_stacks();
			}
			else
			{
				mesh = ellipsoid.mesh;
				slices = ellipsoid.get_slices();
				stacks = ellipsoid.get_stacks();
			}

			// Rotate the surface's 3D mesh using quaternion
			mesh.rotateMesh(viewing_quaternion, viewing_center);

			// Phong Rendering
			if(phongRender) {
				Point3D[] tri_p = {new Point3D(), new Point3D(), new Point3D()};
				for(i=0; i < stacks-1; ++i){
					for(j=0; j < slices-1; ++j){

						//Get triangles 3D vertices
						v0 = mesh.v[i][j];
						v1 = mesh.v[i][j+1];
						v2 = mesh.v[i+1][j+1];
						// Get the triangle normal
						triangle_normal = computeTriangleNormal(v0,v1,v2);

						// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
						// triangle
						if(view_vector.dotProduct(triangle_normal) > 0.0)  {

							// Get triangles 3D normals
							n0 = mesh.n[i][j];
							n1 = mesh.n[i][j+1];
							n2 = mesh.n[i+1][j+1];

							// Set the vertex coordinates using the temp vertices
							tri_p[0].x = (int)v0.x;
							tri_p[0].y = (int)v0.y;
							tri_p[0].z = (int)v0.z;
							tri_p[1].x = (int)v1.x;
							tri_p[1].y = (int)v1.y;
							tri_p[1].z = (int)v0.z;
							tri_p[2].x = (int)v2.x;
							tri_p[2].y = (int)v2.y;
							tri_p[2].z = (int)v2.z;

							//Set the normal coordinates with nx,ny, and nz using the temp normals
							tri_p[0].nx = (int) n0.x;
							tri_p[0].ny = (int) n0.y;
							tri_p[0].nz = (int) n0.z;
							tri_p[1].nx = (int) n1.x;
							tri_p[1].ny = (int) n1.y;
							tri_p[1].nz = (int) n1.z;
							tri_p[2].nx = (int) n2.x;
							tri_p[2].ny = (int) n2.y;
							tri_p[2].nz = (int) n2.z;

							// Draw the triangle mesh using Phong Rendering (function in SketchBase.java)
							SketchBase.drawTriangleWithPhong(buff, depthBuffer, tri_p[0], tri_p[1], tri_p[2], n0, n1, n2, lights, mats[k], view_vector);
						}

						// Get the temporary triangles 3D vertices
						v0 = mesh.v[i][j];
						v1 = mesh.v[i+1][j+1];
						v2 = mesh.v[i+1][j];

						//Compute the Triangle Normal
						triangle_normal = computeTriangleNormal(v0,v1,v2);

						// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
						// triangle
						if(view_vector.dotProduct(triangle_normal) > 0.0) {
							// Get triangles 3D normals
							n0 = mesh.n[i][j];
							n1 = mesh.n[i+1][j+1];
							n2 = mesh.n[i+1][j];

							// Set the vertex coordinates using the temp vertices
							tri_p[0].x = (int)v0.x;
							tri_p[0].y = (int)v0.y;
							tri_p[0].z = (int)v0.z;
							tri_p[1].x = (int)v1.x;
							tri_p[1].y = (int)v1.y;
							tri_p[1].z = (int)v0.z;
							tri_p[2].x = (int)v2.x;
							tri_p[2].y = (int)v2.y;
							tri_p[2].z = (int)v2.z;

							//Set the normal coordinates with nx,ny, and nz using the temp normals
							tri_p[0].nx = (int) n0.x;
							tri_p[0].ny = (int) n0.y;
							tri_p[0].nz = (int) n0.z;
							tri_p[1].nx = (int) n1.x;
							tri_p[1].ny = (int) n1.y;
							tri_p[1].nz = (int) n1.z;
							tri_p[2].nx = (int) n2.x;
							tri_p[2].ny = (int) n2.y;
							tri_p[2].nz = (int) n2.z;

							// Draw the triangle mesh using Phong Rendering (function in SketchBase.java)
							SketchBase.drawTriangleWithPhong(buff, depthBuffer, tri_p[0], tri_p[1], tri_p[2], n0, n1, n2, lights, mats[k], view_vector);
						}
					}
				}
			}

			// Flat Surface Rendering/ Gouraud rendering
			else {
				for(i=0; i < stacks-1; ++i){
					for(j=0; j < slices-1; ++j){
						// Get the temporary triangles 3D vertices
						v0 = mesh.v[i][j];
						v1 = mesh.v[i][j+1];
						v2 = mesh.v[i+1][j+1];

						//Compute the Triangle Plane Normal
						triangle_normal = computeTriangleNormal(v0,v1,v2);

						// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
						// triangle
						if(view_vector.dotProduct(triangle_normal) > 0.0){

							// Gouraud rendering
							if(gouraudRender){
								// Get the temp 3D normals
								n0 = mesh.n[i][j];
								n1 = mesh.n[i][j+1];
								n2 = mesh.n[i+1][j+1];

								// Call applyAllLights function each vertex to get the color determined by lighting equation
								tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
								tri[1].c = applyAllLights(lights, mats[k], view_vector, n1, v1);
								tri[2].c = applyAllLights(lights, mats[k], view_vector, n2, v2);
							}

							// Flat Surface Rendering
							else {
								// Use the Triangle Plane Normal as the temp 3D normals
								n2 = n1 = n0 =  triangle_normal;

								// Call applyAllLights function all have the same normal so can be done with same call for all three
								tri[2].c = tri[1].c = tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
							}

							// Set the vertex coordinates using the temp vertices
							tri[0].x = (int)v0.x;
							tri[0].y = (int)v0.y;
							tri[0].z = (int)v0.z;
							tri[1].x = (int)v1.x;
							tri[1].y = (int)v1.y;
							tri[1].z = (int)v1.z;
							tri[2].x = (int)v2.x;
							tri[2].y = (int)v2.y;
							tri[2].z = (int)v2.z;

							// Draw the triangle mesh works for flat surface and Gouraud rendering (function in SketchBase.java)
							SketchBase.drawTriangle(buff,tri[0],tri[1],tri[2],gouraudRender, flatRender, depthBuffer);
						}

						// Get the temporary triangles 3D vertices
						v0 = mesh.v[i][j];
						v1 = mesh.v[i+1][j+1];
						v2 = mesh.v[i+1][j];

						//Compute the Triangle Plane Normal
						triangle_normal = computeTriangleNormal(v0,v1,v2);

						// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
						// triangle
						if(view_vector.dotProduct(triangle_normal) > 0.0) {

							// Gouraud rendering
							if(gouraudRender){
								// Get the temp 3D normals
								n0 = mesh.n[i][j];
								n1 = mesh.n[i+1][j+1];
								n2 = mesh.n[i+1][j];

								// Call applyAllLights function each vertex to get the color determined by lighting equation
								tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
								tri[1].c = applyAllLights(lights, mats[k], view_vector, n1, v1);
								tri[2].c = applyAllLights(lights, mats[k], view_vector, n2, v2);
							}

							// Flat Surface Rendering
							else{
								// Use the Triangle Plane Normal as the temp 3D normals
								n2 = n1 = n0 =  triangle_normal;

								// Call applyAllLights function all have the same normal so can be done with same call for all three
								tri[2].c = tri[1].c = tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
							}

							// Set the vertex coordinates using the temp vertices
							tri[0].x = (int)v0.x;
							tri[0].y = (int)v0.y;
							tri[1].x = (int)v1.x;
							tri[1].y = (int)v1.y;
							tri[2].x = (int)v2.x;
							tri[2].y = (int)v2.y;

							// Draw the triangle mesh works for flat surface and Gouraud rendering (function in SketchBase.java)
							SketchBase.drawTriangle(buff,tri[0],tri[1],tri[2],gouraudRender, flatRender, depthBuffer);
						}
					}
				}
			}
		}
	}

	private void drawBox(Mesh3D mesh, Point3D triangle_normal, Material[] mats, int k, int n, int m) {
		//View Vector for this scene
        Point3D view_vector = new Point3D((float)0.0,(float)0.0,(float)1.0);

        // Define the variables used in loops
		int i, j;

		// Create temp variables for triangles 3D vertices (v0,v1,v2) and normals (n0,n1,n2)
		Point3D v0,v1, v2, n0, n1, n2;

		// Projected triangle
		Point3D[] tri = {new Point3D(), new Point3D(), new Point3D()};

		// Rotate the surface's 3D mesh using quaternion
		mesh.rotateMesh(viewing_quaternion, viewing_center);

		// Phong Rendering
		if(phongRender) {
			Point3D[] tri_p = {new Point3D(), new Point3D(), new Point3D()};
			for(i=0; i < m-1; ++i){
				for(j=0; j < n-1; ++j){

				//Get triangles 3D vertices
				v0 = mesh.v[i][j];
				v1 = mesh.v[i][j+1];
				v2 = mesh.v[i+1][j+1];
				// Get the triangle normal
				triangle_normal = computeTriangleNormal(v0,v1,v2);

				// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
				// triangle
				if(view_vector.dotProduct(triangle_normal) > 0.0)  {

					// Get triangles 3D normals
					n0 = mesh.n[i][j];
					n1 = mesh.n[i][j+1];
					n2 = mesh.n[i+1][j+1];

					// Set the vertex coordinates using the temp vertices
					tri_p[0].x = (int)v0.x;
					tri_p[0].y = (int)v0.y;
					tri_p[0].z = (int)v0.z;
					tri_p[1].x = (int)v1.x;
					tri_p[1].y = (int)v1.y;
					tri_p[1].z = (int)v0.z;
					tri_p[2].x = (int)v2.x;
					tri_p[2].y = (int)v2.y;
					tri_p[2].z = (int)v2.z;

					//Set the normal coordinates with nx,ny, and nz using the temp normals
					tri_p[0].nx = (int) n0.x;
					tri_p[0].ny = (int) n0.y;
					tri_p[0].nz = (int) n0.z;
					tri_p[1].nx = (int) n1.x;
					tri_p[1].ny = (int) n1.y;
					tri_p[1].nz = (int) n1.z;
					tri_p[2].nx = (int) n2.x;
					tri_p[2].ny = (int) n2.y;
					tri_p[2].nz = (int) n2.z;

					// Draw the triangle mesh using Phong Rendering (function in SketchBase.java)
					SketchBase.drawTriangleWithPhong(buff, depthBuffer, tri_p[0], tri_p[1], tri_p[2], n0, n1, n2, lights, mats[k], view_vector);
				}

				// Get the temporary triangles 3D vertices
				v0 = mesh.v[i][j];
				v1 = mesh.v[i+1][j+1];
				v2 = mesh.v[i+1][j];

				//Compute the Triangle Normal
				triangle_normal = computeTriangleNormal(v0,v1,v2);

				// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
				// triangle
				if(view_vector.dotProduct(triangle_normal) > 0.0) {
					// Get triangles 3D normals
					n0 = mesh.n[i][j];
					n1 = mesh.n[i+1][j+1];
					n2 = mesh.n[i+1][j];

					// Set the vertex coordinates using the temp vertices
					tri_p[0].x = (int)v0.x;
					tri_p[0].y = (int)v0.y;
					tri_p[0].z = (int)v0.z;
					tri_p[1].x = (int)v1.x;
					tri_p[1].y = (int)v1.y;
					tri_p[1].z = (int)v0.z;
					tri_p[2].x = (int)v2.x;
					tri_p[2].y = (int)v2.y;
					tri_p[2].z = (int)v2.z;

					//Set the normal coordinates with nx,ny, and nz using the temp normals
					tri_p[0].nx = (int) n0.x;
					tri_p[0].ny = (int) n0.y;
					tri_p[0].nz = (int) n0.z;
					tri_p[1].nx = (int) n1.x;
					tri_p[1].ny = (int) n1.y;
					tri_p[1].nz = (int) n1.z;
					tri_p[2].nx = (int) n2.x;
					tri_p[2].ny = (int) n2.y;
					tri_p[2].nz = (int) n2.z;

					// Draw the triangle mesh using Phong Rendering (function in SketchBase.java)
					SketchBase.drawTriangleWithPhong(buff, depthBuffer, tri_p[0], tri_p[1], tri_p[2], n0, n1, n2, lights, mats[k], view_vector);
				}
			  }
			}
		}

		// Flat Surface Rendering/ Gouraud rendering
		else {
			for(i=0; i < m-1; ++i){
				for(j=0; j < n-1; ++j){
					// Get the temporary triangles 3D vertices
					v0 = mesh.v[i][j];
					v1 = mesh.v[i][j+1];
					v2 = mesh.v[i+1][j+1];

					//Compute the Triangle Plane Normal
					triangle_normal = computeTriangleNormal(v0,v1,v2);

					// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
					// triangle
					if(view_vector.dotProduct(triangle_normal) > 0.0){

						// Gouraud rendering
						if(gouraudRender){
							// Get the temp 3D normals
							n0 = mesh.n[i][j];
							n1 = mesh.n[i][j+1];
							n2 = mesh.n[i+1][j+1];

							// Call applyAllLights function each vertex to get the color determined by lighting equation
							tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
							tri[1].c = applyAllLights(lights, mats[k], view_vector, n1, v1);
							tri[2].c = applyAllLights(lights, mats[k], view_vector, n2, v2);
						}

						// Flat Surface Rendering
						else {
							// Use the Triangle Plane Normal as the temp 3D normals
							n2 = n1 = n0 =  triangle_normal;

							// Call applyAllLights function all have the same normal so can be done with same call for all three
							tri[2].c = tri[1].c = tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
						}

						// Set the vertex coordinates using the temp vertices
						tri[0].x = (int)v0.x;
						tri[0].y = (int)v0.y;
						tri[0].z = (int)v0.z;
						tri[1].x = (int)v1.x;
						tri[1].y = (int)v1.y;
						tri[1].z = (int)v1.z;
						tri[2].x = (int)v2.x;
						tri[2].y = (int)v2.y;
						tri[2].z = (int)v2.z;

						// Draw the triangle mesh works for flat surface and Gouraud rendering (function in SketchBase.java)
						SketchBase.drawTriangle(buff,tri[0],tri[1],tri[2],gouraudRender, flatRender, depthBuffer);
					}

					// Get the temporary triangles 3D vertices
					v0 = mesh.v[i][j];
					v1 = mesh.v[i+1][j+1];
					v2 = mesh.v[i+1][j];

					//Compute the Triangle Plane Normal
					triangle_normal = computeTriangleNormal(v0,v1,v2);

					// If the dot product of the view vector and triangle_normal is > 0 it is a front facing
					// triangle
					if(view_vector.dotProduct(triangle_normal) > 0.0) {

						// Gouraud rendering
						if(gouraudRender){
							// Get the temp 3D normals
							n0 = mesh.n[i][j];
							n1 = mesh.n[i+1][j+1];
							n2 = mesh.n[i+1][j];

							// Call applyAllLights function each vertex to get the color determined by lighting equation
							tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
							tri[1].c = applyAllLights(lights, mats[k], view_vector, n1, v1);
							tri[2].c = applyAllLights(lights, mats[k], view_vector, n2, v2);
						}

						// Flat Surface Rendering
						else{
							// Use the Triangle Plane Normal as the temp 3D normals
							n2 = n1 = n0 =  triangle_normal;

							// Call applyAllLights function all have the same normal so can be done with same call for all three
							tri[2].c = tri[1].c = tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
						}

						// Set the vertex coordinates using the temp vertices
						tri[0].x = (int)v0.x;
						tri[0].y = (int)v0.y;
						tri[1].x = (int)v1.x;
						tri[1].y = (int)v1.y;
						tri[2].x = (int)v2.x;
						tri[2].y = (int)v2.y;

						// Draw the triangle mesh works for flat surface and Gouraud rendering (function in SketchBase.java)
						SketchBase.drawTriangle(buff,tri[0],tri[1],tri[2],gouraudRender, flatRender, depthBuffer);
					}
				}
			}
		}
	}

	// Helper method that computes the unit normal to the plane of the triangle
	// degenerate triangles (points or lines, etc) yield normal that is numerically zero
	private Point3D computeTriangleNormal(Point3D v0, Point3D v1, Point3D v2)
	{
		Point3D e0 = v1.minus(v2);
		Point3D e1 = v0.minus(v2);
		Point3D norm = e0.crossProduct(e1);

		if(norm.magnitude()>0.000001)
			norm.normalize();
		else 	// detect degenerate triangle and set its normal to zero
			norm.set((float)0.0,(float)0.0,(float)0.0);

		return norm;
	}

}
