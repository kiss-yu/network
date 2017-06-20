package network.windows;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import jpcap.NetworkInterface;
import jpcap.packet.ICMPPacket;
import jpcap.packet.IPPacket;
import network.pcap.Jpcap;
import network.util.Sort;
import network.util.Util;

import javax.print.attribute.standard.Finishings;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Created by 11723 on 2017/6/12.
 */
public class Main extends Application {
    final Pane ROOT_PANE = new Pane();
    final Text bufferPool = new Text();
    final FlowPane bufferBlock = new FlowPane();
    public static final ConcurrentHashMap<Integer,VBox> bufferMaps = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<Integer,List<IPPacket>> packList = new ConcurrentHashMap<>();
    static final ConcurrentLinkedQueue<Integer> emptyLocation = new ConcurrentLinkedQueue<>();
    public static final Text allPackMsg = new Text("0");
    public static final Text shardPacksMsg = new Text("0");
    public static final Text waitMargePackMsg = new Text("0");
    public static final Text finshMargePackMsg = new Text("0");
    public void start(Stage primaryStage) throws Exception {
        _init();
        primaryStage.setTitle("网络协议");
        primaryStage.setScene(new Scene(ROOT_PANE, 1240, 1000));
        primaryStage.show();
        new Util(this).start();//开启util线程
    }

    /**
     * 初始换控件
     * */
    private void _init() throws UnknownHostException {
        bufferBlock.setLayoutX(400);
        bufferBlock.setLayoutY(60);
        bufferBlock.setPrefWidth(820);
        bufferBlock.setMaxHeight(900);
        bufferBlock.setHgap(10);
        bufferPool.setWrappingWidth(820);
        bufferPool.setLayoutX(400);
        bufferPool.setFill(Color.BLUE);
        bufferPool.setFont(Font.font(40));
        bufferPool.setLayoutY(40);
        bufferPool.setTextAlignment(TextAlignment.CENTER);
        Button start = new Button("开始抓包");
        start.setLayoutX(160);
        start.setLayoutY(80);
        showPackMsg();
        for (int i = 0;i < 4;i ++) {
            addBufferBlock(String.valueOf(i));
            addEmptyBuffer(i);
        }
        ROOT_PANE.getChildren().addAll(bufferPool,bufferBlock,start,showNetworkCards());
        start.setOnMouseClicked(event -> {
            if (Jpcap.getCard() == null) showMessageDialog("未选择网卡","错误");
            else{
                Jpcap.start();
            }
        });
    }

    /**
     * 空出一个缓存空区
     * */
    private synchronized void addEmptyBuffer(int i){
        synchronized (emptyLocation){
            for (Integer integer:emptyLocation){
                if (integer == i)
                    return;
            }
            emptyLocation.add(i);
        }
        String str = "缓存池--空区块有";
        for (Integer integer:emptyLocation){
            str += integer + "区  ";
        }
        bufferPool.setText(str);
    }
    /**
     * 取出一个缓存空的区
     * */
    private synchronized Integer removeEmptyBuffer(){
        Integer index;
        synchronized (emptyLocation){
           index = emptyLocation.poll();
        }
        String str = "缓存池--空区块有";
        for (Integer integer:emptyLocation){
            str += integer + "区  ";
        }
        bufferPool.setText(str);
        return index;
    }

    /**
     * 更改一个缓存区的内容
     * */
    public  void changBufferContent(IPPacket packet){
        VBox vBox;
        synchronized (bufferMaps){
            vBox = bufferMaps.get(packet.ident);
        }
        for (Node node:vBox.getChildren()){
            if (node instanceof Text && node.getId().equals("content")){
                Map<String,Object> map = getBytes(packet);
                if ((Boolean) map.get("type")) {
                    ((Text) node).setFill(Color.GREEN);
                        addEmptyBuffer(Integer.valueOf(vBox.getId()));
                }
                else
                    ((Text) node).setFill(Color.BLACK);
                ((Text) node).setText((String) map.get("string"));
            }
        }
    }

