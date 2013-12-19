package com.example.brainstormnotifications;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.koushikdutta.async.http.socketio.Acknowledge;
import com.koushikdutta.async.http.socketio.ConnectCallback;
import com.koushikdutta.async.http.socketio.DisconnectCallback;
import com.koushikdutta.async.http.socketio.ErrorCallback;
import com.koushikdutta.async.http.socketio.EventCallback;
import com.koushikdutta.async.http.socketio.JSONCallback;
import com.koushikdutta.async.http.socketio.SocketIOClient;
import com.koushikdutta.async.http.socketio.StringCallback;

public class ChatMessageService extends Service implements ErrorCallback, JSONCallback,
		DisconnectCallback, StringCallback, EventCallback {
	
	static final String NOTIFICATIONS_SERVER = "http://brainstorm3000-notifications-server.jit.su:80";
	
	public ChatMessageService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		createSocketListener();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Toast.makeText(this, " Service Started", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onEvent(String event, JSONArray argument, Acknowledge acknowledge) {
	    try {
	        Log.d("MainActivity", "Event:" + event + "Arguments:" + argument.toString(2));
			JSONObject message =  (JSONObject)argument.get(0);
			Log.d("MainActivity", "Message:" + message.get("text") + " at " + convertUCTToEST((String)message.get("date")));
			sendNotification((String)message.get("username"), (String)message.get("text"), convertUCTToEST((String)message.get("date")));
	    } catch (JSONException e) {
	        e.printStackTrace();
	    }
	}
	
	// "2013-12-17T21:13:38.370Z"
	private String convertUCTToEST(String utcDate){
		try{
			SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			sourceFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			Date parsed = sourceFormat.parse(utcDate); // => Date is in UTC now
	
			TimeZone tz = TimeZone.getTimeZone("EST");
			SimpleDateFormat destFormat = new SimpleDateFormat("MMM-dd HH:mm:ss");
			destFormat.setTimeZone(tz);
			return destFormat.format(parsed);
		} catch(Exception e){
			e.printStackTrace();
			Log.d("MainActivity", "Error parsing date " + utcDate);
			return utcDate;
		}
	}

	@Override
	public void onString(String string, Acknowledge acknowledge) {
		Log.d("MainActivity", string);
	}

	@Override
	public void onJSON(JSONObject json, Acknowledge acknowledge) {
		try {
			Log.d("MainActivity", "json:" + json.toString(2));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onError(String error) {
		Log.d("MainActivity", error);
	}

	@Override
	public void onDisconnect(Exception e) {
		Log.d("MainActivity", "Disconnected:" + e.getMessage());
		// reconnect
		createSocketListener();
	}

	private void sendNotification(String username, String message, String timestamp) {
		String user = username != null ? username : "Anonymous";  
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
			.setSmallIcon(R.drawable.ic_chat_message)
			.setAutoCancel(true)
            .setLights(Color.YELLOW, 500, 500)
			.setContentTitle(user + " at " + timestamp)
			.setContentText(message);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		int notificationId = (int) System.currentTimeMillis();	
		mNotificationManager.notify(notificationId,	notificationBuilder.build());
	}
	
	private void createSocketListener() {
		
		SocketIOClient.connect(
				NOTIFICATIONS_SERVER,
				new ConnectCallback() {

					@Override
					public void onConnectCompleted(Exception ex, SocketIOClient client) {
						if (ex != null) {
							return;
						}

						// Save the returned SocketIOClient instance into a
						// variable so you can disconnect it later
						client.setDisconnectCallback(ChatMessageService.this);
						client.setErrorCallback(ChatMessageService.this);
						client.setJSONCallback(ChatMessageService.this);
						client.setStringCallback(ChatMessageService.this);

						// You need to explicitly specify which events you are
						// interested in receiving
						client.addListener("messages_broadcast", ChatMessageService.this);

						client.of("/messages_broadcast", new ConnectCallback() {

							@Override
							public void onConnectCompleted(Exception ex,
									SocketIOClient client) {

								if (ex != null) {
									ex.printStackTrace();
									return;
								}

								// This client instance will be using the same
								// websocket as the original client,
								// but will point to the indicated endpoint
								client.setDisconnectCallback(ChatMessageService.this);
								client.setErrorCallback(ChatMessageService.this);
								client.setJSONCallback(ChatMessageService.this);
								client.setStringCallback(ChatMessageService.this);
								client.addListener("a message",	ChatMessageService.this);

							}
						});

					}
				}, new Handler());
	}

}