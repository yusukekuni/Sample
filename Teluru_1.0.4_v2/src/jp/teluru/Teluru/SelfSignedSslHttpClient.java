package jp.teluru.Teluru;
 
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
 
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
 
public class SelfSignedSslHttpClient {
 
	DefaultHttpClient httpclient = new DefaultHttpClient();
    HttpContext httpcontext = new BasicHttpContext();
 
    SelfSignedSslHttpClient() throws Exception
    {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        Scheme https = new Scheme("https", sf, 443);
        httpclient.getConnectionManager().getSchemeRegistry().register(https);
 
        httpcontext.setAttribute(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpcontext.setAttribute(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
        httpcontext.setAttribute(CoreProtocolPNames.HTTP_CONTENT_CHARSET, HTTP.UTF_8);
    }
 
    HttpResponse post(String url, ArrayList <NameValuePair> postParams) throws Exception {
    	HttpPost post = new HttpPost(url);
	    post.setEntity(new UrlEncodedFormEntity(postParams, "utf-8"));
	    return httpclient.execute(post, httpcontext);
    }

    HttpResponse get(String url) throws Exception
    {
        HttpGet httpget = new HttpGet(url);
        return httpclient.execute(httpget, httpcontext);
    }
}