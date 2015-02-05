/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jviewmda;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.magland.jcommon.JUtils;
import org.magland.wfs.WFSClient;

/**
 *
 * @author magland
 */
public class JViewMda extends Application {

	private Stage m_stage;
	private Mda m_array = new Mda();
	private Preferences m_prefs;
	private FileChooser m_fc = new FileChooser();
	private ViewmdaWidget m_widget = new ViewmdaWidget();
	private String m_file_path;
	private Map<String, CheckMenuItem> m_selection_mode_items;

	@Override
	public void start(Stage primaryStage) {

		//test_wfsmda();
		m_stage = primaryStage;

		String array_path = "";
		Parameters params = getParameters();
		List<String> unnamed_params = params.getUnnamed();
		if (unnamed_params.size() > 0) {
			array_path = unnamed_params.get(0);
		}
		// FOR DEBUGING PURPOSES
		if (array_path.length() == 0) {
			//String debug_path = "/home/magland/wisdm/www/wisdmfileserver/files/fetalmri/sessions/SESSION1/crops/FNP001A-coronal.crop.mda";
			String debug_path = "/home/magland/wisdm/www/wisdmfileserver/files/LesionProbe/Images/ID001_FLAIR.nii";
			if ((new File(debug_path)).exists()) {
				array_path = debug_path;
			}
		}

		Menu menu;
		MenuItem item;

		MenuBar menubar = new MenuBar();

		//file menu
		menu = new Menu("File");
		menubar.getMenus().add(menu);
		item = new MenuItem("Open...");
		item.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
		item.setOnAction(e -> on_file_open());
		menu.getItems().add(item);
		item = new MenuItem("Save As...");
		item.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
		item.setOnAction(e -> on_file_saveas());
		menu.getItems().add(item);
		menu.getItems().add(new SeparatorMenuItem()); /////////////////////////////////////////////
		item = new MenuItem("Exit");
		item.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
		item.setOnAction(e -> on_file_exit());
		menu.getItems().add(item);

		//view menu
		menu = new Menu("View");
		menubar.getMenus().add(menu);
		item = new MenuItem("Zoom In");
		item.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
		item.setOnAction(e -> on_zoom_in());
		menu.getItems().add(item);
		item = new MenuItem("Zoom Out");
		item.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
		item.setOnAction(e -> on_zoom_out());
		menu.getItems().add(item);
		menu.getItems().add(new SeparatorMenuItem()); /////////////////////////////////////////////
		{
			CheckMenuItem item0 = new CheckMenuItem("Top Controls");
			item0.setSelected(true);
			item0.setOnAction(e -> {
				m_widget.setTopControlsVisible(item0.isSelected());
			});
			menu.getItems().add(item0);
		}
		{
			CheckMenuItem item0 = new CheckMenuItem("Bottom Controls");
			item0.setSelected(true);
			item0.setOnAction(e -> {
				m_widget.setBottomControlsVisible(item0.isSelected());
			});
			menu.getItems().add(item0);
		}
		{
			CheckMenuItem item0 = new CheckMenuItem("Brightness/Contrast");
			item0.setSelected(true);
			item0.setOnAction(e -> {
				m_widget.setBrightnessContrastControlVisible(item0.isSelected());
			});
			menu.getItems().add(item0);
		}
		{
			CheckMenuItem item0 = new CheckMenuItem("Slice Slider");
			item0.setSelected(true);
			item0.setOnAction(e -> {
				m_widget.setSliceSliderVisible(item0.isSelected());
			});
			menu.getItems().add(item0);
		}

		//selection menu
		menu = new Menu("Selection");
		menubar.getMenus().add(menu);
		Map<String, CheckMenuItem> mode_items = new HashMap<>();
		m_selection_mode_items = mode_items;
		mode_items.put("rectangle", new CheckMenuItem("Rectangle"));
		mode_items.put("ellipse", new CheckMenuItem("Ellipse"));
		Set<String> keys = mode_items.keySet();
		for (String key : keys) {
			CheckMenuItem item0 = mode_items.get(key);
			menu.getItems().add(item0);
			item0.setOnAction(evt -> {
				on_selection_mode_changed(key);
			});

		}
		mode_items.get("rectangle").setSelected(true);

		VBox root = new VBox();
		root.getChildren().addAll(menubar, m_widget);

		Scene scene = new Scene(root, 500, 450);

		primaryStage.setTitle("JViewMda");
		primaryStage.setScene(scene);
		primaryStage.show();

		if (array_path.length() > 0) {
			open_file(array_path);
		}

//		WFSClient CC=new WFSClient("localhost:8006","LesionProbe","Images");
//		WFSMda XX=new WFSMda();
//		XX.setClient(CC);
//		XX.setPath("ID001_FLAIR.nii");
//		m_widget.setRemoteArray(XX, ()->{});
		m_prefs = Preferences.userNodeForPackage(this.getClass());
	}

