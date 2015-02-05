package jviewmda;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;
import static jviewmda.Mda.MAX_MDA_DIMS;
import org.magland.jcommon.JUtils;
import org.magland.jcommon.ObjectCallback;
import org.magland.jcommon.SJO;
import org.magland.jcommon.SJOCallback;
import org.magland.wfs.WFSClient;

/**
 *
 * @author magland
 */
public class WFSMda extends RemoteArray {

	WFSClient m_client = null;
	String m_path = "";
	Boolean m_initialized = false;
	int[] m_N = new int[MAX_MDA_DIMS];
	int m_header_size = 0;
	int m_num_dims = 1;
	String m_data_type = "";
	int m_data_type_size = 1;
	Map<String, Object> m_retrieved_arrays = new HashMap<>();
	Boolean m_is_nii = false;
	NiiHeaderParser m_nii_header = new NiiHeaderParser();
	boolean[] m_dim_swaps = new boolean[Mda.MAX_MDA_DIMS];

	public WFSMda() {
		for (int j = 0; j < m_N.length; j++) {
			m_N[j] = 1;
		}
	}

	public void setClient(WFSClient client) {
		m_client = client;
		m_initialized = false;
	}

	public void setPath(String path) {
		m_path = path;
		m_initialized = false;
	}

	public void initialize(SJOCallback callback) {
		SJO ret = new SJO("{}");
		if ((m_client == null) || (m_initialized)) {
			callback.run(ret);
			return;
		}
		do_initialize(callback);
	}

	public Boolean isInitialized() {
		return m_initialized;
	}

	public int size(int dim) {
		if (dim < 0) {
			return 0;
		}
		if (dim >= m_N.length) {
			return 1;
		}
		return m_N[dim];
	}

	public void getDataXY(int[] inds, ObjectCallback callback, SJO params) {
		if (!isInitialized()) {
			do_initialize(tmp1 -> {
				if (isInitialized()) {
					getDataXY(inds, callback, params);
				} else {
					callback.run(new Mda());
				}
			});
			return;
		}
		String tmp_key0 = "getDataXY--";
		for (int ii = 0; ii < inds.length; ii++) {
			if (inds[ii] > 0) {
				tmp_key0 += String.format("(%d:%d)", ii, inds[ii]);
			}
		}
		final String key0 = tmp_key0;
		if (m_retrieved_arrays.containsKey(key0)) {
			callback.run(m_retrieved_arrays.get(key0));
			return;
		}
		get_data_xy(inds, tmp1 -> {
			m_retrieved_arrays.put(key0, tmp1);
			callback.run(tmp1);
		},
				params);
	}

	public void getDataXZ(int[] inds, ObjectCallback callback, SJO params) {
		if (!isInitialized()) {
			do_initialize(tmp1 -> {
				if (isInitialized()) {
					getDataXZ(inds, callback, params);
				} else {
					callback.run(new Mda());
				}

			});
		}
		String tmp_key0 = "getDataXZ--";
		for (int ii = 0; ii < inds.length; ii++) {
			if (inds[ii] > 0) {
				tmp_key0 += String.format("(%d:%d)", ii, inds[ii]);
			}
		}
		final String key0 = tmp_key0;
		if (m_retrieved_arrays.containsKey(key0)) {
			callback.run(m_retrieved_arrays.get(key0));
			return;
		}
		get_data_xz(inds, tmp1 -> {
			m_retrieved_arrays.put(key0, tmp1);
			callback.run(tmp1);
		},
				params);
	}

	public void getDataYZ(int[] inds, ObjectCallback callback, SJO params) {
		if (!isInitialized()) {
			do_initialize(tmp1 -> {
				if (isInitialized()) {
					getDataYZ(inds, callback, params);
				} else {
					callback.run(new Mda());
				}

			});
		}
		String tmp_key0 = "getDataYZ--";
		for (int ii = 0; ii < inds.length; ii++) {
			if (inds[ii] > 0) {
				tmp_key0 += String.format("(%d:%d)", ii, inds[ii]);
			}
		}
		final String key0 = tmp_key0;
		if (m_retrieved_arrays.containsKey(key0)) {
			callback.run(m_retrieved_arrays.get(key0));
			return;
		}
		get_data_yz(inds, tmp1 -> {
			m_retrieved_arrays.put(key0, tmp1);
			callback.run(tmp1);
		},
				params);
	}

