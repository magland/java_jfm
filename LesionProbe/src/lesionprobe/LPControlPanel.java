package lesionprobe;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.controlsfx.dialog.Dialogs;
import org.magland.jcommon.CallbackHandler;
import org.magland.jcommon.StringCallback;

/**
 *
 * @author magland
 */
public class LPControlPanel extends ScrollPane {
	
	int m_num_arrows = 1;
	List<String> m_arrow_color_names = new ArrayList<>();
	CallbackHandler CH = new CallbackHandler();
	CheckBox m_show_arrows_checkbox = new CheckBox();
	String m_rater = "";
	String m_current_id = "";
	List<RadioButton> m_yes_buttons = new ArrayList<>();
	List<RadioButton> m_no_buttons = new ArrayList<>();
	Boolean m_admin_mode = false;
	Label m_status_label=new Label();
	String m_status="";
	VBox m_vbox=new VBox();
	HBox dummy=new_vspace(20); //hack for fixing scroll problem
	
	LPControlPanel() {
		m_arrow_color_names.add("Yellow");
		m_arrow_color_names.add("Red");
		
		this.setPadding(new Insets(20, 20, 20, 20));
		
		{
			m_status_label=new Label(m_status);
			this.getChildren().add(m_status_label);
		}
		
		setContent(m_vbox);
		
		//This is quite a hack to force the content to refresh properly when the window is scrolled (must be a javafx bug)
		this.vvalueProperty().addListener(tmp0->{
			m_vbox.getChildren().remove(dummy);
			m_vbox.getChildren().add(dummy);
		});
		this.hvalueProperty().addListener(tmp0->{
			m_vbox.getChildren().remove(dummy);
			m_vbox.getChildren().add(dummy);
		});
	}
	
	public void setNumArrows(int num) {
		m_num_arrows = num;
	}
	
	public void onAction(StringCallback callback) {
		CH.bind("action", callback);
	}
	
	public Boolean showArrowsSelected() {
		return m_show_arrows_checkbox.isSelected();
	}
	
	public void setRater(String rater) {
		m_rater = rater;
	}
	
	public void setAdminMode(Boolean mode) {
		m_admin_mode = mode;
	}
	
	public void setCurrentId(String id) {
		m_current_id = id;
	}
	
	public String getCurrentId() {
		return m_current_id;
	}
	
	public void setAnswers(String[] answers) {
		for (int i = 0; (i < m_yes_buttons.size())&&(i<answers.length); i++) {
			if (answers[i].equals("yes"))
				m_yes_buttons.get(i).setSelected(true);
			else if (answers[i].equals("no"))
				m_no_buttons.get(i).setSelected(true);
		}
	}
	
	public String[] getAnswers() {
		String[] answers = new String[m_yes_buttons.size()];
		for (int i = 0; i < m_yes_buttons.size(); i++) {
			if (m_yes_buttons.get(i).isSelected()) {
				answers[i] = "yes";
			} else if (m_no_buttons.get(i).isSelected()) {
				answers[i] = "no";
			} else {
				answers[i] = "";
			}
		}
		return answers;
	}
	