    /**
     * 输出list里的ip数据包的数据
     * */
    private  Map<String,Object> getBytes(IPPacket packet){
        List<IPPacket> list;
        synchronized (packList){
            list = packList.get(packet.ident);
        }
        IPPacket[] ipPackets = new IPPacket[list.size()];
        for (int i = 0;i < ipPackets.length;i++)
            ipPackets[i] = list.get(i);
        StringBuffer stringBuffer = new StringBuffer("          统计信息如下\n接收分片顺序：\n");
        ipPackets = Sort.sort(ipPackets);
        boolean type = isCompleteIpPacket(ipPackets);
        for (IPPacket ipPacket:list){
            stringBuffer.append("(" + ipPacket.offset*8 + " - " + (ipPacket.offset*8 + ipPacket.length - 20) + ") --> ");
        }
        stringBuffer.append("\n\n重组如下");
        String str="";
        for (IPPacket ipPacket:ipPackets){
            str += "(" + (ipPacket.offset*8) + " - " + (ipPacket.offset*8 + ipPacket.length - 20) + ") --> ";
        }
        str += "OK";
        stringBuffer.append(str.replaceFirst(" --> OK",""));
        if (type){
            stringBuffer.append("\n\n重组后的数据如下：");
            for (IPPacket packet1:ipPackets){
                for (byte by:packet1.data)
                stringBuffer.append((char) by);
            }
        }else stringBuffer.append("未完");
        Map<String,Object> map = new HashMap<>();
        map.put("type",type);
        map.put("string",stringBuffer.toString());
        return map;
    }

    /**
     * 判断列ip数据包是否完整
     * */
    private boolean isCompleteIpPacket(IPPacket[] lists){
        int k = 0;
        for (IPPacket packet : lists) {
            if (packet.offset * 8 != k) return false;
            if (packet.length < 1500) return true;
            k += packet.length - 20;
        }
        return false;
    }

    /**
     * 在一个缓存区添加一个ip数据包 在list中排序取出在设置text内容
     * */
    public void addPack(IPPacket packet){
        boolean bool;
        synchronized (bufferMaps){
            bool = bufferMaps.containsKey(packet.ident);
        }
        if (bool){
            synchronized (packList){
                packList.get(packet.ident).add(packet);
            }
            changBufferContent(packet);
        }else {
            synchronized (bufferMaps) {
                bufferMaps.put(packet.ident, writeBuffer(packet));
                changBufferContent(packet);
            }
        }
    }



    /**
     * 在一个空的缓存区写入内容
     * */
    private VBox writeBuffer(IPPacket packet){
        VBox vBox = null;
        String id = String.valueOf(removeEmptyBuffer());
        for (Node node:bufferBlock.getChildren()){
            if (node instanceof ScrollPane && node.getId().equals(id)){
                vBox = (VBox) ((ScrollPane) node).getContent();
                node.setStyle("-fx-border-color: #cf5b11");
                List<IPPacket> lists = new ArrayList<>();
                lists.add(packet);
                synchronized (packList){
                    packList.put(packet.ident,lists);
                }
                for (Node text:((VBox)(((ScrollPane) node).getContent())).getChildren()){
                    if (text instanceof Text){
                        if (text.getId().equals("msg")) {
                            ((Text) text).setText(packet.src_ip.getHostAddress() + "的缓存区");
                        }
                    }
                }
            }
        }
        return vBox;
    }



    /**
     * 添加一个缓存区
     * */
    private void addBufferBlock(String id){
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setPrefWidth(400);
        VBox buffer = new VBox();
        scrollPane.setId(id);
        buffer.setId(id);
        buffer.setPrefWidth(380);
        buffer.setPrefHeight(450);
        buffer.setLayoutX(10);
        buffer.setLayoutY(10);
        Text msg = new Text();
        msg.setId("msg");
        msg.setWrappingWidth(380);
        msg.setFont(Font.font(22));
        msg.setFill(Color.RED);
        msg.setTextAlignment(TextAlignment.CENTER);
        Text content = new Text();
        content.setId("content");
        content.setWrappingWidth(380);
        buffer.getChildren().addAll(msg,content);
        scrollPane.setContent(buffer);
        bufferBlock.getChildren().add(scrollPane);
    }

