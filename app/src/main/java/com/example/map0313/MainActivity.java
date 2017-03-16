package com.example.map0313;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.busline.BusLineSearch;
import com.baidu.mapapi.search.busline.BusLineSearchOption;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView = null;
    private BaiduMap baiduMap;
    public LocationClient mLocationClient = null;
    public BDLocationListener mListener = null;
    private boolean isFirstLocate = true;
    private PoiSearch mPoiSearch;
    private Button mButtonSearch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListener = new MyLocationListener();
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(mListener);

        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        requestPermission();

        mMapView = (MapView) findViewById(R.id.bmapView);
        mButtonSearch = (Button) findViewById(R.id.btnSearch);

        baiduMap = mMapView.getMap();
        baiduMap.setMyLocationEnabled(true);
        mButtonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomizeDialog();

            }
        });


    }

    private void showCustomizeDialog() {
        AlertDialog.Builder customizeDialog = new AlertDialog.Builder(MainActivity.this);
        final View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_customize,null);
        customizeDialog.setTitle("搜索");
        customizeDialog.setView(dialogView);
        customizeDialog.setPositiveButton("确定",new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                EditText add = (EditText) dialogView.findViewById(R.id.address);
                EditText something = (EditText) dialogView.findViewById(R.id.something);
                mPoiSearch = PoiSearch.newInstance();

                mPoiSearch.setOnGetPoiSearchResultListener(poiListener);
                mPoiSearch.searchInCity((new PoiCitySearchOption())
                .city(add.getText().toString().trim())
                .keyword(something.getText().toString().trim())
                .pageNum(1)//分页编号
                .pageCapacity(20));//每页显示的条数，默认10

            }
        });
        customizeDialog.show();
    }

    OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener() {
        @Override
        public void onGetPoiResult(PoiResult result) {
            if(result == null || result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND){
                Toast.makeText(MainActivity.this, "未找到结果", Toast.LENGTH_SHORT).show();
                return;
            }
            if (result.error == SearchResult.ERRORNO.NO_ERROR) {
                final List<PoiInfo> allAddr = result.getAllPoi();
                final String[] ss=new String[20];
                for(int i=0;i<20;i++){
                    PoiInfo p= allAddr.get(i);
                    ss[i]=p.name+" --- "+p.address;
                }

                AlertDialog.Builder listDialog =
                        new AlertDialog.Builder(MainActivity.this);
                listDialog.setTitle("搜索结果：");
                listDialog.setItems(ss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PoiInfo address= allAddr.get(which);
                        double lat= address.location.latitude;
                        double lon = address.location.longitude;

                        //移动地图到相应的经纬度
                        LatLng latLng = new LatLng(lat,lon);
                        MapStatus ms = new MapStatus.Builder().target(latLng).zoom(18f).build();
                        MapStatusUpdate update = MapStatusUpdateFactory.newMapStatus(ms);
                        baiduMap.animateMapStatus(update);

                        //标志位置
                        MyLocationData.Builder locationBuilder = new MyLocationData.Builder();
                        locationBuilder.latitude(lat);
                        locationBuilder.longitude(lon);
                        MyLocationData locationData = locationBuilder.build();
                        baiduMap.setMyLocationData(locationData);


                    }
                });
                listDialog.show();
            }

        }

        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

        }

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

        }
    };


    private void requestPermission() {
        List<String> permissionList = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(!permissionList.isEmpty()){
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);
        }else {
            requestLocation();
        }
    }

    private void requestLocation() {
        initLocation();
        mLocationClient.start();
    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setScanSpan(5000);
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
    }

    private void navigateTo(BDLocation location){
        if(isFirstLocate){
            LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
            //MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(latLng);
            //baiduMap.animateMapStatus(update);
            MapStatus ms = new MapStatus.Builder().target(latLng).zoom(16f).build();
            MapStatusUpdate update = MapStatusUpdateFactory.newMapStatus(ms);
            baiduMap.animateMapStatus(update);
           /* update = MapStatusUpdateFactory.zoomTo(16f);
            baiduMap.animateMapStatus(update);*/
            isFirstLocate = false;

        }
        MyLocationData.Builder locationBuilder = new MyLocationData.Builder();
        locationBuilder.latitude(location.getLatitude());
        locationBuilder.longitude(location.getLongitude());
        MyLocationData locationData = locationBuilder.build();
        baiduMap.setMyLocationData(locationData);

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if(grantResults.length >0){
                    for(int result : grantResults){
                        if(result != PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this,"同意权限才能更好的使用本程序",Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();;
                }else {
                    Toast.makeText(this,"发生未知错误",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mMapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
        mPoiSearch.destroy();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    private class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            if(location.getLocType() == BDLocation.TypeGpsLocation || location.getLocType() == BDLocation.TypeNetWorkLocation){
                navigateTo(location);
            }

        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }
    }
}
