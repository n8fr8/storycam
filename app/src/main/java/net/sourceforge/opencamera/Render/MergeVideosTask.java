package net.sourceforge.opencamera.Render;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.MP3TrackImpl;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by n8fr8 on 9/26/15.
 */
public class MergeVideosTask extends AsyncTask<String, Integer, File> {

    //The working path where the video files are located
    private File fileOutput;
    //The file names to merge
    private ArrayList<String> videosToMerge;
    //Dialog to show to the user
    private ProgressDialog progressDialog;

    private String audioPath;

    private Context context;

    public MergeVideosTask(Context context, File fileOutput, ArrayList<String> videosToMerge, String audioPath) {
        this.fileOutput = fileOutput;
        this.videosToMerge = videosToMerge;
        this.audioPath = audioPath;
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        progressDialog = ProgressDialog.show(context,
                "Merging videos", "Please wait...", true);
    };

    @Override
    protected File doInBackground(String... params) {

        int count = videosToMerge.size();

        try {

            Movie[] inMovies = new Movie[count];
            int i = 0;

            for (String videoPath : videosToMerge)
                inMovies[i++]  = MovieCreator.build(videoPath);

            List<Track> videoTracks = new LinkedList<Track>();
            List<Track> audioTracks = new LinkedList<Track>();

            for (Movie m : inMovies) {
                for (Track t : m.getTracks()) {
                    if (t.getHandler().equals("soun")) {
                        audioTracks.add(t);
                    }
                    if (t.getHandler().equals("vide")) {
                        videoTracks.add(t);
                    }
                    if (t.getHandler().equals("")) {

                    }
                }
            }

            Movie result = new Movie();

            if (audioPath == null || (!new File(audioPath).exists())) {
                if (audioTracks.size() > 0) {
                    result.addTrack(new AppendTrack(audioTracks
                            .toArray(new Track[audioTracks.size()])));
                }
            }
            else {

                Track audioTrack = null;

                if (audioPath.endsWith("aac"))
                {
                    audioTrack = new AACTrackImpl(new FileDataSourceImpl(audioPath));
                }
                else if (audioPath.endsWith("mp3"))
                {
                    try
                    {
                        String newAudioPath = processMP3(audioPath);
                        audioTrack = new MP3TrackImpl(new FileDataSourceImpl(newAudioPath));
                    }
                    catch (Exception e)
                    {
                        Log.e("MergeVideo","Couldn't process mp3",e);
                    }


                }
                else if (audioPath.endsWith("mp4"))
                {
                    Movie movieAudio =  MovieCreator.build(audioPath);
                    for (Track t : movieAudio.getTracks()) {
                        if (t.getHandler().equals("soun")) {
                            audioTrack = t;
                            break;
                        }
                    }

                }
                else if (audioPath.endsWith("m4a"))
                {
                    Movie movieAudio =  MovieCreator.build(audioPath);
                    for (Track t : movieAudio.getTracks()) {
                        if (t.getHandler().equals("soun")) {
                            audioTrack = t;
                            break;
                        }
                    }
                }

                if (audioTrack != null)
                    result.addTrack(audioTrack);
            }

            if (videoTracks.size() > 0) {
                result.addTrack(new AppendTrack(videoTracks
                        .toArray(new Track[videoTracks.size()])));
            }

            if (result.getTracks().size() > 0) {
                Container mp4file = new DefaultMp4Builder().build(result);

                long timestamp = new Date().getTime();
                String timestampS = "" + timestamp;

                fileOutput.getParentFile().mkdirs();

                FileChannel fc = new FileOutputStream(fileOutput).getChannel();
                mp4file.writeContainer(fc);
                fc.close();

                return fileOutput;
            }

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    private String processMP3 (String inPath) throws Exception
    {
        String outPath = null;

        // removing mp3 tags
        Mp3File mp3file = new Mp3File(inPath);
        if (mp3file.hasId3v1Tag()) {
            mp3file.removeId3v1Tag();
        }
        if (mp3file.hasId3v2Tag()) {
            mp3file.removeId3v2Tag();
        }
        if (mp3file.hasCustomTag()) {
            mp3file.removeCustomTag();
        }

        outPath = inPath + ".processed.mp3";

        mp3file.save(outPath);

        return outPath;
    }

    @Override
    protected void onPostExecute(File file) {
        super.onPostExecute(file);
        progressDialog.dismiss();

        if (file != null) {
            MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            context.sendBroadcast(new Intent("android.hardware.action.NEW_VIDEO", uri));
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(uri);
                            context.startActivity(intent);
                        }
                    }
            );
        }
        else
        {
            Toast.makeText(context,"There was a problem rendering your movie",Toast.LENGTH_LONG).show();
        }
    }

}