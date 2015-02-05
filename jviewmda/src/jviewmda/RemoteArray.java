/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewmda;

import static jviewmda.Mda.MAX_MDA_DIMS;
import org.magland.jcommon.ObjectCallback;
import org.magland.jcommon.SJO;
import org.magland.jcommon.SJOCallback;

/**
 *
 * @author magland
 */
public abstract class RemoteArray {

	public abstract void initialize(SJOCallback callback);

	public abstract int size(int dim);

	public abstract void getDataXY(int[] inds, ObjectCallback callback, SJO params);

	public abstract void getDataXZ(int[] inds, ObjectCallback callback, SJO params);

	public abstract void getDataYZ(int[] inds, ObjectCallback callback, SJO params);

	public int N1() {
		return size(0);
	}

	public int N2() {
		return size(1);
	}

	public int N3() {
		return size(2);
	}

	public int N4() {
		return size(3);
	}

	public int N5() {
		return size(4);
	}

	public int N6() {
		return size(5);
	}

	public int dimCount() {
		int ret = 2;
		for (int i = 2; i < MAX_MDA_DIMS; i++) {
			if (size(i) > 1) {
				ret = i + 1;
			}
		}
		return ret;
	}

	public int totalSize() {
		int ret = 1;
		int numdims = dimCount();
		for (int i = 0; i < numdims; i++) {
			ret *= size(i);
		}
		return ret;
	}

	public void getDataXY(int[] inds, ObjectCallback callback) {
		getDataXY(inds, callback, new SJO("{}"));
	}

	public void getDataXZ(int[] inds, ObjectCallback callback) {
		getDataXZ(inds, callback, new SJO("{}"));
	}

	public void getDataYZ(int[] inds, ObjectCallback callback) {
		getDataYZ(inds, callback, new SJO("{}"));
	}

}
