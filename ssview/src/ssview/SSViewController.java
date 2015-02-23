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
		Stage stage=new Stage();
		
		SSTimeSeriesWidget WW=new SSTimeSeriesWidget();
		
		StackPane root = new StackPane();
		root.getChildren().add(WW);
		
		Scene scene = new Scene(root, 600, 500);
		
		stage.setTitle("SSView");
		stage.setScene(scene);
		stage.show();
		
		Mda X=new Mda();
		if (!X.read(fname)) {
			JUtils.showError("Unable to read","Unable to load array: "+fname);
		}
		WW.setData(X);
	}
}
