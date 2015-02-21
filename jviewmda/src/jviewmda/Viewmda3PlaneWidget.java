package jviewmda;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.magland.jcommon.CallbackHandler;
import org.magland.jcommon.JUtils;

/**
 *
 * @author magland
 */
public class Viewmda3PlaneWidget extends VBox {

	private RemoteArray m_remote_array = new WFSMda();
	private int[] m_current_index = new int[Mda.MAX_MDA_DIMS];
	private ViewmdaWidget m_view_XY = new ViewmdaWidget();
	private ViewmdaWidget m_view_XZ = new ViewmdaWidget();
	private ViewmdaWidget m_view_YZ = new ViewmdaWidget();
	private List<ViewmdaWidget> m_views = new ArrayList<>();
	private Label m_status_label;
	private HBox m_top_controls = new HBox();
	private HBox m_bottom_controls = new HBox();
	private HBox m_view_section = new HBox();
	private BrightnessContrastControl m_bc_control = new BrightnessContrastControl();
	private CallbackHandler CH = new CallbackHandler();
	private boolean m_setting_current_index = false;
	private boolean m_setting_selected_rects = false;
	private Map<String, CheckMenuItem> m_selection_mode_items=new HashMap<>();

	//TO DO: Figure out window levels synchronization
	//TO DO: pass in reference to the single b/c control
	public Viewmda3PlaneWidget() {

		try {
			this.getStylesheets().add(getClass().getResource("resources/viewmda3planewidget.css").toExternalForm());
		} catch (Exception ee) {
			System.err.println("Unable to load viewmda3dplanewidget.css");
		}

		m_views.add(m_view_XY);
		m_views.add(m_view_XZ);
		m_views.add(m_view_YZ);

		int[] tmp1 = {0, 1, 2};
		int[] tmp2 = {0, 2, 1};
		int[] tmp3 = {1, 2, 0};
		m_view_XY.setDimensionChoices(tmp1);
		m_view_XZ.setDimensionChoices(tmp2);
		m_view_YZ.setDimensionChoices(tmp3);

		m_view_section.setPrefHeight(Integer.MAX_VALUE);
		m_view_section.setPrefWidth(Integer.MAX_VALUE);
		m_view_section.getChildren().addAll(m_view_XY, m_view_XZ, m_view_YZ, m_bc_control);
		
		HBox top_controls = new HBox();
		m_top_controls=top_controls;
		{
			MenuButton menu_button=new MenuButton("View");
			{
				MenuItem item = new MenuItem("Open single plane view");
				item.setOnAction(e -> on_open_single_plane_view());
				menu_button.getItems().add(item);
			}
			{
				MenuItem item = new MenuItem("Zoom In");
				item.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
				item.setOnAction(e -> on_zoom_in());
				menu_button.getItems().add(item);
			}
			{
				MenuItem item = new MenuItem("Zoom Out");
				item.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN,KeyCombination.SHIFT_DOWN));
				item.setOnAction(e -> on_zoom_out());
				menu_button.getItems().add(item);
			}
			{
				CheckMenuItem item = new CheckMenuItem("Rectangle selection mode");
				item.setSelected(true);
				item.setOnAction(e -> {on_set_selection_mode("rectangle");});
				menu_button.getItems().add(item);
				m_selection_mode_items.put("rectangle", item);
			}
			{
				CheckMenuItem item = new CheckMenuItem("Ellipse selection mode");
				item.setOnAction(e -> {on_set_selection_mode("ellipse");});
				menu_button.getItems().add(item);
				m_selection_mode_items.put("ellipse", item);
			}
			top_controls.getChildren().addAll(menu_button);
		}
		

		this.getChildren().addAll(m_top_controls, m_view_section, m_bottom_controls);

		for (ViewmdaWidget W : m_views) {
			W.setTopControlsVisible(false);
			W.setBottomControlsVisible(false);
			W.setBrightnessContrastControlVisible(false);

			W.onCurrentIndexChanged(() -> {
				if (!m_setting_current_index) {
					int[] ind = W.currentIndex();
					set_current_index(ind);
				}
			});
			W.onSelectedRectChanged(() -> {
				if (!m_setting_selected_rects) {
					on_selected_rect_changed(W);
				}
			});
		}
	}

	public void setArray(Mda X) {
		LocalArray Y = new LocalArray(X);
		this.setRemoteArray(Y, () -> {
		});
	}

	public void setRemoteArray(RemoteArray X, Runnable callback) {
		m_remote_array = X;
		X.initialize((tmp1) -> {
			for (int i = 0; i < m_current_index.length; i++) {
				m_current_index[i] = X.size(i) / 2;
			}
			for (ViewmdaWidget W : m_views) {
				W.setRemoteArray(X, () -> {
					W.setCurrentIndex(m_current_index);
				});
			}
			callback.run();
		});
	}

	//////////////////////
	private void set_current_index(int[] ind) {
		m_setting_current_index = true;
		for (ViewmdaWidget W : m_views) {
			W.setCurrentIndex(ind);
		}
		m_setting_current_index = false;
	}

	private void on_selected_rect_changed(ViewmdaWidget W_in) {
		m_setting_selected_rects = true;
		int[] rr = W_in.selectedRect();
		if (rr[0] >= 0) { //this is necessary since we don't want to clear the selected rect on others just because the selected rect on this one was cleared -- problem with delayed signals
			for (ViewmdaWidget W : m_views) {
				if (W != W_in) {
					int[] tmp = {-1, -1, 0, 0};
					W.setSelectedRect(tmp);
				}
			}
		}
		m_setting_selected_rects = false;
	}
	
	private void on_open_single_plane_view() {
		ViewmdaWidget W=new ViewmdaWidget();
		W.setRemoteArray(m_remote_array, ()->{
			Stage stage=(Stage)this.getScene().getWindow();
			Stage W2=JUtils.popupWidget(W, stage.getTitle());
			W2.setWidth(350); W2.setHeight(300);
			Platform.runLater(()->{
				stage.hide();
			});
		});
	}
	private void on_zoom_in() {
		boolean found=false;
		for (ViewmdaWidget view:m_views) {
			if (view.selectedRect()[0]>=0) {
				view.zoomIn();
				found=true;
			}
		}
		if (!found) {
			JUtils.showInformation("Unable to zoom in", "You must select a rectangle prior to zooming in.");
		}
	}
	private void on_zoom_out() {
		for (ViewmdaWidget view:m_views) {
			view.zoomOut();
		}
	}
	private void on_set_selection_mode(String mode) {
		for (ViewmdaWidget view:m_views) {
			view.setSelectionMode(mode);
		}
		m_selection_mode_items.forEach((mode0,item)->{
			item.setSelected(mode0.equals(mode));
		});	
	}
}
