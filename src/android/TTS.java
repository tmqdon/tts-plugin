package com.wordsbaking.cordova.tts;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewImpl;
import org.apache.cordova.CordovaInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import java.util.*;
import java.lang.reflect.Field;

import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;

import android.content.Intent;
import android.content.Context;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.tts.Voice;
import android.util.Log;

/*
    Cordova Text-to-Speech Plugin
    https://github.com/vilic/cordova-plugin-tts

    by VILIC VANE
    https://github.com/vilic

    updated by SEBASTIAAN PASMA
    https://github.com/spasma

    MIT License
*/

public class TTS extends CordovaPlugin implements OnInitListener {

    public static final String ERR_INVALID_OPTIONS = "ERR_INVALID_OPTIONS";
    public static final String ERR_NOT_INITIALIZED = "ERR_NOT_INITIALIZED";
    public static final String ERR_ERROR_INITIALIZING = "ERR_ERROR_INITIALIZING";
    public static final String ERR_UNKNOWN = "ERR_UNKNOWN";

    boolean ttsInitialized = false;
    TextToSpeech tts = null;
    Context context = null;
    CordovaWebView webView = null;
    CallbackContext synthesisCallback = null;
    CallbackContext rangeStartCallback = null;
    CallbackContext synthesisDoneCallback = null;
    String stopReason = null;

    @Override
    public void initialize(CordovaInterface cordova, final CordovaWebView webView) {

        context = cordova.getActivity().getApplicationContext();

        tts = new TextToSpeech(cordova.getActivity().getApplicationContext(), this);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {

                System.out.println("starting speech for Id: " + s);
            }

            @Override
            public void onBeginSynthesis(String utteranceId, int sampleRateInHz, int audioFormat, int channelCount) {

                sendEventToCordova("onBeginSynthesis", "utteranceId", utteranceId, "sampleRateInHz", sampleRateInHz,
                        "audioFormat", audioFormat, "channelCount", channelCount);

            }

            @Override
            public void onDone(String callbackId) {
                System.out.println("done " + callbackId);
                if (!callbackId.equals("")) {

                    try {
                        JSONObject eventData = new JSONObject();

                        if (stopReason != null) {
                            eventData.put("stopReason", stopReason);
                        }

                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, eventData);
                        pluginResult.setKeepCallback(true);
                        synthesisDoneCallback.sendPluginResult(pluginResult);
                    } catch (JSONException e) {

                        synthesisDoneCallback.error("Failed to create JSON object");
                    }
                }
            }