	private void do_initialize(SJOCallback callback) {
		if (m_client == null) {
			System.err.println("Error reading mda, client is null: " + m_path);
			callback.run(new SJO("{success:false}"));
			return;
		}
		if (m_initialized) {
			callback.run(new SJO("{success:true}"));
			return;
		}
		String suf = JUtils.getFileSuffix(m_path);
		if (suf.equals("nii")) {
			do_initialize_nii(callback);
			return;
		}
		m_client.readFileBytes(m_path, "0:59", tmp1 -> {
			byte[] bytes = tmp1.get("data").toByteArray();
			if (bytes.length < 60) {
				System.err.println("Error reading mda header: " + m_path);
				m_initialized = true;
				callback.run(new SJO("{success:false}"));
				return;
			}

			int[] XX = bytearray_to_intarray(bytes);

			int ii = 0;
			int code = XX[ii];
			ii++;
			int num_dims = 1;
			if (code > 0) {
				num_dims = code;
				code = -1;
			} else {
				ii++;
				num_dims = XX[ii];
				ii++;
			}
			if (num_dims > 6) {
				System.err.format("Problem reading mda, too many dimensions %d\n", num_dims);
				m_initialized = true;
				callback.run(new SJO("{success:false}"));
				return;
			}
			int[] S = new int[6];
			for (int j = 0; j < num_dims; j++) {
				S[j] = XX[ii];
				ii++;
			}
			m_header_size = ii * 4;
			m_num_dims = num_dims;
			for (int k = 0; k < m_num_dims; k++) {
				m_N[k] = S[k];
			}
			m_data_type = "";
			if (code == -1) {
				m_data_type = "complex";
				m_data_type_size = 8;
			} else if (code == -2) {
				m_data_type = "byte";
				m_data_type_size = 1;
			} else if (code == -3) {
				m_data_type = "float32";
				m_data_type_size = 4;
			} else if (code == -4) {
				m_data_type = "int16";
				m_data_type_size = 2;
			} else if (code == -5) {
				m_data_type = "int32";
				m_data_type_size = 4;
			} else {
				System.err.format("Problem reading mda, unexpected data type code: %d\n", code);
				m_initialized = true;
				callback.run(new SJO("{success:false}"));
				return;
			}
			m_initialized = true;
			callback.run(new SJO("{success:true}"));
		});
	}

	public void do_initialize_nii(SJOCallback callback) {
		m_is_nii = true;
		m_client.readFileBytes(m_path, "0:359", tmp1 -> {
			byte[] bytes = tmp1.get("data").toByteArray();
			if (bytes.length < 348) {
				System.err.println("Error reading nii header: " + m_path);
				m_initialized = true;
				callback.run(new SJO("{success:false}"));
				return;
			}
			NiiHeaderParser header = m_nii_header;
			header.parse(bytes);
			int num_dims = header.dim[0];
			m_num_dims = num_dims;
			if ((num_dims <= 0) || (num_dims >= 10)) {
				System.err.println("Error reading nii header (unexpected number of dims): " + m_path);
				m_initialized = true;
				callback.run(new SJO("{success:false}"));
				return;
			}
			for (int k = 0; k < num_dims; k++) {
				m_N[k] = header.dim[k + 1];
			}
			m_data_type = header.datatype_name;
			m_data_type_size = header.datatype_size;
			m_header_size = (int) header.vox_offset;
			if (m_header_size <= 50) {
				System.err.println("Header size is too small: " + m_path);
				callback.run(new SJO("{success:false}"));
				return;
			}
			/*
			 //fix this!
			 m_transformation.setIdentity();
			 for (var i=0; i<=3; i++) {
			 m_transformation.set(m_header.srow_x[i]||0,0,i);
			 m_transformation.set(m_header.srow_y[i]||0,1,i);
			 m_transformation.set(m_header.srow_z[i]||0,2,i);
			 }
			 */

			m_initialized = true;
			callback.run(new SJO("{success:true}"));

		});
	}

