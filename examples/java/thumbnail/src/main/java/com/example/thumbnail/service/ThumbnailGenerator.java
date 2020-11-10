package com.example.thumbnail.service;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public final class ThumbnailGenerator {

    public enum ThumbnailOutputFileType {
        PNG, JPG, GIF, BMP
    }

    public static String WATERMARKFILENAME = "watermarkFileName";

    private ThumbnailGenerator(){}

    public static void main_test(String[] args) throws FileNotFoundException, Exception {
        ArrayList<InputStream> inputStreams = new ArrayList<InputStream>();
        ArrayList<OutputStream> outputStreams = new ArrayList<OutputStream>();

        String inputFileName = "/home/ubuntu/images/sample.jpg";
        String outputFileName = "/home/ubuntu/images/sample_thumbnail.png";
        String watermarkFileName = "/home/ubuntu/images/watermark.png";
        String outputFileNameWithWatermark = "/home/ubuntu/images/sample_thumbnail_watermark.png";

        InputStream is = new BufferedInputStream(new FileInputStream(inputFileName));
        inputStreams.add(is);

        //OutputStream os = new FileOutputStream(outputFileName);
        //outputStreams.add(os);

        Map<String, String> parameters = new HashMap<String, String>();
        //parameters.put(WATERMARKFILENAME, watermarkFileName);

        //OutputStream os = new FileOutputStream(outputFileNameWithWatermark);

        OutputStream os = new FileOutputStream(outputFileName);
        outputStreams.add(os);
        invoke(inputStreams, outputStreams, parameters);
    }

    public static void invoke(ArrayList<InputStream> inputStreams,
                       ArrayList<OutputStream> outputStreams,
                       Map<String, String> parameters)
            throws Exception {

        System.out.println("Thumbnail UDF Invoked");

        // one stream for now
        InputStream thumbnailInputStream = inputStreams.get(0);
        OutputStream thumbnailOutputStream = outputStreams.get(0);

        String watermarkFileName = null;
        if (parameters.isEmpty() == false && parameters.containsKey(WATERMARKFILENAME))
            watermarkFileName = parameters.get(WATERMARKFILENAME);

        /*
        can add different file extension later
        Field pathField = FileOutputStream.class.getDeclaredField("path");
            pathField.setAccessible(true);
            String path = (String) pathField.get(thumbnailOutputStream);
            String fileExtension = FilenameUtils.getExtension(path);
         */
        if (watermarkFileName != null) {
            Thumbnails.fromInputStreams(inputStreams)
                    .size(200, 200)
                    .watermark(Positions.BOTTOM_RIGHT, ImageIO.read(new File(watermarkFileName)), 0.5f)
                    .toOutputStream(thumbnailOutputStream);
        } else {
            Thumbnails.fromInputStreams(inputStreams)
                    .size(200, 200)
                    .toOutputStream(thumbnailOutputStream);
        }
        System.out.println("Done");

    }

}
