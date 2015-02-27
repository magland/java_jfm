/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssview;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import jviewmda.Mda;
import org.magland.jcommon.JUtils;

/**
 *
 * @author magland
 */
public class SSViewController {

	public SSViewController() {

	}

	public void showTimeSeries(String fname) {
		showTimeSeries(fname, "", "", "");
	}

	public void showTimeSeries(String fname, String labels_fname) {
		showTimeSeries(fname, labels_fname, "", "");
	}

	public void showTimeSeries(String fname, String labels_fname, String labels2_fname) {
		showTimeSeries(fname, labels_fname, labels2_fname, "");
	}

	public void showTimeSeries(String fname, String labels_fname, String labels2_fname, String labels3_fname) {
		Stage stage = new Stage();

		SSTimeSeriesWidget WW = new SSTimeSeriesWidget();

		StackPane root = new StackPane();
		root.getChildren().add(WW);

		Scene scene = new Scene(root, 1000, 500);

		stage.setTitle("SSView");
		stage.setScene(scene);
		stage.show();

		Mda X = new Mda();
		if (!X.read(fname)) {
			JUtils.showError("Unable to read", "Unable to load array: " + fname);
			return;
		}
		WW.addDataArray(X, null);
		Mda all_labels = new Mda();

		if (!labels_fname.isEmpty()) {
			Mda Y = new Mda();
			if (!Y.read(labels_fname)) {
				JUtils.showError("Unable to read", "Unable to load array: " + labels_fname);
				return;
			}
			WW.addDataArray(Y, Y);
		}

		if (!labels2_fname.isEmpty()) {
			Mda Y = new Mda();
			if (!Y.read(labels2_fname)) {
				JUtils.showError("Unable to read", "Unable to load array: " + labels2_fname);
				return;
			}
			WW.addDataArray(Y, Y);

		}

		if (!labels3_fname.isEmpty()) {
			Mda Y = new Mda();
			if (!Y.read(labels3_fname)) {
				JUtils.showError("Unable to read", "Unable to load array: " + labels3_fname);
				return;
			}
			WW.addDataArray(Y, Y);
		}

	}
}
