
package ssview;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import jviewmda.Mda;

/**
 *
 * @author magland
 */
public class Ssview extends Application {
	
	@Override
	public void start(Stage primaryStage) {
		
		SSTimeSeriesWidget WW=new SSTimeSeriesWidget();
		
		StackPane root = new StackPane();
		root.getChildren().add(WW);
		
		Scene scene = new Scene(root, 600, 500);
		
		primaryStage.setTitle("SSView");
		primaryStage.setScene(scene);
		primaryStage.show();
		
		Mda X=new Mda();
		X.allocate(8,20000);
		for (int ch=0; ch<X.size(0); ch++) {
			for (int t=0; t<X.size(1); t++) {
				X.setValue((Math.sqrt(1+ch))*(Math.sin(t*1.0/2000*(ch+1)*2*Math.PI*(t*1.0/X.size(1)-0.5))),ch,t);
			}
		}
		WW.setData(X);
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}
	
}
