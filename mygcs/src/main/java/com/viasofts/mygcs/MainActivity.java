package com.viasofts.mygcs;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.util.FusedLocationSource;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;
import com.o3dr.services.android.lib.model.action.Action;

import org.jetbrains.annotations.NotNull;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DroneListener, TowerListener, LinkListener {

    private NaverMap mMap;
//    public  NaverAddrApi naverAddrApi;
    private double mTakeoffAltitude = 5.5;
    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();
    private FusedLocationSource mLocationSource;
    private Marker dronePositionMarker = new Marker();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    public static Context mContext;
    private LatLng mCurrentDronePosition;
    private Marker mDroneMarker;
    Button onAddTakeoffAltitude;
    Button onSubTakeoffAltitude;
    Marker marker = new Marker();


    private Spinner modeSelector;

    Handler mainHandler;
    Spinner maptype_spinner;
    ConnectionParameter connParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnArmTakeOff2 = findViewById(R.id.btnArmTakeOff);
        mContext = this;
        initViews();

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);
        mDroneMarker = new Marker();

        mLocationSource =
                new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);


        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);

        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
            }



            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        mainHandler = new Handler(getApplicationContext().getMainLooper());



    }
    protected void onPostExecute(String jsonStr) {
        JsonParser jsonParser = new JsonParser();
        double pnuLatitude;
        double pnuLongitude;
        JsonObject jsonObj = (JsonObject) jsonParser.parse(jsonStr);
        JsonArray jsonArray = (JsonArray) jsonObj.get("addresses");
        jsonObj = (JsonObject) jsonArray.get(0);
        pnuLatitude =jsonObj.get("x").getAsDouble();
        pnuLongitude =jsonObj.get("y").getAsDouble();

        Marker markers = new Marker();
        markers.setPosition(new LatLng(pnuLongitude, pnuLatitude));
        markers.setMap(mMap);

    }
    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        updateVehicleModesForType(this.droneType);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }


    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }

    // Drone Listener
    // ==========================================================


    @Override
    public void onDroneEvent(String event, Bundle extras) {
        Button connetcButton = (Button) findViewById(R.id.connectBtn);
        switch (event) {

            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                connetcButton.setText("Disconnect");
                addRecyclerViewText("드론 연결");

                updateConnectedButton(this.drone.isConnected());
                updateArmButton();

                checkSoloState();

                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                connetcButton.setText("Connect");
                addRecyclerViewText("드론 연결 해제");

                updateConnectedButton(this.drone.isConnected());
                updateArmButton();

                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;

            case AttributeEvent.MISSION_SENT:
                addRecyclerViewText("미션 보내기 완료");
                MissionApi.getApi(drone).startMission(true,true,null);
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();

                    updateVehicleModesForType(this.droneType);

                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.HOME_UPDATED:
//                updateDistanceFromHome();
                break;

            case AttributeEvent.BATTERY_UPDATED:
                updateBattery();
                break;

            case AttributeEvent.GPS_COUNT:
                countGPS();
                break;

            case AttributeEvent.ATTITUDE_UPDATED:
                updateAttitude();
                break;

            case AttributeEvent.GPS_POSITION:
                Gps gps = this.drone.getAttribute(AttributeType.GPS);
                LatLong latLong = gps.getPosition();
                if (latLong == null) {
                    break;
                } else {
                    mCurrentDronePosition = new LatLng(gps.getPosition().getLatitude(), gps.getPosition().getLongitude());
                    Log.d("myLog", "현재드론의 위치 : " + mCurrentDronePosition);
                    updateDronePostion(mCurrentDronePosition);
                }
                break;

            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }


    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }


    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null){
            alertUser("Unable to retrieve the solo state.");
        }
        else {
            alertUser("Solo state is up to date.");
        }
    }


    //비행모드 변경
    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Vehicle mode change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }

