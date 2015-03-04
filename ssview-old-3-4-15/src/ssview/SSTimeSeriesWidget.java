package ssview;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import jviewmda.Mda;
import org.magland.jcommon.CallbackHandler;

/**
 *
 * @author magland
 */
public class SSTimeSeriesWidget extends VBox {

	Mda m_data = new Mda();
	Mda m_timepoints_labels = new Mda();
	List<SSTimeSeriesView> m_views = new ArrayList<>();
	List<SSTimeSeriesView> m_magnified_views = new ArrayList<>();
	SSTimeSeriesView m_current_view = null;
	List<SSTimeSeriesView> m_all_views = new ArrayList<>();
	SplitPane m_SP_main = new SplitPane();
	SplitPane m_SP_left = new SplitPane();
	SplitPane m_SP_right = new SplitPane();
	private HBox m_top_controls = new HBox();
	Label m_info_label = new Label();
	double m_sampling_frequency = 20000; //Hz
	Mda m_timepoint_mapping=null;
	CallbackHandler CH=new CallbackHandler();

	public SSTimeSeriesWidget() {

		SplitPane SP_main = new SplitPane();

		m_SP_left.setOrientation(Orientation.VERTICAL);
		m_SP_right.setOrientation(Orientation.VERTICAL);
		SP_main.getItems().add(m_SP_left);
		SP_main.getItems().add(m_SP_right);
		m_SP_main = SP_main;

		this.setOnKeyPressed(evt -> {
			if (m_current_view != null) {
				m_current_view.sendKeyPress(evt);
			}
		});
		this.setOnKeyReleased(evt -> {
			if (m_current_view != null) {
				m_current_view.sendKeyRelease(evt);
			}
		});

		do_resize();

		{
			MenuButton menu_button = new MenuButton("Tools");
			{
				MenuItem item = new MenuItem("Show clips...");
				item.setOnAction(e -> on_show_clips());
				menu_button.getItems().add(item);
			}

			m_top_controls.getChildren().addAll(menu_button);
		}

		this.getChildren().add(m_top_controls);
		this.getChildren().add(SP_main);
		this.getChildren().add(m_info_label);
	}

	public void addDataArray(Mda X) {
		addDataArray(X, null);
	}
	
	public void addDataArray(Mda X,Mda timepoints_labels) {
		add_data_array(X,timepoints_labels,null);
	}
	
	public void addTimepointsLabels(Mda TL) {
		add_data_array(null,null,TL);
	}
	public void setTimepointMapping(Mda TM) {
		m_timepoint_mapping=TM;
	}
	public Mda timepointMapping() {
		return m_timepoint_mapping;
	}
	public int currentTimepoint() {
		if (m_views.size()==0) return -1;
		SSTimeSeriesView VV=m_views.get(0);
		int x0=VV.currentXCoord();
		if ((m_timepoint_mapping!=null)&&(x0>=0)) {
			x0=(int)m_timepoint_mapping.value(0,x0);
		}
		return x0;
	}
	public void setCurrentTimepoint(int t0) {
		if (t0<0) return;
		if (m_views.size()==0) return;
		SSTimeSeriesView VV=m_views.get(0);
		if (m_timepoint_mapping!=null) {
			boolean found=false;
			int x0=VV.currentXCoord();
			if ((int)m_timepoint_mapping.value(0,x0)==t0) {
				return; //already at that timepoint
			}
			for (int i=0; i<m_timepoint_mapping.size(1); i++) {
				if ((int)m_timepoint_mapping.value(0,i)==t0) {
					t0=i;
					found=true;
					break;
				}
			}
			if (!found) return;
		}
		if (t0<0) return;
		VV.setCurrentXCoord(t0, true);
	}
	public void onCurrentTimepointChanged(Runnable X) {
		CH.bind("current-timepoint-changed", X);
	}
	public void synchronizeWith(SSTimeSeriesWidget W) {
		synchronize_with(W);
	}

