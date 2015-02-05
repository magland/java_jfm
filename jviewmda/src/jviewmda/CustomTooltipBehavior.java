package jviewmda;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/**
 *
 * @author magland Taken from this:
 * http://www.coderanch.com/t/622070/JavaFX/java/control-Tooltip-visible-time-duration
 */
public class CustomTooltipBehavior {

	public static void setup(int openDelayInMillis, int visibleDurationInMillis, int closeDelayInMillis) {
		try {

			Class TTBehaviourClass = null;
			Class<?>[] declaredClasses = Tooltip.class.getDeclaredClasses();
			for (Class c : declaredClasses) {
				if (c.getCanonicalName().equals("javafx.scene.control.Tooltip.TooltipBehavior")) {
					TTBehaviourClass = c;
					break;
				}
			}
			if (TTBehaviourClass == null) {
				// abort  
				return;
			}
			Constructor constructor = TTBehaviourClass.getDeclaredConstructor(
					Duration.class, Duration.class, Duration.class, boolean.class);
			if (constructor == null) {
				// abort  
				return;
			}
			constructor.setAccessible(true);
			Object newTTBehaviour = constructor.newInstance(
					new Duration(openDelayInMillis), new Duration(visibleDurationInMillis),
					new Duration(closeDelayInMillis), false);
			if (newTTBehaviour == null) {
				// abort  
				return;
			}
			Field ttbehaviourField = Tooltip.class.getDeclaredField("BEHAVIOR");
			if (ttbehaviourField == null) {
				// abort  
				return;
			}
			ttbehaviourField.setAccessible(true);

			// Cache the default behavior if needed.  
			Object defaultTTBehavior = ttbehaviourField.get(Tooltip.class);
			ttbehaviourField.set(Tooltip.class, newTTBehaviour);

		} catch (Exception e) {
			System.out.println("Aborted setup due to error:" + e.getMessage());
		}
	}
}
