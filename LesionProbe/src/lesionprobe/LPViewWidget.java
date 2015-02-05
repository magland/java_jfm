package lesionprobe;

import static java.lang.Integer.min;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import jviewmda.ExpandingCanvas;
import jviewmda.Mda;
import jviewmda.RemoteArray;
import jviewmda.ViewmdaWidget;
import jviewmda.WFSMda;
import org.magland.jcommon.JUtils;
import org.magland.wfs.WFSClient;

/**
 *
 * @author magland
 */
public class LPViewWidget extends GridPane {

	List<ViewmdaWidget> m_views = new ArrayList<>();
	boolean m_show_arrows = true;
	WFSClient m_client = null;
	String m_id;
	int[][] m_arrow_coordinates = new int[0][0];
	int m_current_slice = -1;

	LPViewWidget() {

		for (int i = 0; i < 4; i++) {
			m_views.add(new ViewmdaWidget());
		}

		add(m_views.get(0), 1, 1);
		add(m_views.get(1), 2, 1);
		add(m_views.get(2), 1, 2);
		add(m_views.get(3), 2, 2);

		//sync the slices
		for (ViewmdaWidget view : m_views) {
			ViewmdaWidget view0 = view;
			view0.setTopControlsVisible(false);
			view0.setBottomControlsVisible(false);
			view0.setCursorVisible(false);
			view0.onCurrentSliceChanged(() -> {
				Platform.runLater(() -> {
					int[] ind0 = view0.currentIndex();
					for (ViewmdaWidget view2 : m_views) {
						view2.setCurrentIndex(ind0);
					}
				});
			});
			view0.onSelectedRectChanged(() -> {
				Platform.runLater(() -> {
					int[] rect0 = view0.selectedRect();
					for (ViewmdaWidget view2 : m_views) {
						view2.setSelectedRect(rect0);
					}
				});
			});
			view0.customCanvas("test");
			view0.onImageRefreshed(() -> draw_overlay(view0));
		}

	}

	public void setClient(WFSClient client) {
		m_client = client;
	}

	public void setDataId(String id) {
		m_id = id;
		for (ViewmdaWidget view:m_views) {
			int[] tmp={-1,-1,0,0};
			view.setSelectedRect(tmp);
			view.setZoomRect(tmp);
		}
	}

	public void setArrowCoordinates(int[][] coords) {
		m_arrow_coordinates = coords.clone();
	}

	public int[][] getArrowCoordinates() {
		return m_arrow_coordinates;
	}

	public void refreshViews(Runnable callback) {
		refresh_views(callback);
	}

	public void preloadCase(String id, int[][] coords, Runnable callback) {
		preload_case(id, coords, callback);
	}

	public void setShowArrows(boolean val) {
		if (m_show_arrows == val) {
			return;
		}
		m_show_arrows = val;
		for (ViewmdaWidget view : m_views) {
			draw_overlay(view);
		}
	}

	public int getCurrentSlice() {
		return m_views.get(0).currentIndex()[2];
	}

	public void setCurrentSlice(int slice) {
		if (slice < 0) {
			slice = 0;
		}
		final int slice0 = slice;
		m_views.get(0).array().initialize(tmp1 -> {
			//if (slice0 >= m_views.get(0).array().N1()) {
			//	slice0 = m_views.get(0).array().N1() - 1;
			//}
			m_current_slice = slice0;
			int[] ind = m_views.get(0).currentIndex();
			ind[2] = slice0;
			m_views.get(0).setCurrentIndex(ind);
		});
	}

	public void zoomIn() {
		int[] RR = m_views.get(0).selectedRect();
		if (RR[0] < 0) {
			JUtils.showInformation("Zoom In", "To zoom in, you must select a rectangle within the image.");
			return;
		}
		for (ViewmdaWidget view : m_views) {
			view.setZoomRect(RR);
		}
	}

	public void zoomOut() {
		for (ViewmdaWidget view : m_views) {
			view.setZoomRect(new int[]{-1, -1, 0, 0});
		}
	}

	/////////// PRIVATE /////////////////////////////////
	private List<String> get_image_paths(String id) {
		List<String> image_paths = new ArrayList<>();
		image_paths.add("Images/" + id + "_FLAIR.nii");
		image_paths.add("Images/" + id + "_PD.nii");
		image_paths.add("Images/" + id + "_T1.nii");
		image_paths.add("Images/" + id + "_T2.nii");
		return image_paths;
	}

