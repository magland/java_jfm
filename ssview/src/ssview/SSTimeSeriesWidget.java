package ssview;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
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
public class SSTimeSeriesWidget extends StackPane {

	Mda m_data = new Mda();
	SSTimeSeriesView m_view = new SSTimeSeriesView();

	public SSTimeSeriesWidget() {
		this.getChildren().add(m_view);
	}

	public void setData(Mda X) {
		m_data = X;
		m_view.setData(X);
	}

}

class SSTimeSeriesView extends StackPane {
	Mda m_data = new Mda();
	double m_current_x=-1;
	double m_hovered_x=-1;
	ExpandingCanvas m_underlay=new ExpandingCanvas();
	SSTimeSeriesPlot m_plot=new SSTimeSeriesPlot();

	public SSTimeSeriesView() {
		this.getChildren().add(m_underlay);
		this.getChildren().add(m_plot);
		this.setOnMouseReleased(evt->{
			Vec2 coord=m_plot.pixToCoord(new Vec2(evt.getX(),evt.getY()));
			m_current_x=coord.x;
			refresh_underlay();
		});
		this.setOnMouseMoved(evt->{
			Vec2 hovered_coordinate=m_plot.pixToCoord(new Vec2(evt.getX(),evt.getY()));
			m_hovered_x=hovered_coordinate.x;
			refresh_underlay();
		});
		this.setOnMouseExited(evt->{
			m_hovered_x=-1;
			refresh_underlay();
		});
		this.setOnScroll(evt->{
			double delta_y=evt.getDeltaY();
			if (delta_y>0) {
				zoom_in();
			}
			else if (delta_y<0) {
				zoom_out();
			}
			
		});
	}

	public void setData(Mda X) {
		m_data = X;
		m_plot.setData(X);
		refresh_underlay();
	}
	
	void zoom_in() {
		do_zoom(0.9);
	}
	
	void zoom_out() {
		do_zoom(1/0.9);
	}
	
	void do_zoom(double frac) {
		int xmin=m_plot.xrangeMin();
		int xmax=m_plot.xrangeMax();
		int diff=xmax-xmin;
		int new_diff=(int)Math.floor(diff*frac);
		int x0=(int)m_hovered_x;
		if (x0<0) x0=(xmax+xmin)/2;
		
		//we need to make sure that the hovered point stays in the same place
		//right now, the x0 is in the center. we need to shift it over
		Vec2 pt_left=m_plot.coordToPix(new Vec2(xmin,0));
		Vec2 pt_right=m_plot.coordToPix(new Vec2(xmax,0));
		Vec2 pt_hover=m_plot.coordToPix(new Vec2(x0,0));
		
		if (pt_right.x<=pt_left.x) {
			return;
		}
		double pct_pt_hover=(pt_hover.x-pt_left.x)/(pt_right.x-pt_left.x);
		//x_left = x0-pct_pt_hover*new_diff
		int x_left=(int)Math.floor(x0-pct_pt_hover*new_diff);
		int x_right=x_left+new_diff;
		
		if (x_left<0) {
			x_left=0;
			x_right=x_left+new_diff;
		}
		if (x_right>=m_data.size(1)) {
			x_right=m_data.size(1)-1;
			x_left=x_right-new_diff;
			if (x_left<0) x_left=0;
		}
		m_plot.setXRange(x_left, x_right);
	}
	
	////////////////// PRIVATE //////////////////
	void refresh_underlay() {
		GraphicsContext gc=m_underlay.getGraphicsContext2D();
		
		gc.clearRect(0,0,getWidth(),getHeight());
		
		{
			Vec2 p0=m_plot.coordToPix(new Vec2(m_hovered_x,0));
			Vec2 p1=m_plot.coordToPix(new Vec2(m_hovered_x,0));
			p0.y=0;
			p1.y=m_underlay.getHeight();

			gc.beginPath();
			gc.setStroke(Color.LIGHTCYAN);
			gc.setLineWidth(12);
			gc.moveTo(p0.x,p0.y);
			gc.lineTo(p1.x,p1.y);
			gc.stroke();
		}
		{
			Vec2 p0=m_plot.coordToPix(new Vec2(m_current_x,0));
			Vec2 p1=m_plot.coordToPix(new Vec2(m_current_x,0));
			p0.y=0;
			p1.y=m_underlay.getHeight();

			gc.beginPath();
			gc.setStroke(Color.LIGHTBLUE);
			gc.setLineWidth(3);
			gc.moveTo(p0.x,p0.y);
			gc.lineTo(p1.x,p1.y);
			gc.stroke();
		}
	}
}

