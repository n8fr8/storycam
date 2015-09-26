package net.sourceforge.opencamera;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by n8fr8 on 9/26/15.
 */
public class AudioRecorder implements Runnable {

    MediaRecorder mMediaRecorder;
    File mAudioFilePath;
    boolean mIsAudioRecording = false;

    AlertDialog alertDialog;
    TextView messageView;

    int timeRecording = 0;

    Handler handler = new Handler();
    Timer timer = null;

    public void run ()
    {
        messageView.setText(timeRecording + " seconds");
        getAudioAmplitude();

    }

    public void showAudioRecording (Context context, File fileAudio)
    {
        alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle("Recording...");
        alertDialog.setMessage("0 seconds");
        alertDialog.setButton(Dialog.BUTTON_POSITIVE, "Stop", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                alertDialog.dismiss();

                stopAudioRecording();
                timer.cancel();
            }
        });
        alertDialog.show();   //

        messageView = (TextView) alertDialog.findViewById(android.R.id.message);


        timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                timeRecording++;
                handler.post(AudioRecorder.this);
            }

        },0,1000);

        startAudioRecording(fileAudio);
    }
    public void startAudioRecording (File fileAudio)
    {
        mMediaRecorder = new MediaRecorder();

        mAudioFilePath = fileAudio;

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
        mMediaRecorder.setAudioChannels(1);
        mMediaRecorder.setAudioEncodingBitRate(96000);
        mMediaRecorder.setAudioSamplingRate(44100);
        mMediaRecorder.setOutputFile(mAudioFilePath.getAbsolutePath());

        try {
            mIsAudioRecording = true;
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        }
        catch (Exception e)
        {
            Log.e("AudioRecorder", "couldn't start audio", e);
        }
    }

    public int getAudioAmplitude ()
    {
        return mMediaRecorder.getMaxAmplitude();
    }

    public void stopAudioRecording ()
    {
        if (mMediaRecorder != null && mAudioFilePath != null) {

            mMediaRecorder.stop();

            mMediaRecorder.reset();
            mMediaRecorder.release();

            mIsAudioRecording = false;
        }

    };
}