	public void add_data_array(Mda X, Mda timepoints_labels, Mda TL) {
		if ((m_views.size()==0)&&(X!=null)) {
			m_data = X;
			if (timepoints_labels != null) {
				m_timepoints_labels = timepoints_labels;
			} else {
				m_timepoints_labels= new Mda();
			}
		}
		
		SSTimeSeriesView VV = new SSTimeSeriesView();
		SSTimeSeriesView VVmag = new SSTimeSeriesView();

		m_views.add(VV);
		m_magnified_views.add(VVmag);
		m_all_views.add(VV);
		m_all_views.add(VVmag);

		m_SP_left.getItems().add(VV);
		m_SP_right.getItems().add(VVmag);

		setup_new_views(VV, VVmag);

		VVmag.setShowPlots(false);
		if (X!=null) VVmag.setData(X);
		else if (TL!=null) VVmag.setTimepointsLabels(TL);
		VVmag.setXRange(0, 200);
		if (X!=null) VV.setData(X); //important to do this after so that we get the signal to the magnified view
		else if (TL!=null) VV.setTimepointsLabels(TL);

		if (timepoints_labels != null) {
			Color[] view_colors = ViewColors.getViewColors(1);
			List<SSMarker> markers = new ArrayList<>();
			for (int i = 0; i < timepoints_labels.size(1); i++) {
				SSMarker MM = new SSMarker();
				MM.x = timepoints_labels.value(0,i) - 1; //minus 1 for a zero-based indexing
				MM.color = view_colors[((int)timepoints_labels.value(1,i)-1) % view_colors.length]; //minus 1 for a zero-based indexing
				markers.add(MM);
			}
			VV.setMarkers(markers);
			VVmag.setMarkers(markers);
		}

		if (m_views.size() == 1) {
			m_views.get(0).setXRange(0, Math.min(m_data.N2() - 1, 10000 - 1));
			m_views.get(0).onCurrentXChanged(()->{
				CH.trigger("current-timepoint-changed", true);
			});
		} else {
			VV.setXRange(m_views.get(0).xRange());
		}

		update_info();
	}

	public void setSamplingFrequency(double freq) {
		m_sampling_frequency = freq;
		update_info();
	}

	//////////// PRIVATE //////////////////////////////////////////////////////
	void do_resize() {
		m_SP_main.setDividerPositions(0.7);

		if (m_views.size() > 1) {
			double offset = 0.7;
			double tmp = offset;
			for (int i = 0; i < m_views.size() - 1; i++) {
				if (i > 0) {
					tmp += (1 - offset) / (m_views.size() - 1);
				}
				m_SP_left.setDividerPosition(i, tmp);
				m_SP_right.setDividerPosition(i, tmp);
			}
		}
	}

	void connect_top_view_to_magnified_view(SSTimeSeriesView top_view, SSTimeSeriesView magnified_view) {
		top_view.onCurrentChannelChanged(() -> {
			magnified_view.setCurrentChannel(top_view.currentChannel());
		});
		magnified_view.onCurrentChannelChanged(() -> {
			top_view.setCurrentChannel(magnified_view.currentChannel());
			update_info();
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
			magnified_view.setCurrentXCoord(x0, false);
			update_info();
		});
		magnified_view.onCurrentXChanged(() -> {
			top_view.setCurrentXCoord(magnified_view.currentXCoord(), false);
		});
	}

	void synchronize_views(SSTimeSeriesView V1, SSTimeSeriesView V2) {
		V1.onCurrentXChanged(() -> {
			V2.setCurrentXCoord(V1.currentXCoord(), true); //important to trigger so this propogates to the label views or vice versa
		});
		V2.onCurrentXChanged(() -> {
			V1.setCurrentXCoord(V2.currentXCoord(), true); //important to trigger so this propogates to the label views or vice versa
		});
		V1.onXRangeChanged(() -> {
			V2.setXRange(V1.xRange());
		});
		V2.onXRangeChanged(() -> {
			V1.setXRange(V2.xRange());
		});
		V1.onSelectionRangeChanged(() -> {
			V2.setSelectionRange(V1.selectionRange());
		});
		V2.onSelectionRangeChanged(() -> {
			V1.setSelectionRange(V2.selectionRange());
		});
		/*V1.onCurrentChannelChanged(()->{
		 V2.setCurrentChannel(V1.currentChannel());
		 });
		 V2.onCurrentChannelChanged(()->{
		 V1.setCurrentChannel(V2.currentChannel());
		 });*/
	}

