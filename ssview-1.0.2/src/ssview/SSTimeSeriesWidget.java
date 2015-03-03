package ssview;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import jviewmda.ExpandingCanvas;
import jviewmda.Mda;
import org.magland.jcommon.CallbackHandler;
import org.magland.jcommon.JUtils;

/**
 *
 * @author magland
 */
public class SSTimeSeriesWidget extends VBox {

	List<Mda> m_data_arrays=new ArrayList<>();
	Mda m_data=new Mda();
	List<SSTimeSeriesView> m_views = new ArrayList<>();
	List<SSTimeSeriesView> m_magnified_views = new ArrayList<>();
	SSTimeSeriesView m_current_view = null;
	List<SSTimeSeriesView> m_all_views=new ArrayList<>();
	SplitPane m_SP_main=new SplitPane();
	SplitPane m_SP_left=new SplitPane();
	SplitPane m_SP_right=new SplitPane();
	Label m_info_label=new Label();

	public SSTimeSeriesWidget() {	
		
		SplitPane SP_main=new SplitPane();
		
		m_SP_left.setOrientation(Orientation.VERTICAL);
		m_SP_right.setOrientation(Orientation.VERTICAL);
		SP_main.getItems().add(m_SP_left);
		SP_main.getItems().add(m_SP_right);
		m_SP_main=SP_main;
		
		this.setOnKeyPressed(evt -> {
			if (m_current_view != null) {
				m_current_view.sendKeyPress(evt);
			}
		});
		this.setOnKeyReleased(evt->{
			if (m_current_view!=null) {
				m_current_view.sendKeyRelease(evt);
			}
		});
		
		do_resize();
		
		this.getChildren().add(SP_main);
		this.getChildren().add(m_info_label);
	}
	public void addDataArray(Mda X) {
		addDataArray(X,null);
	}
	public void addDataArray(Mda X,Mda label_data) {
		m_data_arrays.add(X);
		if (m_data_arrays.size()==1) {
			m_data=X;
		}
		SSTimeSeriesView VV=new SSTimeSeriesView();
		SSTimeSeriesView VVmag=new SSTimeSeriesView();
		
		m_views.add(VV);
		m_magnified_views.add(VVmag);
		m_all_views.add(VV);
		m_all_views.add(VVmag);
		
		m_SP_left.getItems().add(VV);
		m_SP_right.getItems().add(VVmag);
		
		setup_new_views(VV,VVmag);
		
		VVmag.setShowPlots(false);
		VVmag.setData(X);
		VVmag.setXRange(0, 200);
		VV.setData(X); //important to do this after so that we get the signal to the magnified view
		
		if (label_data!=null) {
			Color[] view_colors=ViewColors.getViewColors(1);
			List<SSMarker> markers=new ArrayList<>();
			for (int i=0; i<label_data.size(1); i++) {
				for (int j=0; j<label_data.size(0); j++) {
					if (label_data.value(j,i)!=0) {
						SSMarker MM=new SSMarker();
						MM.x=i;
						MM.color=view_colors[j % view_colors.length];
						markers.add(MM);
					}
				}
			}
			VV.setMarkers(markers);
			VVmag.setMarkers(markers);
		}
		
		if (m_views.size()==1) {
			m_views.get(0).setXRange(0,Math.min(m_data.N2()-1, 10000-1));
		}
		else {
			VV.setXRange(m_views.get(0).xRange());
		}
		
		update_info();
	}
	
	//////////// PRIVATE //////////////////////////////////////////////////////
	
	void do_resize() {
		m_SP_main.setDividerPositions(0.7);
		
		if (m_views.size()>1) {
			double offset=0.7;
			double tmp=offset;
			for (int i=0; i<m_views.size()-1; i++) {
				if (i>0) tmp+=(1-offset)/(m_views.size()-1);
				m_SP_left.setDividerPosition(i,tmp);
				m_SP_right.setDividerPosition(i,tmp);
			}
		}
	}
	
