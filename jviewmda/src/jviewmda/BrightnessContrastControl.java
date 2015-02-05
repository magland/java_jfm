package jviewmda;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Orientation;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import org.magland.jcommon.CallbackHandler;

/**
 *
 * @author magland
 */
public class BrightnessContrastControl extends HBox {

	Slider m_brightness_slider;
	Slider m_contrast_slider;
	CallbackHandler CH = new CallbackHandler();

	public BrightnessContrastControl() {
		m_brightness_slider = new Slider(-1, 1, 0);
		m_contrast_slider = new Slider(-1, 1, 0);

		List<Slider> sliders = new ArrayList();
		sliders.add(m_brightness_slider);
		sliders.add(m_contrast_slider);

		for (Slider slider : sliders) {
			slider.setOrientation(Orientation.VERTICAL);
			slider.setMinorTickCount(201);
			slider.setSnapToTicks(true);
			slider.setShowTickMarks(false);
			slider.setShowTickLabels(false);
			slider.valueProperty().addListener(ov -> on_slider_changed());
			slider.setMinWidth(20);
			/*
			 URL url=this.getClass().getResource("resources/style.css");
			 if (url!=null) {
			 slider.getStylesheets().add(url.toExternalForm());
			 }
			 else System.err.println("Unable to find stylesheet for slider");
			 */
			getChildren().addAll(slider);
		}
		setMinWidth(40);
	}

	public void onChanged(Runnable callback) {
		CH.bind("changed", callback);
	}

	public double brightness() {
		return m_brightness_slider.getValue();
	}

	public double contrast() {
		return m_contrast_slider.getValue();
	}

	private void on_slider_changed() {
		CH.scheduleTrigger("changed", 100);
	}
}