	public void refresh() {
		
		m_vbox.getChildren().removeAll(m_vbox.getChildren());
		m_yes_buttons.removeAll(m_yes_buttons);
		m_no_buttons.removeAll(m_no_buttons);
		
		String str0="Rater: " + m_rater + " --- ID: " + m_current_id;
		if (m_admin_mode) str0+=" (ADMIN)";
		m_vbox.getChildren().add(new Label(str0));
		
		m_vbox.getChildren().add(new_vspace(30));
		
		for (int i = 0; ((i < m_num_arrows) && (i < m_arrow_color_names.size())); i++) {
			String col = m_arrow_color_names.get(i);
			Text L = new Text("Does the " + col + " arrow point to a lesion?");
			L.setWrappingWidth(this.minWidth(100));
			m_vbox.getChildren().add(L);
			
			{
				HBox spacer = new HBox();
				spacer.setMinHeight(8);
				m_vbox.getChildren().add(spacer);
			}
			
			HBox hbox = new HBox();
			ToggleGroup TG = new ToggleGroup();
			RadioButton RBY = new RadioButton();
			RBY.setText("Yes");
			RBY.setToggleGroup(TG);
			hbox.getChildren().add(RBY);
			RadioButton RBN = new RadioButton();
			{
				HBox spacer = new HBox();
				spacer.setMinWidth(12);
				hbox.getChildren().add(spacer);
			}
			RBN.setText("No");
			RBN.setToggleGroup(TG);
			hbox.getChildren().add(RBN);
			m_vbox.getChildren().add(hbox);
			
			m_yes_buttons.add(RBY);
			m_no_buttons.add(RBN);
			
			{
				HBox spacer = new HBox();
				spacer.setMinHeight(40);
				m_vbox.getChildren().add(spacer);
			}
		}
		
		{
			Button Bprev = new Button("Prev slice");
			Bprev.setOnAction(evt -> {
				CH.trigger("action", "prev-slice",true);
			});
			Button Bnext = new Button("Next slice");
			Bnext.setOnAction(evt -> {
				CH.trigger("action", "next-slice",true);
			});
			Button Breset = new Button("Reset slice");
			Breset.setOnAction(evt -> {
				CH.trigger("action", "reset-slice",true);
			});
			
			HBox hbox = new HBox();
			hbox.getChildren().addAll(Bprev, Bnext, Breset);
			m_vbox.getChildren().add(hbox);
		}
		
		m_vbox.getChildren().add(new_vspace(20));
		
		{
			Button Bzoom_in = new Button("Zoom in");
			Bzoom_in.setOnAction(evt -> {
				CH.trigger("action", "zoom-in",true);
			});
			Button Bzoom_out = new Button("Zoom out");
			Bzoom_out.setOnAction(evt -> {
				CH.trigger("action", "zoom-out",true);
			});
			
			HBox hbox = new HBox();
			hbox.getChildren().addAll(Bzoom_in, Bzoom_out);
			m_vbox.getChildren().add(hbox);
		}
		
		m_vbox.getChildren().add(new_vspace(20));
		
		{
			CheckBox cb = new CheckBox("Show arrows");
			cb.setOnAction(evt -> {
				CH.trigger("action", "show-arrows-changed",true);
			});
			cb.setSelected(true);
			m_show_arrows_checkbox = cb;
			m_vbox.getChildren().add(cb);
		}
		
		m_vbox.getChildren().add(new_vspace(20));
		
		{
			Button submit_button = new Button("SUBMIT");
			submit_button.setOnAction(evt -> {
				if (okay_to_submit()) {
					CH.trigger("action", "submit",true);
				}
			});
			if (m_admin_mode) {
				submit_button.setDisable(true);
			}
			m_vbox.getChildren().add(submit_button);
		}
		
		m_vbox.getChildren().add(new_vspace(20));
		
		if (m_admin_mode) {
			Button previous_button = new Button("PREVIOUS CASE");
			previous_button.setOnAction(evt -> {
				CH.trigger("action","previous-case",true);
			});
			Button next_button = new Button("NEXT CASE");
			next_button.setOnAction(evt -> {
				CH.trigger("action","next-case",true);
			});
			HBox hbox=new HBox();
			hbox.getChildren().addAll(previous_button,next_button);
			m_vbox.getChildren().add(hbox);
		}
		
		m_vbox.getChildren().add(new_vspace(20));
		
		if (m_admin_mode) {
			Button reset_answers_button = new Button("RESET ANSWERS");
			reset_answers_button.setOnAction(evt -> {
				CH.trigger("action","reset-answers",true);
			});
			Button view_answers_button = new Button("VIEW ANSWERS");
			view_answers_button.setOnAction(evt -> {
				CH.trigger("action","view-answers",true);
			});
			HBox hbox=new HBox();
			hbox.getChildren().addAll(reset_answers_button,view_answers_button);
			m_vbox.getChildren().add(hbox);
		}
		
		this.getChildren().add(new_vspace(20));
		
		{
			m_status_label=new Label(m_status);
			m_vbox.getChildren().add(m_status_label);
		}
		
	}
	public void setStatus(String status) {
		System.out.format("Status = %s\n", status);
		m_status=status;
		m_status_label.setText(status);
	}
	
	private HBox new_vspace(int height) {
		HBox spacer = new HBox();
		spacer.setMinHeight(height);
		return spacer;
	}
	
	private Boolean okay_to_submit() {
		for (int i = 0; i < m_yes_buttons.size(); i++) {
			if ((!m_yes_buttons.get(i).isSelected()) && (!m_no_buttons.get(i).isSelected())) {
				Dialogs.create()
						.owner(null)
						.title("Error submitting")
						.message("You must answer the questions before submitting.")
						.showError();
				return false;
			}
		}
		return true;
	}
	
}