	void connect_top_view_to_magnified_view(SSTimeSeriesView top_view,SSTimeSeriesView magnified_view) {
		top_view.onCurrentChannelChanged(()-> {
			magnified_view.setCurrentChannel(top_view.currentChannel());
		});
		magnified_view.onCurrentChannelChanged(()-> {
			top_view.setCurrentChannel(magnified_view.currentChannel());
		});
		
		top_view.onCurrentXChanged(() -> {
			this.requestFocus();
			magnified_view.setShowPlots(true);
			int x0 = top_view.currentXCoord();
			int diff0 = (int) (magnified_view.xRange().y - magnified_view.xRange().x);
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
			magnified_view.setXRange(x1, x2);
			magnified_view.setCurrentXCoord(x0,false);
		});
		magnified_view.onCurrentXChanged(() -> {
			top_view.setCurrentXCoord(magnified_view.currentXCoord(),false);
		});
	}
	void synchronize_views(SSTimeSeriesView V1,SSTimeSeriesView V2) {
		V1.onCurrentXChanged(()->{
			V2.setCurrentXCoord(V1.currentXCoord(),true); //important to trigger so this propogates to the label views or vice versa
		});
		V2.onCurrentXChanged(()->{
			V1.setCurrentXCoord(V2.currentXCoord(),true); //important to trigger so this propogates to the label views or vice versa
		});
		V1.onXRangeChanged(()->{
			V2.setXRange(V1.xRange());
		});
		V2.onXRangeChanged(()->{
			V1.setXRange(V2.xRange());
		});
		/*V1.onCurrentChannelChanged(()->{
			V2.setCurrentChannel(V1.currentChannel());
		});
		V2.onCurrentChannelChanged(()->{
			V1.setCurrentChannel(V2.currentChannel());
		});*/
	}
	
	
	void setup_new_views(SSTimeSeriesView VV,SSTimeSeriesView VVmag) {
		//////////////////////////////////////////
		do_resize();
		
		if (m_views.size()>1) {
			synchronize_views(VV,m_views.get(0));
			synchronize_views(VVmag,m_magnified_views.get(0));
		}
		
		VV.onCurrentChannelChanged(()->{
			if (VV.currentChannel()>=0) {
				m_views.forEach(view->{
					if (view!=VV) {
						view.setCurrentChannel(-1);
					}
				});
			}
		});
		VVmag.onCurrentChannelChanged(()->{
			if (VV.currentChannel()>=0) {
				m_magnified_views.forEach(view->{
					if (view!=VVmag) {
						view.setCurrentChannel(-1);
					}
				});
			}
		});
		
		VV.onClicked(()->{
			m_current_view=VV;
		});
		VVmag.onClicked(()->{
			m_current_view=VVmag;
		});
//		VV.onHoveredXChanged(()->{
//			m_current_view=VV;
//		});
//		VVmag.onHoveredXChanged(()->{
//			m_current_view=VVmag;
//		});
		connect_top_view_to_magnified_view(VV, VVmag);
		
		VVmag.setCanZoomAllTheWayOut(false);
		
		if (m_views.size()==1) {
			VV.setChannelColors(ViewColors.getViewColors(2));
			VVmag.setChannelColors(ViewColors.getViewColors(2));
		}
		else {
			VV.setChannelColors(ViewColors.getViewColors(1));
			VVmag.setChannelColors(ViewColors.getViewColors(1));
		}
		
		VV.onCurrentXChanged(()->update_info());
		VVmag.onCurrentXChanged(()->update_info());
		VV.onCurrentChannelChanged(()->update_info());
		VVmag.onCurrentChannelChanged(()->update_info());
	}
	
	void update_info() {
		String txt="";
		int ind=get_current_view_index();
		if (ind>=0) {
			SSTimeSeriesView VV=m_views.get(ind);
			SSTimeSeriesView VVmag=m_magnified_views.get(ind);
			Mda data_array=m_data_arrays.get(ind);
			int ch=VV.currentChannel();
			if (ch>=0) {
				txt+=String.format("Channel = %d; ",ch);
			}
			int x0=VV.currentXCoord();
			if (x0>=0) {
				txt+=String.format("Timepoint = %d; ",x0);
			}
			double val0=data_array.value(ch,x0);
			{
				txt+=String.format("Value = %f; ",val0);
			}
		}
		else {
			txt+="; ";
		}
		m_info_label.setText(txt);
	}
	int get_current_view_index() {
		for (int i=0; i<m_views.size(); i++) {
			if (m_views.get(i).currentChannel()>=0) return i;
		}
		return -1;
	}
}

