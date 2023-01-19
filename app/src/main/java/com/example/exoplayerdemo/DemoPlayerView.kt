package com.example.exoplayerdemo

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SET_VIDEO_SURFACE
import androidx.media3.common.Player.State
import androidx.media3.common.VideoSize
import androidx.media3.common.util.Assertions
import com.example.exoplayerdemo.databinding.DemoPlayerViewBinding

class DemoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {
    private val surfaceView by lazy { SurfaceView(context) }
    private val binding = DemoPlayerViewBinding.inflate(LayoutInflater.from(context), this, true)
    private val componentListener: ComponentListener = ComponentListener()
    private var player: Player? = null
    private var playState = false
    private val progressChangeList: MutableList<ProgressStateChangeListener> = mutableListOf()
    private var playbackStateChanged: ((@Player.State Int) -> Unit)? = null
    private var errorCallback: ((PlaybackException?) -> Unit)? = null
    private var refreshClickListener: (() -> Unit)? = null

    private val progressHandler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            val currentPlayer = player
            //获取进度并通知
            if (currentPlayer != null && currentPlayer.playbackState == Player.STATE_READY && currentPlayer.isPlaying) {
                val currentPosition = currentPlayer.currentPosition.toInt()
                val bufferedPosition = currentPlayer.bufferedPosition.toInt()
                val duration = currentPlayer.duration.toInt()
                progressChangeList.forEach {
                    it.onProgressChanged(currentPosition, bufferedPosition, duration)
                }
                //根据Player.STATE_ENDED回调判断不靠谱，额外增加采用进度判断结束状态
                if (currentPosition >= duration) {
                    playbackStateChanged?.invoke(Player.STATE_ENDED)
                }
                //0.5秒后自动获取进度
                sendEmptyMessageDelayed(1, 500)
            }
        }
    }

    init {
        val params = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        surfaceView.layoutParams = params
        binding.exoContentFrame.addView(surfaceView, 0)
        binding.exoErrorMessage.setOnClickListener { refreshClickListener?.invoke() }
        visibility = View.INVISIBLE
    }

    fun setPlayer(player: Player) {
        Assertions.checkState(Looper.myLooper() == Looper.getMainLooper())
        Assertions.checkArgument(
            player.applicationLooper == Looper.getMainLooper()
        )
        if (this.player == player) {
            return
        }
        val oldPlayer: Player = player
        oldPlayer.removeListener(componentListener)
        oldPlayer.clearVideoSurfaceView(surfaceView)
        this.player = player
        if (player.isCommandAvailable(COMMAND_SET_VIDEO_SURFACE)) {
            player.setVideoSurfaceView(surfaceView)
            updateAspectRatio()
        }
        player.addListener(componentListener)
        updateErrorMessage()
    }

    private fun updateAspectRatio() {
        player?.videoSize?.let { videoSize ->
            if (videoSize.width > 0 && videoSize.height > 0) {
                val width = videoSize.width
                val height = videoSize.height
                //更改比例，根据视频自身宽高比展示
                (binding.exoContentFrame.layoutParams as? LayoutParams)?.let {
                    it.dimensionRatio = "$width:$height"
                    binding.exoContentFrame.layoutParams = it
                }
            }
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        surfaceView.visibility = visibility
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        //释放资源
        try {
            player?.setVideoSurfaceView(null)
            player?.stop()
            player?.release()
            player?.removeListener(componentListener)
            progressHandler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == View.VISIBLE) {
            if (playState) {
                player?.play()
            }
        } else {
            playState = (player?.isPlaying == true)
            if (player?.isPlaying == true) {
                player?.pause()
            }
        }
    }

    /**
     * 更新进度条状态
     */
    private fun updateProgressState() {
        if (player?.playbackState == Player.STATE_READY && (player?.isPlaying == true)) {
            progressHandler.removeCallbacksAndMessages(null)
            progressHandler.sendEmptyMessage(1)
        } else {
            //清空进度
            progressHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun updateErrorMessage() {
        val error = player?.playerError
        error?.let {
            //错误记录到本地日志
            binding.exoErrorMessage.visibility = View.VISIBLE
        } ?: kotlin.run {
            binding.exoErrorMessage.visibility = View.GONE
        }
        errorCallback?.invoke(player?.playerError)
    }

    private inner class ComponentListener : Player.Listener {

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            updateAspectRatio()
        }

        override fun onPlayerError(error: PlaybackException) {
            updateErrorMessage()
        }

        override fun onPlaybackStateChanged(playbackState: @State Int) {
            if (playbackState == Player.STATE_READY && visibility != View.VISIBLE) {
                visibility = View.VISIBLE
            }
            updateProgressState()
            playbackStateChanged?.invoke(playbackState)
        }

        override fun onPlayWhenReadyChanged(
            playWhenReady: Boolean, reason: @Player.PlayWhenReadyChangeReason Int
        ) {
            //播放暂停会回调该方法，播放时playWhenReady为true
            updateProgressState()
        }
    }

    fun addProgressChangeListener(listener: ProgressStateChangeListener) {
        if (!progressChangeList.contains(listener)) {
            progressChangeList.add(listener)
        }
    }

    fun addPlaybackStateChanged(playbackStateChanged: (@Player.State Int) -> Unit) {
        this.playbackStateChanged = playbackStateChanged
    }

    fun setOnErrorCallback(errorCallback: (playerError: PlaybackException?) -> Unit) {
        this.errorCallback = errorCallback
    }

    fun setRefreshClickListener(refreshClickListener: () -> Unit) {
        this.refreshClickListener = refreshClickListener
    }

    /**
     * 播放进度信息
     */
    interface ProgressStateChangeListener {
        fun onProgressChanged(currentPosition: Int, bufferedPosition: Int, duration: Int)
    }
}