package jviewmda;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.magland.jcommon.CallbackHandler;

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
		System.out.format("::: %d,%d,%d,%d\n", rr[0], rr[1], rr[2], rr[3]);
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
}