    /**
     * 显示网卡列表多选框
     * */
    private ChoiceBox showNetworkCards(){
        NetworkInterface[] devies = Jpcap.getNetworkCards();
        ChoiceBox<String> choiceBox = new ChoiceBox();
        String[] names = new String[devies.length];
        for (int i = 0;i < names.length;i ++)
            names[i] = devies[i].description;
        choiceBox.setItems(FXCollections.observableArrayList(names));
        choiceBox.setValue(names[0]);
        Jpcap.setNetworkCard(devies[0]);
        choiceBox.setLayoutX(20);
        choiceBox.setLayoutY(20);
        choiceBox.setPrefWidth(360);
        choiceBox.setOnAction(event -> {
            for (NetworkInterface networkInterface:devies)
                if (networkInterface.description.equals(choiceBox.getValue())) {
                    Jpcap.setNetworkCard(networkInterface);
                    return;
            }
        });
        return choiceBox;
    }


    /**
     * 更新抓包信息
     * */
    public static void setPackMsg(){
        synchronized (allPackMsg){
            allPackMsg.setText(String.valueOf(Util.allPacksCount));
        }
        synchronized (shardPacksMsg){
            shardPacksMsg.setText(String.valueOf(Util.shardPacksCount));
        }
        synchronized (waitMargePackMsg){
            waitMargePackMsg.setText(String.valueOf(Util.waitMargePacksCount));
        }
        synchronized (finshMargePackMsg){
            finshMargePackMsg.setText(String.valueOf(Util.shardPacksCount - Util.waitMargePacksCount));
        }
    }

    /**
     * 显示抓包信息
     * */
    private void showPackMsg(){
        Text one = new Text("已获取的ip数据包：");
        one.setWrappingWidth(200);
        one.setTextAlignment(TextAlignment.RIGHT);
        allPackMsg.setTextAlignment(TextAlignment.LEFT);
        allPackMsg.setWrappingWidth(150);
        HBox OneHBox = new HBox(one,allPackMsg);
        OneHBox.setSpacing(10);
        Text two = new Text("获取到的分片数据包：");
        two.setWrappingWidth(200);
        two.setTextAlignment(TextAlignment.RIGHT);
        shardPacksMsg.setTextAlignment(TextAlignment.LEFT);
        shardPacksMsg.setWrappingWidth(150);
        HBox twoHBox = new HBox(two,shardPacksMsg);
        twoHBox.setSpacing(10);

        Text three = new Text("等待重组的分片数据包：");
        three.setWrappingWidth(200);
        three.setTextAlignment(TextAlignment.RIGHT);
        waitMargePackMsg.setTextAlignment(TextAlignment.LEFT);
        waitMargePackMsg.setWrappingWidth(150);
        HBox threeHBox = new HBox(three,waitMargePackMsg);
        threeHBox.setSpacing(10);

        Text four = new Text("完成重组的分片数据包：");
        four.setWrappingWidth(200);
        four.setTextAlignment(TextAlignment.RIGHT);
        finshMargePackMsg.setTextAlignment(TextAlignment.LEFT);
        finshMargePackMsg.setWrappingWidth(150);
        HBox fourHBox = new HBox(four,finshMargePackMsg);
        fourHBox.setSpacing(10);

        VBox vBox = new VBox(OneHBox,twoHBox,threeHBox,fourHBox);
        vBox.setSpacing(10);
        vBox.setLayoutY(150);
        vBox.setLayoutX(10);
        vBox.setPrefWidth(380);
        ROOT_PANE.getChildren().addAll(vBox);
    }


    public boolean isFull(){
        boolean bool;
        synchronized (emptyLocation){
            bool = !emptyLocation.isEmpty();
        }
        return bool;
    }

    public static void showMessageDialog( String message, String title) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setResizable(false);
        Text text = new Text(message);
        text.setWrappingWidth(200);
        text.setTextAlignment(TextAlignment.CENTER);
        Button ok = new Button("确定");
        BorderPane vBox = new BorderPane();
        vBox.setTop(text);
        vBox.setCenter(ok);
        stage.setScene(new Scene(vBox, 200, 100));
        stage.show();
        ok.setOnMouseClicked(event -> stage.close());
    }
    public boolean isHave(int ident){
        synchronized (bufferMaps){
            return bufferMaps.containsKey(ident);
        }
    }
    public static void main(String[] args) {
        launch(args);
    }
}
