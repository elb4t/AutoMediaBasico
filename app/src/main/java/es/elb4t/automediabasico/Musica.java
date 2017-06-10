package es.elb4t.automediabasico;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by eloy on 10/6/17.
 */

public class Musica {
    @SerializedName("music")
    @Expose
    private List<PistaAudio> musica = null;

    public List<PistaAudio> getMusica() {
        return musica;
    }

    public void setMusic(List<PistaAudio> musica) {
        this.musica = musica;
    }
}
