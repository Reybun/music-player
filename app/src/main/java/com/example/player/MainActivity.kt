package com.example.player

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.media.session.MediaSessionCompat
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.databinding.DataBindingUtil
import androidx.palette.graphics.Palette
import com.example.player.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import io.github.jeffshee.visualizer.painters.misc.SimpleIcon
import io.github.jeffshee.visualizer.painters.modifier.Shake
import io.github.jeffshee.visualizer.utils.Preset
import io.github.jeffshee.visualizer.utils.VisualizerHelper

class MainActivity : AppCompatActivity() {

    private lateinit var mp: MediaPlayer
    private var totalTime: Int = 0
    var currentSessionId = 0
    private var track = Track()

    private var current = 0
    lateinit var binding: ActivityMainBinding
    lateinit var playBt: Drawable
    lateinit var stopBt: Drawable


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0 && grantResults[0] == 0) initVisualizer(currentSessionId)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val mediaSession = MediaSessionCompat(this, "player")
        val mediaSessionConnector = MediaSessionConnector(mediaSession)
        mp = MediaPlayer.create(this, R.raw.bloom)

        val player = SimpleExoPlayer.Builder(this).build()
        player.audioComponent?.audioSessionId
        player.play()
        mp.isLooping = true
        mp.setVolume(0.5f, 0.5f)
        currentSessionId = mp.audioSessionId

        //get data
        val data = MediaMetadataRetriever()
        val uri: Uri = Uri.parse("android.resource://com.example.player/raw/bloom");
        data.setDataSource(this, uri)
        track.title = data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
        track.album = data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: ""
        track.artist = data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
        val trackArt = data.embeddedPicture
        track.albumCover = if (trackArt != null) {
            BitmapFactory.decodeByteArray(trackArt, 0, trackArt.size)
        } else {
            BitmapFactory.decodeResource(this.resources, R.drawable.image)
        }
        track.albumCover?.let {
            track.squareAlbumCover = createSquaredBitmap(it)
        }

        val palette = Palette.generate(track.albumCover)
        val darkColor = palette.getDarkVibrantColor(getColor(R.color.offWhite))
        val lightColor = palette.getLightMutedColor(getColor(R.color.offWhite))
        val mainColor = palette.getVibrantColor(getColor(R.color.offWhite))

        track.colors = listOf(mainColor, darkColor, lightColor)

        this.window.statusBarColor = mainColor



        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 0)
        } else initVisualizer(currentSessionId)

        totalTime = mp.duration

        initComponents()
    }


    private fun initComponents() {
        //Album Art
        val drawable = BitmapDrawable(resources, track.albumCover)

        //Texts
        binding.title.text = "${track.artist} ${track.title}"
        binding.title.setTextColor(track.colors[0])
        binding.title.isSelected = true
        binding.title.typeface = resources.getFont(R.font.raleway_bold)
        //binding.title.startAnimation(AnimationUtils.loadAnimation(this, R.anim.translate_title))
        binding.remainingTimeLabel.setTextColor(track.colors[1])
        binding.elapsedTimeLabel.setTextColor(track.colors[1])

        //binding.visual.setBackgroundColor(palette.getVibrantColor(getColor(R.color.red)))

        //Backgrounds
        binding.albumArt.setImageDrawable(drawable)
        binding.background.setImageDrawable(drawable)
        binding.background.alpha = 0.4f


        playBt = ContextCompat.getDrawable(this, R.drawable.play)!!
        stopBt = ContextCompat.getDrawable(this, R.drawable.stop)!!

        DrawableCompat.setTint(playBt, track.colors[0])
        DrawableCompat.setTint(stopBt, track.colors[0])

        DrawableCompat.setTint(binding.volumeMinus.drawable, track.colors[0])
        DrawableCompat.setTint(binding.volumePlus.drawable, track.colors[0])

        binding.volumeBar.progressDrawable.setTint(track.colors[1])
        binding.volumeBar.thumb.setTint(track.colors[0])

        binding.positionBar.progressDrawable.setTint(track.colors[1])
        binding.positionBar.thumb.setTint(track.colors[0])

        binding.playBtn.setImageDrawable(playBt)


        // Volume Bar
        binding.volumeBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekbar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        var volumeNum = progress / 100.0f
                        mp.setVolume(volumeNum, volumeNum)
                    }
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                }
            }
        )

        // Position Bar
        binding.positionBar.max = totalTime
        binding.positionBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        mp.seekTo(progress)
                    }
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                }
            }
        )

        // Thread
        Thread(Runnable {
            while (mp != null) {
                try {
                    var msg = Message()
                    msg.what = mp.currentPosition
                    handler.sendMessage(msg)
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                }
            }
        }).start()
    }

    @SuppressLint("HandlerLeak")
    var handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            var currentPosition = msg.what

            // Update positionBar
            binding.positionBar.progress = currentPosition

            // Update Labels
            var elapsedTime = createTimeLabel(currentPosition)
            binding.elapsedTimeLabel.text = elapsedTime

            var remainingTime = createTimeLabel(totalTime - currentPosition)
            binding.remainingTimeLabel.text = "-$remainingTime"
        }
    }

    fun createTimeLabel(time: Int): String {
        var timeLabel = ""
        var min = time / 1000 / 60
        var sec = time / 1000 % 60

        timeLabel = "$min:"
        if (sec < 10) timeLabel += "0"
        timeLabel += sec

        return timeLabel
    }

    fun playBtnClick(v: View) {
        if (mp.isPlaying) {
            // Stop
            mp.pause()
            binding.playBtn.setImageDrawable(playBt)

        } else {
            // Start
            mp.start()
            binding.playBtn.setImageDrawable(stopBt)
            //startNotification(mp)

        }
    }

    private fun initVisualizer(audioSessionId: Int) {
        //val bitmap = BitmapFactory.decodeResource(resources, R.drawable.image)

        val circleBitmap = SimpleIcon.getCircledBitmap(track.squareAlbumCover!!)

        val helper = VisualizerHelper(audioSessionId)

        val painterLists = listOf(
            listOf(
                Shake(Preset.getPresetWithBitmap("cWaveRgbIcon", circleBitmap, track.colors, 0.70f))
            )
        )
        binding.visual.setPainterList(helper, painterLists[current])


    }

    private fun createSquaredBitmap(srcBmp: Bitmap): Bitmap {
        if (srcBmp.width >= srcBmp.height){
           return Bitmap.createBitmap(
                srcBmp,
                srcBmp.getWidth()/2 - srcBmp.getHeight()/2,
                0,
                srcBmp.getHeight(),
                srcBmp.getHeight()
            );

        }else{
            return Bitmap.createBitmap(
                srcBmp,
                0,
                srcBmp.getHeight()/2 - srcBmp.getWidth()/2,
                srcBmp.getWidth(),
                srcBmp.getWidth()
            );
        }
    }




}