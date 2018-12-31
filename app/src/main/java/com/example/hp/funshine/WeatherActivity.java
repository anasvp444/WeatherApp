package com.example.hp.funshine;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.hp.funshine.model.DialyWeatherReport;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class WeatherActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,GoogleApiClient.ConnectionCallbacks,LocationListener {

    final String URL_BASE = "http://api.openweathermap.org/data/2.5/forecast";
    final String URL_CORD = "/?lat=";//9.9687&lon=76.299";
    final String URL_UNITS = "&units=metric";
    final String URL_API_KEY = "&APPID=2fcd1b692b5d97da7ee3b5d11151964f";

    final int PERMISSION_LOCATION = 111;

    private GoogleApiClient mGoogleApiClient;
    private ArrayList<DialyWeatherReport> weatherReportsList = new ArrayList<>();

    private ImageView weatherIconMini;
    private ImageView weatherIcon;
    private TextView weatherDate;
    private TextView currentTemp;
    private TextView lowTemp;
    private TextView cityCountry;
    private TextView weatherDescription;

    WeatherAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.container_weather_report);
        mAdapter = new WeatherAdapter(weatherReportsList);
        recyclerView.addItemDecoration(new VerticalStationFragmentDecorator(30));
        recyclerView.setAdapter(mAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        recyclerView.setLayoutManager(layoutManager);


        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).enableAutoManage(this,this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();


        weatherIcon = (ImageView)findViewById(R.id.weatherIcon);
        weatherIconMini = (ImageView)findViewById(R.id.weatherIconMini);
        weatherDate = (TextView)findViewById(R.id.weatherDate);
        currentTemp = (TextView)findViewById(R.id.currentTemp);
        lowTemp = (TextView)findViewById(R.id.lowTemp);
        cityCountry = (TextView)findViewById(R.id.cityCountry);
        weatherDescription = (TextView)findViewById(R.id.weatherDescription);

    }

    public void downloadWeatherData(Location location){
        final String FULL_STRING = URL_CORD+location.getLatitude()+"&lon="+location.getLongitude();
        final String url = URL_BASE+FULL_STRING +URL_UNITS+URL_API_KEY;

        final JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url,null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

            try{

                JSONObject city = response.getJSONObject("city");
                String cityName = city.getString("name");
                String country = city.getString("country");
                JSONArray list = response.getJSONArray("list");

                for(int x=0; x<15;x++){
                    JSONObject obj = list.getJSONObject(x);
                    JSONObject main = obj.getJSONObject("main");
                    Double currentTemp = main.getDouble("temp");
                    Double maxTemp = main.getDouble("temp_max");
                    Double minTemp = main.getDouble("temp_min");
                    JSONArray weatherArry= obj.getJSONArray("weather");
                    JSONObject weather = weatherArry.getJSONObject(0);
                    String weatherType = weather.getString("main");
                    String rawDate = obj.getString("dt_txt");
                    Log.v("FUN!",weatherType);
                    DialyWeatherReport dialyWeatherReport = new DialyWeatherReport(cityName,country,currentTemp.intValue(),maxTemp.intValue(),minTemp.intValue(),weatherType,rawDate);

                    weatherReportsList.add(dialyWeatherReport);

                }


            }catch (JSONException e){
                Log.v("FUN!","ERR: ");
            }
             updateUi();
            mAdapter.notifyDataSetChanged();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("FUN!","ERR: "+error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(this).add(jsonRequest);
    }

    public void updateUi(){
        if(weatherReportsList.size()>0){

            DialyWeatherReport report = weatherReportsList.get(0);

            switch (report.getWeather()) {
                case DialyWeatherReport.WEATHER_TYPE_CLEAR:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
                    break;
                case DialyWeatherReport.WEATHER_TYPE_CLOUDS:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    break;
                case DialyWeatherReport.WEATHER_TYPE_RAIN:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.rainy));;
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    break;
                default:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
            }
            weatherDescription.setText(report.getWeather());
            weatherDate.setText(report.getFormatedDate());

            currentTemp.setText(Integer.toString(report.getCurrentTemp())+"°");
            lowTemp.setText(Integer.toString(report.getMinTemp())+"°");
            cityCountry.setText(report.getCityName()+","+report.getCountry());
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        downloadWeatherData(location);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION )!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSION_LOCATION);
            Log.v("1MAPS","requesting permissions");
        }else{
            Log.v("1MAPS","STARTING LOCATION SERVICE ON CONNECT");
            startLocationServices();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
    public void startLocationServices(){
        Log.v("1MAPS","Starting Location Services Called");
        try{
            LocationRequest req = LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,req,this);
        }
        catch (SecurityException exception){
            Log.v("1MAPS",exception.toString());
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case PERMISSION_LOCATION:{
                if(grantResults.length >0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    startLocationServices();
                }
                else{

                }
            }
        }
    }

    public class WeatherViewHolder extends RecyclerView.ViewHolder{

        private ImageView weatherImg;
        private TextView weatherDescription;
        private TextView weatherDay;
        private TextView tempHigh;
        private TextView tempLow;


        public WeatherViewHolder(@NonNull View itemView) {

            super(itemView);

            weatherImg = (ImageView)itemView.findViewById(R.id.weatherImage);
            weatherDescription = (TextView)itemView.findViewById(R.id.weather_description);
            weatherDay = (TextView)itemView.findViewById(R.id.weather_day);
            tempHigh = (TextView)itemView.findViewById(R.id.tempHigh);
            tempLow = (TextView)itemView.findViewById(R.id.tempLow);
        }

        public void updateUI(DialyWeatherReport dialyWeatherReport){
            switch (dialyWeatherReport.getWeather()) {
                case DialyWeatherReport.WEATHER_TYPE_CLEAR:
                    weatherImg.setImageDrawable(getResources().getDrawable(R.drawable.sunny_mini));
                    break;
                case DialyWeatherReport.WEATHER_TYPE_CLOUDS:
                    weatherImg.setImageDrawable(getResources().getDrawable(R.drawable.cloudy_mini));
                    break;
                case DialyWeatherReport.WEATHER_TYPE_RAIN:
                    weatherImg.setImageDrawable(getResources().getDrawable(R.drawable.rainy_mini));;
                    break;
                default:
                    weatherImg.setImageDrawable(getResources().getDrawable(R.drawable.sunny_mini));
            }
            weatherDescription.setText(dialyWeatherReport.getWeather());
            weatherDay.setText(dialyWeatherReport.getFormatedDate());
            tempHigh.setText(Integer.toString(dialyWeatherReport.getMaxTemp()));
            tempLow.setText(Integer.toString(dialyWeatherReport.getMinTemp()));
        }
    }

    public class WeatherAdapter extends RecyclerView.Adapter<WeatherViewHolder>{

        private ArrayList<DialyWeatherReport> mDialyWeatherReport;


        public WeatherAdapter(ArrayList<DialyWeatherReport> mDialyWeatherReport) {
            this.mDialyWeatherReport = mDialyWeatherReport;
        }

        @Override
        public void onBindViewHolder(@NonNull WeatherViewHolder weatherViewHolder, int i) {

            DialyWeatherReport report = mDialyWeatherReport.get(i);
            weatherViewHolder.updateUI(report);
        }

        @NonNull
        @Override
        public WeatherViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View card = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.card_layout,viewGroup,false);

            return new WeatherViewHolder(card);
        }

        @Override
        public int getItemCount() {
            return mDialyWeatherReport.size();
        }
    }

    class VerticalStationFragmentDecorator extends RecyclerView.ItemDecoration{

        private final int spacer;

        public VerticalStationFragmentDecorator(int spacer) {
            this.spacer = spacer;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.top = spacer;

        }
    }
}
