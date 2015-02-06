package jviewmda;

import static java.lang.Integer.max;
import static java.lang.Integer.min;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.magland.jcommon.CallbackHandler;

/**
 *
 * @author magland
 */
public class MdaView2D extends StackPane {

	private ExpandingCanvas m_image_canvas;
	private ExpandingCanvas m_cursor_canvas;
	private Mda m_array = new Mda();
	int[] m_current_index = new int[2];
	int[] m_selected_rect = new int[4];
	int[] m_pending_selected_rect = new int[4];
	int[] m_zoom_rect = new int[4];
	double m_scale_x = 1;
	double m_scale_y = 1;
	int m_offset_x = 0;
	int m_offset_y = 0;
	int m_image_width = 1;
	int m_image_height = 1;
	double m_window_min = 0;
	double m_window_max = 100;
	double m_brightness = 0;
	double m_contrast = 0;
	private String m_selection_mode = "rectangle";
	private CallbackHandler CH = new CallbackHandler();
	private BrightnessContrastControl m_brightness_contrast_control = null;
	private boolean m_cursor_visible = true;
	private List<ExpandingCanvas> m_custom_canvases = new ArrayList<>();
	private Map<String, ExpandingCanvas> m_custom_canvas_lookups = new HashMap<>();
	private boolean m_invert_x = false, m_invert_y = false;

	public void setArray(Mda X) {
		m_array = X;
		schedule_refresh_image();
		refresh_cursor();
	}

	public Mda array() {
		return m_array;
	}

	public void setCurrentIndex(int[] ind) {
		if ((m_current_index[0] == ind[0]) && (m_current_index[1] == ind[1])) {
			return;
		}
		m_current_index[0] = ind[0];
		m_current_index[1] = ind[1];
		int[] tmp={-1,-1,0,0};
		m_selected_rect=tmp;
		refresh_cursor();
		CH.scheduleTrigger("current-index-changed", 100);
	}

	private Boolean matches(int[] A, int[] B) {
		if (A.length != B.length) {
			return false;
		}
		for (int i = 0; i < A.length; i++) {
			if (A[i] != B[i]) {
				return false;
			}
		}
		return true;
	}

	public void setSelectedRect(int[] rr) {
		//remove the ambiguity!!!
		if (rr[0] < 0) {
			rr[0] = -1;
			rr[1] = -1;
			rr[2] = 0;
			rr[3] = 0;
		}
		if (matches(m_selected_rect, rr.clone())) {
			return;
		}
		if (rr[0] >= 0) {
			int[] ind = {rr[0] + rr[2] / 2, rr[1] + rr[3] / 2};
			setCurrentIndex(ind);
		}
		m_selected_rect = rr.clone();
		int[] tmp = {-1, -1, 0, 0};
		m_pending_selected_rect = tmp;
		refresh_cursor();
		CH.scheduleTrigger("selected-rect-changed", 500);
	}

	public void onCurrentIndexChanged(Runnable callback) {
		CH.bind("current-index-changed", callback);
	}

	public void onSelectedRectChanged(Runnable callback) {
		CH.bind("selected-rect-changed", callback);
	}

	public int[] currentIndex() {
		return m_current_index.clone();
	}

	public int[] selectedRect() {
		return m_selected_rect.clone();
	}

	public void setWindowLevels(double min, double max) {
		m_window_min = min;
		m_window_max = max;
		schedule_refresh_image();
	}

	public void setBrightnessContrast(double brightness, double contrast) {
		m_brightness = brightness;
		m_contrast = contrast;
		schedule_refresh_image(10);
	}

	public double brightness() {
		return m_brightness;
	}

	public double contrast() {
		return m_contrast;
	}

	public void setBrightnessContrastControl(BrightnessContrastControl control) {
		m_brightness_contrast_control = control;
		control.onChanged(() -> on_brightness_contrast_control_changed());
		on_brightness_contrast_control_changed();
	}

	public void setSelectionMode(String mode) {
		if (m_selection_mode.equals(mode)) {
			return;
		}
		m_selection_mode = mode;
		CH.scheduleTrigger("selected-rect-changed", 500);
		refresh_cursor();
	}

	public String selectionMode() {
		return m_selection_mode;
	}

	public void setZoomRect(int[] rr) {
		m_zoom_rect = rr.clone();
		schedule_refresh_image();
	}

	public int[] zoomRect() {
		return m_zoom_rect.clone();
	}

	public void setCursorVisible(boolean val) {
		m_cursor_visible = val;
		refresh_cursor();
	}

