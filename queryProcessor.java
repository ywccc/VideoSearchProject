package msd_project;

import javax.sound.sampled.*;
import java.io.*;
import java.util.*;

public class queryProcessor {

    //public static String SRC_FOLDER = "/Users/cappuccio/Desktop/576project/576project";
    public static String[] DATABASE_VIDEOS = {"flowers","interview","movie","musicvideo","sports","starcraft","traffic"};
    public static int DATABASE_FRAME_COUNT = 600;


    //deprecated since this is duplicated for each query metric call
    // public static void extractQueryMotion(String[] args) {
    //   String startingFrame = args[0];
    //   int numFrames = Integer.parseInt(args[1]);
    //   int runMode = 1;
    //   msd_project.frameComparator.extractMotion(startingFrame, numFrames, runMode);
    // }

        //expected input:
    //arg0: first rgb frame of query video
    //arg1: wav file of query
    //arg2: number of frames in video
    //Expected file structure:
    //Database > tag > rgb + wav + metric files
    //query > all rgb + all wav + all metric files

    public static void main (String[] args) throws IOException{
        int runMode = 1; //live query processing mode
        String startingFrame = args[0];
        String audioFile = args[1];
        int numFrames = 0;
        int duration_1;

        String filePath = "src/msd_project/query/"+audioFile;
        try
        {
            AudioInputStream a1;
            AudioFormat format1;
            a1 = AudioSystem.getAudioInputStream(new File(filePath).getAbsoluteFile());
            Clip c1 = AudioSystem.getClip();
            c1.open(a1);
            format1 = a1.getFormat();
            duration_1 = (int) Math.round(a1.getFrameLength() + 0.0);
            numFrames = (int)(duration_1/format1.getFrameRate())*30;
            c1.close();
            a1.close();
        }
        catch (LineUnavailableException e1)
        {
            e1.printStackTrace();
        }
        catch (UnsupportedAudioFileException e1)
        {
            e1.printStackTrace();
        }

        double[] asdValues = frameComparator.extractMotion(startingFrame, numFrames, runMode);
        int[][] avgColors = colorComparator.extractColor(startingFrame, numFrames, runMode);
        //extractQueryAudio(args);

        //compare query metrics to database
        Map<String,Double[]> motion = frameComparator.compareMotion(startingFrame, numFrames, asdValues);
        Map<String,Double[]> color = colorComparator.compareColor(startingFrame,numFrames,avgColors);
        Map<String, Double> audio = audioMetric.compareAudio(audioFile);

        //Compute total weighted similarity score and sort results above threshold 1/3
        Map <Double, String> order = new TreeMap<Double, String>(Collections.reverseOrder());
        for (String dbVideo: DATABASE_VIDEOS) {
            double motionScore = motion.get(dbVideo)[0];
            double colorScore = color.get(dbVideo)[0];
            double audioScore = audio.get(dbVideo);
            double weightedScore;
            if(audioScore < 0.5)
                weightedScore = (motionScore + colorScore) / 2;
            else
                weightedScore = (motionScore + colorScore + audioScore) / 3;
            order.put(weightedScore, dbVideo);
        }
        ArrayList<String> dbList = new ArrayList<String>();
        ArrayList<Double[]> scoresOut = new ArrayList<Double[]>();
        ArrayList<Integer> motionIndex = new ArrayList<Integer>();
        ArrayList<Integer> colorIndex = new ArrayList<Integer>();

        for (Map.Entry<Double, String> entry : order.entrySet()){
            Double combinedWeight = entry.getKey();

            if (combinedWeight < 0.25) break;

            String dbVideo = entry.getValue();
            Double motionInd = motion.get(dbVideo)[1];
            Double colorInd = color.get(dbVideo)[1];
            dbList.add(dbVideo);
            Double[] metrics = {color.get(dbVideo)[0],motion.get(dbVideo)[0],audio.get(dbVideo)};
            scoresOut.add(metrics);
            motionIndex.add(motionInd.intValue());
            colorIndex.add(colorInd.intValue());
        }

        //load interface with top results
        player.main(args[0],args[1], dbList, scoresOut, motionIndex, colorIndex, numFrames);
    }


//end of class
}
