package jviewmda;

import java.util.Arrays;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 *
 * @author magland
 */
public class Mda {

	static public final int MAX_MDA_DIMS = 50;
	static final int MAX_SIZE = 512 * 512 * 512;
	static final int MDA_TYPE_COMPLEX = -1;
	static final int MDA_TYPE_BYTE = -2;
	static final int MDA_TYPE_REAL = -3;
	static final int MDA_TYPE_SHORT = -4;
	static final int MDA_TYPE_INT32 = -5;
	static final int MDA_TYPE_UINT16 = -6;

	private int[] m_size;
	private double[] m_data_real;
	private int m_data_type = MDA_TYPE_REAL;

	public Mda() {
		m_size = new int[MAX_MDA_DIMS];
		Arrays.fill(m_size, 1);
		m_data_real = new double[1];
	}

	// data type
	public void setDataType(int dt) {
		m_data_type = dt;
		this.allocate(1, 1);
	}

	public int dataType() {
		return m_data_type;
	}

	// allocate
	public void allocate(int N1, int N2) {
		final int[] tmp = {N1, N2};
		allocate(tmp);
	}

	public void allocate(int N1, int N2, int N3) {
		final int[] tmp = {N1, N2, N3};
		allocate(tmp);
	}

	public void allocate(int N1, int N2, int N3, int N4) {
		final int[] tmp = {N1, N2, N3, N4};
		allocate(tmp);
	}

	public void allocate(int[] size) {
		Arrays.fill(m_size, 1);
		int NN = 1;
		for (int i = 0; i < size.length; i++) {
			if (size[i] <= 0) {
				size[i] = 1;
			}
			m_size[i] = size[i];
			NN *= size[i];
		}
		if (NN > MAX_SIZE) {
			System.err.println(String.format("Unable to allocate mda. Size is too large: %d.", NN));
			allocate(1, 1);
			return;
		}
		if (NN > 0) {
			m_data_real = new double[NN];
		}
	}

	// size
	public int[] size() {
		return m_size;
	}

	public int size(int dim) {
		if (dim >= MAX_MDA_DIMS) {
			return 0;
		}
		if (dim < 0) {
			return 0;
		}
		return m_size[dim];
	}

	public int N1() {
		return m_size[0];
	}

	;
	public int N2() {
		return m_size[1];
	}

	;
	public int N3() {
		return m_size[2];
	}

	;
	public int N4() {
		return m_size[3];
	}

	;
	public int N5() {
		return m_size[4];
	}

	;

	public int dimCount() {
		int ret = 2;
		for (int i = 2; i < MAX_MDA_DIMS; i++) {
			if (m_size[i] > 1) {
				ret = i + 1;
			}
		}
		return ret;
	}

	public int totalSize() {
		int ret = 1;
		for (int j = 0; j < m_size.length; j++) {
			ret *= m_size[j];
		}
		return ret;
	}

	// value
	public double value1(int i) {
		return m_data_real[i];
	}

	public double value(int i1, int i2) {
		final int[] tmp = {i1, i2};
		return value(tmp);
	}

	public double value(int i1, int i2, int i3) {
		final int[] tmp = {i1, i2, i3};
		return value(tmp);
	}

	public double value(int i1, int i2, int i3, int i4) {
		final int[] tmp = {i1, i2, i3, i4};
		return value(tmp);
	}

	public double value(int[] ind) {
		final int ii = get_index(ind);
		return m_data_real[ii];
	}

	// setValue
	public double setValue(double val, int i1, int i2) {
		final int[] tmp = {i1, i2};
		return setValue(val, tmp);
	}

	public double setValue(double val, int i1, int i2, int i3) {
		final int[] tmp = {i1, i2, i3};
		return setValue(val, tmp);
	}

	public double setValue(double val, int i1, int i2, int i3, int i4) {
		final int[] tmp = {i1, i2, i3, i4};
		return setValue(val, tmp);
	}

	public double setValue(double val, int[] ind) {
		final int ii = get_index(ind);
		return m_data_real[ii] = val;
	}

