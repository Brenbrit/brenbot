package com.brenbrit.brenbot.utils;

import com.brenbrit.brenbot.utils.Downloader;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbedFixer {

    String[] FIXABLE_EXTENSIONS = {
        "mp4",
        "mkv",
        "mov"
    };

    String DOWNLOAD_LOC = "/tmp/brenbot/";
    double H264_SIZE_MULTIPLIER = 1.25d;

    FFmpeg ffmpeg;
    FFprobe ffprobe;
    FFmpegExecutor ffmpegExecutor;
    private Logger logger = LoggerFactory.getLogger(EmbedFixer.class);

    private class VideoFile {
        public String fileLoc;
        public FFmpegProbeResult probeResult;

        public VideoFile(String loc, FFmpegProbeResult result) {
            fileLoc = loc;
            probeResult = result;
        }
    }

    public EmbedFixer() {
        if (!Files.isDirectory(Paths.get(DOWNLOAD_LOC))){
            new File(DOWNLOAD_LOC).mkdirs();
        }
        try {
            ffmpeg = new FFmpeg();
            ffprobe = new FFprobe();
            ffmpegExecutor = new FFmpegExecutor(ffmpeg, ffprobe);
        } catch (IOException e) {
            logger.error("Failed to initialize FFmpeg.");
            logger.error(e.getStackTrace().toString());
        }
    }

    public void checkAndFixEmbeds(Message message) {

        for (Attachment attachment : message.getAttachments()) {
            VideoFile result = checkEmbed(attachment);
            if (result != null) {
                logger.info("h.265 attachment found! Fixing.");
                File fixed = fixEmbed(result);
                MessageChannel chan = message.getChannel();
<<<<<<< HEAD
                System.out.println("Sending fixed file.");
                chan.sendFile(fixed).reference(message).queue();
=======
                logger.info("Sending fixed file.");
                chan.sendFile(fixed).queue();
>>>>>>> e9b5ea083afe052e1b95a585dcd0bcda01bc943e

                try {
                    logger.info("Deleting " + fixed.getPath());
                    fixed.delete();
                    logger.info("Deleting " + result.fileLoc);
                    new File(result.fileLoc).delete();
                } catch (Exception e) {
                    logger.error(String.format("Failed to delete file %s.%n", result.fileLoc));
                }
            }
        }

    }

    private VideoFile checkEmbed(Attachment attachment) {
        String url = attachment.getUrl();
        String[] split = url.split("\\.", 0);

        String fileExtension = split[split.length - 1];

        boolean goodExtension = false;
        for (String extension : FIXABLE_EXTENSIONS) {
            if (extension.equalsIgnoreCase(fileExtension)) {
                goodExtension = true;
                break;
            }
        }
        if (!goodExtension) {
            split = url.split("/");
            logger.info(String.format("%s is not a repairable video file, so it was not downloaded.%n",
                    split[split.length - 1]));
            return null;
        }

        String fileLoc = DOWNLOAD_LOC + System.currentTimeMillis()
            + "." + split[split.length - 1];

        logger.info(String.format("Downloading %s -> %s...", url, fileLoc));

        // We have a good extension on our hands!
        if (!Downloader.downloadFromURL(url, fileLoc)) {
            logger.error("Failed to download " + url);
            return null;
        } else logger.info("done");

        try {
            FFmpegProbeResult res = ffprobe.probe(fileLoc);
            for (FFmpegStream stream : res.getStreams()) {
                if (stream.codec_name.equalsIgnoreCase("hevc")) {
                    return new VideoFile(fileLoc, res);
                }
            }
            logger.info("Deleting " + fileLoc);
            new File(fileLoc).delete();
        } catch (IOException e) {
            logger.error("Failed to probe or delete" + fileLoc);
            new File(fileLoc).delete();
            logger.error(e.getStackTrace().toString());
        }


        return null;
    }


    private File fixEmbed(VideoFile input) {
        String[] splitLoc = input.fileLoc.split("/");
        String newLoc = "";
        for (int i = 0; i < splitLoc.length - 1; i++) {
            newLoc += splitLoc[i] + "/";
        }
        newLoc += "h264_" + splitLoc[splitLoc.length - 1];
        String format = splitLoc[splitLoc.length - 1].split("\\.", 2)[1];

        long oldSize = new File(input.fileLoc).length();
        long targetSize = (long)((double)oldSize * H264_SIZE_MULTIPLIER);
        if (targetSize > 8000000L) targetSize = 7000000L;

        FFmpegBuilder builder = new FFmpegBuilder()
            .setInput(input.probeResult)
            .addOutput(newLoc)
                .setFormat(format)
                .disableSubtitle()
                .setTargetSize(targetSize)
                .setVideoCodec("libx264")
                .done();
        ffmpegExecutor.createTwoPassJob(builder).run();

        return new File(newLoc);
    }
}