	final ByteOrder byte_order = ByteOrder.LITTLE_ENDIAN;

	private int[] bytearray_to_intarray(byte[] bytes) {
		IntBuffer intBuf
				= ByteBuffer.wrap(bytes)
				.order(byte_order)
				.asIntBuffer();
		int[] XX = new int[intBuf.remaining()];
		intBuf.get(XX);
		return XX;
	}

	private float[] bytearray_to_floatarray(byte[] bytes) {
		FloatBuffer floatBuf
				= ByteBuffer.wrap(bytes)
				.order(byte_order)
				.asFloatBuffer();
		float[] XX = new float[floatBuf.remaining()];
		floatBuf.get(XX);
		return XX;
	}

	private short[] bytearray_to_shortarray(byte[] bytes) {
		ShortBuffer shortBuf
				= ByteBuffer.wrap(bytes)
				.order(byte_order)
				.asShortBuffer();
		short[] XX = new short[shortBuf.remaining()];
		shortBuf.get(XX);
		return XX;
	}

	private void get_data_nii(int[] inds, String plane, ObjectCallback callback, SJO params) {
		int b1 = m_header_size;

		if (!m_initialized) {
			System.err.println("Unexpected problem, not initialized");
		}
		if (inds[0] < 0) {
			callback.run(new Mda());
			return;
		}

		String bytes0 = "";
		int expected_length = 0;
		if (plane.equals("XY")) {
			bytes0 = String.format("subarray;offset=%d;", m_header_size)
					+ String.format("dimensions=%d,%d,%d,%d;", m_N[0], m_N[1], m_N[2], m_N[3])
					+ String.format("index=*,*,%d,%d;", inds[0], inds[1])
					+ String.format("size=%d", m_data_type_size);
			expected_length = m_N[0] * m_N[1] * m_data_type_size;
		} else if (plane.equals("XZ")) {
			bytes0 = String.format("subarray;offset=%d;", m_header_size)
					+ String.format("dimensions=%d,%d,%d,%d;", m_N[0], m_N[1], m_N[2], m_N[3])
					+ String.format("index=*,%d,*,%d;", inds[0], inds[1])
					+ String.format("size=%d", m_data_type_size);
			expected_length = m_N[0] * m_N[2] * m_data_type_size;
		} else if (plane.equals("YZ")) {
			bytes0 = String.format("subarray;offset=%d;", m_header_size)
					+ String.format("dimensions=%d,%d,%d,%d;", m_N[0], m_N[1], m_N[2], m_N[3])
					+ String.format("index=%d,*,*,%d;", inds[0], inds[1])
					+ String.format("size=%d", m_data_type_size);
			expected_length = m_N[1] * m_N[2] * m_data_type_size;
		}

		final int expected_length0 = expected_length;

		final String bytes_final = bytes0;
		m_client.readFileBytes(m_path, bytes0, tmp1 -> {
			byte[] bytes = tmp1.get("data").toByteArray();
			if (bytes.length != expected_length0) {
				System.err.format("Problem reading nii data... unexpected number of bytes %d != %d: %s %s\n", bytes.length, expected_length0, m_path, bytes_final);
				callback.run(new Mda());
				return;
			}
			Mda ret = new Mda();
			if (plane.equals("XY")) {
				ret.allocate(m_N[0], m_N[1]);
			} else if (plane.equals("XZ")) {
				ret.allocate(m_N[0], m_N[2]);
			} else if (plane.equals("YZ")) {
				ret.allocate(m_N[1], m_N[2]);
			}
			if (m_data_type.equals("uchar")) {
				ret.setValues(bytes);
			} else if (m_data_type.equals("sshort")) {
				ShortBuffer short_buffer = ByteBuffer.wrap(bytes).order(m_nii_header.nii_byte_order).asShortBuffer();
				short[] short_array = new short[short_buffer.remaining()];
				short_buffer.get(short_array);
				ret.setValues(short_array);
			} else if (m_data_type.equals("sint")) {
				IntBuffer int_buffer = ByteBuffer.wrap(bytes).order(m_nii_header.nii_byte_order).asIntBuffer();
				int[] int_array = new int[int_buffer.remaining()];
				int_buffer.get(int_array);
				ret.setValues(int_array);
			} else if (m_data_type.equals("float")) {
				FloatBuffer float_buffer = ByteBuffer.wrap(bytes).order(m_nii_header.nii_byte_order).asFloatBuffer();
				float[] float_array = new float[float_buffer.remaining()];
				float_buffer.get(float_array);
				ret.setValues(float_array);
			} else if (m_data_type.equals("complex")) {
				System.err.format("nii data format not supported: %s\n", m_data_type);
				callback.run(new Mda());
				return;
			} else if (m_data_type.equals("double")) {
				DoubleBuffer double_buffer = ByteBuffer.wrap(bytes).order(m_nii_header.nii_byte_order).asDoubleBuffer();
				double[] double_array = new double[double_buffer.remaining()];
				double_buffer.get(double_array);
				ret.setValues(double_array);
			} else if (m_data_type.equals("schar")) {
				ret.setValues(bytes); //fix this!!
			} else if (m_data_type.equals("ushort")) {
				ShortBuffer short_buffer = ByteBuffer.wrap(bytes).order(m_nii_header.nii_byte_order).asShortBuffer();
				short[] short_array = new short[short_buffer.remaining()];
				short_buffer.get(short_array);
				int[] int_array = new int[short_array.length];
				for (int i = 0; i < short_array.length; i++) {
					short shortval = short_array[i];
					int_array[i] = shortval >= 0 ? shortval : 0x10000 + shortval;
				}
				ret.setValues(int_array);
			} else if (m_data_type.equals("uint")) {
				IntBuffer int_buffer = ByteBuffer.wrap(bytes).order(m_nii_header.nii_byte_order).asIntBuffer();
				int[] int_array = new int[int_buffer.remaining()];
				int_buffer.get(int_array);
				long[] long_array = new long[int_array.length];
				for (int i = 0; i < int_array.length; i++) {
					int intval = int_array[i];
					long_array[i] = intval & 0xFFFFFFFFL;
				}
				ret.setValues(int_array);
			} else {
				System.err.format("nii data format not supported: %s\n", m_data_type);
				callback.run(new Mda());
				return;
			}
			callback.run(ret);
		});
	}