	public void setValue1(double val, int i) {
		m_data_real[i] = val;
	}

	public void setValues(float[] data) {
		if (data.length != m_data_real.length) {
			System.err.format("Error in mda setValues (float) %d!=%d\n", data.length, m_data_real.length);
			return;
		}
		if (m_data_real.length != data.length) {
			return;
		}
		for (int i = 0; i < m_data_real.length; i++) {
			m_data_real[i] = data[i];
		}
	}

	public void setValues(double[] data) {
		if (data.length != m_data_real.length) {
			System.err.format("Error in mda setValues (double) %d!=%d\n", data.length, m_data_real.length);
			return;
		}
		if (m_data_real.length != data.length) {
			return;
		}
		for (int i = 0; i < m_data_real.length; i++) {
			m_data_real[i] = data[i];
		}
	}

	public void setValues(byte[] data) {
		if (data.length != m_data_real.length) {
			System.err.format("Error in mda setValues (byte) %d!=%d\n", data.length, m_data_real.length);
			return;
		}
		if (m_data_real.length != data.length) {
			return;
		}
		for (int i = 0; i < m_data_real.length; i++) {
			m_data_real[i] = data[i];
		}
	}

	public void setValues(short[] data) {
		if (data.length != m_data_real.length) {
			System.err.format("Error in mda setValues (short) %d!=%d\n", data.length, m_data_real.length);
			return;
		}
		if (m_data_real.length != data.length) {
			return;
		}
		for (int i = 0; i < m_data_real.length; i++) {
			m_data_real[i] = data[i];
		}
	}

	public void setValues(int[] data) {
		if (data.length != m_data_real.length) {
			System.err.format("Error in mda setValues (int) %d!=%d\n", data.length, m_data_real.length);
			return;
		}
		if (m_data_real.length != data.length) {
			return;
		}
		for (int i = 0; i < m_data_real.length; i++) {
			m_data_real[i] = data[i];
		}
	}

	public Mda getDataXY(int inds[]) {
		Mda ret = new Mda();
		ret.allocate(N1(), N2());
		int[] inds0 = new int[inds.length + 2];
		for (int j = 0; j < inds.length; j++) {
			inds0[j + 2] = inds[j];
		}
		for (int y = 0; y < m_size[1]; y++) {
			inds0[1] = y;
			for (int x = 0; x < m_size[0]; x++) {
				inds0[0] = x;
				ret.setValue(value(inds0), x, y);
			}
		}
		return ret;
	}

	public Mda getDataXZ(int inds[]) {
		Mda ret = new Mda();
		ret.allocate(N1(), N3());
		int[] inds0 = new int[inds.length + 2];
		inds0[1] = inds[0];
		for (int j = 1; j < inds.length; j++) {
			inds0[j + 2] = inds[j];
		}
		for (int z = 0; z < m_size[2]; z++) {
			inds0[2] = z;
			for (int x = 0; x < m_size[0]; x++) {
				inds0[0] = x;
				ret.setValue(value(inds0), x, z);
			}
		}
		return ret;
	}

	public Mda getDataYZ(int inds[]) {
		Mda ret = new Mda();
		ret.allocate(N2(), N3());
		int[] inds0 = new int[inds.length + 2];
		inds0[0] = inds[0];
		for (int j = 1; j < inds.length; j++) {
			inds0[j + 2] = inds[j];
		}
		for (int z = 0; z < m_size[2]; z++) {
			inds0[2] = z;
			for (int y = 0; y < m_size[1]; y++) {
				inds0[1] = y;
				ret.setValue(value(inds0), y, z);
			}
		}
		return ret;
	}

	public Mda transpose() {
		Mda ret = new Mda();
		int[] size = m_size.clone();
		int s1 = size[0], s2 = size[1];
		size[0] = s2;
		size[1] = s1;
		ret.allocate(size);
		int tot_size = this.totalSize();
		int num_planes = tot_size / (size[0] * size[1]);
		for (int i = 0; i < num_planes; i++) {
			int offset = i * size[0] * size[1];
			for (int y = 0; y < m_size[1]; y++) {
				for (int x = 0; x < m_size[0]; x++) {
					ret.setValue1(this.value1(offset + x + y * m_size[0]), offset + y + x * m_size[1]);
				}
			}
		}
		return ret;
	}

