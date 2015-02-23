
package ssview;

import java.io.FileReader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import jviewmda.Mda;

/*
TO DO:
* left/right arrows for navigation (when go off range, scroll)
double click centers
message saying loading
Separation bar between windows
Problem when zooming in too much (near boundary?)
Fix problem with left/right shift on lower window
Fix problem when scrolling past edge
Set fixed shift
*/

/**
 *
 * @author magland
 */
public class Ssview extends Application {
	
	@Override
	public void start(Stage primaryStage) {
		
		ScriptEngine engine=new ScriptEngineManager().getEngineByName("nashorn");
		Invocable invocable=(Invocable)engine;
		
		try {
			Object SSVIEW=new SSViewController();
			engine.put("SSVIEW", SSVIEW);
			engine.eval(new FileReader("src/ssview/testing.js"));
			invocable.invokeFunction("fun1","Babe Ruth");
		}
		catch(Exception e) {
			System.err.println(e.getMessage());
		}
	}
	

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}
	
}
