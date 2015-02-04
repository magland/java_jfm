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

		String fshost = "";
		if (JUtils.connectedToInternet()) {
			fshost = "97.107.129.125:8080";
			//fshost="localhost:8006";
		} else {
			System.err.println("Not connected to the internet.");
			fshost = "localhost:8006";
		}

		//WFSClient CC = new WFSClient("localhost:8006", "LesionProbe", "");
		WFSClient CC = new WFSClient(fshost, "fetalmri", "");

		DoubleWFSBrowser BB = new DoubleWFSBrowser();
		BB.addOpenFileHandler(new MdaOpenFileHandler());
		BB.setClient(CC);
		BB.initialize();

		StackPane root = new StackPane();
		root.getChildren().add(BB);

		Scene scene = new Scene(root, 300, 250);

		primaryStage.setTitle("Hello World!");
		primaryStage.setScene(scene);
		primaryStage.show();

//		BB.onSelectedItemChanged(()->{
//			System.out.println(wfs_browser.selectedItemPath());
//		});
//		BB.onItemActivated(()->{
//			System.out.println("ACTIVATED: "+wfs_browser.selectedItemPath());
//		});
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}

}
