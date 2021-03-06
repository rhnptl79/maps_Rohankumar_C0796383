package com.example.maps_rohankumar_c0796383;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPolygonClickListener, GoogleMap.OnPolylineClickListener, GoogleMap.OnMarkerDragListener {

    private GoogleMap mMap;

    private static final int REQUEST_CODE = 1;
    private Marker homeMarker;
    private Marker destMarker;
    private int count = 0;
    private String[] city = {"A", "B", "C", "D"};
    //private String[] distance = {"45km", "32km", "66Km", "15km"};
    double startlat, startlng, endlat, endlng;
    float results[] = new float[8];
    float totaldis;
    int infocount = 0;

    Polyline line;
    List<Polyline> lines = new ArrayList<>();

    Polygon shape;
    private static final int POLYGON_SIDES = 4;
    List<Marker> markerList = new ArrayList<>();
    List<LatLng> latList = new ArrayList<>();
    List<Marker> dragList = new ArrayList<>();
    //For Address
    String address = " ";

    //location manager and location listener
    LocationManager locationManager;
    LocationListener locationListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //set home marker
                setHomeMarker(location);
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

        if (!isGrantedPermission())
            requestLocationPermission();
        else
            startUpdateLocation();

        //Apply LongPress
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                //set the marker
                if (count < 4) {
                    endlat = latLng.latitude;
                    endlng = latLng.longitude;
                    Location.distanceBetween(startlat, startlng, endlat, endlng, results);
                    totaldis = results[0] + results[1] + results[2] + results[3];
                    setMarker(latLng);
                } else {
                    clearMap();
                }
            }
        });

        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                updateLocationInfo(lastKnownLocation);
                Toast.makeText(MapsActivity.this, "Additional Info "+address, Toast.LENGTH_SHORT).show();
                return false;
            }
        });*/

        //For Additional Information
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {

                        if (infocount > 4) {
                            infocount = 0;
                        }
                        updateLocationInfo(lastKnownLocation);
                        Toast.makeText(MapsActivity.this, "Additional Info " + address, Toast.LENGTH_SHORT).show();
                        infocount++;
                        return false;

                    }


                });
            }

        }


        mMap.setOnPolygonClickListener(this);
        mMap.setOnPolylineClickListener(this);
        mMap.setOnMarkerDragListener(this);

    }

    private void updateLocationInfo(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 4);

            if (addressList != null && addressList.size() > 0) {
                address = "\n";

                if (addressList.get(infocount).getThoroughfare() != null)
                    address += addressList.get(infocount).getThoroughfare() + "\n";
                if (addressList.get(infocount).getLocality() != null)
                    address += addressList.get(infocount).getLocality() + " ";
                if (addressList.get(infocount).getPostalCode() != null)
                    address += addressList.get(infocount).getPostalCode() + " ";
                if (addressList.get(infocount).getAdminArea() != null)
                    address += addressList.get(infocount).getAdminArea();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


    private void setMarker(LatLng latLng) {
        if (count < 4) {
//            float res[] = new float[4];
            double rr = 3958.8; // Radius of the Earth in miles 6371.0710 for km
            double rlat1 = homeMarker.getPosition().latitude * (Math.PI/180); // Convert degrees to radians
            double rlat2 = latLng.latitude * (Math.PI/180); // Convert degrees to radians
            double difflat = rlat2-rlat1; // Radian difference (latitudes)
            double difflon = (latLng.longitude-homeMarker.getPosition().longitude) * (Math.PI/180); // Radian difference (longitudes)

            double d = 2 * rr * Math.asin(Math.sqrt(Math.sin(difflat/2)*Math.sin(difflat/2)+Math.cos(rlat1)*Math.cos(rlat2)*Math.sin(difflon/2)*Math.sin(difflon/2)));

            
            MarkerOptions options = new MarkerOptions().position(latLng)
                    .snippet(String.valueOf(d)+" miles")
                    .icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_baseline_directions_bike_24))

                    .title(" " + city[count]);

            //Below for polyline
        /*if(destMarker != null)
           clearMap();*/
            //destMarker = mMap.addMarker(options);
            //drawLine();

            //Below for polygon
            //check if there are already the same number of markers, we clear the map
            if (markerList.size() == POLYGON_SIDES)
                clearMap();

            markerList.add(mMap.addMarker(options));
            markerList.get(markerList.size() - 1).setDraggable(true);
            latList.add(latLng);
            if (markerList.size() == POLYGON_SIDES)
                drawShape();
            count++;
        }
    }

    private void drawShape() {
        Log.d("draw","called");
//        mMap.clear
        PolygonOptions options = new PolygonOptions().add(latList.get(0), latList.get(1), latList.get(2), latList.get(3))
                .fillColor(Color.parseColor("#5900FF00"))
                .strokeColor(Color.RED)
                .strokeWidth(5);

//        for(int i=0; i<POLYGON_SIDES; i++)
//            options.add(markerList.get(i).getPosition());

        shape = mMap.addPolygon(options);
        shape.setClickable(true);
        drawLine();
    }

    private void drawLine() {
        if (count < 4) {
            for (int i = 0; i < 4; i++) {
                PolylineOptions options = new PolylineOptions()
                        .color(Color.RED)
                        .width(10)
                        .add(latList.get(i == 0 ? 3 : i - 1), latList.get(i))
                        .clickable(true);
                lines.add(mMap.addPolyline(options));
                lines.get(lines.size() - 1).setTag(markerList.get(i == 0 ? 3 : i - 1).getTitle().toString() + "-" + markerList.get(i).getTitle().toString() + " Distance = " + results[i]);
            }


            count++;
        } else {

        }
    }

    private void clearMap() {
        //for polyline
        /*if(destMarker != null){
            destMarker.remove();
            destMarker = null;
        }
        line.remove();*/

        //for polygon
        for (Marker marker : markerList)
            marker.remove();
        markerList.clear();
        shape.remove();
        shape = null;
        mMap.clear();
        count = 0;
        markerList.clear();
        lines.clear();
        latList.clear();
    }

    private void startUpdateLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
    }

    private boolean isGrantedPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void setHomeMarker(Location location) {
        LatLng usrLoc = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions options = new MarkerOptions().position(usrLoc)
                .title("You are here")
                //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_baseline_directions_bike_24))
                .snippet("Your Location");

        homeMarker = mMap.addMarker(options);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(usrLoc, 10));

        startlat = location.getLatitude();
        startlng = location.getLongitude();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (REQUEST_CODE == requestCode) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
            }
        }
    }

    @Override
    public void onPolygonClick(Polygon polygon) {
        Log.d("app", "clicked gon");
        Toast.makeText(this, "Total Distance " + totaldis, Toast.LENGTH_SHORT).show();
//        mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
//            @Override
//            public void onPolygonClick(Polygon polygon) {
//                shape.setClickable(true);
//                Toast.makeText(MapsActivity.this, "HHHHH", Toast.LENGTH_SHORT).show();
//
//            }
//        });
    }


    @Override
    public void onPolylineClick(Polyline polyline) {
        Log.d("app", "clicked line");
        Toast.makeText(this, "Route type " + polyline.getTag().toString(),
                Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onMarkerDragStart(Marker marker) {
//        Log.d("app","clicked marker drag started");
    }

    @Override
    public void onMarkerDrag(Marker marker) {
//        Log.d("app","clicked marker drag");
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        Log.d("app", "clicked marker drag end"+markerList.size());
        count = 0;
        if(markerList.size() > 3){
            shape.remove();
            for(Polyline l : lines){
                l.remove();
            }
            lines.clear();
            latList.clear();
            for(Marker mark: markerList){
                latList.add(mark.getPosition());
            }
            drawLine();
            drawShape();

        }
    }
}
































