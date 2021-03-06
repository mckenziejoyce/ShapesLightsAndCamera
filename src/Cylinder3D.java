//****************************************************************************
//    Cylinder with End Caps Class
//****************************************************************************
// McKenzie Joyce
// CS480
public class Cylinder3D{
  private Point3D center;
  private float rx;
  private float ry;
  private float uMin;
  private float uMax;
  private int stacks,slices;
  public Mesh3D mesh;
  private final float vMin = (float)-Math.PI;
  private final float vMax = (float)Math.PI;

  public Cylinder3D(float _x, float _rx, float _y, float _ry, float _z, float _u, int _stacks, int _slices){
	this.center = new Point3D(_x,_y,_z);
	this.rx = _rx;
	this.ry = _ry;
	this.uMin = -_u;
	this.uMax = _u;
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
  public void set_radius(float _rx, float _ry){
	this.rx = _rx;
	this.ry = _ry;
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
  //Set mesh vertices and normals
  private void fillMesh(){
	int i, j;
	float theta, phi;
	float d_phi = (uMax-uMin)/((float)slices-1);
	float d_theta = (vMax-vMin)/((float)stacks-1);
		
	float cos_theta;
	float sin_theta;
		
	Point3D d_uVector = new Point3D();
	Point3D d_vVector = new Point3D();
		
	for (i = 0, theta = vMin; i < stacks; ++i, theta += d_theta){
		cos_theta = (float)Math.cos(theta);
		sin_theta = (float)Math.sin(theta);
			
		for (j = 0, phi = uMin; j < slices; ++j, phi += d_phi) {			
			// Compute vertex
			this.mesh.v[i][j].x = center.x + rx * cos_theta;
			this.mesh.v[i][j].y = center.y + ry * sin_theta;
			this.mesh.v[i][j].z = center.z + phi;
				
			// Compute unit normal at vertex
			d_uVector.x = -rx * sin_theta;
			d_uVector.y = ry * cos_theta;
			d_uVector.z = 0;
				
			d_vVector.x = 0;
			d_vVector.y = 0;
			d_vVector.z = 1;
				
			d_uVector.crossProduct(d_vVector, mesh.n[i][j]);
			this.mesh.n[i][j].normalize();
		}
	}
	fillCaps(d_theta);
  }
  
  private void fillCaps(float d_theta){
	for (int i = 0; i < stacks; ++i){
		this.mesh.n[i][0] = new Point3D(0,0,-1);
		this.mesh.v[i][0] = new Point3D(center.x,center.y,center.z+uMin);
		this.mesh.n[i][slices-1] = new Point3D(0,0,1);
		this.mesh.v[i][slices-1] = new Point3D(center.x,center.y,center.z+uMax);
	}
  }

}
