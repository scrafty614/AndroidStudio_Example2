package com.noctisdrakon.tomaleapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.Toast;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by NoctisDrakon on 23/11/2015.
 */
public class LocationService extends Service

{
    public static final String BROADCAST_ACTION = "Hello World";
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    public LocationManager locationManager;
    public MyLocationListener listener;
    public Location previousBestLocation = null;
    public Criteria criteria;
    public String provider;

    //Cosas add imageview
    private WindowManager windowManager;
    private de.hdodenhof.circleimageview.CircleImageView imagencircular;
    private Animation bounce;
    private LinearLayout layout;


    int veces=0;


    Intent intent;
    int counter = 0;

    @Override
    public void onCreate()
    {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);


    }

    @Override
    public void onStart(Intent intent, int startId)
    {

        Toast.makeText( getApplicationContext(), "Inicia el servicio de localización!", Toast.LENGTH_SHORT ).show();


        addView();

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("OnClickListener", "Clickeo D:");
                bounce = AnimationUtils.loadAnimation(getApplication(), R.anim.bounce);
                imagencircular.startAnimation(bounce);

                //  imagencircular.setVisibility(View.GONE);
            }
        });



        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();

        //Criteria
        criteria=new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        /*criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);*/
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        provider=locationManager.getBestProvider(criteria,true);
        Log.d("OnStart","El mejor provider que se encontró fué: "+provider);

        if(ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, listener);
            //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, listener);
            locationManager.requestLocationUpdates(provider, 4000, 5, listener);
        }
    }

    public void addView()
    {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        imagencircular = new de.hdodenhof.circleimageview.CircleImageView(getApplication());
        imagencircular.setImageResource(R.drawable.tomalebg);

        layout = new LinearLayout(this);
        layout.addView(imagencircular); // And attach your objects

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;
        params.width=100;
        params.height=100;

        windowManager.addView(layout, params);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }



    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }



    @Override
    public void onDestroy() {
        // handler.removeCallbacks(sendUpdatesToUI);
        super.onDestroy();
        Log.v("STOP_SERVICE", "DONE");
        if(ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(listener);
        }
    }



    public class MyLocationListener implements LocationListener
    {

        public void onLocationChanged(final Location loc)
        {
            Log.i("**********", "Location changed");
            Log.d("LocationChanged", "Cambió el GPS D:");

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(getApplication())
                            .setSmallIcon(R.drawable.tomalelogo)
                            .setContentTitle("Tómale informa")
                            .setContentText("Le informamos que recibió una actualización de su ubicación. Hasta la próxima!");

            int mNotificationId = 001;
// Gets an instance of the NotificationManager service
            NotificationManager mNotifyMgr =  (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
// Builds the notification and issues it.

            //mNotifyMgr.notify(mNotificationId, mBuilder.build());


            veces++;
            Toast.makeText( getApplicationContext(), "Actualización número: "+veces+"La Latitud es: "+loc.getLatitude() + " Y la longitud es: " + loc.getLongitude(), Toast.LENGTH_SHORT).show();

            if(isBetterLocation(loc, previousBestLocation)) {
                Log.d("IsBetterLocation", "Si es mejor");
                loc.getLatitude();
                loc.getLongitude();
                intent.putExtra("Latitude", loc.getLatitude());
                intent.putExtra("Longitude", loc.getLongitude());
                intent.putExtra("Provider", loc.getProvider());
                sendBroadcast(intent);

            }
        }

        public void onProviderDisabled(String provider2)
        {
            //Toast.makeText( getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT ).show();
            Log.d("OnProviderDisabled", "Provider deshabilitado D:");

        }


        public void onProviderEnabled(String provider3)
        {
           // Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
            Log.d("OnProviderEnabled", "Provider Habilitado: "+provider);

        }

        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.d("OnStatusChanged", "El provider: " + provider + " Cambió y su status es: " + status);

        }



    }
}