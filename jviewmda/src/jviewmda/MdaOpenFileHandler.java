package jviewmda;

import java.util.HashSet;
import java.util.Set;
import org.magland.jcommon.JUtils;
import org.magland.wfs.OpenFileHandler;
import org.magland.wfs.WFSClient;

/**
 *
 * @author magland
 */
public class MdaOpenFileHandler implements OpenFileHandler {

	public Set<String> fileTypes() {
		Set<String> ret = new HashSet<>();
		ret.add("mda");
		ret.add("nii");
		return ret;
	}

	public void open(WFSClient client, String path) {
		WFSMda X = new WFSMda();
		X.setClient(client);
		X.setPath(path);

		ViewmdaWidget W = new ViewmdaWidget();
		JUtils.popupWidget(W, path);
		W.setRemoteArray(X, () -> {
		});
	}
}