	public ExpandingCanvas customCanvas(String name) {
		if (!m_custom_canvas_lookups.containsKey(name)) {
			ExpandingCanvas CC = new ExpandingCanvas();
			m_custom_canvas_lookups.put(name, CC);
			m_custom_canvases.add(CC);
			getChildren().add(CC);
		}
		return m_custom_canvas_lookups.get(name);
	}

	public void onImageRefreshed(Runnable callback) {
		CH.bind("image-refreshed", callback);
	}

	public void clearView() {
		List<ExpandingCanvas> tmplist = new ArrayList<>();
		tmplist.add(m_image_canvas);
		tmplist.add(m_cursor_canvas);
		for (ExpandingCanvas CC : m_custom_canvases) {
			tmplist.add(CC);
		}
		for (ExpandingCanvas CC : tmplist) {
			GraphicsContext gc = CC.getGraphicsContext2D();
			gc.clearRect(0, 0, getWidth(), getHeight());
		}
	}

	public void setInvertX(boolean val) {
		m_invert_x = val;
		schedule_refresh_image(100);
	}

	public void setInvertY(boolean val) {
		m_invert_y = val;
		schedule_refresh_image(100);
	}

	public MdaView2D() {
		m_current_index[0] = -1;
		m_current_index[1] = -1;
		m_selected_rect[0] = -1;
		m_selected_rect[1] = -1;
		m_selected_rect[2] = -1;
		m_selected_rect[3] = -1;
		m_pending_selected_rect = m_selected_rect.clone();
		m_zoom_rect[0] = -1;
		m_zoom_rect[1] = -1;
		m_zoom_rect[2] = -1;
		m_zoom_rect[3] = -1;
		m_image_canvas = new ExpandingCanvas();
		m_cursor_canvas = new ExpandingCanvas();
		getChildren().add(m_image_canvas);
		getChildren().add(m_cursor_canvas);
		m_image_canvas.setOnRefresh(() -> schedule_refresh_image());
		m_cursor_canvas.setOnRefresh(() -> refresh_cursor());

		this.setOnMousePressed(evt -> {
			on_mouse_pressed(evt, evt.getX(), evt.getY());
		});
		this.setOnMouseReleased(evt -> {
			on_mouse_released(evt, evt.getX(), evt.getY());
		});
		this.setOnMouseDragged(evt -> {
			on_mouse_dragged(evt, evt.getX(), evt.getY());
		});
		this.setOnKeyPressed(evt -> on_key_pressed(evt));
	}

	public int[] pixelToIndex(double x, double y) {
		return pixel2index(x, y);
	}

	public int[] indexToPixel(double x, double y) {
		return index2pixel(x, y);
	}

	public int[] imageRect() {
		int[] ret = new int[4];
		ret[0] = m_offset_x;
		ret[1] = m_offset_y;
		ret[2] = m_image_width;
		ret[3] = m_image_height;
		return ret;
	}

	////////////////// PRIVATE /////////////////////////////////////////
	private void on_brightness_contrast_control_changed() {
		this.setBrightnessContrast(m_brightness_contrast_control.brightness(), m_brightness_contrast_control.contrast());
	}

	private void schedule_refresh_image() {
		schedule_refresh_image(100);
	}

	boolean m_refresh_image_scheduled = false;

	private void schedule_refresh_image(int delay) {
		if (m_refresh_image_scheduled) {
			return;
		}
		m_refresh_image_scheduled = true;
		new Timeline(new KeyFrame(Duration.millis(delay), e -> {
			m_refresh_image_scheduled = false;
			do_refresh_image();
		})).play();
	}

	private void do_refresh_image() {
		GraphicsContext gc = m_image_canvas.getGraphicsContext2D();
		gc.clearRect(0, 0, getWidth(), getHeight());

		int margin = 5;

		double scale_factor = 1;
		int N1 = m_array.N1();
		int N2 = m_array.N2();
		if ((m_zoom_rect[2] > 1) && (m_zoom_rect[3] > 1)) {
			N1 = m_zoom_rect[2] + 1;
			N2 = m_zoom_rect[3] + 1;
		}
		double W0 = getWidth() - margin * 2;
		double H0 = getHeight() - margin * 2;
		if (W0 * N2 < N1 * H0) { //width is the limiting direction
			scale_factor = W0 * 1.0 / N1;
		} else { //height is the limiting direction
			scale_factor = H0 * 1.0 / N2;
		}
		m_scale_x = scale_factor;
		m_scale_y = scale_factor;
		m_offset_x = (int) (margin + (W0 - N1 * m_scale_x) / 2);
		m_offset_y = (int) (margin + (H0 - N2 * m_scale_y) / 2);

		int M1 = (int) (N1 * m_scale_x);
		int M2 = (int) (N2 * m_scale_y);

		if (M1 < 1) {
			return;
		}
		if (M2 < 1) {
			return;
		}

		m_image_width = M1;
		m_image_height = M2;

		WritableImage img = new WritableImage(M1, M2);
		PixelWriter W = img.getPixelWriter();
		int[] tmp = new int[2];
		for (int y = 0; y < M2; y++) {
			for (int x = 0; x < M1; x++) {
				tmp = pixel2index(x + m_offset_x, y + m_offset_y);
				Color c = get_color_at(tmp[0], tmp[1]);
				W.setColor(x, y, c);
			}
		}
		gc.drawImage(img, m_offset_x, m_offset_y);

		refresh_cursor();

		CH.trigger("image-refreshed", true);
	}

