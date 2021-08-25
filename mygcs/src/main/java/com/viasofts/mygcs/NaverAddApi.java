//package com.viasofts.mygcs;
//
//import android.os.AsyncTask;
//import android.support.annotation.NonNull;
//
//import com.google.gson.JsonArray;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//import com.naver.maps.geometry.LatLng;
//import com.naver.maps.map.overlay.InfoWindow;
//
//import org.jetbrains.annotations.NotNull;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//
//class NaverAddrApi extends AsyncTask<LatLng, String, String> {
//    private StringBuilder urlBuilder;
//    private URL url;
//    private HttpURLConnection conn;
//    public MainActivity mMainActivity;
//
//    public  NaverAddrApi(MainActivity mainActivity) {
//        mMainActivity = mainActivity;
//    }
//    @Override
//    protected String doInBackground(LatLng... latLngs) {
//        String strCoord = String.valueOf(latLngs[0].longitude) + "," + String.valueOf(latLngs[0].latitude);
//        StringBuilder sb = new StringBuilder();
//
//        urlBuilder = new StringBuilder("https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?request=coordsToaddr&coords=" +strCoord+ "&sourcecrs=epsg:4326&output=json&orders=addr"); /* URL */
//        try {
//            url = new URL(urlBuilder.toString());
//
//            conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod("GET");
//            conn.setRequestProperty("Content-type", "application/json");
//            conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID","d6nvpc15uv");
//            conn.setRequestProperty("X-NCP-APIGW-API-KEY","fzGdGvib2YvAL8I3KAnS4PGCubwhLNsq8yHIO8o5");
//
//            BufferedReader rd;
//            if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
//                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//            } else {
//                rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
//            }
//            String line;
//            while ((line = rd.readLine()) != null) {
//                sb.append(line);
//            }
//            rd.close();
//            conn.disconnect();
//
//        } catch (Exception e) {
//            return null;
//        }
//        return sb.toString();
//    }
//
////    @Override
////    protected void onPostExecute(String jsonStr) {
////        super.onPostExecute(jsonStr);
////
////        JsonParser jsonParser = new JsonParser();
////        String pnu = " ";
////
////        JsonObject jsonObj = (JsonObject) jsonParser.parse(jsonStr);
////        JsonArray jsonArray = (JsonArray) jsonObj.get("results");
////        for(int i=1;i<=4;i++){
////            jsonObj = (JsonObject) jsonArray.get(0);
////            jsonObj = (JsonObject) jsonObj.get("region");
////            jsonObj = (JsonObject) jsonObj.get("area"+i);
////            pnu = pnu + " "+jsonObj.get("name").getAsString();
////        }
////
////        jsonObj = (JsonObject) jsonParser.parse(jsonStr);
////        jsonArray = (JsonArray) jsonObj.get("results");
////        jsonObj = (JsonObject) jsonArray.get(0);
////        jsonObj = (JsonObject) jsonObj.get("land");
////        String number1 = jsonObj.get("number1").getAsString();
////        String number2 = jsonObj.get("number2").getAsString();
////        pnu = pnu + number1 +"-"+ number2;
////
////
////        String cc = pnu;
////        InfoWindow infowindow = new InfoWindow();
////
////        infowindow.setAdapter(new InfoWindow.DefaultTextAdapter(mMainActivity.getApplication()) {
////            @NonNull
////            @NotNull
////            @Override
////            public CharSequence getText(@NonNull @NotNull InfoWindow infoWindow) {
////                return cc;
////            }
////        });
////
////        infowindow.open(mMainActivity.marker);
////    }
//
//
//
//    private String makeStringNum(String number) {
//        String strNum="";
//        for (int i=0; i<4-number.length(); i++) {
//            strNum = strNum + "0";
//        }
//        strNum=strNum+number;
//        return strNum;
//    }
//}