class SSTimeSeriesPlot extends ExpandingCanvas {

	Mda m_data = new Mda();
	PlotArea m_plot_area = new PlotArea();
	double[] m_minvals=new double[0];
	double[] m_maxvals=new double[0];
	double[] m_plot_offsets=new double[0];
	int m_xrange_min=50;
	int m_xrange_max=550;

	public SSTimeSeriesPlot() {
		this.setOnRefresh(()->{
			Platform.runLater(()->{
				refresh_plot();
			});
		});
	}
	public void setData(Mda X) {
		m_data = X;
		int M=X.size(0);
		int N=X.size(1);
		m_minvals=new double[M];
		m_maxvals=new double[M];
		for (int ch=0; ch<M; ch++) {
			if (N>0) {
				m_minvals[ch]=X.value(ch,0);
				m_maxvals[ch]=X.value(ch,0);
			}
			for (int i=0; i<N; i++) {
				double val=X.value(ch,i);
				if (val<m_minvals[ch]) m_minvals[ch]=val;
				if (val>m_maxvals[ch]) m_maxvals[ch]=val;
			}
		}
		refresh_plot();
	}
	public Vec2 coordToPix(Vec2 p) {
		return m_plot_area.coordToPix(p);
	}
	public Vec2 pixToCoord(Vec2 p) {
		return m_plot_area.pixToCoord(p);
	}
	public int xrangeMin() {
		return m_xrange_min;
	}
	public int xrangeMax() {
		return m_xrange_max;
	}
	public void setXRange(int xmin,int xmax) {
		m_xrange_min=xmin;
		m_xrange_max=xmax;
		this.refresh_plot();
	}

