package com.example.mehdi.wsnandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    MQTTHelper mqttHelper;
    private TextView internetStatusTextView, funCodeTextView, resfuncCodeTextView, nodeTypeTextView, reqValueTextView;
    private Button buttonPublish;
    private EditText nodeIDEditText, pinNumberEditText;
    private String reqFunCode,reqNodeType, reqNodeID,reqPinNumber,reqMsg;
    private String resPayload,resFunCode,
            resNodeType, resNodeID,resMsg, resValues, resLable, requestMsgHex, responseMsgHex;
    
    private String[] resValuesArray;
    int resFunCodeDigit = 2;
    int resNodeTypeDigit = 1;
    int resNodeIDDigit = 3;
    int resMsgDigit = 4;
    
    private char[] resFunCodeChar = new char[resFunCodeDigit];
    private char[] resNodeTypeChar = new char[resNodeTypeDigit];
    private char[] resNodeIDChar = new char[resNodeIDDigit];
    private char[] resMsgChar = new char[resMsgDigit];

    SharedPreferences intConnectionSatus;

    ExpandableListView funCodeExpandableView,nodeTypeExpandableView;

    List<String> ChildListFC,ChildListNT;
    Map<String, List<String>> ParentListItemsFC,ParentListItemsNT;
    // Assign Parent list items here.
    List<String> ParentListFC = new ArrayList<String>();
    { ParentListFC.add("Function");}

    List<String> ParentListNT = new ArrayList<String>();
    { ParentListNT.add("Node Type");}

    String [] childNamesFC= {"Flash LED" , "Read Pin" , "Read Temp" , "Write Pin"};

    String [] childNamesNT= {"Router" , "End Device"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intConnectionSatus = getSharedPreferences("connStatus", Context.MODE_PRIVATE);

        ParentListItemsFC = new LinkedHashMap<String, List<String>>();

        for ( String HoldItem : ParentListFC) {
            if (HoldItem.equals("Function")) {
                loadChildFC(childNamesFC);
            }
            ParentListItemsFC.put(HoldItem, ChildListFC);
        }

        ParentListItemsNT = new LinkedHashMap<String, List<String>>();

        for ( String HoldItem : ParentListNT) {
            if (HoldItem.equals("Node Type")) {
                loadChildNT(childNamesNT);
            }
            ParentListItemsNT.put(HoldItem, ChildListNT);
        }


        funCodeExpandableView = (ExpandableListView) findViewById(R.id.funCodeELV);
        nodeTypeExpandableView = (ExpandableListView) findViewById(R.id.nodeTypeELV);

        internetStatusTextView = (TextView) findViewById(R.id.intStatus);
        funCodeTextView = (TextView) findViewById(R.id.funCodeText);
        nodeTypeTextView = (TextView) findViewById(R.id.nodeTypeText);
        nodeIDEditText = (EditText) findViewById(R.id.NodIDEditText);
        pinNumberEditText = (EditText) findViewById(R.id.PinNoEditText);

        buttonPublish = (Button) findViewById(R.id.buttonPublish);
        buttonPublish.setOnClickListener(this);

        resfuncCodeTextView = (TextView) findViewById(R.id.dataReceivedLabel);
        reqValueTextView = (TextView) findViewById(R.id.dataReceived);
        reqValueTextView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        final ExpandableListAdapter funCodeExpandableAdapter = new MyExpandableAdapter(this, ParentListFC, ParentListItemsFC);
        funCodeExpandableView.setAdapter(funCodeExpandableAdapter);

        final ExpandableListAdapter nodeTypeExpandableAdapter = new MyExpandableAdapter(this, ParentListNT, ParentListItemsNT);
        nodeTypeExpandableView.setAdapter(nodeTypeExpandableAdapter);


        funCodeExpandableView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                // TODO Auto-generated method stub

                final String selectedFC = (String) funCodeExpandableAdapter.getChild(
                        groupPosition, childPosition);

                switch (selectedFC) {
                    case "Flash LED":
                        reqFunCode = "01";

                        funCodeTextView.setText("Flash LED");
                        nodeIDEditText.setEnabled(true);
                        pinNumberEditText.setEnabled(true);
                        break;
                    case "Read Pin":
                        reqFunCode = "02";
                        funCodeTextView.setText("Read Pin Status");
                        nodeIDEditText.setEnabled(true);
                        pinNumberEditText.setEnabled(true);
                        break;

                    case "Read Temp":
                        reqFunCode = "03";
                        funCodeTextView.setText("Read Local Temperature");
                        nodeIDEditText.setEnabled(true);
                        pinNumberEditText.setEnabled(false);
                        break;
                    case "Write Pin":
                        reqFunCode = "04";
                        funCodeTextView.setText("Write Pin");
                        nodeIDEditText.setEnabled(true);
                        pinNumberEditText.setEnabled(true);
                        break;

                }
                return true;
            }
        });

        nodeTypeExpandableView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                // TODO Auto-generated method stub

                final String selectedNT = (String) nodeTypeExpandableAdapter.getChild(
                        groupPosition, childPosition);

                switch (selectedNT) {
                    case "Router":
                        reqNodeType = "R";
                        nodeTypeTextView.setText("Router");
                        break;
                    case "End Device":
                        reqNodeType = "E";
                        nodeTypeTextView.setText("End Device");
                        break;
                }
                return true;
            }
        });

        internetStatusTextView.setText("Disconnected");
        internetStatusTextView.setTextColor(0xffff4444);

        startMqtt();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == buttonPublish.getId()) {

            reqNodeID = String.format("%3S", nodeIDEditText.getText().toString().trim()).replace(' ', '0');

            reqMsg = reqFunCode + reqNodeType + reqNodeID;

            if (reqFunCode == "01" || reqFunCode == "02" || reqFunCode == "04") {
                reqPinNumber = pinNumberEditText.getText().toString().trim();
                reqMsg = reqMsg + reqPinNumber;
            }

            requestMsgHex = convertStringToHex(reqMsg);

            Log.d("Result", reqMsg);

            if (reqNodeID.equals("") || reqNodeID.equals(null) || reqNodeType.equals("") || reqNodeType.equals(null) ||
                    ((reqFunCode == "01" || reqFunCode == "02" || reqFunCode == "04") && (reqPinNumber.equals("") || reqPinNumber.equals(null)))) {
                Toast.makeText(getApplicationContext(), "Please Specify All The Items!", Toast.LENGTH_LONG).show();
            } else {
                try {
                    mqttHelper.publishMessage(reqMsg, 1, "android2Node");
                    Toast.makeText(getApplicationContext(), "Sending ...", Toast.LENGTH_SHORT).show();
                    nodeIDEditText.setText("");
                    pinNumberEditText.setText("");
                    reqValueTextView.setText("");

                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startMqtt() {
        mqttHelper = new MQTTHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Toast.makeText(getApplicationContext(), "Connected To Broker!", Toast.LENGTH_LONG).show();
                // Check Internet Connection
                try{
                    mqttHelper.publishMessage("99R0000200", 1, "android2Node");
                }catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Toast.makeText(getApplicationContext(), "Could Not Connect To Broker!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {

                //responseMsgHex = mqttMessage.toString();
                //responseMsg = convertHexToString(responseMsgHex);
                resPayload = mqttMessage.toString();

                resValuesArray = new String[2];
                Log.w("Debug", resPayload);
                //Log.w("Length ",Integer.toString(lengthofResponse));

                resPayload.getChars(0, resFunCodeDigit, resFunCodeChar, 0);
                resPayload.getChars(resFunCodeDigit, resFunCodeDigit + resNodeTypeDigit, resNodeTypeChar, 0);
                resPayload.getChars(resFunCodeDigit + resNodeTypeDigit, resFunCodeDigit + resNodeTypeDigit + resNodeIDDigit, resNodeIDChar, 0);
                resPayload.getChars(resFunCodeDigit + resNodeTypeDigit + resNodeIDDigit, resFunCodeDigit + resNodeTypeDigit + resNodeIDDigit + resMsgDigit, resMsgChar, 0);

                resFunCode = new String(resFunCodeChar);
                resNodeType = new String(resNodeTypeChar);
                resNodeID = new String(resNodeIDChar);
                resMsg = new String(resMsgChar);

                Log.w("FunCode", resFunCode);
                Log.w("Node Type", resNodeType);
                Log.w("Node ID", resNodeID);
                Log.w("Message", resMsg);

                reqValueTextView.setText("");

                if (resNodeType.equals("R")) {
                    resLable = String.format("%-20s%17s\n", "Router", resNodeID);
                } else if (resNodeType.equals("E")) {
                    resLable = String.format("%-20s%17s\n", "End Device", resNodeID);
                }

                if (resFunCode.equals("99") && resMsg.equals("0201")){
                    internetStatusTextView.setText("Connected");
                    internetStatusTextView.setTextColor(0xff99cc00);
                } else if (resFunCode.equals("99") && resMsg.equals("0101")) {
                    reqValueTextView.setText("Error No. 1\nFailed to Connect Serial Port!");
                } else if (resFunCode.equals("99") && resMsg.equals("0102")) {
                    reqValueTextView.setText("Error No. 2\nCould Not Find The Device!");
                } else if (resFunCode.equals("99") && resMsg.equals("0103")) {
                    reqValueTextView.setText("Error No. 3\nCould Not Send The Data To The Device!");
                }else
                {
                    resValues = resLable + resMsg;
                    reqValueTextView.setText(resValues);
                }

                switch (resFunCode) {
                    case "02":
                        resfuncCodeTextView.setText("Pin Status ");
                        break;
                    case "04":
                        resfuncCodeTextView.setText("Local Temperature (C) ");
                        break;
                    case "99":
                        resfuncCodeTextView.setText("Error ");
                        break;
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

    private void loadChildFC(String[] ParentElementsName) {
        ChildListFC = new ArrayList<String>();
        for (String model : ParentElementsName)
            ChildListFC.add(model);
    }

    private void loadChildNT(String[] ParentElementsName) {
        ChildListNT = new ArrayList<String>();
        for (String model : ParentElementsName)
            ChildListNT.add(model);
    }

    public String convertStringToHex(String str){

        char[] chars = str.toCharArray();

        StringBuffer hex = new StringBuffer();
        for(int i = 0; i < chars.length; i++){
            hex.append(Integer.toHexString((int)chars[i]));
        }

        return hex.toString();
    }

    public String convertHexToString(String hex){

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for( int i=0; i<hex.length()-1; i+=2 ){

            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char)decimal);

            temp.append(decimal);
        }
        System.out.println("Decimal : " + temp.toString());

        return sb.toString();
    }
}