	private int[] pixel2index(double x, double y) {

		if (m_invert_x) {
			x = 2 * m_offset_x + m_image_width - x;
		}
		if (m_invert_y) {
			y = 2 * m_offset_y + m_image_height - y;
		}

		int tmp[] = new int[2];
		int N1 = m_array.N1();
		int N2 = m_array.N2();
		int x0 = 0, y0 = 0;
		if ((m_zoom_rect[2] > 1) && (m_zoom_rect[3] > 1)) {
			N1 = m_zoom_rect[2] + 1;
			N2 = m_zoom_rect[3] + 1;
			x0 = m_zoom_rect[0];
			y0 = m_zoom_rect[1];
		}

		tmp[0] = x0 + (int) ((x - m_offset_x) / m_scale_x);
		tmp[1] = y0 + (int) ((y - m_offset_y) / m_scale_y);
		if ((tmp[0] < x0) || (tmp[1] < y0) || (tmp[0] >= x0 + N1) || (tmp[1] >= y0 + N2)) {
			tmp[0] = -1;
			tmp[1] = -1;
		}
		return tmp;
	}

	private int[] index2pixel(double x, double y) {
		int tmp[] = new int[2];
		int x0 = 0, y0 = 0;
		if ((m_zoom_rect[2] > 1) && (m_zoom_rect[3] > 1)) {
			x0 = m_zoom_rect[0];
			y0 = m_zoom_rect[1];
		}
		tmp[0] = (int) (m_offset_x + (x - x0 + 0.5) * m_scale_x);
		tmp[1] = (int) (m_offset_y + (y - y0 + 0.5) * m_scale_y);

		if (m_invert_x) {
			tmp[0] = 2 * m_offset_x + m_image_width - tmp[0];
		}
		if (m_invert_y) {
			tmp[1] = 2 * m_offset_y + m_image_height - tmp[1];
		}

		return tmp;
	}

	private Color get_color_at(int x, int y) {
		double wmin0 = m_window_min;
		double wmax0 = m_window_max;
		double brightness = m_brightness;
		if (m_brightness > 0) {
			brightness *= 2;
		}
		double sum = wmin0 + wmax0 - brightness * (wmax0 - wmin0);
		double diff = (wmax0 - wmin0) * exp(-m_contrast * 3);
		double wmax = sum / 2 + diff / 2;
		double wmin = sum / 2 - diff / 2;
		double val = m_array.value(x, y);
		double val0;
		if (wmax > wmin) {
			val0 = (val - wmin) / (wmax - wmin);
		} else {
			if (val > wmin) {
				val0 = 1;
			} else {
				val0 = 0;
			}
		}
		if (val0 > 1) {
			val0 = 1;
		}
		if (val0 < 0) {
			val0 = 0;
		}
		return new Color((float) val0, (float) val0, (float) val0, 1);
	}

	private void refresh_cursor() {
		GraphicsContext gc = m_cursor_canvas.getGraphicsContext2D();
		gc.clearRect(0, 0, getWidth(), getHeight());
		gc.setLineWidth(3);

		if (m_pending_selected_rect[0] >= 0) {
			gc.setStroke(Color.BLUE);
			draw_rect(gc, m_pending_selected_rect);
		}
		if (m_selected_rect[0] >= 0) {
			gc.setStroke(Color.RED);
			draw_rect(gc, m_selected_rect);
		}

		if (m_current_index[0] < 0) {
			return;
		}
		if (m_current_index[1] < 0) {
			return;
		}

		if ((m_cursor_visible) && (m_selected_rect[0] < 0)) {
			gc.setStroke(Color.RED);
			int[] pix = index2pixel(m_current_index[0], m_current_index[1]);
			gc.strokeLine(m_offset_x, pix[1], m_offset_x + m_image_width, pix[1]);
			gc.strokeLine(pix[0], m_offset_y, pix[0], m_offset_y + m_image_height);
		}
	}