	private void get_data_xy(int[] inds, ObjectCallback callback, SJO params) {
		if (m_is_nii) {
			get_data_nii(inds, "XY", callback, params);
			return;
		}

		if (m_client == null) {
			callback.run(new Mda());
			return;
		}

		int b1_tmp = m_header_size;
		int factor = m_N[0] * m_N[1] * m_data_type_size;
		for (int k = 2; k < m_num_dims; k++) {
			b1_tmp = b1_tmp + factor * (inds[k - 2]);
			factor = factor * m_N[k];
		}
		final int b1 = b1_tmp;
		final int b2 = b1 + m_N[0] * m_N[1] * m_data_type_size;
		final String bytes0 = String.format("%d:%d", b1, b2);
		if (params.get("check_only").toBoolean()) {
			callback.run(new Mda());
			return;
		}
		m_client.readFileBytes(m_path, bytes0, d0 -> {
			final byte[] data = d0.get("data").toByteArray();
			if (data.length != b2 - b1 + 1) {
				System.err.format("Problem reading mda dataXY... unexpected number of bytes %d != %d\n", data.length, b2 - b1);
				callback.run(null);
				return;
			}
			Mda ret = new Mda();
			ret.allocate(m_N[0], m_N[1]);
			if (m_data_type == "complex") {
				float[] tmp = bytearray_to_floatarray(data);
				float[] tmp_real = new float[tmp.length / 2];
				float[] tmp_imag = new float[tmp.length / 2];
				for (int i = 0; i < tmp_real.length; i++) {
					tmp_real[i] = tmp[i * 2];
					tmp_imag[i] = tmp[i * 2 + 1];
				}
				ret.setValues(tmp_real);
			} else if (m_data_type == "byte") {
				ret.setValues(data);
			} else if (m_data_type == "float32") {
				ret.setValues(bytearray_to_floatarray(data));
			} else if (m_data_type == "int16") {
				ret.setValues(bytearray_to_shortarray(data));
			} else if (m_data_type == "int32") {
				ret.setValues(bytearray_to_intarray(data));
			} else {
				System.err.println("Unexpected data type in getDataXY.");
				callback.run(null);
				return;
			}
			callback.run(ret);
		});
	}