	void setup_new_views(SSTimeSeriesView VV, SSTimeSeriesView VVmag) {
		//////////////////////////////////////////
		do_resize();

		if (m_views.size() > 1) {
			synchronize_views(VV, m_views.get(0));
			synchronize_views(VVmag, m_magnified_views.get(0));
		}

		VV.onCurrentChannelChanged(() -> {
			if (VV.currentChannel() >= 0) {
				m_views.forEach(view -> {
					if (view != VV) {
						view.setCurrentChannel(-1);
					}
				});
			}
		});
		VVmag.onCurrentChannelChanged(() -> {
			if (VV.currentChannel() >= 0) {
				m_magnified_views.forEach(view -> {
					if (view != VVmag) {
						view.setCurrentChannel(-1);
					}
				});
			}
		});

		VV.onSelectionRangeChanged(() -> {
			Vec2 selrange = VV.selectionRange();
			if (selrange.x >= 0) {
				VVmag.setXRange(selrange);
			}
			update_info();
		});

		VV.onClicked(() -> {
			m_current_view = VV;
		});
		VVmag.onClicked(() -> {
			m_current_view = VVmag;
			this.update_info();
		});
//		VV.onHoveredXChanged(()->{
//			m_current_view=VV;
//		});
//		VVmag.onHoveredXChanged(()->{
//			m_current_view=VVmag;
//		});
		connect_top_view_to_magnified_view(VV, VVmag);

		VVmag.setCanZoomAllTheWayOut(false);

		if (m_views.size() == 1) {
			VV.setChannelColors(ViewColors.getViewColors(2));
			VVmag.setChannelColors(ViewColors.getViewColors(2));
		} else {
			VV.setChannelColors(ViewColors.getViewColors(1));
			VVmag.setChannelColors(ViewColors.getViewColors(1));
		}

		VV.onCurrentXChanged(() -> update_info());
		VVmag.onCurrentXChanged(() -> update_info());
		VV.onCurrentChannelChanged(() -> update_info());
		VVmag.onCurrentChannelChanged(() -> update_info());
	}

	double to_ms(double val) {
		return val / m_sampling_frequency * 1000; //ms
	}

	String form(double val) {
		String ret = String.format("%f", val);
		while ((ret.length() > 0) && ((ret.charAt(ret.length() - 1) == '0') || (ret.charAt(ret.length() - 1) == '.'))) {
			ret = ret.substring(0, ret.length() - 1);
		}
		return ret;
	}

	void update_info() {
		String txt = "";
		int ind = get_current_view_index();
		if (ind >= 0) {
			SSTimeSeriesView VV = m_views.get(ind);
			SSTimeSeriesView VVmag = m_magnified_views.get(ind);
			int ch = VV.currentChannel();
			if (ch >= 0) {
				txt += String.format("Channel = %d; ", ch);
			}
			Vec2 selrange = VV.selectionRange();
			if (selrange.x >= 0) {
				txt += String.format("Time = (%s, %s) ms; Duration = %s ms; ", form(to_ms(selrange.x)), form(to_ms(selrange.y)), form(to_ms(selrange.y - selrange.x)));
			} else {
				int x0 = VV.currentXCoord();
				if (x0 >= 0) {
					if (m_timepoint_mapping!=null) {
						x0=(int)m_timepoint_mapping.value(0,x0);
					}
					if (x0>0) {
						txt += String.format("Time = %s ms; ", form(to_ms(x0)));
					}
				}
				//double val0 = data_array.value(ch, x0);
				//{
				//	txt += String.format("Value = %f; ", val0);
				//}
			}
		} else {
			txt += "; ";
		}
		m_info_label.setText(txt);
	}

