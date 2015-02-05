package lesionprobe;

import java.io.File;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.control.ButtonBar;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;
import org.magland.jcommon.JUtils;
import org.magland.jcommon.SJO;
import org.magland.jcommon.SJOCallback;
import org.magland.wfs.WFSClient;

/*
 TO DO
 In admin reset answers
 Somehow pre-specify the config options (esp. the rater)
 Better options for admin to explore images and answers
 Web browser interface to some features including downloading of answer spreadsheet
 Notification of new version available
 */
/**
 *
 * @author magland
 */
public class LesionProbe extends Application {

	LPMainWidget m_widget;

	static final String LesionProbe_version = "1.03";

	@Override
	public void start(Stage primaryStage) {

		show_config_dlg(params -> {

			LPMainWidget m_widget = new LPMainWidget();
			m_widget.onFinished(() -> {
				Platform.runLater(() -> {
					Dialogs.create()
							.owner(m_widget)
							.title("LesionProbe Completed")
							.masthead(null)
							.message("Thank you for your submissions.")
							.showInformation();
					primaryStage.close();
				});

				//System.exit(0);
			});

			VBox root = new VBox();
			root.getChildren().addAll(/*menubar,*/m_widget);

			Scene scene = new Scene(root, 1000, 650);

			primaryStage.setTitle("LesionProbe");
			primaryStage.setScene(scene);

			WFSClient CC = new WFSClient(params.get("fshost").toString(), params.get("fsname").toString(), params.get("folder").toString());
			String cache_dir = JUtils.createTemporaryDirectory("LesionProbe-cache");
			System.out.println("Using cach path: " + cache_dir);
			CC.setCachePath(cache_dir);
			m_widget.setClient(CC);
			m_widget.setRater(params.get("rater").toString());
			m_widget.setAdminMode(params.get("admin_mode").toBoolean());

			primaryStage.show();

			m_widget.run();
		});

	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}

	private SJO get_parameters(String str) {
		SJO ret = new SJO("{}");
		String[] strings = str.split("&");
		for (String str0 : strings) {
			String str1 = str0.trim();
			String[] vals = str1.split("=");
			if (vals.length == 2) {
				ret.set(vals[0], vals[1]);
			}
		}
		return ret;
	}

	private void show_config_dlg(SJOCallback callback) {

		Dialog dlg = new Dialog(null, "LesionProbe Configuration - Version " + LesionProbe_version);

		final GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);

		int row = 0;
		grid.add(new Label("FSHost"), 0, row);
		TextField fshost_field = new TextField();
		//fshost_field.setText("localhost:8006");
		if (JUtils.connectedToInternet()) {
			fshost_field.setText("45.56.110.165:8080"); //pennsive01
			//fshost_field.setText("97.107.129.125:8080"); //peregrine01
			//fshost_field.setText("localhost:8006");
		} else {
			System.err.println("Not connected to the internet.");
			fshost_field.setText("localhost:8006");
		}
		grid.add(fshost_field, 1, row);
		grid.setHgrow(fshost_field, Priority.ALWAYS);
		row++;

		grid.add(new Label("FSName"), 0, row);
		TextField fsname_field = new TextField();
		fsname_field.setText("LesionProbe");
		grid.add(fsname_field, 1, row);
		grid.setHgrow(fsname_field, Priority.ALWAYS);
		row++;

		grid.add(new Label("Folder"), 0, row);
		TextField folder_field = new TextField();
		folder_field.setText("");
		grid.add(folder_field, 1, row);
		grid.setHgrow(folder_field, Priority.ALWAYS);
		row++;

		grid.add(new Label("Rater"), 0, row);
		TextField rater_field = new TextField();
		rater_field.setText("Reich");
		grid.add(rater_field, 1, row);
		grid.setHgrow(rater_field, Priority.ALWAYS);
		row++;

		CheckBox admin_checkbox = new CheckBox("Admin mode");
		admin_checkbox.setSelected(false);
		grid.add(admin_checkbox, 1, row);
		row++;

		Button ok_button = new Button("OK");
		ok_button.setOnAction(evt -> {
			SJO ret = new SJO();
			ret.set("fshost", fshost_field.getText());
			ret.set("fsname", fsname_field.getText());
			ret.set("folder", folder_field.getText());
			ret.set("rater", rater_field.getText());
			ret.set("admin_mode", admin_checkbox.isSelected());
			dlg.hide();

			if (ret.get("admin_mode").toBoolean()) {
				Platform.runLater(() -> {
					check_admin_password(() -> {
						callback.run(ret);
					});
				});
			} else {
				callback.run(ret);
			}
		});

		Button cancel_button = new Button("Cancel");
		cancel_button.setOnAction(evt -> {
			dlg.hide();
		});

		ButtonBar bar = new ButtonBar();

		bar.getButtons().addAll(ok_button, cancel_button);
		grid.add(bar, 1, row);
		row++;

		fshost_field.setOnKeyPressed(evt -> {
			if (evt.getCode().equals(KeyCode.ENTER)) {
				Platform.runLater(() -> { //apparently we need this or the program will crash, go figure
					ok_button.fire();
				});
			}
		});
		rater_field.setOnKeyPressed(evt -> {
			if (evt.getCode().equals(KeyCode.ENTER)) {
				Platform.runLater(() -> { //apparently we need this or the program will crash, go figure
					ok_button.fire();
				});
			}
		});

		dlg.setResizable(false);
		dlg.setIconifiable(false);
		dlg.setContent(grid);
		Action action = dlg.show();

	}

	private void check_admin_password(Runnable callback) {

		Dialog dlg = new Dialog(null, "Admin Mode");

		final GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);

		grid.add(new Label("Admin Password"), 0, 0);
		PasswordField password_field = new PasswordField();
		grid.add(password_field, 1, 0);
		grid.setHgrow(password_field, Priority.ALWAYS);

		final String the_password = "crab";

		Button ok_button = new Button("OK");
		ok_button.setOnAction(evt -> {
			dlg.hide();
			if (password_field.getText().equals(the_password)) {
				callback.run();
			} else {
				Platform.runLater(() -> {
					JUtils.showInformation("Incorrect password", "Incorrect admin password.");
					System.exit(0);
				});
			}
		});
		Button cancel_button = new Button("Cancel");
		cancel_button.setOnAction(evt -> {
			System.exit(0);
		});

		ButtonBar bar = new ButtonBar();

		bar.getButtons().addAll(ok_button, cancel_button);
		grid.add(bar, 1, 2);

		password_field.setOnKeyPressed(evt -> {
			if (evt.getCode().equals(KeyCode.ENTER)) {
				Platform.runLater(() -> {
					ok_button.fire();
				});
			}
		});

		dlg.setResizable(false);
		dlg.setIconifiable(false);
		dlg.setContent(grid);
		Action action = dlg.show();

	}

}