            @Override
            public void onError(String utteranceId) {

                try {
                    System.out.println("Error encountered in id: " + utteranceId);
                    JSONObect reason = new JSONObject();
                    reason.put("stopReason", "ERROR");

                    stop(new JSONArray().put(reason), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            public void onError(String utteranceId, int errorCode) {
                try {

                    String errorType = getErrorConstantName(errorCode);
                    System.out.println("Error encountered in id: " + utteranceId + " errorCode: " + errorType);

                    JSONObect reason = new JSONObject();
                    reason.put("stopReason", "ERROR");

                    stop(new JSONArray().put(reason), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onRangeStart(String utteranceId, int start, int end, int frame) {

                sendEventToCordova("onRangeStart", "startIdx", start, "endIdx", end, "frame", frame);

            }
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
            throws JSONException {

        if (action.equals("speak")) {
            speak(args, callbackContext);
        } else if (action.equals("stop")) {
            stop(args, callbackContext);
        } else if (action.equals("checkLanguage")) {
            checkLanguage(args, callbackContext);
        } else if (action.equals("getVoices")) {
            getVoices(args, callbackContext);
        } else if (action.equals("openInstallTts")) {
            callInstallTtsActivity(args, callbackContext);
        } else if (action.equals("registerSynthesisCallback")) {
            synthesisCallback = callbackContext;
            System.out.println("SYNTHESIS_CALLBACK REGISTERED");
        } else if (action.equals("registerRangeStartCallback")) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);

            rangeStartCallback = callbackContext;
            System.out.println("RANGE_START_CALLBACK REGISTERED");
        } else if (action.equals("registerStopCallback")) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);

            synthesisDoneCallback = callbackContext;
            System.out.println("SYNTHESIS_DONE_CALLBACK REGISTERED");
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void onInit(int status) {
        System.out.println("TTS: tts STARTED");
        if (status != TextToSpeech.SUCCESS) {
            tts = null;
            System.out.println("TTS: NO SUCCESS");
        } else {
            // warm up the tts engine with an empty string
            HashMap<String, String> ttsParams = new HashMap<String, String>();
            ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");
            tts.setLanguage(new Locale("en", "US"));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak("", TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                tts.speak("", TextToSpeech.QUEUE_FLUSH, null);
            }
            // tts.speak("", TextToSpeech.QUEUE_FLUSH, ttsParams);
            System.out.println("TTS: SUCCESS");
            ttsInitialized = true;
        }
    }

    private void stop(JSONArray args, CallbackContext callbackContext)
            throws JSONException, NullPointerException {

        if (tts != null) {
            if (args.length() > 0) {
                JSONObject params = args.getJSONObject(0);

                stopReason = params.isNull("stopReason") ? "stop" : params.getString("stopReason");

            } else {
                stopReason = "stop";
            }

            System.out.println("STOPPING UTTERANCE");
            System.out.println("STOP_REASON: " + stopReason);
            tts.stop();

            try {
                JSONObject eventData = new JSONObject();

                if (stopReason != null) {
                    eventData.put("stopReason", stopReason);
                } else {
                    eventData.put("stopReason", "UNKNOWN");
                }

                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, eventData);
                pluginResult.setKeepCallback(true);
                synthesisDoneCallback.sendPluginResult(pluginResult);
            } catch (JSONException e) {

                e.printStackTrace();
                synthesisDoneCallback.error("Failed to create JSON object");
            }

        }
    }

    private void callInstallTtsActivity(JSONArray args, CallbackContext callbackContext)
            throws JSONException, NullPointerException {

        PackageManager pm = context.getPackageManager();
        Intent installIntent = new Intent();
        installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        ResolveInfo resolveInfo = pm.resolveActivity(installIntent, PackageManager.MATCH_DEFAULT_ONLY);

        if (resolveInfo == null) {
            // Not able to find the activity which should be started for this intent
        } else {
            installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(installIntent);
        }
    }

    private void checkLanguage(JSONArray args, CallbackContext callbackContext)
            throws JSONException, NullPointerException {
        Set<Locale> supportedLanguages = tts.getAvailableLanguages();
        String languages = "";
        if (supportedLanguages != null) {
            for (Locale lang : supportedLanguages) {
                languages = languages + "," + lang;
            }
        }
        if (languages != "") {
            languages = languages.substring(1);
        }

        final PluginResult result = new PluginResult(PluginResult.Status.OK, languages);
        callbackContext.sendPluginResult(result);
    }

    private void speak(JSONArray args, CallbackContext callbackContext)
            throws JSONException, NullPointerException {
        JSONObject params = args.getJSONObject(0);

        if (params == null) {
            callbackContext.error(ERR_INVALID_OPTIONS);
            return;
        }

        String text;
        String locale;
        double rate;
        double pitch;
        boolean cancel = false;
        String identifier;

        if (params.isNull("text")) {
            callbackContext.error(ERR_INVALID_OPTIONS);
            return;
        } else {
            text = params.getString("text");
        }

        if (params.isNull("identifier")) {
            identifier = "";
            Log.v("TTS", "No voice identifier");
        } else {
            identifier = params.getString("identifier");
            Log.v("TTS", "got identifier: " + identifier);
        }

        if (params.isNull("locale")) {
            locale = Locale.getDefault().toLanguageTag();
        } else {
            locale = params.getString("locale");
        }

        if (!params.isNull("cancel")) {
            cancel = params.getBoolean("cancel");
        }
        Log.v("TTS", "cancel is set to " + cancel + "("
                + (cancel ? "TextToSpeech.QUEUE_FLUSH" : "TextToSpeech.QUEUE_ADD") + ")");

        if (params.isNull("rate")) {
            rate = 1.0;
            Log.v("TTS", "No rate provided, so rate is set to " + rate);
        } else {
            rate = params.getDouble("rate");
            Log.v("TTS", "rate is set to " + rate);
        }

        if (params.isNull("pitch")) {
            pitch = 1.0;
            Log.v("TTS", "No pitch provided, so pitch set to " + pitch);
        } else {
            pitch = params.getDouble("pitch");
            Log.v("TTS", "Pitch set to " + pitch);
        }

        if (tts == null) {
            callbackContext.error(ERR_ERROR_INITIALIZING);
            return;
        }

        if (!ttsInitialized) {
            callbackContext.error(ERR_NOT_INITIALIZED);
            return;
        }

        HashMap<String, String> ttsParams = new HashMap<String, String>();
        ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, callbackContext.getCallbackId());
        Set<Voice> voices = tts.getVoices();
        Voice voice = null;

        if (!identifier.equals("")) {
            for (Voice tmpVoice : voices) {
                if (tmpVoice.getName().contains(identifier)) {
                    Log.v("TTS", "Found Voice for identifier: " + tmpVoice.getName());
                    voice = tmpVoice;
                    break;
                } else {
                    voice = null;
                }
            }
            if (voice == null) {
                Log.v("TTS", "No Voice for identifier: " + identifier + ", we'll try the locale");
            }
        }
        if (voice == null) {
            String[] localeArgs = locale.split("-");
            tts.setLanguage(new Locale(localeArgs[0], localeArgs[1]));
            for (Voice tmpVoice : voices) {
                if (tmpVoice.getName().toLowerCase().contains(locale.toLowerCase())) {
                    Log.v("TTS", "Found Voice for locale: " + tmpVoice.getName());
                    voice = tmpVoice;
                    break;
                } else {
                    voice = null;
                }
            }
        }

        if (voice != null) {
            Log.v("TTS", "We've got a voice: " + voice.getName());
            tts.setVoice(voice);
        } else {
            Log.v("TTS", "No voice found..");
        }

        if (Build.VERSION.SDK_INT >= 27) {
            tts.setSpeechRate((float) rate * 0.7f);
        } else {
            tts.setSpeechRate((float) rate);
        }
        tts.setPitch((float) pitch);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, cancel ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, null,
                    callbackContext.getCallbackId());
        } else {
            tts.speak(text, cancel ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, ttsParams);
        }

        stopReason = null;
    }

