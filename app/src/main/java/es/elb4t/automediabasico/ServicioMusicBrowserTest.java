package es.elb4t.automediabasico;

import android.app.Service;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.service.media.MediaBrowserService;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServicioMusicBrowserTest extends MediaBrowserService {
    private final String TAG = ServicioMusicBrowserTest.this.getClass().getSimpleName();
    private MediaSession mSession;
    private List<MediaMetadata> mMusic;
    private MediaPlayer mPlayer;
    private MediaMetadata mCurrentTrack;

    private final String URL = "http://storage.googleapis.com/automotive-media/music.json";
    private RequestQueue requestQueue;
    private Gson gson;
    private Musica musica;

    @Override
    public void onCreate() {
        super.onCreate();
        requestQueue = Volley.newRequestQueue(this);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();

        mMusic = new ArrayList<MediaMetadata>();
        getRepositorioMusical();

        //Añadimos 3 canciones desde la librería de audio de youtube
/*        mMusic.add(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "https://www.youtube.com/audiolibrary_download?vid=f5cfb6bd8c048b98")
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Primera canción")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Artista 1")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 109000)
                .build());
        mMusic.add(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "https://www.youtube.com/audiolibrary_download?vid=ac7a38f4a568229c")
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Segunda canción")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Artista 2")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 65000)
                .build());
        mMusic.add(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "https://www.youtube.com/audiolibrary_download?vid=456229530454affd")
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Tercera canción")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Artista 3")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 121000)
                .build());*/
        mPlayer = new MediaPlayer();
        mSession = new MediaSession(this, "MiServicioMusical");
        setSessionToken(mSession.getSessionToken());

        mSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlayFromMediaId(String mediaId, Bundle extras) {
                for (MediaMetadata item : mMusic) {
                    if (item.getDescription().getMediaId().equals(mediaId)) {
                        mCurrentTrack = item;
                        break;
                    }
                }
                handlePlay();
            }

            @Override
            public void onPlay() {
                if (mCurrentTrack == null) {
                    mCurrentTrack = mMusic.get(0);
                    handlePlay();
                } else {
                    mPlayer.start();
                    mSession.setPlaybackState(buildState(PlaybackState.STATE_PLAYING));
                }
            }

            @Override
            public void onPause() {
                mPlayer.pause();
                mSession.setPlaybackState(buildState(PlaybackState.STATE_PAUSED));
            }

            @Override
            public void onSkipToNext() {
                int cancionSiguiente = mMusic.indexOf(mCurrentTrack) +1;
                if (cancionSiguiente == mMusic.size()){
                    mCurrentTrack = mMusic.get(0);
                    handlePlay();
                }else{
                    mCurrentTrack = mMusic.get(cancionSiguiente);
                    handlePlay();
                }
            }

            @Override
            public void onSeekTo(long pos) {
                if (pos >= mPlayer.getCurrentPosition()){
                    onSkipToNext();
                }
            }

            @Override
            public void onSkipToPrevious() {
                int cancionSiguiente = mMusic.indexOf(mCurrentTrack) - 1;
                if (cancionSiguiente < 0){
                    mCurrentTrack = mMusic.get(mMusic.size() - 1);
                    handlePlay();
                }else{
                    mCurrentTrack = mMusic.get(cancionSiguiente);
                    handlePlay();
                }
            }
        });
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mSession.setActive(true);

    }

    private PlaybackState buildState(int state) {

        return new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                | PlaybackState.ACTION_SKIP_TO_NEXT
                | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID | PlaybackState.ACTION_PLAY_PAUSE)
                .setState(state, mPlayer.getCurrentPosition(), 1, SystemClock.elapsedRealtime())
                .build();
    }

    private void handlePlay() {
        mPlayer.seekTo(0);
        mSession.setPlaybackState(buildState(PlaybackState.STATE_PLAYING));
        mSession.setMetadata(mCurrentTrack);
        try {
            mPlayer.reset();
            mPlayer.setDataSource(ServicioMusicBrowserTest.this,
                    Uri.parse(mCurrentTrack.getDescription().getMediaId()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.seekTo(0);
                mSession.setPlaybackState(buildState(PlaybackState.STATE_PLAYING));
            }
        });
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
            }
        });

        mPlayer.prepareAsync();
    }

    @Override
    public MediaBrowserService.BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new MediaBrowserService.BrowserRoot("ROOT", null);
    }

    @Override
    public void onLoadChildren(String s, MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result) {
        List<MediaBrowser.MediaItem> list = new ArrayList<MediaBrowser.MediaItem>();
        for (MediaMetadata m : mMusic) {
            list.add(new MediaBrowser.MediaItem(m.getDescription(), MediaBrowser.MediaItem.FLAG_PLAYABLE));
        }
        result.sendResult(list);
    }

    @Override
    public void onDestroy() {
        mSession.release();
    }

    private void getRepositorioMusical() {
        StringRequest request = new StringRequest(Request.Method.GET, URL, onPostsLoaded, onPostsError);
        requestQueue.add(request);
    }

    private final Response.Listener<String> onPostsLoaded = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            musica = gson.fromJson(response, Musica.class);
            Log.d(TAG, "Número de pistas de audio: " + musica.getMusica().size());

            int slashPos = URL.lastIndexOf('/');
            String path = URL.substring(0, slashPos + 1);

            for (int i = 0; i < musica.getMusica().size(); i++) {
                PistaAudio pista = musica.getMusica().get(i);
                if (!pista.getSource().startsWith("http"))
                    pista.setSource(path + pista.getSource());
                if (!pista.getImage().startsWith("http"))
                    pista.setImage(path + pista.getImage());
                musica.getMusica().set(i, pista);

                mMusic.add(new MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, musica.getMusica().get(i).getSource())
                        .putString(MediaMetadata.METADATA_KEY_TITLE, musica.getMusica().get(i).getTitle())
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, musica.getMusica().get(i).getArtist())
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, musica.getMusica().get(i).getDuration() * 1000)
                        .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, musica.getMusica().get(i).getImage())
                        .build());
            }

        }
    };
    private final Response.ErrorListener onPostsError = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e(TAG, error.toString());
        }
    };
}
