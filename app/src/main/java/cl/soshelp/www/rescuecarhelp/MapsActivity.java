package cl.soshelp.www.rescuecarhelp;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends ConexionMysqlHelper implements OnMapReadyCallback {

    private GoogleMap mMap;
    Switch serv;
    TextView est, dir, lt, lg,con;
    LocationManager locationManager;
    Location location;
    double lat = 0.0;
    double lng = 0.0;
    Marker marcador;
    android.app.AlertDialog alert = null;
    android.app.AlertDialog alert2 = null;
    String ciudad, ciudad2,direc, id_mob, json_string, JSON_STRING, id_alert ;
    int estCon=0, cuentaAlerta=0;
    JSONObject jsonObject;
    JSONArray jsonArray;
    MediaPlayer eco;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        id_mob = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        //Creacion de boton y estado
        con = (TextView) findViewById(R.id.tvEst);
        eco = MediaPlayer.create(this,R.raw.eco13);
        serv = (Switch) findViewById(R.id.swServ);
        est = (TextView) findViewById(R.id.tvEstEnvio);
        serv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    est.setText("Estableciendo...");
                    estCon=1;
                } else {
                    est.setText("Desconectado");
                    estCon=0;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    /******Envio de datos a mysql ******/
                                    new CargarDatos().execute("http://www.webinfo.cl/soshelp/del_driv.php?id_mob="+id_mob);
                                }
                            });
                        }
                    },0);
                }
            }
        });

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            AlertNoGps();
        }


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        miUbicacion();
        // Controles UI
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Mostrar diálogo explicativo
            } else {
                // Solicitar permiso
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 3);
            }
        }
        mMap.getUiSettings().setZoomControlsEnabled(true);

   }

    private void agregarMarcador(double lat, double lng) {
        LatLng coordenadas = new LatLng(lat, lng);
        CameraUpdate miUbicacion = CameraUpdateFactory.newLatLngZoom(coordenadas, 15);
        if (marcador != null) marcador.remove();
        marcador = mMap.addMarker(new MarkerOptions()
                .position(coordenadas)
                .visible(false)
                .title("Mi Ubicación"));
        mMap.animateCamera(miUbicacion);
    }


    private void actualizarUbicacion(Location location) {
        if (location != null) {
            lat = location.getLatitude();
            lng = location.getLongitude();
            agregarMarcador(lat, lng);
            if (lat!=0.0 && lng!=0.0){
                con.setText("Establecida");
                BuscarAlerta();
                try{
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> list = geocoder.getFromLocation(lat,lng,5);
                    if (!list.isEmpty()){
                        Address direccion = list.get(0);
                        ciudad = direccion.getLocality();
                        ciudad2 = direccion.getSubAdminArea();
                        direc = direccion.getAddressLine(0);
                        ciudad=ciudad.replaceAll(" ", "%20");
                        envioDatosMysql(id_mob, ciudad, lat, lng);
                        }
                }catch (IOException e){
                    //dir.setText(""+e);

                }
            }

        }
    }

    LocationListener locListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            actualizarUbicacion(location);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    };

    private void miUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        actualizarUbicacion(location);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,500,0,locListener);
    }

    private void AlertNoGps() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage("El sistema GPS esta desactivado, ¿Desea activarlo?")
                .setCancelable(false)
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        alert = builder.create();
        alert.show();
    }

    private void AlertAlerta(final String id_alert, String est_alert, final String mob, final double lati, final double lngi) {
        if (cuentaAlerta==1) {
            eco.start();
            final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setMessage("Hay una nueva solicitud de servicio de emergencia, desea acudir?")
                    .setCancelable(false)
                    .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            new CargarDatos().execute("http://www.webinfo.cl/soshelp/ins_driv_alert.php?id_alert=" + id_alert + "&lati=" + lat + "&lngi=" + lng + "&mob_cond=" + mob);
                            new CargarDatos().execute("http://www.webinfo.cl/soshelp/del_driv.php?id_mob="+id_mob);
                            eco.stop();
                            cuentaAlerta=0;
                            Intent m = new Intent(getApplicationContext(), Maps2Activity.class);
                            m.putExtra("id_alert", id_alert);
                            m.putExtra("lat", lat);
                            m.putExtra("lng", lng);
                            m.putExtra("mob", mob);
                            m.putExtra("lati", lati);
                            m.putExtra("lngi", lngi);
                            startActivity(m);

                        }
                    })
                    .setNegativeButton("No Aceptar", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            dialog.cancel();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    alert2.cancel();
                                    BuscarAlerta();
                                    eco.pause();
                                    cuentaAlerta=0;
                                }
                            }, 20000);
                        }
                    });

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    alert2.cancel();
                    eco.pause();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            BuscarAlerta();
                            cuentaAlerta=0;
                        }
                    }, 20000);
                }
            }, 10000);

            alert2 = builder.create();
            alert2.show();
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            } else {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 0, locListener);
            }
        } else {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,500, 0, locListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
           return;
        }
        locationManager.removeUpdates(locListener);
    }

    private void envioDatosMysql(final String id_mob, final String ciudad, final Double lat, final Double lng) {

        if (estCon==1) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            /******Envio de datos a mysql ******/
                            new CargarDatos().execute("http://www.webinfo.cl/soshelp/act_driv.php?id_mob=" + id_mob + "&ciu=" + ciudad + "&lat=" + lat + "&lng=" + lng + "&est=1");
                            est.setText("Conectado");

                        }
                    });
                }
            },0);
        }
    }

    public void BuscarAlerta() {
        if (estCon == 1) {
            new BackgroundTask().execute();
        }
    }
    class BackgroundTask extends AsyncTask<Void,Void,String>
    {
        String json_url;
        @Override
        protected void onPreExecute() {
            json_url = "http://www.webinfo.cl/soshelp/cons_alerta.php";
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(json_url);
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                while ((JSON_STRING = bufferedReader.readLine())!=null)
                {
                    stringBuilder.append(JSON_STRING+"\n");
                }

                bufferedReader.close();
                inputStream.close();
                httpURLConnection.disconnect();
                return stringBuilder.toString().trim();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            JSON_STRING = result;
            presentarDatos();

        }
    }
    public void presentarDatos() {
        if (JSON_STRING != null) {
            json_string = JSON_STRING;
            if (json_string.length()<23) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                BuscarAlerta();
                            }
                        });
                    }
                },10000);

            }else {
                try {

                    jsonObject = new JSONObject(json_string);
                    jsonArray = jsonObject.getJSONArray("server_response");
                    JSONObject JO = jsonArray.getJSONObject(0);
                    String mob, est_alert;
                    Double lati, longi;
                    id_alert = JO.getString("id_alert");
                    est_alert = JO.getString("est_alert");
                    mob = JO.getString("id_mob_cond");
                    lati = Double.parseDouble(JO.getString("lat_cond"));
                    longi = Double.parseDouble(JO.getString("lng_cond"));
                    AlertAlerta(id_alert, est_alert, mob, lati, longi);
                    cuentaAlerta++;

                } catch (JSONException e) {
                    e.printStackTrace();

                }
            }
        }
    }

}