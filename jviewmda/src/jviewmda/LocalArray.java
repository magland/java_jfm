package jviewmda;

import org.magland.jcommon.ObjectCallback;
import org.magland.jcommon.SJO;
import org.magland.jcommon.SJOCallback;

/**
 *
 * @author magland
 */
class LocalArray extends RemoteArray {

	Mda m_array = new Mda();
	Boolean m_initialized = false;

	public LocalArray(Mda X) {
		m_array = X;
	}

	public int size(int dim) {
		return m_array.size(dim);
	}

	public void initialize(SJOCallback callback) {
		m_initialized = true;
		callback.run(new SJO("{success:true}"));
	}

	public void getDataXY(int[] inds, ObjectCallback callback, SJO params) {
		if (m_initialized) {
			callback.run(m_array.getDataXY(inds));
		} else {
			callback.run(new Mda());
		}
	}

	public void getDataXZ(int[] inds, ObjectCallback callback, SJO params) {
		if (m_initialized) {
			callback.run(m_array.getDataXZ(inds));
		} else {
			callback.run(new Mda());
		}
	}

	public void getDataYZ(int[] inds, ObjectCallback callback, SJO params) {
		if (m_initialized) {
			callback.run(m_array.getDataYZ(inds));
		} else {
			callback.run(new Mda());
		}
	}
}
