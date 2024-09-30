package com.saitejajanjirala.musicdemo

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaSession2
import android.media.session.MediaSession
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.compose.runtime.MutableState
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val PREV ="prev"
const val NEXT = "next"
const val PLAY_PAUSE ="play_pause"
class MusicPlayerService :Service() {

    val binder = MusicBinder()
    inner class MusicBinder : Binder(){
        fun getService() = this@MusicPlayerService
        fun setMusicList(list:List<Track>) {
            this@MusicPlayerService.musicList = list.toMutableList()
        }

        fun currentDuration() = this@MusicPlayerService.currentDuration
        fun maxDuration() = this@MusicPlayerService.maxDuration
        fun isPlaying() = this@MusicPlayerService.isPlaying

        fun currentTrack() = this@MusicPlayerService.currentTrack

    }
    private lateinit var mediaSession: MediaSessionCompat


    private val maxDuration = MutableStateFlow<Float>(0f)
    private val currentDuration = MutableStateFlow<Float>(0f)
    private val scope = CoroutineScope(Dispatchers.Main)
    private var mediaPlayer = MediaPlayer()
    private val currentTrack = MutableStateFlow<Track>(Track())
    private var musicList = mutableListOf<Track>()
    private var job : Job? = null
    private val isPlaying = MutableStateFlow<Boolean>(false)
    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this,"Music Service")
        mediaSession.isActive  = true
    }
    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(intent.action){
                PREV->{
                    prev()
                }
                NEXT->{
                    next()
                }
                PLAY_PAUSE->{
                    playPause()
                }
                else->{
                    currentTrack.update {
                        songs.get(0)
                    }
                    play(currentTrack.value)
                }
            }
        }
        return START_STICKY
    }

    private fun play(track: Track){
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(this,getRawURi(track.id))
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            sendNotification(track)
            updateDurations()
        }
    }

    fun updateDurations(){
        job = scope.launch {
            if(mediaPlayer.isPlaying.not()) return@launch
            maxDuration.update { mediaPlayer.duration.toFloat() }
            while(true){
                currentDuration.update { mediaPlayer.currentPosition.toFloat() }
                delay(1000)

            }
        }
    }


    fun prev(){
        job?.cancel()
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer()
        val index = musicList.indexOf(currentTrack.value)
        val prevIndex = if(index<=0) musicList.size.minus(1) else index.minus(1)
        val prevItem = musicList.get(prevIndex)
        currentTrack.update { prevItem }
        mediaPlayer.setDataSource(this,getRawURi(currentTrack.value.id))
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            sendNotification(currentTrack.value)
            updateDurations()
        }
    }

    fun next(){
        job?.cancel()
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer()
        val index = musicList.indexOf(currentTrack.value)
        val nextIndex = index.plus(1).mod(musicList.size)
        val nextItem = musicList.get(nextIndex)
        currentTrack.update { nextItem }
        mediaPlayer.setDataSource(this,getRawURi(currentTrack.value.id))
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            sendNotification(currentTrack.value)
            updateDurations()
        }
    }

    fun playPause(){
        if(mediaPlayer.isPlaying){
            mediaPlayer.pause()
        }
        else{
            mediaPlayer.start()
        }
        sendNotification(currentTrack.value)
    }

    private fun getRawURi(id : Int) = Uri.parse("android.resource://${packageName}/$id")

    private fun sendNotification(track: Track){
        isPlaying.update { mediaPlayer.isPlaying }
        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0,1,2)
            .setMediaSession(mediaSession.sessionToken)
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val channel = NotificationChannel(channel_id, channel_name, NotificationManager.IMPORTANCE_HIGH)
            channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this@MusicPlayerService, channel_id)
            .setStyle(style)
            .setContentTitle(track.name)
            .setContentText(track.desc)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setLargeIcon(BitmapFactory.decodeResource(resources,R.drawable.big_image))
            .addAction(R.drawable.ic_prev,"prev",createPrevPendingIntent())
            .addAction(if (mediaPlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,"play_pause",createPlayPausePendingIntent())
            .addAction(R.drawable.ic_next,"next",createNextPendingIntent()) // Only notify once when updated
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission(this,POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED){
                startForeground(1,notification)
            }
        }
        else{
            startForeground(1,notification)
        }
    }

    fun createPrevPendingIntent():PendingIntent{
        val intent = Intent(this,MusicPlayerService::class.java).apply {
            action = PREV
        }
        return PendingIntent.getService(this,0,intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
    fun createNextPendingIntent():PendingIntent{
        val intent = Intent(this,MusicPlayerService::class.java).apply {
            action = NEXT
        }
        return PendingIntent.getService(this,0,intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
    fun createPlayPausePendingIntent():PendingIntent{
        val intent = Intent(this,MusicPlayerService::class.java).apply {
            action = PLAY_PAUSE
        }
        return PendingIntent.getService(this,0,intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }


}