	private void on_selection_mode_changed(String selection_mode) {
		Set<String> keys = m_selection_mode_items.keySet();
		for (String key : keys) {
			if (key != selection_mode) {
				m_selection_mode_items.get(key).setSelected(false);
			}
		}
		m_widget.setSelectionMode(selection_mode);
	}

	private void on_file_open() {
		m_fc.setInitialDirectory(new File(m_prefs.get("open_file_directory", System.getProperty("user.home"))));
		m_fc.setTitle("Open File");
		File file0 = m_fc.showOpenDialog(m_stage);
		if (file0 == null) {
			return;
		}
		m_prefs.put("open_file_directory", file0.getParentFile().getAbsolutePath());
		String path0 = file0.getAbsolutePath();

		open_file(path0);
	}

	private void open_file(String path0) {
		String suf = JUtils.getFileSuffix(path0);
		if (suf.equals("mda")) {
			if (!m_array.read(path0)) {
				System.err.println("Problem reading mda file.");
				return;
			}
		} else if ((suf.equals("nii")) || (suf.equals("gz"))) {
			JNifti X = new JNifti();
			try {
				X.read(path0);
				m_array = X.array();
			} catch (IOException ee) {
				System.err.println("Unable to read nifti file.");
				return;
			}
		}

		m_file_path = path0;
		update_title();
		m_widget.setArray(m_array);
	}

	private void on_file_saveas() {
		m_fc.setInitialDirectory(new File(m_prefs.get("open_file_directory", System.getProperty("user.home"))));
		m_fc.setTitle("Open File");
		File file0 = m_fc.showSaveDialog(m_stage);
		if (file0 == null) {
			return;
		}
		m_prefs.put("open_file_directory", file0.getParentFile().getAbsolutePath());
		String path0 = file0.getAbsolutePath();

		if (!m_array.write(path0)) {
			System.err.println("Problem writing mda file.");
			return;
		}
		m_file_path = path0;
		update_title();
	}

	private void on_file_exit() {
		Platform.exit();
	}

	private void update_title() {
		File FF = new File(m_file_path);
		try {
			String str = FF.getName() + ": " + FF.getCanonicalPath();
			m_stage.setTitle(str);
		} catch (IOException ee) {

		}
	}

	private void on_zoom_in() {
		m_widget.zoomIn();
	}

	private void on_zoom_out() {
		m_widget.zoomOut();
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}

	private void test_wfsmda() {
		//WFSClient CC=new WFSClient("localhost:8006","LSNI","MAVR/OX/MAVR_001");
		WFSClient CC = new WFSClient("localhost:8006", "LesionProbe", "Images");
		WFSMda XX = new WFSMda();
		XX.setClient(CC);
		XX.setPath("ID001_FLAIR.nii");
		XX.initialize(tmp1 -> {
			System.out.format("dimensions: %d x %d x %d\n", XX.N1(), XX.N2(), XX.N3());
		});
	}

}