//    //가이드 모드
//   public static class GuideMode{
//        static LatLng mGuidedPoint; //가이드모드 목적지 저장
//        static Marker mMarkerGuide = new com.naver.maps.map.overlay.Marker(); //gcs 위치 표시 마커옵션
//        static OverlayImage guideIcon = OverlayImage.fromResource(R.drawable.marker_guide);
//        static void DialogSimple(final Drone drone, final LatLong point) {
//            AlertDialog.Builder alt_bld = new AlertDialog.Builder(MapActivity.MapsContext);
//            alt_bld.setMessage("확인하시면 가이드모드로 전환후 기체가 이동합니다.").setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener(){
//                public void onClick(DialogInterface dialog, int id) {
//                    VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new AbstractCommandListener() {
//                        @Override
//                        public void onSuccess() {
//                            ControlApi.getApi(drone).goTo(point, true, null);
//                        }
//
//                        @Override
//                        public void onError(int i) {
//
//                        }
//
//                        @Override
//                        public void onTimeout() {
//
//                        }
//                    });
//                }
//            }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    dialog.cancle();
//                }
//            });
//            AlertDialog alert = alt_bld.create();
//            alert.setTitle("Title");
//            alert.setIcon(R.drawable.drone);
//            alert.show();
//        }
//        public static boolean CheckGoal(final Drone drone, LatLng recentLatLng) {
//            GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
//            LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(),
//                    guidedState.getCoordinate().getLongitude());
//            return target.distanceTo(recentLatLng) <=1 ;
//        }
//    }



    public void onArmButtonTap(View view) {

        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land 착륙
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to land the vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to land the vehicle.");
                }
            });
        } else if (vehicleState.isArmed()) {
            // Take off 이륙
            AlertDialog.Builder msgbuilder = new AlertDialog.Builder(MainActivity.this)
                    .setMessage("지정한 이륙 고도까지 기체가 상승합니다.\n 안전거리를 유지하세요")
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ControlApi.getApi(drone).takeoff(mTakeoffAltitude, new AbstractCommandListener() {

                                @Override
                                public void onSuccess() {
                                    alertUser("Taking off...");
                                }

                                @Override
                                public void onError(int i) {
                                    alertUser("Unable to take off.");
                                }

                                @Override
                                public void onTimeout() {
                                    alertUser("Unable to take off.");
                                }
                            });
                        }
                    })
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Toast.makeText(MainActivity.this,"취소되었습니다.",Toast.LENGTH_SHORT).show();
                        }
                    });
            AlertDialog msgDlg = msgbuilder.create();
            msgDlg.show();

        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {

            AlertDialog.Builder msgbuilder = new AlertDialog.Builder(MainActivity.this)
                    .setMessage("확인 누르면 시동걸립니다. 조심하세요.")
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Connected but not Armed 시동
                            VehicleApi.getApi(drone).arm(true, false, new SimpleCommandListener() {
                                @Override
                                public void onError(int executionError) {
                                    alertUser("Unable to arm vehicle.");
                                }

                                @Override
                                public void onTimeout() {
                                    alertUser("Arming operation timed out.");
                                }
                            });
                        }
                    })
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Toast.makeText(MainActivity.this,"취소되었습니다.",Toast.LENGTH_SHORT).show();
                        }
                    });
            AlertDialog msgDlg = msgbuilder.create();
            msgDlg.show();


        }
    }


    public void tapConnectBTN(View view) {
        AlertDialog.Builder myAlertBuilder =
                new AlertDialog.Builder(MainActivity.this);
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        }
        else {
            myAlertBuilder.setTitle("드론 연결");
            myAlertBuilder.setMessage("어느 방식으로 연결하시겠습니까");
            myAlertBuilder.setPositiveButton("UDP",new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    ConnectionParameter connectionParams
                            = ConnectionParameter.newUdpConnection(null);
                    drone.connect(connectionParams);
                }
            });
            myAlertBuilder.setNegativeButton("Bluetooth", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    drone.connect(connParams);
                }
            });
            myAlertBuilder.show();
        }
            /*
            ConnectionParameter connectionParams
                    = ConnectionParameter.newUdpConnection(null);
            this.drone.connect(connectionParams);
            this.drone.connect(connParams);
            */
    }


    @Override
    public void onMapReady(@NonNull @org.jetbrains.annotations.NotNull NaverMap naverMap) {
        mMap = naverMap;
        mMap.setMapType(NaverMap.MapType.Satellite);
        naverMap.setLocationSource(mLocationSource);

        //군산대로 위치 이동
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(35.945256,126.682133));
        naverMap.moveCamera(cameraUpdate);

        maptype_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    mMap.setMapType(NaverMap.MapType.Basic);
                } else if (position == 1) {
                    mMap.setMapType(NaverMap.MapType.Satellite);
                } else if (position == 2) {
                    mMap.setMapType(NaverMap.MapType.Terrain);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
//        naverMap.setOnMapLongClickListener((point, coord) -> {
//
//            marker.setPosition(new LatLng(coord.latitude, coord.longitude));
//            marker.setMap(naverMap);
//            naverAddrApi = new NaverAddrApi(this);
//            naverAddrApi.execute(new LatLng(coord.latitude, coord.longitude));
//
//        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (mLocationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!mLocationSource.isActivated()) { // 권한 거부됨
                mMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }


    public void addRecyclerViewText(String msg){
//        mTextInRecycleerView.add(msg);
//        // 리사이클러뷰에 객체 지정.
//        recyclerViewText adapter = new recyclerViewText(mTextInRecycleerView) ;
//        mRecyclerView.setAdapter(adapter) ;
    }





    @Override
    public void onLinkStateUpdated(@NonNull @NotNull LinkConnectionStatus connectionStatus) {
//        switch(connectionStatus.getStatusCode()){
//            case LinkConnectionStatus.FAILED:
//                Bundle extras = connectionStatus.getExtras();
//                String msg = null;
//                if (extras != null) {
//                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
//                }
//                alertUser("Connection Failed:" + msg);
//                break;
//        }

    }




    // UI updating
    // ==========================================================



    protected void updateConnectedButton(@NotNull Boolean isConnected) {
        Button connetButton = (Button) findViewById(R.id.connectBtn);
        if (isConnected) {
            connetButton.setText("Disconnect");
        } else {
            connetButton.setText("Connect");
        }
    }



    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.btnArmTakeOff);

        if (!this.drone.isConnected()) {
            armButton.setVisibility(View.INVISIBLE);
        } else {
            armButton.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            // Land
            armButton.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }


     // 고도
    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }
    //배터리
    protected void updateBattery(){
        TextView batteryView = (TextView) findViewById(R.id.valueVolt);
        Battery droneBattery = this.drone.getAttribute(AttributeType.BATTERY);
        batteryView.setText(String.format("%3.1f", droneBattery.getBatteryVoltage()) + "V");
    }

    //gps
    protected void countGPS(){
        TextView countGPS = (TextView) findViewById(R.id.valueSatellite);
        Gps gps = this.drone.getAttribute(AttributeType.GPS);
        countGPS.setText(String.format("%d", gps.getSatellitesCount()));
    }


    protected void updateAttitude(){
        TextView viewYaw = (TextView) findViewById(R.id.valueYAW);
        Attitude yaw = this.drone.getAttribute(AttributeType.ATTITUDE);
        float yaw_360 = (float) yaw.getYaw();
        if(yaw_360 < 0){
            yaw_360 = 360 - Math.abs(yaw_360);
            if(yaw_360 == 360) yaw_360 = 0;
        }
        viewYaw.setText(String.format("%d", (int) yaw_360 ) + "deg");
        dronePositionMarker.setAngle(yaw_360);
        //mLocationOverlay.setBearing(yaw_360);
    }



    // 속도
    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    protected void updateDistanceFromHome() {
//        TextView distanceTextView = (TextView) findViewById(R.id.distanceValueTextView);
//        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
//        double vehicleAltitude = droneAltitude.getAltitude();
//        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
//        LatLong vehiclePosition = droneGps.getPosition();
//
//        double distanceFromHome = 0;
//
//        if (droneGps.isValid()) {
//            LatLongAlt vehicle3DPosition = new LatLongAlt(vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), vehicleAltitude);
//            Home droneHome = this.drone.getAttribute(AttributeType.HOME);
//            distanceFromHome = distanceBetweenPoints(droneHome.getCoordinate(), vehicle3DPosition);
//        } else {
//            distanceFromHome = 0;
//        }
//
//        distanceTextView.setText(String.format("%3.1f", distanceFromHome) + "m");
    }

    protected void updateVehicleModesForType(int droneType) {

        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }



    private void initViews() {
        maptype_spinner = findViewById(R.id.maptype_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.map_type, R.layout.custom_spinner_item);
        adapter.setDropDownViewResource(R.layout.custom_spinner_item_click);
        maptype_spinner.setAdapter(adapter);
        onAddTakeoffAltitude = findViewById(R.id.addAltitude);
        onSubTakeoffAltitude= findViewById(R.id.subAltitude);
        onAddTakeoffAltitude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mTakeoffAltitude<10)
                {
                    mTakeoffAltitude += 0.5;
                    Button Altitudebutton = (Button) findViewById(R.id.Altitudecontrol);
                    Altitudebutton.setText("이륙고도\n"+(double) mTakeoffAltitude+"M");

                }
            }
        });
        onSubTakeoffAltitude.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                if(mTakeoffAltitude>3)
                {
                    mTakeoffAltitude -= 0.5;
                    Button Altitudebutton = (Button) findViewById(R.id.Altitudecontrol);
                    Altitudebutton.setText("이륙고도\n"+(double) mTakeoffAltitude+"M");
                }

            }

        });
    }



    // Helper methods
    // ==========================================================
    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d("mylog", message);
    }

    private void updateDronePostion(LatLng latLng) {
        //TODO 가져온 좌표를 가지고 드론마커를 갱신하세요.
        mDroneMarker.setPosition(latLng);
        mDroneMarker.setMap(mMap);
    }


    private void onAddTakeoffAltitude(View view) {

        if(mTakeoffAltitude+0.5<10)
        {
            mTakeoffAltitude += 0.5;
            Button Altitudebutton = (Button) findViewById(R.id.Altitudecontrol);
            Altitudebutton.setText((int) mTakeoffAltitude);
        }
        else{
            AlertDialog.Builder msgbuilder = new AlertDialog.Builder(MainActivity.this)
                    .setMessage("이륙고도 범위는 10m 이하입니다.");
        }

    }
    private void onSubTakeoffAltitude(View view) {
        if(mTakeoffAltitude-0.5>3)
        {
            mTakeoffAltitude -= 0.5;
            Button Altitudebutton = (Button) findViewById(R.id.Altitudecontrol);
            Altitudebutton.setText((int) mTakeoffAltitude);
        }
        else{
            AlertDialog.Builder msgbuilder = new AlertDialog.Builder(MainActivity.this)
                    .setMessage("이륙고도 범위는 3m 이상 입니다.");
        }
    }

}

