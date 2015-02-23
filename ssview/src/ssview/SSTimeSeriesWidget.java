package ssview;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import jviewmda.ExpandingCanvas;
import jviewmda.Mda;
import org.magland.jcommon.CallbackHandler;

/**
 *
 * @author magland
 */
public class SSTimeSeriesWidget extends VBox {

	Mda m_data = new Mda();
	SSTimeSeriesView m_view = new SSTimeSeriesView();
	SSTimeSeriesView m_magnified_view = new SSTimeSeriesView();
	SSTimeSeriesView m_current_view = null;

	public SSTimeSeriesWidget() {
		this.getChildren().add(m_view);
		this.getChildren().add(m_magnified_view);
		m_view.onHoveredXChanged(() -> {
			m_current_view = m_view;
		});
		m_view.onCurrentXChanged(() -> {
			this.requestFocus();
			m_magnified_view.setShowPlots(true);
			int x0 = m_view.currentXCoord();
			int diff0 = (int)(m_magnified_view.xRange().y - m_magnified_view.xRange().x);
			if (diff0 < 0) {
				diff0 = 10;
			}
			int x1 = x0 - diff0 / 2;
			if (x1 < 0) {
				x1 = 0;
			}
			int x2 = x1 + diff0;
			if (x2 >= m_data.size(1)) {
				x2 = m_data.size(1) - 1;
				x1 = x2 - diff0;
				if (x1 < 0) {
					x1 = 0;
				}
			}
			m_view.setXShiftAmount(x2-x1);
			m_magnified_view.setXRange(x1, x2);
			m_magnified_view.setCurrentXCoord(x0);
		});
		m_magnified_view.onHoveredXChanged(() -> {
			m_current_view = m_magnified_view;
		});
		m_magnified_view.onCurrentXChanged(() -> {
			m_view.setCurrentXCoord(m_magnified_view.currentXCoord());
		});
		this.setOnKeyPressed(evt -> {
			if (m_current_view != null) {
				m_current_view.sendKeyPress(evt);
			}
		});
	}

	public void setData(Mda X) {
		m_data = X;
		m_view.setData(X);
		m_magnified_view.setShowPlots(false);
		m_magnified_view.setData(X);
		m_magnified_view.setXRange(0, 100);
	}
}

class SSTimeSeriesView extends StackPane {

	Mda m_data = new Mda();
	double m_current_x = -1;
	double m_hovered_x = -1;
	double m_selected_xmin=-1;
	double m_selected_xmax=-1;
	ExpandingCanvas m_underlay = new ExpandingCanvas();
	SSTimeSeriesPlot m_plot = new SSTimeSeriesPlot();
	CallbackHandler CH = new CallbackHandler();
	int m_x_shift_amount=-1;

	public SSTimeSeriesView() {
		this.getChildren().add(m_underlay);
		this.getChildren().add(m_plot);
		this.setOnMousePressed(evt->{
			m_selected_xmin=m_selected_xmax=-1;
			if (evt.getButton()==MouseButton.PRIMARY) {
				refresh_underlay();
			}
			else if (evt.getButton()==MouseButton.SECONDARY) {
				on_mouse_released(evt);
			}
		});
		this.setOnMouseReleased(evt -> {
			on_mouse_released(evt);

		});
		this.setOnMouseDragged(evt->{
			int x0=(int)m_plot.pixToCoord(new Vec2(evt.getX(), evt.getY())).x;
			if (evt.getButton()==MouseButton.PRIMARY) {
				if (m_current_x != x0) {
					m_current_x = x0;
					refresh_underlay();
					CH.trigger("current-x-changed", true);
				}
			}
			else if (evt.getButton()==MouseButton.SECONDARY) {
				m_selected_xmin=Math.min(m_current_x, x0);
				m_selected_xmax=Math.max(m_current_x, x0);
				refresh_underlay();
			}
		});
		this.setOnMouseMoved(evt -> {
			//this.requestFocus();
			Vec2 hovered_coordinate = m_plot.pixToCoord(new Vec2(evt.getX(), evt.getY()));
			if (m_hovered_x != hovered_coordinate.x) {
				m_hovered_x = hovered_coordinate.x;
				refresh_underlay();
				CH.trigger("hovered-x-changed", false);
			}
		});
		this.setOnMouseExited(evt -> {
			m_hovered_x = -1;
			refresh_underlay();
		});
		this.setOnScroll(evt -> {
			double delta_y = evt.getDeltaY();
			if (delta_y > 0) {
				zoom_in((int)m_hovered_x);
			} else if (delta_y < 0) {
				zoom_out((int)m_hovered_x);
			}

		});
		this.setOnKeyPressed(evt -> {
			sendKeyPress(evt);
		});
	}