	private void get_data_xz(int[] inds, ObjectCallback callback, SJO params) {
		if (m_is_nii) {
			get_data_nii(inds, "XZ", callback, params);
			return;
		}
		callback.run(new Mda());
	}

	private void get_data_yz(int[] inds, ObjectCallback callback, SJO params) {
		if (m_is_nii) {
			get_data_nii(inds, "YZ", callback, params);
			return;
		}
		callback.run(new Mda());
	}
}

class NiiHeaderParser {

	public long sizeof_hdr = 0; //uint
	public byte[] data_type = new byte[10];
	public byte[] db_name = new byte[18];
	public long extents = 0; //uint
	public int session_error = 0; //ushort
	public byte regular = 0;
	public byte dim_info = 0;
	public int[] dim = new int[8]; //ushort
	public float intent_p1 = 0;
	public float intent_p2 = 0;
	public float intent_p3 = 0;
	public int intent_code = 0; //ushort
	public int datatype = 0; //ushort
	public int bitpix = 0; //ushort
	public int slice_start = 0; //ushort
	public float[] pixdim = new float[8];
	public float vox_offset = 0;
	public float scl_slope = 0;
	public float scl_inter = 0;
	public float slice_end = 0;
	public byte slice_code = 0;
	public byte xyzt_units = 0;
	public float cal_max = 0;
	public float cal_min = 0;
	public float slice_duration = 0;
	public float toffset = 0;
	public long glmax = 0; //uint
	public long glmin = 0; //uint
	public byte[] descrip = new byte[80];
	public byte[] aux_file = new byte[24];
	public int qform_code = 0; //ushort
	public int sform_code = 0; //ushort
	public float quatern_b = 0;
	public float quatern_c = 0;
	public float quatern_d = 0;
	public float qoffset_x = 0;
	public float qoffset_y = 0;
	public float qoffset_z = 0;
	public float[] srow_x = new float[4];
	public float[] srow_y = new float[4];
	public float[] srow_z = new float[4];
	public byte[] intent_name = new byte[16];
	public byte[] magic = new byte[4];

	public String datatype_name = "";
	public int datatype_size = 0;

	byte[] m_header = new byte[0];
	int m_index = 0;

	public ByteOrder nii_byte_order = ByteOrder.LITTLE_ENDIAN;

