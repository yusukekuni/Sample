package jp.teluru.Teluru;
import jp.teluru.Teluru.Config;

import android.content.Context;
import android.content.Intent;


public class Utils {
    public static void loadCookie() {
    	
    }
    
    public static void saveCookie() {
    	
    }
    
    public static String urlEncode(String string){
    	return "";
    }
    
    public static String teluruUrl(String path){
        return Config.PROTOCOL + Config.HOST_NAME + "/" + path;
   }
}