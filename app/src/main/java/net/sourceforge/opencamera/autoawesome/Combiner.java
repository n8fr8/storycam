package net.sourceforge.opencamera.autoawesome;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.MP3TrackImpl;

import net.sourceforge.opencamera.MyDebug;
import net.sourceforge.opencamera.StorageUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by n8fr8 on 9/25/15.
 */
public class Combiner {


    private Context context = null;

    public Combiner(Context context)
    {
        this.context = context;
    }

    public void merge (String workingPath, ArrayList<String> videos, String audioPath)
    {
        new MergeVideos(workingPath, videos, audioPath).execute();
    }


    private class MergeVideos extends AsyncTask<String, Integer, File> {

        //The working path where the video files are located
        private String workingPath;
        //The file names to merge
        private ArrayList<String> videosToMerge;
        //Dialog to show to the user
        private ProgressDialog progressDialog;

        private String audioPath;

        private MergeVideos(String workingPath, ArrayList<String> videosToMerge, String audioPath) {
            this.workingPath = workingPath;
            this.videosToMerge = videosToMerge;
            this.audioPath = audioPath;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context,
                    "Merging videos", "Please wait...", true);
        };

        @Override
        protected File doInBackground(String... params) {
            File storagePath = new File(workingPath);
            File myMovie = null;

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

                if (audioPath == null) {
                    if (audioTracks.size() > 0) {
                        result.addTrack(new AppendTrack(audioTracks
                                .toArray(new Track[audioTracks.size()])));
                    }
                }
                else {

                    // MP3TrackImpl mp3Track = new MP3TrackImpl(new FileDataSourceImpl(audioFile));

                    Track audioTrack = null;

                    if (audioPath.endsWith("aac"))
                    {
                        audioTrack = new AACTrackImpl(new FileDataSourceImpl(audioPath));
                    }
                    else if (audioPath.endsWith("mp3"))
                    {
                        audioTrack = new MP3TrackImpl(new FileDataSourceImpl(audioPath));

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
                      result.addTrack(new AppendTrack(audioTrack));
                }

                if (videoTracks.size() > 0) {
                    result.addTrack(new AppendTrack(videoTracks
                            .toArray(new Track[videoTracks.size()])));
                }

                Container mp4file = new DefaultMp4Builder().build(result);

                long timestamp=new Date().getTime();
                String timestampS="" + timestamp;

                storagePath.mkdirs();

                myMovie = new File(storagePath, String.format("output-%s.mp4", timestampS));

                FileChannel fc = new FileOutputStream(myMovie).getChannel();
                mp4file.writeContainer(fc);
                fc.close();


            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return myMovie;
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            progressDialog.dismiss();

            MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            context.sendBroadcast(new Intent("android.hardware.action.NEW_VIDEO", uri));

                        }
                    }
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.fromFile(file));
            context.startActivity(intent);
        }

    }


}