	private void preload_case(String id, int[][] coords, Runnable callback) {
		if (m_client == null) {
			callback.run();
			return;
		}

		if (coords.length == 0) {
			callback.run();
			return;
		}

		List<String> image_paths = get_image_paths(id);

		final int num = image_paths.size();
		m_num_loaded = 0;
		for (int i = 0; i < num; i++) {
			final ViewmdaWidget view0 = m_views.get(i);
			final int ind = i;
			RemoteArray X = load_array(image_paths.get(ind));
			int[] inds = new int[Mda.MAX_MDA_DIMS];
			inds[0] = coords[0][2];
			X.getDataXY(inds, (tmp1) -> {
				m_num_loaded++;
				if (m_num_loaded >= num) {
					callback.run();
				}
			});
		}
	}

	int m_num_loaded = 0;

	private void refresh_views(Runnable callback) {

		if (m_client == null) {
			callback.run();
			return;
		}

		List<String> image_paths = get_image_paths(m_id);

		final int num = min(m_views.size(), image_paths.size());
		m_num_loaded = 0;
		for (int i = 0; i < num; i++) {
			final ViewmdaWidget view0 = m_views.get(i);
			final int ind = i;
			view0.clearView();
			RemoteArray X = load_array(image_paths.get(ind));
			view0.setRemoteArray(X, () -> {
				m_num_loaded++;
				if (m_num_loaded >= num) {
					callback.run();
				}
			});
		}
	}

	private void draw_overlay(ViewmdaWidget view) {
		int current_slice = getCurrentSlice();

		ExpandingCanvas CC = view.customCanvas("test");
		GraphicsContext gc = CC.getGraphicsContext2D();
		gc.clearRect(0, 0, CC.getWidth(), CC.getHeight());
		if (m_show_arrows) {
			List<Color> colors = new ArrayList<>();
			colors.add(Color.YELLOW);
			colors.add(Color.RED);
			double[] mean0 = new double[2];
			mean0[0] = 0;
			mean0[1] = 0;
			for (int i = 0; i < m_arrow_coordinates.length; i++) {
				mean0[0] += m_arrow_coordinates[i][0];
				mean0[1] += m_arrow_coordinates[i][1];
			}
			if (m_arrow_coordinates.length > 0) {
				mean0[0] /= m_arrow_coordinates.length;
				mean0[1] /= m_arrow_coordinates.length;
			}
			for (int i = 0; i < m_arrow_coordinates.length; i++) {
				String dir = "left";
				if (m_arrow_coordinates[i][0] > mean0[0]) {
					dir = "right";
				}
				if (m_arrow_coordinates[i][2] == current_slice) {
					draw_arrow(view, m_arrow_coordinates[i][0], m_arrow_coordinates[i][1], dir, colors.get(i % colors.size()));
				}
			}
		}
	}

	private void draw_arrow(ViewmdaWidget view, int x, int y, String direction, Color color) {
		ExpandingCanvas CC = view.customCanvas("test");
		GraphicsContext gc = CC.getGraphicsContext2D();
		gc.setStroke(color);
		gc.setLineWidth(3);
		int[] pix1 = view.indexToPixel(x, y);
		int[] pix2 = new int[2];
		int[] pix3 = new int[2];
		int[] pix4 = new int[2];
		int sgn = 1;
		if (direction == "right") {
			sgn = -1;
		}

		pix2[0] = pix1[0] - 20 * sgn;
		pix2[1] = pix1[1];
		pix3[0] = pix1[0] - 5 * sgn;
		pix3[1] = pix1[1] + 6;
		pix4[0] = pix1[0] - 5 * sgn;
		pix4[1] = pix1[1] - 6;

		gc.strokeLine(pix1[0], pix1[1], pix2[0], pix2[1]);
		gc.strokeLine(pix1[0], pix1[1], pix3[0], pix3[1]);
		gc.strokeLine(pix1[0], pix1[1], pix4[0], pix4[1]);
	}

	private RemoteArray load_array(String path0) {
		WFSMda X = new WFSMda();
		X.setClient(m_client);
		X.setPath(path0);
//		String suf = FilenameUtils.getExtension(path0);
//		if (suf.equals("mda")) {
//			if (!X.read(path0)) {
//				System.err.println("Problem reading mda file.");
//			}
//		} else if ((suf.equals("nii")) || (suf.equals("gz"))) {
//			JNifti Y = new JNifti();
//			try {
//				Y.read(path0);
//				X = Y.array();
//			} catch (IOException ee) {
//				ee.printStackTrace();
//				System.err.println("Unable to read nifti file.");
//			}
//		} else {
//			System.err.println("Unrecognized file type: " + suf);
//		}
		return X;
	}
}
