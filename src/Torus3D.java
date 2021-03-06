//****************************************************************************
//     Torus class
//****************************************************************************
// McKenzie Joyce
// CS480
public class Torus3D{
	private Point3D center;
	private float r;
	private float rAxial;
	private int stacks,slices;
	public Mesh3D mesh;
	
	private final float uMin = (float)-Math.PI;
	private final float uMax = (float)Math.PI;
	private final float vMin = (float)-Math.PI;
	private final float vMax = (float)Math.PI;

  public Torus3D(float _x, float _y, float _z, float _r, float _rAxial, int _stacks, int _slices){
	this.center = new Point3D(_x,_y,_z);
	this.r = _r;
	this.rAxial = _rAxial;
	this.stacks = _stacks;
	this.slices = _slices;
	initMesh();
  }

  public void set_center(float _x, float _y, float _z){
	this.center.x=_x;
	this.center.y=_y;
	this.center.z=_z;
	fillMesh();  
  }

  public void set_radius(float _r, float _rAxial){
	this.r = _r;
	this.rAxial = _rAxial;
	fillMesh(); 
  }

  public void set_stacks(int _stacks){
	  this.stacks = _stacks;
	  initMesh(); 
  }

  public void set_slices(int _slices){
	  this.slices = _slices;
	initMesh(); 
  }

  public int get_slices(){
	return this.slices;
  }

  public int get_stacks(){
	return this.stacks;
  }

  private void initMesh(){
	this.mesh = new Mesh3D(stacks,slices);
	fillMesh();  
  }

  private void fillMesh(){
    int i, j;
	float theta, phi;
	float d_phi = (uMax-uMin)/((float)slices-1);
	float d_theta = (vMax-vMin)/((float)stacks-1);
    float cos_theta, sin_theta;
	float cos_phi, sin_phi;
    Point3D d_uVector = new Point3D();
	Point3D d_vVector = new Point3D();

	for(i=0,theta=(float)-Math.PI;i<stacks;++i,theta += d_theta)
    {
		// Get cos and sin of theta
		cos_theta=(float)Math.cos(theta);
		sin_theta=(float)Math.sin(theta);
		
		for(j=0,phi=(float)-Math.PI;j<slices;++j,phi += d_phi)
		{
			// Get cos and sin of phi 
			cos_phi = (float)Math.cos(phi);
			sin_phi = (float)Math.sin(phi);
			
			// Compute vertex
			this.mesh.v[i][j].x=center.x+(rAxial+r*cos_phi)*cos_theta;
			this.mesh.v[i][j].y=center.y+(rAxial+r*cos_phi)*sin_theta;
			this.mesh.v[i][j].z=center.z+r*sin_phi;
			
			// Compute partial derivatives
			d_uVector.x = -(rAxial+r*cos_phi)*sin_theta;
			d_uVector.y = (rAxial+r*cos_phi)*cos_theta;
			d_uVector.z = 0;
			
			d_vVector.x = -r*sin_phi*cos_theta;
			d_vVector.y = -r*sin_phi*sin_theta;
			d_vVector.z = r*cos_phi;
			
			//Use cross product to get the normal 
			d_uVector.crossProduct(d_vVector, this.mesh.n[i][j]);
			//Normalize to produce a unit vector for the normal
			this.mesh.n[i][j].normalize();
		}
    }
  }
}