class SSMarker {
	double x=0;
	Color color=Color.BLACK;
}

class SSTimeSeriesView extends StackPane {

	Mda m_data = new Mda();
	double m_current_x = -1;
	double m_hovered_x = -1;
	int m_control_anchor_x=-1;
	double m_selected_xmin = -1;
	double m_selected_xmax = -1;
	double m_selected_x_anchor=-1;
	int m_current_channel=-1;
	boolean m_cursor_visible=true;
	ExpandingCanvas m_underlay = new ExpandingCanvas();
	ExpandingCanvas m_marker_underlay = new ExpandingCanvas();
	SSTimeSeriesPlot m_plot = new SSTimeSeriesPlot();
	CallbackHandler CH = new CallbackHandler();
	boolean m_can_zoom_all_the_way_out=true;
	List<SSMarker> m_markers=new ArrayList<>();

	public SSTimeSeriesView() {
		this.setStyle("-fx-border-width: 2px;-fx-border-color: brown;");
		
		this.getChildren().add(m_underlay);
		this.getChildren().add(m_marker_underlay);
		this.getChildren().add(m_plot);
		this.setOnMousePressed(evt -> {
			CH.trigger("clicked",true);
			int x0 = (int) m_plot.pixToCoord(new Vec2(evt.getX(), evt.getY())).x;
			m_selected_xmin = m_selected_xmax = -1;
			m_selected_x_anchor=x0;
			
			if (m_current_x != x0) {
				m_current_x = x0;
				refresh_underlay();
				CH.trigger("current-x-changed", true);
			}
			
			if (evt.getClickCount()==2) {
				do_translate((int)(x0-(this.xRange().x+this.xRange().y)/2));
				return;
			}
			
			if (evt.isControlDown()) {
				m_control_anchor_x=x0;
			}
			else if (evt.getButton() == MouseButton.PRIMARY) {
				refresh_underlay();
			}
		});
		this.setOnMouseReleased(evt -> {
			on_mouse_released(evt);
			m_control_anchor_x=-1;

		});
		this.setOnMouseDragged(evt -> {
			int x0 = (int) m_plot.pixToCoord(new Vec2(evt.getX(), evt.getY())).x;
			if (evt.isControlDown()) {
				this.getScene().setCursor(Cursor.MOVE);
				if (m_control_anchor_x>=0) {
					do_translate((int)(m_control_anchor_x-x0));
				}
			}
			else if (evt.getButton() == MouseButton.PRIMARY) {
				if (m_current_x != x0) {
					m_current_x = x0;
					refresh_underlay();
					CH.trigger("current-x-changed", true);
				}
				m_selected_xmin = Math.min(m_selected_x_anchor, x0);
				m_selected_xmax = Math.max(m_selected_x_anchor, x0);
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
				schedule_zoom_in((int) m_current_x);
			} else if (delta_y < 0) {
				schedule_zoom_out((int) m_current_x);
			}

		});
		this.setOnKeyPressed(evt -> {
			sendKeyPress(evt);
		});
		this.setOnKeyReleased(evt->{
			sendKeyRelease(evt);
		});
		
		m_plot.onRefreshRequired(()->{
			this.refresh_underlay();
			this.refresh_marker_underlay();
		});
	}

	public void setData(Mda X) {
		m_data = X;
		m_plot.setData(X);
		m_current_x=X.size(1)/2;
		CH.trigger("current-x-changed", true);
		refresh_underlay();
		refresh_marker_underlay();
	}
	
