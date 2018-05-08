package msd_project;

import msd_project.queryProcessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;

public class colorComparator {
    private static int width = 352;
    private static int height = 288;


    public static void main(String[] args) throws IOException
    {
        int runMode = 0;
        for(String dbVideo : queryProcessor.DATABASE_VIDEOS)
        {
            extractColor(dbVideo + "001.rgb", queryProcessor.DATABASE_FRAME_COUNT, runMode);
        }
    }

    public static int[][] extractColor(String inputFrame, int numFrames, int mode) throws IOException
    {
        int[][] avgColors = new int[numFrames][3];
        // store the result
        //avgColors[i][0] for red ,avgColors[i][1] for green, avgColors[i][2] for blue

        String tag = inputFrame.substring(0, inputFrame.length()-7);

        String fileName;

        if(mode == 1)
        {
            fileName = "src/msd_project/query/" + tag + "_AVC.txt";
        }
        else
        {
            fileName = "src/msd_project/database/" + tag + "/" + tag + "_AVC.txt";
        }

        FileWriter fw = new FileWriter(fileName);
        PrintWriter pw = new PrintWriter(fw);
        avgColors = getAverageColor(tag,numFrames,mode);

        for(int i = 0; i < avgColors.length; ++i)
        {
            for(int j = 0; j < 3; ++j)
            {
                pw.println(avgColors[i][j]);
            }

        }

        pw.close();
        return avgColors;
    }

    public static int[][] getAverageColor(String tag, int numFrames, int mode)
    {
        int[][] avgColor = new int[numFrames][3];
        String folder;

        if(mode == 0)
        {
            folder = "src/msd_project/database/" + tag;
        }
        else
        {
            folder = "src/msd_project/query";
        }

        for(int frameCounter = 0; frameCounter < numFrames; frameCounter++)
        {
            int frameNum = frameCounter + 1;
            String fileName = folder + "/" + tag + String.format("%03d",frameNum) + ".rgb";



            try {
                File frame = new File(fileName);
                InputStream is = new FileInputStream(frame);
                long len = frame.length();

                byte[] bytes = new byte[(int)len];
                int offset = 0;
                int numRead = 0;
                while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                    offset += numRead;
                }

                int ind = 0;
                int rsum = 0;
                int gsum = 0;
                int bsum = 0;
                for(int y = 0; y < height; ++y)
                {
                    for(int x = 0; x < width; ++x)
                    {
                        int r = (int)bytes[ind];
                        int g = (int)bytes[ind + height*width];
                        int b = (int)bytes[ind + height*width*2];

                        r = (r < 0)?r+256:r;
                        g = (g < 0)?g+256:g;
                        b = (b < 0)?b+256:b;

                        rsum += r;
                        gsum += g;
                        bsum += b;

                        ++ind;
                    }
                }
                int total = width*height;
                avgColor[frameCounter][0] = rsum/total;
                avgColor[frameCounter][1] = gsum/total;
                avgColor[frameCounter][2] = bsum/total;



            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        return avgColor;
    }

    public static Map<String,Double[]> compareColor(String inputFrame, int numFrames, int[][] queryAVC) throws IOException
    {
        int[][] databaseAVC = new int[queryProcessor.DATABASE_FRAME_COUNT][3];
        String tag = inputFrame.substring(0, inputFrame.length()-7);
        Map<String, Double[]> colorSimilarityScores = new HashMap<String,Double[]>();

        String line = "";
        int colornum = 0;
        int framenum = 0;

        for(String dbVideo : queryProcessor.DATABASE_VIDEOS)
        {
            File database = new File("src/msd_project/database/" + dbVideo + "/" + dbVideo + "_AVC.txt");
            BufferedReader dbBR = new BufferedReader(new FileReader(database));


            framenum = 0;
            colornum = 0;
            while((line = dbBR.readLine()) != null) {
                databaseAVC[framenum][colornum++] = Integer.parseInt(line);
                if(colornum == 3)
                {
                    colornum = 0;
                    ++framenum;
                }
            }

            double minDifference= Double.MAX_VALUE;
            int window = numFrames;
            int minWindowStart = -1;
            for(int i = 0; i < databaseAVC.length-window+1; ++i)
            {
                double relDifference = 0;
                for(int j = 0; j < window; ++j)
                {
                    double difr = Math.abs((double)(databaseAVC[i+j][0] - queryAVC[j][0]) / (double)(databaseAVC[i+j][0]));
                    double difg = Math.abs((double)(databaseAVC[i+j][1] - queryAVC[j][1]) / (double)(databaseAVC[i+j][1]));
                    double difb = Math.abs((double)(databaseAVC[i+j][2] - queryAVC[j][2]) / (double)(databaseAVC[i+j][2]));
                    double diff = (difr + difg + difb) / 3.0;
                    relDifference += diff;
                }
                if(relDifference < minDifference)
                {
                    minDifference = relDifference;
                    minWindowStart = i;
                }
            }

            minDifference /= window;
            double similarity = 1-minDifference;
            if(similarity < 0)
            {
                similarity = 0;
            }
            Double[] result = {similarity, (double)minWindowStart};
            colorSimilarityScores.put(dbVideo, result);

        }


        return colorSimilarityScores;
    }

}
