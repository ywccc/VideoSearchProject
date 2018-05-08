package msd_project;

import java.util.*;

// musicg library
import com.musicg.fingerprint.FingerprintSimilarity;
import com.musicg.wave.Wave;

public class audioMetric {

    public static Map<String, Double> compareAudio(String audioInput) {
        Map<String, Double> audioSimilarity = new HashMap<String, Double>();
        Wave queryAudio = new Wave("src/msd_project/query/" +audioInput);

        for (String dbVideo : queryProcessor.DATABASE_VIDEOS) {
            Wave dbAudio = new Wave("src/msd_project/database/" + dbVideo + "/" + dbVideo + ".wav");
            FingerprintSimilarity fingerprintSimilarity = queryAudio.getFingerprintSimilarity(dbAudio);
            double similarity = fingerprintSimilarity.getSimilarity();

            audioSimilarity.put(dbVideo, similarity);
        }

        return audioSimilarity;
    }

}