	public void setData(Mda X) {
		m_data = X;
		m_plot.setData(X);
		refresh_underlay();
	}

	public void setShowPlots(boolean val) {
		m_plot.setShowPlots(val);

	}

	public void onHoveredXChanged(Runnable X) {
		CH.bind("hovered-x-changed", X);
	}

	public void onCurrentXChanged(Runnable X) {
		CH.bind("current-x-changed", X);
	}

	public void sendKeyPress(KeyEvent evt) {
		if (evt.getCode() == KeyCode.EQUALS) {
			zoom_in((int)m_current_x);
			//evt.consume();
		} else if (evt.getCode() == KeyCode.MINUS) {
			zoom_out((int)m_current_x);
			//evt.consume();
		}
		else if (evt.getCode().equals(KeyCode.LEFT)) {
			int shift=(int)Math.max(1, (m_plot.xRange().y-m_plot.xRange().x)/300);
			if ((m_x_shift_amount>=0)&&(m_x_shift_amount<shift)) shift=m_x_shift_amount;
			if (evt.isShiftDown()) {
				do_translate(-shift);
			}
			else {
				move_current_x_coord(-shift);
			}
		}
		else if (evt.getCode().equals(KeyCode.RIGHT)) {
			int shift=(int)Math.max(1, (m_plot.xRange().y-m_plot.xRange().x)/300);
			if ((m_x_shift_amount>=0)&&(m_x_shift_amount<shift)) shift=m_x_shift_amount;
			if (evt.isShiftDown()) {
				do_translate(+shift);
			}
			else {
				move_current_x_coord(+shift);
			}
		}
	}
	public void setXShiftAmount(int val) {
		m_x_shift_amount=val;
	}
	
	//////////////// PRIVATE ////////////////////
	
	void on_mouse_released(MouseEvent evt) {
		Vec2 coord = m_plot.pixToCoord(new Vec2(evt.getX(), evt.getY()));
		if ((m_selected_xmin>=0)&&(m_selected_xmax>=m_selected_xmin+3)) {
			m_plot.setXRange((int)m_selected_xmin, (int)m_selected_xmax);
			m_current_x=(m_selected_xmin+m_selected_xmax)/2;
			m_selected_xmin=-1;
			m_selected_xmax=-1;
			CH.trigger("current-x-changed", true);
			refresh_underlay();
		}
		else {
			if (m_current_x != coord.x) {
				m_current_x = coord.x;
				refresh_underlay();
				CH.trigger("current-x-changed", true);
			}
		}
	}

	void zoom_in(int center_x) {
		do_zoom(center_x,0.9);
	}

	void zoom_out(int center_x) {
		do_zoom(center_x,1 / 0.9);
	}
	
	void do_translate(int dx) {
		int x_left = (int)m_plot.xRange().x;
		int x_right = (int)m_plot.xRange().y;
		System.out.format("debug1 %d,%d\n", x_left,x_right);
		x_left+=dx;
		x_right+=dx;
		System.out.format("debug2 %d,%d\n", x_left,x_right);
		if ((x_left>=0)&&(x_right<m_data.size(1))) {
			m_plot.setXRange(x_left, x_right);
			this.refresh_underlay();
		}
	}
	
	void move_current_x_coord(int dx) {
		int new_x=(int)(m_current_x+dx);
		new_x=Math.max(0, new_x);
		new_x=Math.min(m_data.size(1), new_x);
		System.out.println("testing");
		this.setCurrentXCoord(new_x);
		CH.trigger(("current-x-changed"), true);
		if (new_x<m_plot.xRange().x) {
			do_translate((int)(new_x-m_plot.xRange().x));
		}
		if (new_x>m_plot.xRange().y) {
			do_translate((int)(new_x-m_plot.xRange().y));
		}
	}

