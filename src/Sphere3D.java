//****************************************************************************
//      Sphere class
//****************************************************************************
// History :
//   Nov 6, 2014 Created by Stan Sclaroff
//

public class Sphere3D
{
	private Point3D center;
	private float r;
	private int stacks,slices;
	public Mesh3D mesh;
	
	private final float uMin = (float)-Math.PI/2;
	private final float uMax = (float)Math.PI/2;
	private final float vMin = (float)-Math.PI;
	private final float vMax = (float)Math.PI;
	
	public Sphere3D(float _x, float _y, float _z, float _r, int _stacks, int _slices)
	{
		this.center = new Point3D(_x,_y,_z);
		this.r = _r;
		this.stacks = _stacks;
		this.slices = _slices;
		initMesh();
	}
	
	public void set_center(float _x, float _y, float _z)
	{
		this.center.x=_x;
		this.center.y=_y;
		this.center.z=_z;
		fillMesh();  
	}
	
	public void set_radius(float _r)
	{
		this.r = _r;
		fillMesh(); 
	}
	
	public void set_stacks(int _stacks)
	{
		this.stacks = _stacks;
		initMesh(); 
	}
	
	public void set_slices(int _slices)
	{
		this.slices = _slices;
		initMesh(); 
	}
	
	public int get_n()
	{
		return this.slices;
	}
	
	public int get_m()
	{
		return this.stacks;
	}

	private void initMesh()
	{
		this.mesh = new Mesh3D(stacks,slices);
		fillMesh();  
	}
		
	// fill the triangle mesh vertices and normals
	// using the current parameters for the sphere
	private void fillMesh()
	{
		int i, j;
		float theta, phi;
		float d_phi = (uMax-uMin)/((float)slices-1);
		float d_theta = (vMax-vMin)/((float)stacks-1);
		float cos_theta, sin_theta;
		float cos_phi, sin_phi;
		for (i = 0, theta = vMin; i < stacks; ++i, theta += d_theta){
			cos_theta = (float)Math.cos(theta);
			sin_theta = (float)Math.sin(theta);
			
			for (j = 0, phi = uMin; j < slices; ++j, phi += d_phi) {
				cos_phi = (float)Math.cos(phi);
				sin_phi = (float)Math.sin(phi);
				
				// Compute vertex
				mesh.v[i][j].x = center.x + r * cos_phi * cos_theta;
				mesh.v[i][j].y = center.y + r * cos_phi * sin_theta;
				mesh.v[i][j].z = center.z + r * sin_phi;
				
				// Compute unit normal at vertex
				mesh.n[i][j].x = cos_phi * cos_theta;
				mesh.n[i][j].y = cos_phi * sin_theta;
				mesh.n[i][j].z = sin_phi;
				
				mesh.n[i][j].normalize();
			}
		}
	}
}