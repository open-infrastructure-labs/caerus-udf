package com.example.thumbnail.service;

import com.example.thumbnail.controller.RequestHandler;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

@Service
public final class ThumbnailGenerator {
    static Logger logger = LoggerFactory.getLogger(ThumbnailGenerator.class);

    public enum ThumbnailOutputFileType {
        PNG, JPG, GIF, BMP
    }

    public static String WATERMARKFILENAME = "watermarkFileName";
    public static String DEFAULT_THUMBNAIL_TYPE = "png";
    public static int DEFAULT_THUMBNAIL_WIDTH = 200;
    public static int DEFAULT_THUMBNAIL_HIGHT = 200;
    public static String DEFAULT_INPUT_PARAMETER = "inputParameters";

    private ThumbnailGenerator(){}

    public static void main_test() throws FileNotFoundException, Exception {
        ArrayList<InputStream> inputStreams = new ArrayList<InputStream>();
        ArrayList<OutputStream> outputStreams = new ArrayList<OutputStream>();

        String inputFileName = "/home/ubuntu/images/sample.jpg";
        String outputFileName = "/home/ubuntu/images/sample_thumbnail.png";
        String watermarkFileName = "/home/ubuntu/images/watermark.png";
        String outputFileNameWithWatermark = "/home/ubuntu/images/sample_thumbnail_watermark.png";

        InputStream is = new BufferedInputStream(new FileInputStream(inputFileName));
        inputStreams.add(is);

        Map<String, String> parameters = new HashMap<String, String>();
        //parameters.put(WATERMARKFILENAME, watermarkFileName);

        OutputStream os = new FileOutputStream(outputFileName);
        outputStreams.add(os);
        invoke(inputStreams, outputStreams, parameters);
    }

    public static void invoke(ArrayList<InputStream> inputStreams,
                       ArrayList<OutputStream> outputStreams,
                       Map<String, String> parameters)
            throws Exception {

        logger.info("Thumbnail UDF Invoked");

        // one stream for now
        OutputStream thumbnailOutputStream = outputStreams.get(0);

        String watermarkFileName = null;
        if (parameters.isEmpty() == false && parameters.containsKey(WATERMARKFILENAME))
            watermarkFileName = parameters.get(WATERMARKFILENAME);

        int width = DEFAULT_THUMBNAIL_WIDTH;
        int height = DEFAULT_THUMBNAIL_HIGHT;
        if (parameters != null && !parameters.isEmpty()) {
            //To convert a comma separated String into an ArrayList
            String ips = parameters.get(DEFAULT_INPUT_PARAMETER);
            if (ips != null && !ips.isEmpty()) {
                String[] myArray = ips.split(",");
                width = Integer.parseInt(myArray[0]);
                height = Integer.parseInt(myArray[1]);
            }
        }



        if (watermarkFileName != null) {
            Thumbnails.fromInputStreams(inputStreams)
                    .size(width, height)
                    .watermark(Positions.BOTTOM_RIGHT, ImageIO.read(new File(watermarkFileName)), 0.5f)
                    .outputFormat(DEFAULT_THUMBNAIL_TYPE)
                    .toOutputStream(thumbnailOutputStream);
        } else {
            Thumbnails.fromInputStreams(inputStreams)
                    .size(width, height)
                    .outputFormat(DEFAULT_THUMBNAIL_TYPE)
                    .toOutputStream(thumbnailOutputStream);
        }
        logger.info("Thumbnail has been generated.");

    }

}
