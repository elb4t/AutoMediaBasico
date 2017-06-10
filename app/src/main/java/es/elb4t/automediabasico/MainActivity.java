package es.elb4t.automediabasico;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MainActivity extends AppCompatActivity {
    private final String TAG = MainActivity.this.getClass().getSimpleName();
    private final String URL = "http://storage.googleapis.com/automotive-media/music.json";
    private RequestQueue requestQueue;
    private TextView txtLog;
    private Gson gson;
    private Musica musica;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestQueue = Volley.newRequestQueue(this);
        txtLog = (TextView) findViewById(R.id.txtLog);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //descargamos el fichero JSON mediante Volley
                getRepositorioMusical();
            }
        });

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
            txtLog.setText("Número de pistas de audio: " + musica.getMusica().size());

            int slashPos = URL.lastIndexOf('/');
            String path = URL.substring(0, slashPos + 1);

            for (int i = 0; i < musica.getMusica().size(); i++) {
                PistaAudio pista = musica.getMusica().get(i);
                if (!pista.getSource().startsWith("http"))
                    pista.setSource(path + pista.getSource());
                if (!pista.getImage().startsWith("http"))
                    pista.setImage(path + pista.getImage());
                musica.getMusica().set(i, pista);
            }
        }
    };
    private final Response.ErrorListener onPostsError = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e(TAG, error.toString());
            txtLog.setText(error.toString());
        }
    };

}
