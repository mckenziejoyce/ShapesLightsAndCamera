//****************************************************************************
//      Ellipsoid class
//****************************************************************************
// McKenzie Joyce
// CS480
public class Ellipsoid3D{
  private Point3D center;
  private float rx;
  private float ry;
  private float rz;
  private int stacks,slices;
  public Mesh3D mesh;


  public Ellipsoid3D(float _x, float _y, float _z, float _rx, float _ry, float _rz, int _stacks, int _slices){
	this.center = new Point3D(_x,_y,_z);
	this.rx = _rx;
	this.ry = _ry;
	this.rz = _rz;
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
  public void set_radius(float _rx, float _ry, float _rz){
	this.rx = _rx;
	this.ry = _ry;
	this.rz = _rz;
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
    float d_theta = (float)(2*Math.PI)/(float)(slices-1);
	float d_phi = (float)(Math.PI)/(float)(stacks-1);
    float cos_theta, sin_theta;
	float cos_phi, sin_phi;

    for(i=0, theta=-(float)Math.PI;i<stacks;++i,theta += d_theta){
      //Compute the cos and sin of theta 
      cos_theta = (float)Math.cos(theta);
      sin_theta = (float)Math.sin(theta);
      for(j=0, phi=(float)(-0.5*Math.PI);j<slices;++j,phi += d_phi){
    	//Compute the cos and sin of phi
        cos_phi = (float)Math.cos(phi);
        sin_phi = (float)Math.sin(phi);

        //Vertex location
        this.mesh.v[i][j].x = center.x+rx*cos_phi*cos_theta;
        this.mesh.v[i][j].y = center.y+ry*cos_phi*sin_theta;
        this.mesh.v[i][j].z = center.z+rz*sin_phi;

        // Vertex where unit is normal to sphere
        this.mesh.n[i][j].x = cos_phi*cos_theta;
        this.mesh.n[i][j].y = cos_phi*sin_theta;
        this.mesh.n[i][j].z = sin_phi;
      }
    }
  }
}