	public void parse(byte[] header) {
		m_header = header;
		m_index = 0;

		// header_key substruct
		sizeof_hdr = scan_uint();

		if ((sizeof_hdr <= 0) || (sizeof_hdr > 1000)) {
			if (nii_byte_order == ByteOrder.LITTLE_ENDIAN) {
				nii_byte_order = ByteOrder.BIG_ENDIAN;
				parse(header);
				return;
			}
		}

		data_type = scan_uchar(10);
		db_name = scan_uchar(18);
		extents = scan_uint();
		session_error = scan_ushort();
		regular = scan_uchar();
		dim_info = scan_uchar();

		// image_dimension substruct
		dim = scan_ushort(8);
		intent_p1 = scan_float();
		intent_p2 = scan_float();
		intent_p3 = scan_float();
		intent_code = scan_ushort();
		datatype = scan_ushort();
		bitpix = scan_ushort();
		slice_start = scan_ushort();
		pixdim = scan_float(8);
		vox_offset = scan_float();
		scl_slope = scan_float();
		scl_inter = scan_float();
		slice_end = scan_ushort();
		slice_code = scan_uchar();
		xyzt_units = scan_uchar();
		cal_max = scan_float();
		cal_min = scan_float();
		slice_duration = scan_float();
		toffset = scan_float();
		glmax = scan_uint();
		glmin = scan_uint();

		// data_history substruct
		descrip = scan_uchar(80);
		aux_file = scan_uchar(24);
		qform_code = scan_ushort();
		sform_code = scan_ushort();
		quatern_b = scan_float();
		quatern_c = scan_float();
		quatern_d = scan_float();
		qoffset_x = scan_float();
		qoffset_y = scan_float();
		qoffset_z = scan_float();

		srow_x = scan_float(4);
		srow_y = scan_float(4);
		srow_z = scan_float(4);

		intent_name = scan_uchar(16);

		magic = scan_uchar(4);

		if (datatype == 2) {
			datatype_name = "uchar";
			datatype_size = 1;
		} else if (datatype == 4) {
			datatype_name = "sshort";
			datatype_size = 2;
		} else if (datatype == 8) {
			datatype_name = "sint";
			datatype_size = 4;
		} else if (datatype == 16) {
			datatype_name = "float";
			datatype_size = 4;
		} else if (datatype == 32) {
			datatype_name = "complex";
			datatype_size = 8;
		} else if (datatype == 64) {
			datatype_name = "double";
			datatype_size = 8;
		} else if (datatype == 256) {
			datatype_name = "schar";
			datatype_size = 8;
		} else if (datatype == 512) {
			datatype_name = "ushort";
			datatype_size = 2;
		} else if (datatype == 768) {
			datatype_name = "uint";
			datatype_size = 4;
		}
	}

	long scan_uint() {
		if (m_index + 4 > m_header.length) {
			return 0;
		}
		byte[] tmp = new byte[4];
		tmp[0] = m_header[m_index];
		m_index++;
		tmp[1] = m_header[m_index];
		m_index++;
		tmp[2] = m_header[m_index];
		m_index++;
		tmp[3] = m_header[m_index];
		m_index++;
		return (ByteBuffer.wrap(tmp).order(nii_byte_order).getInt() & 0xFFFFFFFFL);
	}

	byte scan_uchar() {
		if (m_index + 1 > m_header.length) {
			return 0;
		}
		byte[] tmp = new byte[1];
		tmp[0] = m_header[m_index];
		m_index++;
		return tmp[0];
	}

	float scan_float() {
		if (m_index + 4 > m_header.length) {
			return 0;
		}
		byte[] tmp = new byte[4];
		tmp[0] = m_header[m_index];
		m_index++;
		tmp[1] = m_header[m_index];
		m_index++;
		tmp[2] = m_header[m_index];
		m_index++;
		tmp[3] = m_header[m_index];
		m_index++;
		return ByteBuffer.wrap(tmp).order(nii_byte_order).getFloat();
	}

	int scan_ushort() {
		if (m_index + 4 > m_header.length) {
			return 0;
		}
		byte[] tmp = new byte[2];
		tmp[0] = m_header[m_index];
		m_index++;
		tmp[1] = m_header[m_index];
		m_index++;
		short shortval = ByteBuffer.wrap(tmp).order(nii_byte_order).getShort();
		int intval = shortval >= 0 ? shortval : 0x10000 + shortval;
		return intval;
	}

	long[] scan_uint(int num) {
		long[] ret = new long[num];
		for (int i = 0; i < num; i++) {
			ret[i] = scan_uint();
		}
		return ret;
	}

	byte[] scan_uchar(int num) {
		byte[] ret = new byte[num];
		for (int i = 0; i < num; i++) {
			ret[i] = scan_uchar();
		}
		return ret;
	}

	float[] scan_float(int num) {
		float[] ret = new float[num];
		for (int i = 0; i < num; i++) {
			ret[i] = scan_float();
		}
		return ret;
	}

	int[] scan_ushort(int num) {
		int[] ret = new int[num];
		for (int i = 0; i < num; i++) {
			ret[i] = scan_ushort();
		}
		return ret;
	}
}
