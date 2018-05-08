package msd_project;//mcappucc
//CSCI 576 Team Project
//Starter code provided by course staff
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.*;

public class frameComparator {
    private static int width = 352;
    private static int height = 288;

    public static double[] compareFrames(String tag, int numFrames, int mode){
        //int frameCounter = 0;
        //File folder = new File (folder_name);
        //File[] listOfFiles = folder.listFiles();
        //Arrays.sort(listOfFiles); //ensure alphabetical order
        double[][] prevYMatrix = new double[width][height];
        double[][] currYMatrix = new double[width][height];
        double[] absoluteSumDifferences = new double[numFrames - 1];
        String folder;

        if (mode == 0) {
            folder = "src/msd_project/database/" + tag;
        } else {
            //mode is 1 for query processing
            folder = "src/msd_project/query";
        }

        for (int frameCounter = 0; frameCounter < numFrames; frameCounter++) {
            int frameNum = frameCounter + 1; //add 1 for zero based loop index
            String fileName = folder + "/" + tag + String.format("%03d",frameNum)+".rgb";
            File frame = new File(fileName);
            //String fileExt = fileName.substring(fileName.length() -4, fileName.length());

            if (frameCounter == 0) {
                //populate initial frame;
                prevYMatrix = getYMatrix(frame);
            } else {
                //populate current frame
                currYMatrix = getYMatrix(frame);

                //get absoulte sum differences and store in array (-1 for zero based index)
                absoluteSumDifferences[frameCounter-1] = getASD(prevYMatrix,currYMatrix);

                //store curr frame as prev for next comparison
                prevYMatrix = currYMatrix;
            }
        }
        return absoluteSumDifferences;
    }

    public static double[][] getYMatrix (File file) {
        double [][] yValues = new double [width][height];
        try {
            //File file = new File("/Users/cappuccio/Desktop/572PROJECT/database_videos/flowers/flowers001.rgb");
            InputStream is = new FileInputStream(file.getAbsoluteFile());

            long len = file.length();
            byte[] bytes = new byte[(int)len];

            int offset = 0;
            int numRead = 0;

            while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }

            // double [][] u_val = new double [width][height];
            // double [][] v_val = new double [width][height];

            int ind = 0;
            for(int y = 0; y < height; y++){
                for(int x = 0; x < width; x++){

                    byte a = 0;
                    byte r = bytes[ind];
                    byte g = bytes[ind+height*width];
                    byte b = bytes[ind+height*width*2];

                    //populate y matrix
                    yValues[x][y] = 0.299*(r&0xff) + 0.587*(g&0xff) + 0.114*(b&0xff);

                    ind++;
                }
            }
            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return yValues;
    }

    public static double getASD(double[][] prevFrame, double[][] currFrame) {
        double retValue = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                retValue += Math.abs(currFrame[i][j] - prevFrame[i][j]);
            }
        }
        return retValue;
    }


    //input arguments:
    //arg1: first frame of vid to process e.g. flowers001.rgb
    //arg2: integer number of frames to process
    //arg3: 0 for offline processing (writes to db video folder), 1 for qry processing (writes to query folder)
    public static double[] extractMotion (String inputFrame, int numFrames, int mode) {
        double[] asdValues = new double[numFrames - 1];
        try {
            //remove ###.rgb from input file name
            String tag = inputFrame.substring(0 , inputFrame.length() - 7);
            //File folder = new File(inputFolder);
            String fileName;

            if (mode == 1) {
                fileName = "src/msd_project/query/" + tag + "_ASD.txt";
            } else {
                fileName = "src/msd_project/database/" + tag + "/" + tag + "_ASD.txt";
            }
            FileWriter fw = new FileWriter(new File(fileName).getAbsoluteFile());
            PrintWriter  pw = new PrintWriter(fw);
            asdValues = compareFrames(tag,numFrames,mode);

            for (int i = 0; i < asdValues.length; i ++) {
                pw.println(asdValues[i]);
            }

            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return asdValues;
    }

    public static Map<String, Double[]> compareMotion (String inputFrame, int numFrames, double[] queryASD) throws IOException {
        //Called after tag_ASD.txt file created for database videos and query video
        double[] databaseASD = new double[queryProcessor.DATABASE_FRAME_COUNT -1];
        int window = numFrames - 1;
        String tag = inputFrame.substring(0 , inputFrame.length() - 7);
        Map<String, Double[]> motionSimilarityScores = new HashMap<String, Double[]>();

        String line = "";
        int readCounter = 0;

        for (String dbVideo: queryProcessor.DATABASE_VIDEOS) {
            //load dbvid_asd.txt
            File database = new File("src/msd_project/database/" + dbVideo + "/" + dbVideo + "_ASD.txt");
            BufferedReader dbBR = new BufferedReader(new FileReader(database));
            readCounter = 0;

            while ((line = dbBR.readLine()) != null) {
                databaseASD[readCounter++] = Double.parseDouble(line);
            }

            //iterate through sliding window and find min difference
            double minDifference = Double.MAX_VALUE;
            int minWindowStart = -1;
            for (int i = 0; i < databaseASD.length - window; i++) {
                //outer loop controls the sliding window
                double relDifference = 0;
                for (int j = 0; j < window; j++) {
                    //inner loop calculates asd difference at this window position
                    relDifference += Math.abs((databaseASD[i + j] - queryASD[j]) / databaseASD[i + j]);
                    //relDifference =+ Math.abs(queryASD[j] - databaseASD[i + j]);
                }
                //check minDifference
                if (relDifference < minDifference) {
                    minDifference = relDifference;
                    minWindowStart = i;
                }

            }
            minDifference /= window;
            double similarity = 1 - minDifference;
            if (similarity < 0) {
                //cap min value to 0 which whill contribute 0 weight to motion
                similarity = 0;
            }
            //store result in map <database video, similarity score>
            Double[] output = {similarity, (double)minWindowStart};
            motionSimilarityScores.put(dbVideo, output);
        }
        return motionSimilarityScores;
    }

    //main method: used when running offline process for each database folder
    //no input required for offline mode
    public static void main(String[] args) {
        int runMode = 0;
        for (String dbVideo : queryProcessor.DATABASE_VIDEOS) {
            //adding suffix to match syntax for live query processing
            extractMotion(dbVideo + "001.rgb", queryProcessor.DATABASE_FRAME_COUNT, runMode);
        }
    }

//end of class
}