	void do_zoom(int center_x,double frac) {
		int xmin = (int)m_plot.xRange().x;
		int xmax = (int)m_plot.xRange().y;
		int diff = xmax - xmin;
		int new_diff = (int) Math.floor(diff * frac);
		int x0 = (int) center_x;
		if (x0 < 0) {
			x0 = (xmax + xmin) / 2;
		}

		//we need to make sure that the hovered point stays in the same place
		//right now, the x0 is in the center. we need to shift it over
		Vec2 pt_left = m_plot.coordToPix(new Vec2(xmin, 0));
		Vec2 pt_right = m_plot.coordToPix(new Vec2(xmax, 0));
		Vec2 pt_hover = m_plot.coordToPix(new Vec2(x0, 0));

		if (pt_right.x <= pt_left.x) {
			return;
		}
		double pct_pt_hover = (pt_hover.x - pt_left.x) / (pt_right.x - pt_left.x);
		//x_left = x0-pct_pt_hover*new_diff
		int x_left = (int) Math.floor(x0 - pct_pt_hover * new_diff);
		int x_right = x_left + new_diff;

		if (x_left < 0) {
			x_left = 0;
			x_right = x_left + new_diff;
		}
		if (x_right >= m_data.size(1)) {
			x_right = m_data.size(1) - 1;
			x_left = x_right - new_diff;
			if (x_left < 0) {
				x_left = 0;
			}
		}
		m_plot.setXRange(x_left, x_right);
		this.refresh_underlay();
	}

	public void setXRange(int x_left, int x_right) {
		m_plot.setXRange(Math.max(x_left,0), Math.min(m_data.size(1)-1,x_right));
	}

	public Vec2 xRange() {
		return m_plot.xRange();
	}

	public int hoveredXCoord() {
		return (int) m_hovered_x;
	}

	public int currentXCoord() {
		return (int) m_current_x;
	}

	public void setCurrentXCoord(int x0) {
		if (m_current_x != x0) {
			m_current_x = x0;
			this.refresh_underlay();
		}
	}

	////////////////// PRIVATE //////////////////
	void refresh_underlay() {
		GraphicsContext gc = m_underlay.getGraphicsContext2D();

		gc.clearRect(0, 0, getWidth(), getHeight());

		//hover location
		{
			Vec2 p0 = m_plot.coordToPix(new Vec2(m_hovered_x, 0));
			Vec2 p1 = m_plot.coordToPix(new Vec2(m_hovered_x, 0));
			p0.y = 0;
			p1.y = m_underlay.getHeight();

			gc.beginPath();
			gc.setStroke(Color.LIGHTCYAN);
			gc.setLineWidth(12);
			gc.moveTo(p0.x, p0.y);
			gc.lineTo(p1.x, p1.y);
			gc.stroke();
		}
		
		//current location
		{
			Vec2 p0 = m_plot.coordToPix(new Vec2(m_current_x, 0));
			Vec2 p1 = m_plot.coordToPix(new Vec2(m_current_x, 0));
			p0.y = 0;
			p1.y = m_underlay.getHeight();

			gc.beginPath();
			gc.setStroke(Color.LIGHTBLUE);
			gc.setLineWidth(3);
			gc.moveTo(p0.x, p0.y);
			gc.lineTo(p1.x, p1.y);
			gc.stroke();
		}
		
		//selected
		if (m_selected_xmin>=0) {
			double ymin=m_plot.yRange().x;
			double ymax=m_plot.yRange().y;
			Vec2 p0 = m_plot.coordToPix(new Vec2(m_selected_xmin, ymin));
			Vec2 p1 = m_plot.coordToPix(new Vec2(m_selected_xmax, ymax));
			
			gc.beginPath();
			gc.setStroke(Color.PINK);
			gc.setLineWidth(3);
			gc.moveTo(p0.x, p0.y);
			gc.lineTo(p1.x, p0.y);
			gc.lineTo(p1.x, p1.y);
			gc.lineTo(p0.x, p1.y);
			gc.lineTo(p0.x, p0.y);
			gc.stroke();
		}
	}
}

class SSTimeSeriesPlot extends ExpandingCanvas {

	Mda m_data = new Mda();
	PlotArea m_plot_area = new PlotArea();
	double[] m_minvals = new double[1];
	double[] m_maxvals = new double[1];
	double[] m_plot_offsets = new double[0];
	int m_xrange_min = -1;
	int m_xrange_max = -1;
	boolean m_show_plots = true;

	public SSTimeSeriesPlot() {
		this.setOnRefresh(() -> {
			Platform.runLater(() -> {
				refresh_plot();
			});
		});
	}

