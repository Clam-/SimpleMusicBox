package org.nyanya.simplemusixbox

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.content.IntentFilter
import android.media.MediaPlayer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_fullscreen.*
import org.apache.commons.lang3.time.DurationFormatUtils
import java.util.*
import kotlin.concurrent.schedule

class FullscreenActivity : AppCompatActivity() {
    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreen_content.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
    }
    private var mVisible: Boolean = false
    private var mPlayerVis: Boolean = true
    protected var musicIterator: MusicIterator = MusicIterator()
    private var timer = Timer()
    var mediaPlayer: MediaPlayer = MediaPlayer()
    var paused = false
    private val mHideRunnable = Runnable { hide() }
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val mDelayHideTouchListener = View.OnTouchListener { _, _ ->
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    private val mediaReceiver: BroadcastReceiver = MediaUpdated()

    class MediaUpdated : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val mApplication = context.applicationContext as FullscreenActivity
            mApplication.musicIterator.updateState()
        }
    }

    private val mountFilter = IntentFilter(ACTION_MEDIA_MOUNTED)
    private var unmountFilter = IntentFilter(ACTION_MEDIA_EJECT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        button_power.setOnClickListener { togglePlayer() }
        button_nextsong.setOnClickListener{ next() }
        button_stop.setOnClickListener{ stop() }
        button_play.setOnClickListener { play() }

        unmountFilter = IntentFilter(ACTION_MEDIA_EJECT)
        unmountFilter.addAction(ACTION_MEDIA_REMOVED)

        mediaPlayer.setOnPreparedListener{
            mediaPlayer.start()
            paused = false
        }
        mediaPlayer.setOnErrorListener{ _: MediaPlayer, i: Int, i1: Int ->
            Log.e("SMUSIX", "What hapen? $i - $i1")
            mediaPlayer.reset()
            true
        }
        mediaPlayer.setOnCompletionListener{
            next()
        }
        timer.schedule(200, 200) {
            runOnUiThread {
                if (mediaPlayer.isPlaying) {
                    timecode.text = DurationFormatUtils.formatDuration(mediaPlayer.currentPosition.toLong(), "mm:ss") }
                else if (!paused){
                    timecode.text = "00:00"
                }
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
        musicIterator.updateState()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mediaReceiver, mountFilter)
        registerReceiver(mediaReceiver, unmountFilter)
        musicIterator.updateState()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mediaReceiver)
    }

    private fun togglePlayer() {
        if (mPlayerVis) { hidePlayer() }
        else { showPlayer() }
    }

    private fun hideUI() {
        button_prevsong.visibility = View.INVISIBLE
        button_stop.visibility = View.INVISIBLE
        button_nextsong.visibility = View.INVISIBLE
        button_prevrecord.visibility = View.INVISIBLE
        button_play.visibility = View.INVISIBLE
        button_nextrecord.visibility = View.INVISIBLE
        track_title.visibility = View.INVISIBLE
        record.visibility = View.INVISIBLE
        timecode.visibility = View.INVISIBLE
    }
    private fun showUI() {
        button_prevsong.visibility = View.VISIBLE
        button_stop.visibility = View.VISIBLE
        button_nextsong.visibility = View.VISIBLE
        button_prevrecord.visibility = View.VISIBLE
        button_play.visibility = View.VISIBLE
        button_nextrecord.visibility = View.VISIBLE
        track_title.visibility = View.VISIBLE
        record.visibility = View.VISIBLE
        timecode.visibility = View.VISIBLE
    }
    private fun hidePlayer() {
        // hide buttons
        hideUI()
        // change background
        frame.background = getDrawable(R.color.black)
        button_power.text = getString(R.string.on)
        mPlayerVis = false
    }
    private fun showPlayer() {
        frame.background = getDrawable(R.color.colorPrimary)
        button_power.text = getString(R.string.off)
        showUI()
        mPlayerVis = true
    }
    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun next() {
        mediaPlayer.reset()
        try {
            var f = musicIterator.nextTrack()
            if (f == null) {
                track_title.text = "End?"
            } else {
                mediaPlayer.setDataSource(f.path)
                track_title.text = f.name
                mediaPlayer.prepareAsync()
            }
        } catch (e: MusicIterator.NoFiles) {
            track_title.text = "No tracks found"
        } catch (e: MusicIterator.NoMedia) {
            track_title.text = "No media inserted"
        }
    }
    private fun stop() {
        mediaPlayer.reset()
        button_play.text = getText(R.string.play)
    }
    private fun play() {
        if (mediaPlayer.isPlaying) {
            button_play.text = getText(R.string.play)
            mediaPlayer.pause()
        } else if (paused){
            button_play.text = getText(R.string.pause)
            mediaPlayer.start()
        } else {
            mediaPlayer.reset()
            button_play.text = getText(R.string.pause)
            if (musicIterator.current == null) { next() }
            else {
                var f = musicIterator.current
                mediaPlayer.setDataSource(f?.path)
                track_title.text = f?.name
                mediaPlayer.prepareAsync()
            }
        }
    }

    private fun show() {
        // Show the system bar
        fullscreen_content.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300
    }
}