	private void draw_rect(GraphicsContext gc, int[] rect) {
		int[] ind0 = new int[2];
		ind0[0] = rect[0];
		ind0[1] = rect[1];
		int[] ind1 = new int[2];
		ind1[0] = rect[0] + rect[2];
		ind1[1] = rect[1] + rect[3];
		int[] pix0 = index2pixel(ind0[0] - 0.5, ind0[1] - 0.5);
		int[] pix1 = index2pixel(ind1[0] + 0.5, ind1[1] + 0.5);
		int a = pix0[0], b = pix0[1], c = pix1[0], d = pix1[1];
		pix0[0] = Math.min(a, c);
		pix0[1] = Math.min(b, d);
		pix1[0] = Math.max(a, c);
		pix1[1] = Math.max(b, d);
		if (m_selection_mode.equals("rectangle")) {
			gc.strokeRect(pix0[0], pix0[1], pix1[0] - pix0[0], pix1[1] - pix0[1]);
		} else if (m_selection_mode.equals("ellipse")) {
			gc.strokeOval(pix0[0], pix0[1], pix1[0] - pix0[0], pix1[1] - pix0[1]);
		}
	}

	double[] m_anchor_point = new double[2];

	private void on_mouse_pressed(MouseEvent evt, double x, double y) {
		int[] rr = {-1, -1, 0, 0};
		m_pending_selected_rect = rr;
		int[] ind = pixel2index(x, y);
		this.requestFocus();
		m_anchor_point = new double[2];
		m_anchor_point[0] = x;
		m_anchor_point[1] = y;
		refresh_cursor();
	}

	private void on_mouse_released(MouseEvent evt, double x, double y) {
		if (m_pending_selected_rect[0] >= 0) {
			this.setSelectedRect(m_pending_selected_rect); //includes refresh_cursor
		} else {
			int[] ind = pixel2index(x, y);
			this.setCurrentIndex(ind);
			this.requestFocus();
			int[] rr = {-1, -1, 0, 0};
			m_selected_rect = rr;
			refresh_cursor();
		}
	}

	private void on_mouse_dragged(MouseEvent evt, double x, double y) {
		if (m_anchor_point[0] >= 0) {
			if ((abs(m_anchor_point[0] - x) > 2) && (abs(m_anchor_point[1] - y) > 2)) {
				int[] ind0 = pixel2index(m_anchor_point[0], m_anchor_point[1]);
				int[] ind1 = pixel2index(x, y);
				int[] rr = new int[4];
				rr[0] = min(ind0[0], ind1[0]);
				rr[1] = min(ind0[1], ind1[1]);
				rr[2] = max(ind0[0], ind1[0]) - min(ind0[0], ind1[0]);
				rr[3] = max(ind0[1], ind1[1]) - min(ind0[1], ind1[1]);
				m_pending_selected_rect = rr.clone();
				refresh_cursor();
			} else {
				int[] tmp = {-1, -1, 0, 0};
				m_pending_selected_rect = tmp;
				refresh_cursor();
			}
		}
	}

	private void on_key_pressed(KeyEvent evt) {
		KeyCode code = evt.getCode();
		if (!evt.isShiftDown()) {
			if (code == KeyCode.UP) {
				if (!m_invert_y) {
					translate_position(0, -1);
				} else {
					translate_position(0, 1);
				}
				evt.consume();
			} else if (code == KeyCode.DOWN) {
				if (!m_invert_y) {
					translate_position(0, 1);
				} else {
					translate_position(0, -1);
				}
				evt.consume();
			} else if (code == KeyCode.LEFT) {
				if (!m_invert_x) {
					translate_position(-1, 0);
				} else {
					translate_position(1, 0);
				}
				evt.consume();
			} else if (code == KeyCode.RIGHT) {
				if (!m_invert_x) {
					translate_position(1, 0);
				} else {
					translate_position(-1, 0);
				}
				evt.consume();
			}
		}
	}

	private void translate_position(int x0, int y0) {
		int[] ind = this.currentIndex();
		ind[0] += x0;
		if (ind[0] < 0) {
			ind[0] = 0;
		}
		if (ind[0] >= m_array.N1()) {
			ind[0] = m_array.N1() - 1;
		}
		ind[1] += y0;
		if (ind[1] < 0) {
			ind[1] = 0;
		}
		if (ind[1] >= m_array.N2()) {
			ind[1] = m_array.N2() - 1;
		}
		this.setCurrentIndex(ind);
	}
}
