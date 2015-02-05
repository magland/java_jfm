package lesionprobe;

import static java.lang.Integer.parseInt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ButtonBar;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;
import org.magland.jcommon.CallbackHandler;
import org.magland.jcommon.JUtils;
import org.magland.wfs.WFSClient;

/**
 *
 * @author magland
 */
public class LPMainWidget extends HBox {

	WFSClient m_client = null;
	LPViewWidget m_view_widget;
	LPControlPanel m_control_panel;
	List<Case> m_cases = new ArrayList<>();
	int m_case_index = 0;
	String m_rater = "";
	LPAnswers m_answers = new LPAnswers();
	Boolean m_admin_mode = false;
	CallbackHandler CH = new CallbackHandler();

	LPMainWidget() {
		m_control_panel = new LPControlPanel();
		m_control_panel.setMinWidth(360);
		m_control_panel.setMaxWidth(360);

		m_view_widget = new LPViewWidget();

		getChildren().addAll(m_control_panel, m_view_widget);

		m_control_panel.onAction(action -> {
			if (action.equals("prev-slice")) {
				on_previous_slice();
			} else if (action.equals("next-slice")) {
				on_next_slice();
			} else if (action.equals("reset-slice")) {
				on_reset_slice();
			} else if (action.equals("zoom-in")) {
				on_zoom_in();
			} else if (action.equals("zoom-out")) {
				on_zoom_out();
			} else if (action.equals("show-arrows-changed")) {
				on_show_arrows_changed();
			} else if (action.equals("submit")) {
				on_submit();
			} else if (action.equals("previous-case")) {
				on_previous_case();
			} else if (action.equals("next-case")) {
				on_next_case();
			} else if (action.equals("reset-answers")) {
				on_reset_answers();
			} else if (action.equals("view-answers")) {
				on_view_answers();
			}
		});

	}

	public void setClient(WFSClient client) {
		m_client = client;
		m_view_widget.setClient(client);
		m_answers.setClient(client);
	}

	public void setRater(String rater) {
		m_rater = rater;
		m_control_panel.setRater(rater);
		m_answers.setRater(rater);
	}

	public void setAdminMode(Boolean admin_mode) {
		m_admin_mode = admin_mode;
		m_control_panel.setAdminMode(admin_mode);
	}

	public void onFinished(Runnable callback) {
		CH.bind("finished", callback);
	}

	public void run() {

		if (m_client == null) {
			return;
		}
		m_control_panel.setStatus("Loading answers...");
		m_answers.load(() -> {
			m_control_panel.setStatus("Loading coordinates...");
			m_client.readTextFile("coordinates.csv", tmp1 -> {
				String txt = tmp1.get("text").toString();
				txt = txt.replace("\r", "\n");
				String[] lines = txt.split("\n");
				if (lines.length < 2) {
					return;
				}
				m_control_panel.setStatus("Loading name order...");
				m_client.readTextFile("name_order.csv", tmp2 -> {
					m_control_panel.setStatus(".");
					String txt2 = tmp2.get("text").toString();
					txt2 = txt2.replace("\r", "\n");
					String[] lines2 = txt2.split("\n");
					if (lines2.length < 1) {
						return;
					}
					String[] names = lines2[0].split(",");
					int col0 = -1;
					for (int cc = 0; cc < names.length; cc++) {
						if (names[cc].equals(m_rater)) {
							col0 = cc;
						}
					}
					if (col0 < 0) {
						System.err.println("Unable to find rater: " + m_rater);
						Platform.runLater(() -> {
							JUtils.showWarning("LesionProbe", "Unable to find rater");
						});
						return;
					}
					Map<String, Case> cases_by_id = new HashMap<>();
					for (int ii = 1; ii < lines.length; ii++) {
						String line = lines[ii];
						String[] vals = line.split(",");
						Boolean ok = true;
						if (vals.length < 7) {
							ok = false;
						}
						String id = "";
						int[][] coords = new int[2][3];
						try {
							id = vals[0];
							coords[0][0] = parseInt(vals[1]);
							coords[0][1] = parseInt(vals[2]);
							coords[0][2] = parseInt(vals[3]);
							coords[1][0] = parseInt(vals[4]);
							coords[1][1] = parseInt(vals[5]);
							coords[1][2] = parseInt(vals[6]);
						} catch (Throwable t) {
							ok = false;
						}
						if (ok) {
							Case cc = new Case();
							cc.coords = coords;
							cc.id = id;
							cases_by_id.put(id, cc);
						}
					}
					for (int ii = 1; ii < lines2.length; ii++) {
						String line2 = lines2[ii];
						String[] vals = line2.split(",");
						if (col0 < vals.length) {
							String id0 = vals[col0];
							if (cases_by_id.containsKey(id0)) {
								m_cases.add(cases_by_id.get(id0));
							}
						}
					}

					//preload cases
					m_control_panel.setStatus("Preloading images (please wait)...");
					CallbackHandler.scheduleCallback(() -> {
						preload_cases(() -> {
							m_control_panel.setStatus(".");
							m_case_index = -1;
							if (goto_next_case()) {
								show_case();
							} else {
								if (!m_admin_mode) {
									show_final_message();
								}
							}
						});
					}, 1000);

				});
			});
		});
	}

	private void preload_cases(Runnable callback) {
		do_preload_cases(0, callback);
	}

