//****************************************************************************
//       material class
//****************************************************************************
// History :
//   Nov 6, 2014 Created by Stan Sclaroff
//   Nov 19, 2019 Modified by Zezhou Sun

public class Material 
{
	public ColorType k_a, k_d, k_s;
	public int n_s;
	public boolean specular, diffuse, ambient, attenuation;
	
	public Material(ColorType _k_a, ColorType _k_d, ColorType _k_s, int _n_s)
	{
		k_s = new ColorType(_k_s);  // specular coefficient for r,g,b
		k_a = new ColorType(_k_a);  // ambient coefficient for r,g,b
		k_d = new ColorType(_k_d);  // diffuse coefficient for r,g,b
		n_s = _n_s;  // specular exponent
		
		// set boolean variables 
		specular = (n_s>0 && (k_s.r > 0.0 || k_s.g > 0.0 || k_s.b > 0.0));
		diffuse = (k_d.r > 0.0 || k_d.g > 0.0 || k_d.b > 0.0);
		ambient = (k_a.r > 0.0 || k_a.g > 0.0 || k_a.b > 0.0);
		attenuation = true;
	}
	
	public Material(Material mat) {
		k_a = new ColorType(mat.k_a);
		k_d = new ColorType(mat.k_d);
		k_s = new ColorType(mat.k_s);
		n_s = mat.n_s;
		specular = mat.specular;
		diffuse = mat.diffuse;
		ambient = mat.ambient;
	}
	
	public Material(boolean[] matTerms, ColorType _k_a, ColorType _k_d, ColorType _k_s, int _n_s)
	{
		k_s = new ColorType(_k_s);  // specular coefficient for r,g,b
		k_a = new ColorType(_k_a);  // ambient coefficient for r,g,b
		k_d = new ColorType(_k_d);  // diffuse coefficient for r,g,b
		n_s = _n_s;  // specular exponent
		
		// set boolean variables 
		specular = (matTerms[0] && n_s>0 && (k_s.r > 0.0 || k_s.g > 0.0 || k_s.b > 0.0));
		diffuse = (matTerms[1] && (k_d.r > 0.0 || k_d.g > 0.0 || k_d.b > 0.0));
		ambient = (matTerms[2] && (k_a.r > 0.0 || k_a.g > 0.0 || k_a.b > 0.0));
	}
	
	public Material copy() {
		return new Material(k_a, k_d, k_s, n_s);
	}
}