	////////////////////// PRIVATE /////////////////////////
	void refresh_plot() {
		int M=m_data.size(0);
		int NN = m_data.size(1);
		
		m_plot_area.setSize(this.getWidth(), this.getHeight());
		
		m_plot_offsets=new double[M];
		double max00=0;
		for (int ch=0; ch<M; ch++) {
			if (Math.abs(m_minvals[ch])>max00)
				max00=Math.abs(m_minvals[ch]);
			if (Math.abs(m_maxvals[ch])>max00)
				max00=Math.abs(m_maxvals[ch]);
		}
		double offset=0;
		for (int ch=0; ch<M; ch++) {
			offset+=(-m_minvals[ch]);
			m_plot_offsets[ch]=offset;
			offset+=m_maxvals[ch];
			offset+=max00/20;
		}

		m_plot_area.clearSeries();
		GraphicsContext gc = this.getGraphicsContext2D();
		m_plot_area.setSize(this.getWidth(), this.getHeight());
		
		if (M==0) {
			m_plot_area.refresh(gc);
			return;
		}
		
		int xrange_min=m_xrange_min; if (xrange_min<0) xrange_min=0;
		int xrange_max=m_xrange_max; if (xrange_max<0) xrange_max=NN-1;
		
		m_plot_area.setXRange(xrange_min-1, xrange_max+1);
		m_plot_area.setYRange(m_plot_offsets[0]+m_minvals[0]-max00/20, m_plot_offsets[M-1]+m_maxvals[M-1]+max00/20);
		
		for (int ch=0; ch<M; ch++) {
			Mda xvals = new Mda();
			xvals.allocate(1, xrange_max-xrange_min+1);
			for (int x = xrange_min; x <=xrange_max; x++) {
				xvals.setValue1(x, x-xrange_min);
			}
			Mda yvals=new Mda();
			yvals.allocate(1,xrange_max-xrange_min+1);
			for (int ii=xrange_min; ii<=xrange_max; ii++)
				yvals.setValue1(m_data.value(ch,ii)+m_plot_offsets[ch], ii-xrange_min);
			Color color=get_channel_color(ch);
			m_plot_area.addSeries(new PlotSeries(xvals, yvals,color));
			m_plot_area.refresh(gc);
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
	Color[] channel_colors={
		Color.BLUE,Color.GREEN,
		Color.RED,Color.CYAN,
		Color.MAGENTA,Color.ORANGE,
		Color.BLACK
	};
	Color get_channel_color(int ch) {
		return channel_colors[ch % channel_colors.length];
	}
}

class PlotArea {

	double m_width = 0;
	double m_height = 0;
	double m_xmin=0,m_xmax=0;
	double m_ymin=0,m_ymax=0;
	Rect m_plot_rect=new Rect(0,0,0,0);
	
	List<PlotSeries> m_series = new ArrayList<>();

	public PlotArea() {
	}

	public void setSize(double W, double H) {
		m_width = W;
		m_height = H;
		m_plot_rect=new Rect(0,0,W,H);
	}
	public void clearSeries() {
		m_series.clear();
	}
	public PlotSeries addSeries(PlotSeries SS) {
		m_series.add(SS);
		return SS;
	}
	public void setXRange(double xmin,double xmax) {
		m_xmin=xmin;
		m_xmax=xmax;
	}
	public void setYRange(double ymin,double ymax) {
		m_ymin=ymin;
		m_ymax=ymax;
	}
	public void refresh(GraphicsContext gc) {
		gc.clearRect(0,0,m_width,m_height);
		for (PlotSeries SS : m_series) {
			int N=SS.xvals.totalSize();
			gc.beginPath();
			gc.setStroke(SS.color);
			int incr=1;
			if (N>9764) {
				incr=(int) Math.ceil(N*1.0/9764);
			}
			for (int i=0; i<N; i+=incr) {
				double x0=SS.xvals.value1(i);
				double y0=SS.yvals.value1(i);
				Vec2 pix=coordToPix(new Vec2(x0,y0));
				if (i==0) gc.moveTo(pix.x, pix.y);
				else gc.lineTo(pix.x,pix.y);
			}
			gc.stroke();
		}
	}
	public Vec2 coordToPix(Vec2 coord) {
		if (m_xmax<=m_xmin) return new Vec2(0,0);
		if (m_ymax<=m_ymin) return new Vec2(0,0);
		double x0=coord.x;
		double y0=coord.y;
		double pctx=(x0-m_xmin)/(m_xmax-m_xmin);
		double pcty=(y0-m_ymin)/(m_ymax-m_ymin);
		double x1=m_plot_rect.x+m_plot_rect.w*pctx;
		double y1=m_plot_rect.y+m_plot_rect.h*(1-pcty);
		return new Vec2(x1,y1);
	}
	
	public Vec2 pixToCoord(Vec2 pix) {
		if (m_plot_rect.w<=0) return new Vec2(0,0);
		if (m_plot_rect.h<=0) return new Vec2(0,0);
		double x0=pix.x;
		double y0=pix.y;
		double pctx=(x0-m_plot_rect.x)/m_plot_rect.w;
		double pcty=(y0-m_plot_rect.y)/m_plot_rect.h;
		double x1=m_xmin+(m_xmax-m_xmin)*pctx;
		double y1=m_ymax-(m_ymax-m_ymin)*pcty;
		return new Vec2(x1,y1);
	}
}

class PlotSeries {

	public Mda xvals=new Mda();
	public Mda yvals=new Mda();
	public Color color=Color.BLACK;
	
	public PlotSeries(Mda xvals0,Mda yvals0,Color color0) {
		xvals=xvals0;
		yvals=yvals0;
		color=color0;
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