	private void do_preload_cases(int index, Runnable callback) {
		if (index >= m_cases.size()) {
			callback.run();
			return;
		}
		System.out.format("Preloading %s\n", m_cases.get(index).id);
		m_control_panel.setStatus(m_cases.get(index).id + " (preloading)");
		m_view_widget.preloadCase(m_cases.get(index).id, m_cases.get(index).coords, () -> {
			do_preload_cases(index + 1, callback);
		});
	}

	private Boolean goto_next_case() {
		if (m_case_index + 1 >= m_cases.size()) {
			return false;
		}
		m_case_index++;
		if (m_admin_mode) {
			if (m_case_index >= m_cases.size()) {
				m_case_index = m_cases.size() - 1;
			}
			return true;
		}
		while (m_case_index < m_cases.size()) {
			Case cc = m_cases.get(m_case_index);
			String id0 = cc.id + " arrow-0";
			if (m_answers.getAnswer(id0).isEmpty()) {
				return true;
			} else {
				m_case_index++;
			}
		}
		m_case_index = m_cases.size() - 1;
		return false;
	}

	private Boolean goto_previous_case() {
		if (m_case_index - 1 < 0) {
			return false;
		}
		m_case_index--;
		if (m_admin_mode) {
			return true;
		}
		while (m_case_index >= 0) {
			Case cc = m_cases.get(m_case_index);
			String id0 = cc.id + " arrow-0";
			if (m_answers.getAnswer(id0).isEmpty()) {
				return true;
			} else {
				m_case_index--;
			}
		}
		m_case_index = 0;
		return false;
	}

	private void show_final_message() {
		CH.trigger("finished", true);
	}

	private void show_case() {
		if (m_case_index < 0) {
			return;
		}
		if ((m_case_index >= m_cases.size()) || (m_case_index < 0)) {
			if (!m_admin_mode) {
				show_final_message();
			}
			return;
		}
		Case cc = m_cases.get(m_case_index);
		m_view_widget.setArrowCoordinates(cc.coords);
		m_view_widget.setDataId(cc.id);
		m_view_widget.setCurrentSlice(cc.coords[0][2]);
		m_view_widget.refreshViews(() -> {
			m_view_widget.setCurrentSlice(cc.coords[0][2]);
		});
		m_view_widget.setShowArrows(true);

		m_control_panel.setCurrentId(cc.id);

		int num_arrows = 2;
		if (cc.coords[1][0] < 0) {
			num_arrows = 1;
		}
		m_control_panel.setNumArrows(num_arrows);
		m_control_panel.refresh();
		String[] answers = new String[num_arrows];
		for (int i = 0; i < num_arrows; i++) {
			String id0 = cc.id + String.format(" arrow-%d", i);
			answers[i] = m_answers.getAnswer(id0);
		}
		m_control_panel.setAnswers(answers);

		CallbackHandler.scheduleCallback(() -> {
			on_reset_slice();
		}, 250);

	}

	///////////////////// PRIVATE /////////////////////////////
	private void on_previous_slice() {
		m_view_widget.setCurrentSlice(m_view_widget.getCurrentSlice() - 1);
	}

	private void on_next_slice() {
		m_view_widget.setCurrentSlice(m_view_widget.getCurrentSlice() + 1);
	}

	private void on_reset_slice() {
		int[][] coords = m_view_widget.getArrowCoordinates();
		if (coords.length == 0) {
			return;
		}
		m_view_widget.setCurrentSlice(coords[0][2]);
	}

	private void on_zoom_in() {
		m_view_widget.zoomIn();
	}

	private void on_zoom_out() {
		m_view_widget.zoomOut();
	}

	private void on_show_arrows_changed() {
		m_view_widget.setShowArrows(m_control_panel.showArrowsSelected());
	}

	private void on_submit() {
		String[] answers = m_control_panel.getAnswers();
		for (int i = 0; i < answers.length; i++) {
			m_answers.setAnswer(m_control_panel.getCurrentId() + String.format(" arrow-%d", i), answers[i]);
		}
		m_answers.save(false, () -> {
			if (goto_next_case()) {
				show_case();
			} else {
				show_final_message();
			}
		});
	}

	private void on_previous_case() {
		if (goto_previous_case()) {
			show_case();
		}
	}

	private void on_next_case() {
		if (goto_next_case()) {
			show_case();
		}
	}

	private void on_reset_answers() {
		Action response = Dialogs.create()
				.owner(null)
				.title("Reset Answers?")
				.masthead(null)
				.message("Are you sure you want to reset the answers for this rater? (This will erase all answers)")
				.showConfirm();

		if (response == Dialog.ACTION_YES) {
			m_answers.resetAnswers();
			m_answers.save(true, () -> {
				JUtils.showInformation("Answers reset", "Answers have been reset.");
				m_control_panel.refresh();
			});
		}
	}

	private void on_view_answers() {
		m_answers.loadCsv(txt -> {
			show_text("Answers for " + m_rater, txt);
		});
	}

	private void show_text(String title, String txt) {
		Dialog dlg = new Dialog(null, title);

		final VBox vbox = new VBox();

		TextArea TA = new TextArea();
		TA.setEditable(false);
		TA.setText(txt);
		vbox.getChildren().add(TA);

		Button ok_button = new Button("OK");
		ok_button.setOnAction(evt -> {
			dlg.hide();
		});

		ButtonBar bar = new ButtonBar();

		bar.getButtons().addAll(ok_button);
		vbox.getChildren().add(bar);

		dlg.setContent(vbox);

		dlg.setResizable(false);
		dlg.setIconifiable(false);
		dlg.show();
	}

	class Case {

		public int[][] coords = new int[2][3];
		public String id = "";
	}

}
