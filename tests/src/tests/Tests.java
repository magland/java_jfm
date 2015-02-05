/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import jviewmda.MdaOpenFileHandler;
import jviewmda.Viewmda3PlaneWidget;
import jviewmda.WFSMda;
import org.magland.jcommon.JUtils;
import org.magland.wfs.DoubleWFSBrowser;
import org.magland.wfs.WFSClient;

/**
 *
 * @author magland
 */
public class Tests extends Application {

	@Override
	public void start(Stage primaryStage) {

		String fshost;
		if (JUtils.connectedToInternet()) {
			fshost = "97.107.129.125:8080"; //peregrine01
			//fshost="localhost:8006";
			//fshost="bbvhub.org:6021"; //hoy05 (bbv)
		} else {
			System.err.println("Not connected to the internet.");
			fshost = "localhost:8006";
		}

		//WFSClient CC = new WFSClient("localhost:8006", "LesionProbe", "");
		WFSClient CC = new WFSClient(fshost, "LesionProbe", "");
		//String cache_dir = JUtils.createTemporaryDirectory("tests-cache");
		//System.out.println("Using cach path: " + cache_dir);
		//CC.setCachePath(cache_dir);

		if (false) {
			test_double_wfs_browser(primaryStage, CC);
		}
		if (true) {
			test_3plane_view(primaryStage, CC);
		}
	}

	void test_3plane_view(Stage stage, WFSClient CC) {

		WFSMda XX = new WFSMda();
		XX.setClient(CC);
		XX.setPath("Images/ID001_FLAIR.nii");

		Viewmda3PlaneWidget WW = new Viewmda3PlaneWidget();
		WW.setRemoteArray(XX, () -> {
			StackPane root = new StackPane();
			root.getChildren().add(WW);

			Scene scene = new Scene(root, 300, 250);

			stage.setTitle("Hello World!");
			stage.setScene(scene);
			stage.show();
		});
	}

	void test_double_wfs_browser(Stage stage, WFSClient CC) {
		DoubleWFSBrowser BB = new DoubleWFSBrowser();
		BB.addOpenFileHandler(new MdaOpenFileHandler());
		BB.setClient(CC);
		BB.initialize();

		StackPane root = new StackPane();
		root.getChildren().add(BB);

		Scene scene = new Scene(root, 300, 250);

		stage.setTitle("Hello World!");
		stage.setScene(scene);
		stage.show();
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}

}
