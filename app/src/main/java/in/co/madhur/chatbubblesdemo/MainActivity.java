package in.co.madhur.chatbubblesdemo;

import android.app.Activity;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import in.co.madhur.chatbubblesdemo.model.ChatMessage;
import in.co.madhur.chatbubblesdemo.model.UserType;
import in.co.madhur.chatbubblesdemo.widgets.SizeNotifierRelativeLayout;

import static in.co.madhur.chatbubblesdemo.model.Status.DELIVERED;
import static in.co.madhur.chatbubblesdemo.model.Status.SENT;


public class MainActivity extends ActionBarActivity implements SizeNotifierRelativeLayout.SizeNotifierRelativeLayoutDelegate/*, NotificationCenter.NotificationCenterDelegate*/ {
    private final static String TAG = "NBShopBot";
    private ListView chatListView;
    private EditText chatEditText1;
    private ArrayList<ChatMessage> chatMessages;
    private ImageView enterChatView1;
    private ChatListAdapter listAdapter;

    private RadioGroup rgroup;
    private RadioButton rbChat;
    private RadioButton rbSimulation;


    private SizeNotifierRelativeLayout sizeNotifierRelativeLayout;

    private int keyboardHeight;
    private boolean keyboardVisible;

    private ShopBotChatSession chatSession;

    private EditText.OnKeyListener keyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {

            // If the event is a key-down event on the "enter" button
            if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                // Perform action on key press

                EditText editText = (EditText) v;

                if(v==chatEditText1) {
                    sendMessage(editText.getText().toString(), UserType.OTHER);
                }

                chatEditText1.setText("");

                return true;
            }
            return false;

        }
    };

    private ImageView.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if(v==enterChatView1)
            {
                sendMessage(chatEditText1.getText().toString(), UserType.OTHER);
            }

            chatEditText1.setText("");

        }
    };

    private final TextWatcher watcher1 = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            if (chatEditText1.getText().toString().equals("")) {

            } else {
                enterChatView1.setImageResource(R.drawable.ic_chat_send);

            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if(editable.length()==0){
                enterChatView1.setImageResource(R.drawable.ic_chat_send);
            }else{
                enterChatView1.setImageResource(R.drawable.ic_chat_send_active);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatSession = new ShopBotChatSession();
        new CreateChatSessionTask().execute();

        AndroidUtilities.statusBarHeight = getStatusBarHeight();

        rgroup = (RadioGroup) findViewById(R.id.rgroup);
        rbChat = (RadioButton) findViewById(R.id.rbChat);
        rbSimulation = (RadioButton) findViewById(R.id.rbSimulation);
        rgroup.check(rbChat.getId());  //default selection is Real Chat
        initListener();

        chatMessages = new ArrayList<>();

        chatListView = (ListView) findViewById(R.id.chat_list_view);

        chatEditText1 = (EditText) findViewById(R.id.chat_edit_text1);
        enterChatView1 = (ImageView) findViewById(R.id.enter_chat1);

        listAdapter = new ChatListAdapter(chatMessages, this);

        chatListView.setAdapter(listAdapter);

        chatEditText1.setOnKeyListener(keyListener);

        enterChatView1.setOnClickListener(clickListener);

        chatEditText1.addTextChangedListener(watcher1);

        sizeNotifierRelativeLayout = (SizeNotifierRelativeLayout) findViewById(R.id.chat_layout);
        sizeNotifierRelativeLayout.delegate = this;

    }

    private void initListener() {
        rgroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                int p = radioGroup.indexOfChild((RadioButton) findViewById(i));
                switch(i) {
                    case R.id.rbChat:
                        Log.d(TAG, "Select Real Chat");
                        new CreateChatSessionTask().execute();
                        break;
                    case R.id.rbSimulation:
                        Log.d(TAG, "Select Simulation");
                        new CreateSimulatorTask().execute();
                        break;
                }
            }
        });
    }

    private void sendMessage(final String messageText, final UserType userType)
    {
        if(messageText.trim().length()==0)
            return;

        final ChatMessage message = new ChatMessage();
        message.setMessageStatus(SENT);
        message.setMessageText(messageText);
        message.setUserType(userType);
        message.setMessageTime(new Date().getTime());
        chatMessages.add(message);

        if(listAdapter!=null)
            listAdapter.notifyDataSetChanged();

        if (rgroup.getCheckedRadioButtonId() == R.id.rbChat) {
            new SendMessageTask().execute(message);
        } else {
            new UpdateSimulatorTask().execute(message);
        }

    }

    private Activity getActivity()
    {
        return this;
    }

    private class CreateChatSessionTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... args) {
            return chatSession.createChatSession();
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String chat_id) {
            Log.d(TAG, "ChatID=" + chat_id);
        }
    }

    private class SendMessageTask extends AsyncTask<ChatMessage, Void, String> {
        @Override
        protected String doInBackground(ChatMessage... msgs) {
            try {
                ChatMessage chatMsg = msgs[0];
                String respStr = chatSession.sendMessage(chatMsg.getMessageText());
                chatMsg.setMessageStatus(DELIVERED);
                return respStr;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "No message";
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String message) {

            Log.d(TAG, "message=" + message);

            final ChatMessage chatMsg = new ChatMessage();
            chatMsg.setMessageStatus(SENT);
            chatMsg.setMessageText(message);
            chatMsg.setUserType(UserType.SELF);
            chatMsg.setMessageTime(new Date().getTime());
            chatMessages.add(chatMsg);

            if(listAdapter!=null)
                listAdapter.notifyDataSetChanged();
        }
    }

    private class CreateSimulatorTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... args) {
            return chatSession.createUserSimulator();
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String message) {
            Log.d(TAG, "message" + message);

            sendMessage(message, UserType.OTHER);
        }
    }

    private class UpdateSimulatorTask extends AsyncTask<ChatMessage, Void, String> {
        @Override
        protected String doInBackground(ChatMessage... msgs) {
            try {
                ChatMessage chatMsg = msgs[0];
                String respStr = chatSession.updateSimulator(chatMsg.getMessageText());
                chatMsg.setMessageStatus(DELIVERED);
                return respStr;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "No message";
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String message) {
            Log.d(TAG, "message=" + message);

            final ChatMessage chatMsg = new ChatMessage();
            chatMsg.setMessageStatus(SENT);
            chatMsg.setMessageText(message);
            chatMsg.setUserType(UserType.SELF);
            chatMsg.setMessageTime(new Date().getTime());
            chatMessages.add(chatMsg);

            if(listAdapter!=null)
                listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onSizeChanged(int height) {

        Rect localRect = new Rect();
        getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(localRect);

        WindowManager wm = (WindowManager) App.getInstance().getSystemService(Activity.WINDOW_SERVICE);
        if (wm == null || wm.getDefaultDisplay() == null) {
            return;
        }


        if (height > AndroidUtilities.dp(50) && keyboardVisible) {
            keyboardHeight = height;
            App.getInstance().getSharedPreferences("emoji", 0).edit().putInt("kbd_height", keyboardHeight).commit();
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
    }

    /**
     * Get the system status bar height
     * @return
     */
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    protected void onPause() {
        super.onPause();
        
    }
}