	public void setData(Mda X) {
		m_data = X;

		m_xrange_min = 0;
		m_xrange_max = X.size(1)-1;

		int M = X.size(0);
		int N = X.size(1);
		m_minvals = new double[M];
		m_maxvals = new double[M];
		for (int ch = 0; ch < M; ch++) {
			if (N > 0) {
				m_minvals[ch] = X.value(ch, 0);
				m_maxvals[ch] = X.value(ch, 0);
			}
			for (int i = 0; i < N; i++) {
				double val = X.value(ch, i);
				if (val < m_minvals[ch]) {
					m_minvals[ch] = val;
				}
				if (val > m_maxvals[ch]) {
					m_maxvals[ch] = val;
				}
			}
		}
		refresh_plot();
	}

	public void setShowPlots(boolean val) {
		if (m_show_plots == val) {
			return;
		}
		m_show_plots = val;
		this.refresh_plot();
	}

	public Vec2 coordToPix(Vec2 p) {
		return m_plot_area.coordToPix(p);
	}

	public Vec2 pixToCoord(Vec2 p) {
		return m_plot_area.pixToCoord(p);
	}
	
	public Vec2 xRange() {
		return new Vec2(m_xrange_min,m_xrange_max);
	}
	public Vec2 yRange() {
		return m_plot_area.yRange();
	}

	public void setXRange(int xmin, int xmax) {
		m_xrange_min = Math.max(xmin, 0);
		m_xrange_max = Math.min(xmax, m_data.size(1) - 1);
		this.refresh_plot();
	}

	////////////////////// PRIVATE /////////////////////////
	void refresh_plot() {
		int M = m_data.size(0);
		int NN = m_data.size(1);

		m_plot_area.setSize(this.getWidth(), this.getHeight());

		m_plot_offsets = new double[M];
		double max00 = 0;
		for (int ch = 0; ch < M; ch++) {
			if (Math.abs(m_minvals[ch]) > max00) {
				max00 = Math.abs(m_minvals[ch]);
			}
			if (Math.abs(m_maxvals[ch]) > max00) {
				max00 = Math.abs(m_maxvals[ch]);
			}
		}
		double offset = 0;
		for (int ch = 0; ch < M; ch++) {
			offset += (-m_minvals[ch]);
			m_plot_offsets[ch] = offset;
			offset += m_maxvals[ch];
			offset += max00 / 20;
		}

		m_plot_area.clearSeries();
		GraphicsContext gc = this.getGraphicsContext2D();
		m_plot_area.setSize(this.getWidth(), this.getHeight());

		if (M == 0) {
			m_plot_area.refresh(gc);
			return;
		}

		int xrange_min = m_xrange_min;
		int xrange_max = m_xrange_max;

		m_plot_area.setXRange(xrange_min - 1, xrange_max + 1);
		m_plot_area.setYRange(m_plot_offsets[0] + m_minvals[0] - max00 / 20, m_plot_offsets[M - 1] + m_maxvals[M - 1] + max00 / 20);

		for (int ch = 0; ch < M; ch++) {
			Mda xvals = new Mda();
			xvals.allocate(1, xrange_max - xrange_min + 1);
			for (int x = xrange_min; x <= xrange_max; x++) {
				xvals.setValue1(x, x - xrange_min);
			}
			Mda yvals = new Mda();
			yvals.allocate(1, xrange_max - xrange_min + 1);
			for (int ii = xrange_min; ii <= xrange_max; ii++) {
				yvals.setValue1(m_data.value(ch, ii) + m_plot_offsets[ch], ii - xrange_min);
			}
			Color color = get_channel_color(ch);
			m_plot_area.addSeries(new PlotSeries(xvals, yvals, color));
		}
		if (m_show_plots) {
			m_plot_area.refresh(gc);
		} else {
			gc.clearRect(0, 0, getWidth(), getHeight());
		}
	}

	double compute_min(Mda X) {
		int N = X.totalSize();
		if (N == 0) {
			return 0;
		}
		double ret = X.value1(0);
		for (int i = 0; i < N; i++) {
			double val = X.value1(i);
			if (val < ret) {
				ret = val;
			}
		}
		return ret;
	}

	double compute_max(Mda X) {
		int N = X.totalSize();
		if (N == 0) {
			return 0;
		}
		double ret = X.value1(0);
		for (int i = 0; i < N; i++) {
			double val = X.value1(i);
			if (val > ret) {
				ret = val;
			}
		}
		return ret;
	}
	Color[] channel_colors = {
		Color.BLUE, Color.GREEN,
		Color.RED, Color.CYAN,
		Color.MAGENTA, Color.ORANGE,
		Color.BLACK
	};

