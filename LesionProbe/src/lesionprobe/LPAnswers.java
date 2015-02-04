package lesionprobe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.magland.jcommon.JUtils;
import org.magland.jcommon.StringCallback;
import org.magland.wfs.WFSClient;

/**
 *
 * @author magland
 */
public class LPAnswers {

	String m_rater = "";
	WFSClient m_client = null;
	Map<String, String> m_answers = new HashMap<>();

	void setClient(WFSClient client) {
		m_client = client;
	}

	void setRater(String rater) {
		m_rater = rater;
	}

	void load(Runnable callback) {
		loadCsv(txt -> {
			txt = txt.replace("\r", "\n");
			String[] lines = txt.split("\n");
			for (String line : lines) {
				String[] vals = line.split(",");
				if (vals.length == 2) {
					m_answers.put(vals[0], vals[1]);
				}
			}
			callback.run();
		});
	}

	void loadCsv(StringCallback callback) {
		if (m_client == null) {
			return;
		}

		final String path0 = "answers/" + m_rater + ".csv";

		m_answers.clear();
		m_client.readTextFile(path0, tmp1 -> {
			String txt = tmp1.get("text").toString();
			callback.run(txt);
		});
	}

	void save(Boolean do_backup, Runnable callback) {
		if (m_client == null) {
			return;
		}
		final String path0 = "answers/" + m_rater + ".csv";
		if (do_backup) {
			m_client.readTextFile(path0, tmp0 -> {
				m_client.writeTextFile(String.format("%s/tmp.%d.%s", JUtils.getFilePath(path0), rand_int(10001, 99999), JUtils.getFileName(path0)), tmp0.get("text").toString(), tmp00 -> {
					save(false, callback);
				});
			});
			return;
		}
		String txt = "";
		Set<String> keys = m_answers.keySet();
		List<String> keys2 = new ArrayList<>();
		for (String key : keys) {
			keys2.add(key);
		}
		keys2.sort(null);
		for (String key : keys2) {
			txt += key + "," + m_answers.get(key) + "\n";
		}
		m_client.writeTextFile(path0, txt, tmp1 -> {
			callback.run();
		});
	}

	public void resetAnswers() {
		m_answers.clear();
	}

	String getAnswer(String id) {
		if (m_answers.containsKey(id)) {
			return m_answers.get(id);
		} else {
			return "";
		}
	}

	void setAnswer(String id, String answer) {
		m_answers.put(id, answer);
	}

	private static int rand_int(int min, int max) {
		Random rand = new Random();
		int num = rand.nextInt((max - min) + 1) + min;
		return num;
	}
}
