#ifndef mda_H
#define mda_H

#define MDA_MAX_DIMS 50
#define MDA_MAX_SIZE 512 * 512 * 512
#define MDA_TYPE_COMPLEX -1
#define MDA_TYPE_BYTE -2
#define MDA_TYPE_REAL -3
#define MDA_TYPE_SHORT -4
#define MDA_TYPE_INT32 -5
#define MDA_TYPE_UINT16 -6


class MdaPrivate;
class Mda {
public:
	friend class MdaPrivate;
	Mda();
	Mda(const Mda &X);
	virtual ~Mda();
	
	void operator=(const Mda &X);
	void setDataType(int dt);
	int dataType() const;
	void allocate(int N1,int N2,int N3=1,int N4=1,int N5=1,int N6=1);
	void allocate(int num_dims,int *size);
	int size(int dim) const;
	int N1() const;
	int N2() const;
	int N3() const;
	int N4() const;
	int N5() const;
	int N6() const;
	int dimCount() const;
	int totalSize() const;
	float value1(int i) const;
	float value(int i1,int i2,int i3=0,int i4=0) const;
	float value(int num_dims,int *ind) const;
	void setValue1(float val,int i);
	void setValue(float val,int i1,int i2,int i3=0,int i4=0);
	void setValue(float val,int num_dims,int *ind);
	void setValues(float *vals);
	void setValues(int *vals);
	void setValues(short *vals);
	void setValues(unsigned char *vals);
	Mda getDataXY(int num_inds,int *inds) const;
	Mda getDataXZ(int num_inds,int *inds) const;
	Mda getDataYZ(int num_inds,int *inds) const;
	Mda transpose() const;
	bool read(char *path);
	bool write(char *path);
	
private:
	MdaPrivate *d;
};

#endif


	
	

	
	