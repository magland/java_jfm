package jviewmda;

import java.io.IOException;
import niftijio.NiftiHeader;
import niftijio.NiftiVolume;

/**
 *
 * @author magland
 */
public class JNifti {

	Mda m_array = new Mda();
	NiftiHeader m_header = new NiftiHeader();

	public JNifti() {
	}

	public void read(String path) throws IOException {
		NiftiVolume X = NiftiVolume.read(path);
		double[] data = X.data;
		m_header = X.header;
		int N1 = X.N1();
		int N2 = X.N2();
		int N3 = X.N3();
		int N4 = X.N4();
		m_array.allocate(N1, N2, N3, N4);
		int tot = N1 * N2 * N3 * N4;
		for (int ii = 0; ii < tot; ii++) {
			m_array.setValue1(data[ii], ii);
		}
	}

	public Mda array() {
		return m_array;
	}
}