    private void getVoices(JSONArray args, CallbackContext callbackContext)
            throws JSONException, NullPointerException {

        Set<Voice> voices = tts.getVoices();
        JSONArray languages = new JSONArray();
        for (Voice tmpVoice : voices) {
            JSONObject lang = new JSONObject();
            Log.v("TTS", "Voice: " + tmpVoice.getName());
            lang.put("name", tmpVoice.getName());
            lang.put("identifier", tmpVoice.getName());
            lang.put("language", tmpVoice.getLocale());
            languages.put(lang);
        }

        final PluginResult result = new PluginResult(PluginResult.Status.OK, languages);
        callbackContext.sendPluginResult(result);
    }

    private void sendEventToCordova(String event, Object... data) {

        try {
            JSONObject eventData = new JSONObject();
            eventData.put("event", event);

            // Add additional data to the eventData
            for (int i = 0; i < data.length; i += 2) {
                if (i + 1 < data.length) {
                    eventData.put(data[i].toString(), data[i + 1]);
                }
            }

            System.out.println("sending eventData: " + eventData.toString());

            if (event == "onRangeStart") {
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, eventData);
                pluginResult.setKeepCallback(true);
                rangeStartCallback.sendPluginResult(pluginResult);

            } else if (event == "onBeginSynthesis") {
                synthesisCallback.success(eventData);
            }

        } catch (JSONException e) {
            synthesisCallback.error("Failed to create JSON object");
        }

    }

    private String getErrorConstantName(int errorCode) {
        try {
            Class<?> ttsClass = TextToSpeech.class;

            // Get all declared fields in the TextToSpeech class
            Field[] fields = ttsClass.getDeclaredFields();

            // Iterate through the fields to find the one with the specified value
            for (Field field : fields) {
                if (field.getType() == int.class && field.getInt(null) == errorCode) {
                    return field.getName();
                }
            }

        } catch (IllegalAccessException | SecurityException e) {
            e.printStackTrace();
        }

        return "UNKNOWN_ERROR_CODE";
    }

}
