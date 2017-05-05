package in.co.madhur.chatbubblesdemo;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class ShopBotChatSession {

    private final static String TAG = "ShopBotChatSession";
    private final static String UserSimulatorURL = "http://linux15.csie.org:30678/nbshopbot/api/v0/usersims";
    private final static String ChatSessionURL = "http://linux15.csie.org:30678/nbshopbot/api/v0/chats";

    private String userSimulatorID = null;
    private String chatSessionID = null;

    private JSONObject objSemanticFrame = null;

    public ShopBotChatSession() {
    }

    public ShopBotChatSession(String userSimulatorID, String chatSessionID) {
        this.userSimulatorID = userSimulatorID;
        this.chatSessionID = chatSessionID;
    }

    public String getUserSimulatorID() {
        return userSimulatorID;
    }

    public String getChatSessionID() {
        return chatSessionID;
    }

    public void setUserSimulatorID(String userSimulatorID) {
        this.userSimulatorID = userSimulatorID;
    }

    public void setChatSessionID(String chatSessionID) {
        this.chatSessionID = chatSessionID;
    }

    public JSONObject getObjSemanticFrame() {
        return objSemanticFrame;
    }

    public void setObjSemanticFrame(JSONObject objSemanticFrame) {
        this.objSemanticFrame = objSemanticFrame;
    }

    public String createUserSimulator() {
        String message = null;
        try {
            String response = POST(UserSimulatorURL);
            JSONObject jsonObj = new JSONObject(response);
            userSimulatorID = jsonObj.getString("simulator_id");
            message = jsonObj.getString("message");
            return message;
        } catch (IOException e) {
            Log.d(TAG, "Can't create User simulator");
        } catch (JSONException e) {
            Log.d(TAG, "JSON parse error");
        }
        return null;
    }

    public String createChatSession() {
        try {
            String response = POST(ChatSessionURL);
            JSONObject jsonObj = new JSONObject(response);
            chatSessionID = jsonObj.getString("chat_id");
            return chatSessionID;
        } catch (IOException e) {
            Log.d(TAG, "Can't create Chat Session");
        } catch (JSONException e) {
            Log.d(TAG, "JSON parse error");
        }
        return null;
    }

    public String sendMessage(String message) throws IOException {
        if (chatSessionID == null) {
            Log.d(TAG, "Chat Session ID is not created!");
            throw new IOException("Chat Session ID is not created");
        }

        String chatUrl = ChatSessionURL + "/" + chatSessionID;

        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "Unable to add message to JSON object");
        }

        try {
            String respStr = POST(chatUrl, jsonObj);
            Log.d(TAG, "response=" + respStr);
            JSONObject respJson = new JSONObject(respStr);
            String chatID = respJson.getString("chat_id");
            String respMessage = respJson.getString("bot_reply_message");
            objSemanticFrame = respJson.getJSONObject("bot_semantic_frame");

            if (!chatID.equals(chatSessionID)) {
                Log.d(TAG, "chat ID mismatch");
                //TODO: throw exception here
            }
            return respMessage;
        } catch (IOException e) {
            Log.d(TAG, "Can't create Chat Session");
        } catch (JSONException e) {
            Log.d(TAG, "JSON parse error");
        }
        return null;

    }

    public String updateSimulator(String message) throws IOException {
        if (userSimulatorID == null) {
            Log.d(TAG, "User simulator ID is not created!");
            throw new IOException("User Simulator ID is not created");
        }

        String simUrl = UserSimulatorURL + "/" +userSimulatorID;

        JSONObject jsonObj = new JSONObject();
        try {
            //jsonObj.put("bot_semantic_frame", objSemanticFrame);
            jsonObj.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "Unable to add semantic frame object to JSON object");
        }

        try {
            String respStr = POST(simUrl, jsonObj);
            Log.d(TAG, "response=" + respStr);
            JSONObject respJson = new JSONObject(respStr);
            String simulatorID = respJson.getString("simulator_id");
            String respMessage = respJson.getString("message");

            if (!simulatorID.equals(userSimulatorID)) {
                Log.d(TAG, "Simulator ID mismatch");
                //TODO: throw exception here
            }
            return respMessage;
        } catch (IOException e) {
            Log.d(TAG, "Can't update Simulator state");
        } catch (JSONException e) {
            Log.d(TAG, "JSON parse error");
        }

        return null;
    }

    public static String POST(String url) throws IOException {
        InputStream inputStream = null;
        String result = "";
        try {
            Log.d(TAG, "Post " + url);
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            HttpResponse httpResponse = httpclient.execute(httpPost);

            inputStream = httpResponse.getEntity().getContent();

            if(inputStream != null) {
                result = convertInputStreamToString(inputStream);
            } else {
                throw new IOException("Get nothing from web");
            }

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    public static String POST(String url, JSONObject jsonObj) throws IOException {
        InputStream inputStream = null;
        String result = "";
        try {
            Log.d(TAG, "Post " + jsonObj.toString() + " to " + url);
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);

            String jsonStr = jsonObj.toString();
            StringEntity se = new StringEntity(jsonStr);
            httpPost.setEntity(se);

            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            HttpResponse httpResponse = httpclient.execute(httpPost);

            inputStream = httpResponse.getEntity().getContent();

            if(inputStream != null) {
                result = convertInputStreamToString(inputStream);
            } else {
                throw new IOException("Get nothing from web");
            }

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }
}
