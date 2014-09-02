package com.mixpanel.android.mpmetrics;

import android.util.Log;

import com.mixpanel.android.util.Base64Coder;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class StayAnalyticsExportMessages {

    public static final int MIXPANEL_ACCEPTED_MAX_TIME_INTERVAL = 7; // Days
    public static final String URL = "http://api.mixpanel.com/import/?api_key=";
    public static final String LOGTAG = "StayAnalyticsExportMessages";


    protected static void sendData(String messages, String apiKey) {
        Calendar currentTime = new GregorianCalendar();
        currentTime.setTimeInMillis(System.currentTimeMillis());

        JSONArray agedEventList = new JSONArray();
        JSONArray messageArray;

        try {
            messageArray = new JSONArray(messages);

            JSONObject msg;
            for (int i = 0; i < messageArray.length(); i++) {
                Calendar eventTime = new GregorianCalendar();
                msg = messageArray.getJSONObject(i);
                if (MPConfig.DEBUG) {
                    Log.e(LOGTAG, "EVENT :" + msg.toString());
                }

                String evtTime = null;
                if (msg.has("properties")) {
                    JSONObject eventProp = msg.getJSONObject("properties");
                    evtTime = eventProp.getString("time");
                }

                if (evtTime != null) {
                    eventTime.setTimeInMillis(Long.parseLong(evtTime));

                    if (((currentTime.getTimeInMillis() - eventTime.getTimeInMillis()) / (24 * 60 * 60)) > MIXPANEL_ACCEPTED_MAX_TIME_INTERVAL) {
                        agedEventList.put(msg);
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(LOGTAG, "Could not parse malformed JSON: \"" + t.toString());
        }

        if (agedEventList != null && agedEventList.length() > 0) {
            importMessage(agedEventList, apiKey);
        }
    }

    private static void importMessage(JSONArray messages, String apiKey) {
        if (null == apiKey) {
            if (MPConfig.DEBUG) {
                Log.d(LOGTAG, "API Key is null");
            }
            return;
        }
        final String encodedData = Base64Coder.encodeString(messages.toString());
        final List<NameValuePair> params = new ArrayList<NameValuePair>(1);
        params.add(new BasicNameValuePair("data", encodedData));

        final ServerMessage poster = new ServerMessage();

        boolean deleteEvents = true;
        byte[] response;

        try {
            response = poster.performRequest(URL + apiKey, params);
            if (null == response) {
                if (MPConfig.DEBUG) {
                    Log.d( LOGTAG, "Response was null, unexpected failure posting to " + URL + ".");
                }
            } else {
                String parsedResponse;
                try {
                    parsedResponse = new String(response, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("UTF not supported on this platform?", e);
                }
                if (MPConfig.DEBUG) {
                    Log.e(LOGTAG, "Parsed response:" + parsedResponse);
                }
            }
        } catch (final OutOfMemoryError e) {
            if (MPConfig.DEBUG) {
                Log.e(LOGTAG, "Out of memory when posting to import url" + URL + ".", e);
            }
        } catch (final MalformedURLException e) {
            if (MPConfig.DEBUG) {
                Log.e(LOGTAG, "Cannot interpret " + URL + " as a URL.", e);
            }
        } catch (final IOException e) {
            if (MPConfig.DEBUG) {
                Log.d(LOGTAG, "Cannot post message to " + URL + ".", e);
            }
        }
    }
}