	// get_index
	public int get_index(int[] ind) {
		int ret = 0;
		int prod = 1;
		for (int i = 0; i < ind.length; i++) {
			if (ind[i] >= m_size[i]) {
				return 0;
			}
			if (ind[i] < 0) {
				return 0;
			}
			ret += ind[i] * prod;
			if (i < m_size.length) {
				prod *= m_size[i];
			}
		}
		return ret;
	}

	// read
	public boolean read(String path) {

		this.allocate(1, 1);

		File file = new File(path);
		FileInputStream fis = null;

		try {
			fis = new FileInputStream(file);
			DataInputStream dis = new DataInputStream(fis);
			if (!do_read(dis)) {
				fis.close();
				return false;

			}
			fis.close();
			return true;

		} catch (Exception e) {
			try {
				fis.close();
			} catch (Exception e2) 
			{
				e2.printStackTrace();
			}
			e.printStackTrace();
			return false;
		}
	}

	public static int swap(int value) {
		int b1 = (value >> 0) & 0xff;
		int b2 = (value >> 8) & 0xff;
		int b3 = (value >> 16) & 0xff;
		int b4 = (value >> 24) & 0xff;

		return b1 << 24 | b2 << 16 | b3 << 8 | b4 << 0;
	}

	public static short swap(short value) {
		int b1 = value & 0xff;
		int b2 = (value >> 8) & 0xff;

		return (short) (b1 << 8 | b2 << 0);
	}

	public static float swap(float value) {
		int intValue = Float.floatToIntBits(value);
		intValue = swap(intValue);
		return Float.intBitsToFloat(intValue);
	}

	public static int read_int(DataInputStream dis) throws IOException {
		return swap(dis.readInt());
	}

	public static short read_short(DataInputStream dis) throws IOException {
		return swap(dis.readShort());
	}

	public static short read_unsigned_short(DataInputStream dis) throws IOException {
		return swap((short) dis.readUnsignedShort());
	}

	public static float read_float(DataInputStream dis) throws IOException {
		return swap(dis.readFloat());
	}

	public static void write_int(DataOutputStream dos, int val) throws IOException {
		dos.writeInt(swap(val));
	}

	public static void write_short(DataOutputStream dos, short val) throws IOException {
		dos.writeShort(swap(val));
	}

	public static void write_float(DataOutputStream dos, float val) throws IOException {
		dos.writeFloat(swap(val));
	}