	int get_current_view_index() {
		for (int i = 0; i < m_views.size(); i++) {
			if (m_views.get(i).currentChannel() >= 0) {
				return i;
			}
		}
		return -1;
	}
	
	void synchronize_with(SSTimeSeriesWidget W) {
		do_sync(W,this);
		do_sync(this,W);
	}
	void do_sync(SSTimeSeriesWidget W1,SSTimeSeriesWidget W2) {
		W1.onCurrentTimepointChanged(()->{
			W2.setCurrentTimepoint(W1.currentTimepoint());
		});
	}

	void on_show_clips() {
		if (m_timepoints_labels==null) return;
		Mda TL=m_timepoints_labels;
		
		int M = m_data.size(0); //number of channels
		int ML=0; //number of label types
		for (int ii=0; ii<TL.size(1); ii++) {
			if ((int)(TL.value(1,ii)-1+1)>ML) { //minus 1 for zero-based indexing
				ML=(int)(TL.value(1,ii)-1+1);
			}
		}
		int N = m_data.size(1); //number of timepoints
		int num_clips = TL.size(1); //number of clips (i.e. number of labels)
		Mda label_data=new Mda(); label_data.allocate(ML,N);
		for (int ii=0; ii<TL.size(1); ii++) {
			label_data.setValue(1, (int)TL.value(1,ii)-1,(int)TL.value(0,ii)-1); //minus 1 for zero-based indexing
		}

		int interval_width=40;
		int pad=15;
		Mda data2 = new Mda();
		data2.allocate(M, num_clips * (interval_width+pad));
		Mda labels2 = new Mda();
		labels2.allocate(ML, num_clips * (interval_width+pad));
		Mda timepoint_mapping=new Mda(); timepoint_mapping.allocate(1,num_clips*(interval_width+pad));
		int n2 = 0;
		int offset1 = -(int) Math.ceil(interval_width / 2);
		int offset2 = offset1 + interval_width;
		for (int n = 0; n < N; n++) {
			for (int m = 0; m < ML; m++) {
				if (label_data.value(m, n) != 0) {
					for (int dn = offset1; dn < offset2; dn++) {
						if ((0 <= n + dn) && (n + dn < N)) {
							for (int mm=0; mm<M; mm++) {
								data2.setValue(m_data.value(mm, n + dn), mm, n2);
							}
							for (int mml=0; mml<ML; mml++) {
								labels2.setValue(label_data.value(mml, n + dn), mml, n2);
							}
							timepoint_mapping.setValue(n+dn,0, n2);
							n2++;
						}
					}
					for (int ii=0; ii<pad; ii++) {
						for (int mm=0; mm<M; mm++) {
							data2.setValue(0, mm, n2);
						}
						for (int mml=0; mml<ML; mml++) {
							labels2.setValue(0, mml, n2);
						}
						timepoint_mapping.setValue(-1,0, n2);
						n2++;
					}
				}
			}
		}
		
		int num_labels2=0;
		for (int ii=0; ii<N; ii++) {
			for (int jj=0; jj<ML; jj++)
				if (labels2.value(jj,ii)!=0) num_labels2++;
		}
		Mda TL2=new Mda(); TL2.allocate(2,num_labels2);
		int ind0=0;
		for (int ii=0; ii<N; ii++) {
			for (int jj=0; jj<ML; jj++) {
				if (labels2.value(jj,ii)!=0) {
					TL2.setValue(ii+1,0,ind0); //plus one for one-based indexing
					TL2.setValue(jj+1,1,ind0); //plus one for one-based indexing
					ind0++;
				}
			}
		}
		
		SSTimeSeriesWidget WW = (new SSViewController()).createTimeSeriesWidget();
		WW.setSamplingFrequency(m_sampling_frequency);
		WW.setTimepointMapping(timepoint_mapping);
		WW.addDataArray(data2, TL2);
		WW.addTimepointsLabels(TL2);
		this.synchronizeWith(WW);
	}
}
