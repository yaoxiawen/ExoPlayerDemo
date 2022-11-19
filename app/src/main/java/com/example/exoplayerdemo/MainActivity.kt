package com.example.exoplayerdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.example.exoplayerdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val url =
        "https://storage.googleapis.com/exoplayer-test-media-1/mp4/dizzy-with-tx3g.mp4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(binding.root)
        val player = ExoPlayer.Builder(this).build()
        player.addMediaItem(MediaItem.fromUri(url))
        binding.demoPlayerView.run {
            setPlayer(player)
            addProgressChangeListener(object : DemoPlayerView.ProgressStateChangeListener {
                override fun onProgressChanged(
                    currentPosition: Int,
                    bufferedPosition: Int,
                    duration: Int
                ) {
                    binding.tvTime.text =
                        getMinSecFormat(currentPosition) + " / " + getMinSecFormat(duration)
                    binding.progress.progress = currentPosition * 1.0f / duration
                }
            })
            setOnErrorCallback {
                Log.d("ExoPlayerDemo", "setOnErrorCallback:${it == null}")
            }
            setRefreshClickListener {
                player.prepare()
            }
        }
        player.playWhenReady = true
        player.prepare()

        binding.tvPlay.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
                binding.tvPlay.text = "播放"
            } else {
                player.play()
                binding.tvPlay.text = "暂停"
            }
        }
        binding.progress.listener = object : VideoProgressBar.Listener {
            override fun onChangeProgress(progress: Float) {
                player.seekTo((progress * player.duration).toLong())
            }
        }
    }

    fun getMinSecFormat(millisecond: Int): String {
        val minutes = millisecond / 60 / 1000
        val seconds = millisecond / 1000 % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

}