	public void setMarkers(List<SSMarker> markers) {
		m_markers=markers;
		refresh_marker_underlay();
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
	public void onXRangeChanged(Runnable X) {
		CH.bind("x-range-changed", X);
	}
	public void onCurrentChannelChanged(Runnable X) {
		CH.bind("current-channel-changed", X);
	}
	public void onClicked(Runnable X) {
		CH.bind("clicked", X);
	}
	
	public void sendKeyRelease(KeyEvent evt) {
		if (evt.getCode()==KeyCode.CONTROL) {
			getScene().setCursor(Cursor.DEFAULT);
		}
	}

	public void sendKeyPress(KeyEvent evt) {
		if (evt.getCode()==KeyCode.CONTROL) {
			this.getScene().setCursor(Cursor.MOVE);
		}
		else if (evt.getCode() == KeyCode.EQUALS) {
			schedule_zoom_in((int) m_current_x);
			//evt.consume();
		} else if (evt.getCode() == KeyCode.MINUS) {
			schedule_zoom_out((int) m_current_x);
			//evt.consume();
		} else if (evt.getCode().equals(KeyCode.LEFT)) {
			if (evt.isControlDown()) {
				do_translate(-1);
			} else {
				move_current_x_coord(-1);
			}
		} else if (evt.getCode().equals(KeyCode.RIGHT)) {
			if (evt.isControlDown()) {
				do_translate(+1);
			} else {
				move_current_x_coord(+1);
			}
		} else if (evt.getCode().equals(KeyCode.UP)) {
			if (this.currentChannel()+1<m_data.size(0))
				this.setCurrentChannel(this.currentChannel()+1);
		} else if (evt.getCode().equals(KeyCode.DOWN)) {
			if (this.currentChannel()-1>=0)
				this.setCurrentChannel(this.currentChannel()-1);
		}
		else if (evt.getCode().equals(KeyCode.DIGIT0)) {
			zoom_all_the_way_out();
		}
		else if (evt.getCode().equals(KeyCode.S)) {
			save_image();
		}
		else if (evt.getCode().equals(KeyCode.M)) {
			m_marker_underlay.setVisible(!m_marker_underlay.isVisible());
		}
		else if (evt.getCode().equals(KeyCode.C)) {
			m_cursor_visible=!m_cursor_visible;
			refresh_underlay();
		}
	}


	public void setCanZoomAllTheWayOut(boolean val) {
		m_can_zoom_all_the_way_out=val;
	}
	
	public void setChannelColors(Color[] colors) {
		m_plot.setChannelColors(colors);
	}

	//////////////// PRIVATE ////////////////////
	
	void save_image() {
		Canvas CC=m_plot.canvas();
		WritableImage wim=new WritableImage((int)CC.getWidth(),(int)CC.getHeight());
		CC.snapshot(null, wim);
		ImageView VV=new ImageView();
		VV.setImage(wim);
		StackPane PP=new StackPane();
		PP.setMinSize(wim.getWidth(), wim.getHeight());
		PP.setMaxSize(wim.getWidth(), wim.getHeight());
		PP.getChildren().add(VV);
		JUtils.popupWidget(PP, "");
	}
	int MIN_PIX_FOR_ZOOM_SELECTION=10;
	boolean far_enough_away_in_terms_of_pixels(double x1,double x2,int min_pix) {
		Vec2 A=m_plot.coordToPix(new Vec2(x1,0));
		Vec2 B=m_plot.coordToPix(new Vec2(x2,0));
		return (Math.abs(A.x-B.x)>=min_pix);
	}
	
	void on_mouse_released(MouseEvent evt) {
		if (evt.isControlDown()) return;
		Vec2 coord = m_plot.pixToCoord(new Vec2(evt.getX(), evt.getY()));
		if ((m_selected_xmin >= 0) && (far_enough_away_in_terms_of_pixels(m_selected_xmin,m_selected_xmax,MIN_PIX_FOR_ZOOM_SELECTION))) {
			this.setXRange((int) m_selected_xmin, (int) m_selected_xmax);
			m_current_x = (m_selected_xmin + m_selected_xmax) / 2;
			m_selected_xmin = -1;
			m_selected_xmax = -1;
			CH.trigger("current-x-changed", true);
			refresh_underlay();
			refresh_marker_underlay();
		}
		setCurrentChannel(m_plot.pixToChannel(new Vec2(evt.getX(), evt.getY())));
	}

	boolean zoom_in_scheduled=false;
	void schedule_zoom_in(int center_x) {
		if (zoom_in_scheduled) return;
		zoom_in_scheduled=true;
		CallbackHandler.scheduleCallback(()->{
			zoom_in(center_x);
			zoom_in_scheduled=false;
		},10);
	}
	void zoom_in(int center_x) {
		do_zoom(center_x, 0.8);
	}

	boolean zoom_out_scheduled=false;
	void schedule_zoom_out(int center_x) {
		if (zoom_out_scheduled) return;
		zoom_out_scheduled=true;
		CallbackHandler.scheduleCallback(()->{
			zoom_out(center_x);
			zoom_out_scheduled=false;
		},10);
	}
	void zoom_out(int center_x) {
		do_zoom(center_x, 1 / 0.8);
	}

	void do_translate(int dx) {
		int x_left = (int) m_plot.xRange().x;
		int x_right = (int) m_plot.xRange().y;
		x_left += dx;
		x_right += dx;
		if ((x_left >= 0) && (x_right < m_data.size(1))) {
			this.setXRange(x_left, x_right);
			this.refresh_underlay();
			this.refresh_marker_underlay();
		}
	}

	void move_current_x_coord(int dx) {
		int new_x = (int) (m_current_x + dx);
		new_x = Math.max(0, new_x);
		new_x = Math.min(m_data.size(1), new_x);
		if (dx<0) {
			//find the next non-zero entry
			boolean done=false;
			while ((!done)&&(new_x-1>=0)) {
				boolean all_zeros=true;
				for (int ch=0; ch<m_data.size(0); ch++) {
					if (m_data.value(ch,new_x)!=0) {
						all_zeros=false;
					}
				}
				if (all_zeros) {
					new_x--;
				}
				else done=true;
			}
		}
		else if (dx>0) {
			//find the next non-zero entry
			boolean done=false;
			while ((!done)&&(new_x+1<m_data.size(1))) {
				boolean all_zeros=true;
				for (int ch=0; ch<m_data.size(0); ch++) {
					if (m_data.value(ch,new_x)!=0) {
						all_zeros=false;
					}
				}
				if (all_zeros) {
					new_x++;
				}
				else done=true;
			}
		}
		this.setCurrentXCoord(new_x,true);
		CH.trigger(("current-x-changed"), true);
		if (new_x < m_plot.xRange().x) {
			do_translate((int) (new_x - m_plot.xRange().x));
		}
		if (new_x > m_plot.xRange().y) {
			do_translate((int) (new_x - m_plot.xRange().y));
		}
	}
	
	void zoom_all_the_way_out() {
		if (m_can_zoom_all_the_way_out) {
			int x_left=0;
			int x_right=m_data.size(1)-1;
			this.setXRange(x_left, x_right);
			this.refresh_underlay();
			this.refresh_marker_underlay();
		}
	}

	void do_zoom(int center_x, double frac) {
		int xmin = (int) m_plot.xRange().x;
		int xmax = (int) m_plot.xRange().y;
		int diff = xmax - xmin;
		int new_diff = (int) Math.floor(diff * frac);
		if ((new_diff==diff)&&(frac>1)) new_diff+=5;
		if ((new_diff==diff)&&(frac<1)) new_diff-=5;
		if ((new_diff<8)&&(new_diff<diff)) return;
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
		this.setXRange(x_left, x_right);
		this.refresh_underlay();
		this.refresh_marker_underlay();
	}

	public void setXRange(int x_left, int x_right) {
		int x1=Math.max(x_left, 0);
		int x2=Math.min(m_data.size(1) - 1, x_right);
		if ((x1==m_plot.xRange().x)&&(x2==m_plot.xRange().y)) {
			return; //important to do this so we don't end up in infinite recursion
		}
		m_plot.setXRange(x1, x2);
		refresh_underlay();
		refresh_marker_underlay();
		CH.trigger("x-range-changed",true);
	}
	public void setXRange(Vec2 range) {
		this.setXRange((int)range.x,(int)range.y);
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
	public int currentChannel() {
		return m_current_channel;
	}

	public void setCurrentXCoord(int x0,boolean do_trigger) {
		if (m_current_x != x0) {
			m_current_x = x0;
			if (do_trigger) {
				CH.trigger("current-x-changed",true);
			}
			this.refresh_underlay();
		}
	}
	public void setCurrentChannel(int ch) {
		setCurrentChannel(ch,true);
	}
	public void setCurrentChannel(int ch,boolean do_trigger) {
		if (ch!=m_current_channel) {
			if (ch>=m_data.size(0)) return; //allow -1
			m_current_channel=ch;
			if (do_trigger) {
				CH.trigger("current-channel-changed", do_trigger);
			}
			this.refresh_underlay();
		}
	}

	////////////////// PRIVATE //////////////////
	void refresh_underlay() {
		GraphicsContext gc = m_underlay.getGraphicsContext2D();

		gc.clearRect(0, 0, getWidth(), getHeight());

		//current channel
		if (m_current_channel>=0) {
			gc.setFill(Color.LIGHTGRAY);
			gc.setStroke(Color.LIGHTGRAY);
			gc.setLineWidth(2);
			Rect RR = m_plot.channelToRect(m_current_channel);
			//gc.strokeRect(RR.x, RR.y, RR.w, RR.h);
			gc.fillRect(RR.x, RR.y, RR.w, RR.h);
		}
		
		//hover location
		{
			Vec2 p0 = m_plot.coordToPix(new Vec2(m_hovered_x, 0));
			Vec2 p1 = m_plot.coordToPix(new Vec2(m_hovered_x, 0));
			p0.y = 0;
			p1.y = m_underlay.getHeight();

			gc.beginPath();
			gc.setStroke(Color.LIGHTBLUE);
			gc.setLineWidth(2);
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

			if (m_cursor_visible) {
				gc.beginPath();
				gc.setStroke(Color.GRAY);
				gc.setLineWidth(5);
				gc.moveTo(p0.x, p0.y);
				gc.lineTo(p1.x, p1.y);
				gc.stroke();

				gc.beginPath();
				gc.setStroke(Color.LIGHTGRAY);
				gc.setLineWidth(3);
				gc.moveTo(p0.x, p0.y);
				gc.lineTo(p1.x, p1.y);
				gc.stroke();
			}
			else {
			}
		}

		//selected
		if (m_selected_xmin >= 0) {
			double ymin = m_plot.yRange().x;
			double ymax = m_plot.yRange().y;
			Vec2 p0 = m_plot.coordToPix(new Vec2(m_selected_xmin, ymin));
			Vec2 p1 = m_plot.coordToPix(new Vec2(m_selected_xmax, ymax));

			gc.beginPath();
			gc.setStroke(Color.RED);
			if (Math.abs(p0.x-p1.x)<MIN_PIX_FOR_ZOOM_SELECTION)
				gc.setStroke(Color.GRAY);
			gc.setLineWidth(6);
			gc.moveTo(p0.x, p0.y);
			gc.lineTo(p1.x, p0.y);
			gc.lineTo(p1.x, p1.y);
			gc.lineTo(p0.x, p1.y);
			gc.lineTo(p0.x, p0.y);
			gc.stroke();
		}
	}
	void refresh_marker_underlay() {
		GraphicsContext gc = m_marker_underlay.getGraphicsContext2D();

		gc.clearRect(0, 0, getWidth(), getHeight());

		double xmin=m_plot.xRange().x;
		double xmax=m_plot.xRange().y;

		//if (xmax-xmin>8000) return;
		
		for (int i=0; i<m_markers.size(); i++) {
			SSMarker MM=m_markers.get(i);
			if ((xmin<=MM.x)&&(MM.x<=xmax)) {
				
				Vec2 P0=m_plot.coordToPix(new Vec2(MM.x,m_plot.yRange().x));
				Vec2 P1=m_plot.coordToPix(new Vec2(MM.x,m_plot.yRange().y));
				
				gc.setStroke(MM.color);
				
				gc.beginPath();
				gc.moveTo(P0.x, P0.y);
				gc.lineTo(P1.x, P1.y);
				gc.stroke();
			}
		}
	}
}

class SSTimeSeriesPlot extends ExpandingCanvas {

	Mda m_data = new Mda();
	PlotArea m_plot_area = new PlotArea();
	double[] m_minvals = new double[1];
	double[] m_maxvals = new double[1];
	double[] m_plot_offsets = new double[0];
	double[] m_plot_y1=new double[0];
	double[] m_plot_y2=new double[0];
	int m_xrange_min = -1;
	int m_xrange_max = -1;
	boolean m_show_plots = true;
	Color[] m_channel_colors = ViewColors.getViewColors(1);
	CallbackHandler CH=new CallbackHandler();

	public SSTimeSeriesPlot() {
		this.setOnRefresh(() -> {
			Platform.runLater(() -> {
				refresh_plot();
				CH.trigger("refresh-required",true);
			});
		});
	}
	public void onRefreshRequired(Runnable X) {
		CH.bind("refresh-required",X);
	}

	public void setData(Mda X) {
		m_data = X;

		m_xrange_min = 0;
		m_xrange_max = X.size(1) - 1;

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
	public int pixToChannel(Vec2 p) {
		Vec2 coord=m_plot_area.pixToCoord(p);
		for (int i=0; i<m_plot_y1.length; i++) {
			if ((coord.y>=m_plot_y1[i])&&(coord.y<=m_plot_y2[i])) return i;
		}
		return -1;
	}
	public Rect channelToRect(int ch) {
		if ((ch<0)||(ch>=m_plot_y1.length)) return new Rect(-1,-1,-1,-1);
		Vec2 p0=coordToPix(new Vec2(0,m_plot_y1[ch]));
		Vec2 p1=coordToPix(new Vec2(m_data.size(1),m_plot_y2[ch]));
		return new Rect(Math.min(p0.x,p1.x),Math.min(p0.y,p1.y),Math.abs(p0.x-p1.x),Math.abs(p0.y-p1.y));
	}

	public Vec2 xRange() {
		return new Vec2(m_xrange_min, m_xrange_max);
	}

	public Vec2 yRange() {
		return m_plot_area.yRange();
	}

	public void setXRange(int xmin, int xmax) {
		m_xrange_min = Math.max(xmin, 0);
		m_xrange_max = Math.min(xmax, m_data.size(1) - 1);
		this.refresh_plot();
	}
	public void setChannelColors(Color[] colors) {
		m_channel_colors=colors.clone();
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
		m_plot_y1=new double[M];
		m_plot_y2=new double[M];
		double offset = 0;
		for (int ch = 0; ch < M; ch++) {
			m_plot_y1[ch]=offset;
			offset += (-m_minvals[ch]);
			m_plot_offsets[ch] = offset;
			offset += m_maxvals[ch];
			offset += max00 / 20;
			m_plot_y2[ch]=offset;
		}

		m_plot_area.clearSeries();
		GraphicsContext gc = this.getGraphicsContext2D();
		m_plot_area.setSize(this.getWidth(), this.getHeight());

		if (M == 0) {
			start_refreshing_plot_area(gc);
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
			start_refreshing_plot_area(gc);
		} else {
			gc.clearRect(0, 0, getWidth(), getHeight());
		}
	}
	
	void start_refreshing_plot_area(GraphicsContext gc) {
		m_plot_area.refresh(gc);
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

	Color get_channel_color(int ch) {
		return m_channel_colors[ch % m_channel_colors.length];
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
		return new Vec2(m_ymin, m_ymax);
	}

	int[] pix_res_values={500,1000,2000,4000};
	int current_refresh_pix_res_index=0;
	int current_refresh_code=1;
	int pix_res_index=0;
	public void refresh(GraphicsContext gc) {
		gc.clearRect(0, 0, m_width, m_height);
		current_refresh_code++;
		int pr=pix_res_values[pix_res_index];
		do_refresh(current_refresh_code,0,m_xmin-1,gc);
	}
	void do_refresh(int code,int pr_index,double xmin,GraphicsContext gc) {
		long starttime0=System.nanoTime();
		
		if (code!=current_refresh_code) return;
		if (pr_index>=pix_res_values.length) return;
		int pr=pix_res_values[pr_index];
		
		int tot_num_points=(int)(m_xmax-m_xmin+1);
		
		int incr = 1;
		if (tot_num_points > pr) {
			incr = (int) Math.ceil(tot_num_points * 1.0 / pr);
		}
		double tmpx1=coordToPix(new Vec2(xmin,0)).x;
		double tmpx2=coordToPix(new Vec2(xmin+tot_num_points,0)).x+1;
		
		int num_points_per_run=1000*1000;
		double xmax=xmin+num_points_per_run*1.0/m_series.size()*incr;
		//double xmax=xmin+(tmpx2-tmpx1)/tot_num_points*incr*num_points_per_run/m_series.size();
		Vec2 PP0=coordToPix(new Vec2(xmin,m_ymin));
		Vec2 PP1=coordToPix(new Vec2(xmax,m_ymax));
		/*
		String[] color_strings={
		"#F7977A","#FDC68A",
		"#C4DF9B","#82CA9D",
		"#6ECFF6","#8493CA",
		"#A187BE","#F49AC2",
		"#F9AD81","#FFF79A",
		"#A2D39C","#7BCDC8",
		"#7EA7D8","#8882BE",
		"#BC8DBF","#F6989D"
		};
		gc.setFill(Color.web(color_strings[((int)Math.abs(xmin)) % color_strings.length]));
		gc.fillRect(Math.min(PP0.x,PP1.x),Math.min(PP0.y,PP1.y), Math.abs(PP0.x-PP1.x), Math.abs(PP0.y-PP1.y));
		*/
		gc.clearRect(Math.min(PP0.x,PP1.x),Math.min(PP0.y,PP1.y), Math.abs(PP0.x-PP1.x), Math.abs(PP0.y-PP1.y));
				
		for (int ss=0; ss<m_series.size(); ss++) {
			PlotSeries SS=m_series.get(ss);
			int N = SS.xvals.totalSize();
			boolean is_first=true;
			gc.beginPath();
			gc.setStroke(SS.color);
			int i1=(int)(xmin-incr*10 - m_xmin); 
			int i2=(int)(xmax+incr*10 - m_xmin); 
			i1=(i1/incr-1)*incr; i1=(int)Math.min(Math.max(i1,0),N);
			i2=(i2/incr+1)*incr; i2=(int)Math.min(Math.max(i2,0),N);
			for (int i = i1; i < i2; i += incr) {
				double x0 = SS.xvals.value1(i);
				double minval = Double.POSITIVE_INFINITY;
				double maxval = Double.NEGATIVE_INFINITY;
				int incr2=1;
				for (int j = 0; (j < incr) && (i + j < N); j+=incr2) {
					double val = SS.yvals.value1(i + j);
					if (val < minval) {
						minval = val;
					}
					if (val > maxval) {
						maxval = val;
					}
				}
				double y1 = minval;
				double y2 = maxval;
				Vec2 pix1 = coordToPix(new Vec2(x0, y1));
				Vec2 pix2 = coordToPix(new Vec2(x0, y2));
				if (is_first) {
					is_first=false;
					gc.moveTo(pix1.x, pix1.y);
				} else {
					gc.lineTo(pix1.x, pix1.y);
				}
				if (y2 != y1) {
					gc.lineTo(pix2.x, pix2.y);
				}
			}
			gc.stroke();
		}
		double elapsed=(System.nanoTime()-starttime0)*1.0/(1000*1000);
		if (xmax<m_xmax) {
			CallbackHandler.scheduleCallback(()->{
				do_refresh(code, pr_index, xmax, gc);
			},(int)elapsed+50);
		}
		else {
			if ((incr>1)&&(pr_index+1<pix_res_values.length)) {
				CallbackHandler.scheduleCallback(()->{
					do_refresh(code, pr_index+1, m_xmin-1, gc);
				},(int)elapsed+400); //add the 400 to give it some delay to enhance user experience
			}
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

class ViewColors {
	static public Color[] getViewColors(int num) {
		if (num==1) {
			String[] color_strings={
			"#F7977A","#FDC68A",
			"#C4DF9B","#82CA9D",
			"#6ECFF6","#8493CA",
			"#A187BE","#F49AC2",
			"#F9AD81","#FFF79A",
			"#A2D39C","#7BCDC8",
			"#7EA7D8","#8882BE",
			"#BC8DBF","#F6989D"
			};

			Color[] view_colors=new Color[color_strings.length];
			for (int i=0; i<color_strings.length; i++) {
				view_colors[i]=Color.web(color_strings[i]);
			}

			return view_colors;
		}
		else {
			Color[] ret={
				Color.DARKBLUE, Color.DARKGREEN,
				Color.DARKRED, Color.DARKCYAN,
				Color.DARKMAGENTA, Color.DARKORANGE,
				Color.BLACK
			};
			return ret;
		}
	}
}