	Color get_channel_color(int ch) {
		return channel_colors[ch % channel_colors.length];
	}
}

class PlotArea {

	double m_width = 0;
	double m_height = 0;
	double m_xmin = 0, m_xmax = 0;
	double m_ymin = 0, m_ymax = 0;
	Rect m_plot_rect = new Rect(0, 0, 0, 0);

	List<PlotSeries> m_series = new ArrayList<>();

	public PlotArea() {
	}

	public void setSize(double W, double H) {
		m_width = W;
		m_height = H;
		m_plot_rect = new Rect(0, 0, W, H);
	}

	public void clearSeries() {
		m_series.clear();
	}

	public PlotSeries addSeries(PlotSeries SS) {
		m_series.add(SS);
		return SS;
	}

	
	public void setXRange(double xmin, double xmax) {
		m_xmin = xmin;
		m_xmax = xmax;
	}

	public void setYRange(double ymin, double ymax) {
		m_ymin = ymin;
		m_ymax = ymax;
	}
	public Vec2 yRange() {
		return new Vec2(m_ymin,m_ymax);
	}

	public void refresh(GraphicsContext gc) {
		gc.clearRect(0, 0, m_width, m_height);
		for (PlotSeries SS : m_series) {
			int N = SS.xvals.totalSize();
			gc.beginPath();
			gc.setStroke(SS.color);
			int incr = 1;
			int tmp=500;
			if (N > tmp) {
				incr = (int) Math.ceil(N * 1.0 / tmp);
			}
			for (int i = 0; i < N; i += incr) {
				double minval=Double.POSITIVE_INFINITY;
				double maxval=Double.NEGATIVE_INFINITY;
				for (int j=0; (j<incr)&&(i+j<N); j++) {
					double val=SS.yvals.value1(i+j);
					if (val<minval) minval=val;
					if (val>maxval) maxval=val;
				}
				double x0 = SS.xvals.value1(i);
				double y1 = minval;
				double y2 = maxval;
				Vec2 pix1=coordToPix(new Vec2(x0, y1));
				Vec2 pix2=coordToPix(new Vec2(x0, y2));
				if (i == 0) {
					gc.moveTo(pix1.x, pix1.y);
				} else {
					gc.lineTo(pix1.x, pix1.y);
				}
				if (y2!=y1) {
					gc.lineTo(pix2.x, pix2.y);
				}
			}
			gc.stroke();
		}
	}

	public Vec2 coordToPix(Vec2 coord) {
		if (m_xmax <= m_xmin) {
			return new Vec2(0, 0);
		}
		if (m_ymax <= m_ymin) {
			return new Vec2(0, 0);
		}
		double x0 = coord.x;
		double y0 = coord.y;
		double pctx = (x0 - m_xmin) / (m_xmax - m_xmin);
		double pcty = (y0 - m_ymin) / (m_ymax - m_ymin);
		double x1 = m_plot_rect.x + m_plot_rect.w * pctx;
		double y1 = m_plot_rect.y + m_plot_rect.h * (1 - pcty);
		return new Vec2(x1, y1);
	}

	public Vec2 pixToCoord(Vec2 pix) {
		if (m_plot_rect.w <= 0) {
			return new Vec2(0, 0);
		}
		if (m_plot_rect.h <= 0) {
			return new Vec2(0, 0);
		}
		double x0 = pix.x;
		double y0 = pix.y;
		double pctx = (x0 - m_plot_rect.x) / m_plot_rect.w;
		double pcty = (y0 - m_plot_rect.y) / m_plot_rect.h;
		double x1 = m_xmin + (m_xmax - m_xmin) * pctx;
		double y1 = m_ymax - (m_ymax - m_ymin) * pcty;
		return new Vec2(x1, y1);
	}
}

class PlotSeries {

	public Mda xvals = new Mda();
	public Mda yvals = new Mda();
	public Color color = Color.BLACK;

	public PlotSeries(Mda xvals0, Mda yvals0, Color color0) {
		xvals = xvals0;
		yvals = yvals0;
		color = color0;
	}

}

class Vec2 {

	public double x = 0, y = 0;

	public Vec2(double x0, double y0) {
		x = x0;
		y = y0;
	}
}

class Rect {

	public double x, y, w, h;

	public Rect(double x0, double y0, double w0, double h0) {
		x = x0;
		y = y0;
		w = w0;
		h = h0;
	}
}