	public boolean do_read(DataInputStream dis) throws IOException {
		int hold_num_dims;
		int[] hold_dims = new int[MAX_MDA_DIMS];
		hold_num_dims = read_int(dis);
		int data_type;
		if (hold_num_dims < 0) {
			data_type = hold_num_dims;
			int num_bytes;
			num_bytes = read_int(dis);
			hold_num_dims = read_int(dis);
		} else {
			data_type = MDA_TYPE_COMPLEX;
		}
		if (hold_num_dims > MAX_MDA_DIMS) {
			System.err.println(String.format("number of dimensions exceeds maximum: %d", hold_num_dims));
			return false;
		}
		if (hold_num_dims <= 0) {
			System.err.println(String.format("unexpected number of dimensions: %d", hold_num_dims));
			return false;
		}
		hold_dims = Arrays.copyOfRange(hold_dims, 0, hold_num_dims);
		for (int j = 0; j < hold_num_dims; j++) {
			int holdval = read_int(dis);
			hold_dims[j] = holdval;
		}
		{
			this.setDataType(data_type);
			this.allocate(hold_dims);
			int N = this.totalSize();
			if (data_type == MDA_TYPE_COMPLEX) {
				//need to improve efficiency of this read
				for (int ii = 0; ii < N; ii++) {
					double re0 = read_float(dis);
					double im0 = read_float(dis);
					m_data_real[ii] = re0;
				}
			} else if (data_type == MDA_TYPE_REAL) {
				byte[] bytes=new byte[N*4];
				dis.read(bytes);
				FloatBuffer FB=ByteBuffer.wrap(bytes).asFloatBuffer();
				for (int ii = 0; ii < N; ii++) {
					float re0 = FB.get(ii);
					re0=swap(re0);
					m_data_real[ii] = re0;
				}
			} else if (data_type == MDA_TYPE_SHORT) {
				for (int ii = 0; ii < N; ii++) {
					double re0 = read_short(dis);
					m_data_real[ii] = re0;
				}
			} else if (data_type == MDA_TYPE_UINT16) {
				for (int ii = 0; ii < N; ii++) {
					int re0 = read_unsigned_short(dis);
					m_data_real[ii] = re0;
				}
			} else if (data_type == MDA_TYPE_INT32) {
				for (int ii = 0; ii < N; ii++) {
					int re0 = read_int(dis);
					m_data_real[ii] = re0;
				}
			} else if (data_type == MDA_TYPE_BYTE) {
				byte[] bytes=new byte[N];
				dis.read(bytes);
				ByteBuffer BB=ByteBuffer.wrap(bytes);
				for (int ii = 0; ii < N; ii++) {
					float re0 = BB.get(ii);
					m_data_real[ii] = re0;
				}
			} else {
				System.err.println(String.format("Unrecognized data type %d", data_type));
				return false;
			}
		}
		return true;
	}

	// write
	public boolean write(String path) {

		File file = new File(path);
		FileOutputStream fos = null;

		try {
			fos = new FileOutputStream(file);
			DataOutputStream dos = new DataOutputStream(fos);
			if (!do_write(dos)) {
				fos.close();
				return false;

			}
			fos.close();
			return true;

		} catch (IOException e) {
			try {
				fos.close();
			} catch (IOException e2) {
			}
			e.printStackTrace();
			return false;
		}
	}

	public boolean do_write(DataOutputStream dos) throws IOException {
		write_int(dos, m_data_type);
		if (m_data_type == MDA_TYPE_UINT16) {
			m_data_type = MDA_TYPE_SHORT; // cannot write unsigned short. Java
		}											// doesn't support it. :(
		int num_bytes = 4;
		if (m_data_type == MDA_TYPE_COMPLEX) {
			num_bytes = 8;
		} else if (m_data_type == MDA_TYPE_BYTE) {
			num_bytes = 1;
		} else if (m_data_type == MDA_TYPE_SHORT) {
			num_bytes = 2;
		} else if (m_data_type == MDA_TYPE_UINT16) {
			num_bytes = 2;
		}
		write_int(dos, num_bytes);
		int num_dims = 2;
		for (int i = 2; i < m_size.length; i++) {
			if (m_size[i] > 1) {
				num_dims = i + 1;
			}
		}
		write_int(dos, num_dims);
		for (int ii = 0; ii < num_dims; ii++) {
			write_int(dos, m_size[ii]);
		}
		int N = this.totalSize();
		if (m_data_type == MDA_TYPE_COMPLEX) {
			for (int i = 0; i < N; i++) {
				float re0 = (float) m_data_real[i];
				write_float(dos, re0);
				write_float(dos, 0);
			}
		} else if (m_data_type == MDA_TYPE_REAL) {
			for (int i = 0; i < N; i++) {
				float re0 = (float) m_data_real[i];
				write_float(dos, re0);
			}
		} else if (m_data_type == MDA_TYPE_BYTE) {
			for (int i = 0; i < N; i++) {
				int re0 = (int) m_data_real[i];
				dos.writeByte(re0);
			}
		} else if (m_data_type == MDA_TYPE_SHORT) {
			for (int i = 0; i < N; i++) {
				int re0 = (int) m_data_real[i];
				write_short(dos, (short) re0);
			}
		} else if (m_data_type == MDA_TYPE_INT32) {
			for (int i = 0; i < N; i++) {
				int re0 = (int) m_data_real[i];
				write_int(dos, re0);
			}
		}

		return true;
	}
}
