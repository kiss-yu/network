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
import network.util.PaneUtil;
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
    public static final ConcurrentHashMap<Integer,Pane> bufferMaps = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<Integer,List<IPPacket>> packList = new ConcurrentHashMap<>();
    public static final Text allPackMsg = new Text("0");
    public static final Text shardPacksMsg = new Text("0");
    public static final Text waitMargePackMsg = new Text("0");
    public static final Text finshMargePackMsg = new Text("0");
    static final VBox listVbox = new VBox();
    public static VBox blockQueue = new VBox();


    /**
     * 添加一个包显示在缓存队列中
     * */
    public static void addBufferQueue(VBox vBox){
        blockQueue.getChildren().add(vBox);
    }

    public void start(Stage primaryStage) throws Exception {
        _init();
        primaryStage.setTitle("网络协议");
        primaryStage.setScene(new Scene(ROOT_PANE, 1500, 600));
        primaryStage.show();
        new Util(this).start();//开启util线程
    }

    public static void removeBufferQueue(VBox vBox){
        blockQueue.getChildren().remove(vBox);
    }

    /**
     * 初始换控件
     * */
    private void _init() throws UnknownHostException {
        blockQueue.setSpacing(5);
        ScrollPane scrollPane = new ScrollPane(blockQueue);
        scrollPane.setStyle("-fx-border-color: #8dcfb7");
        scrollPane.setPrefWidth(170);
        scrollPane.setPrefHeight(580);
        scrollPane.setLayoutX(400);
        scrollPane.setLayoutY(10);
        Button start = new Button("开始抓包");
        start.setLayoutX(160);
        start.setLayoutY(80);
        showPackMsg();
        listVbox.setSpacing(10);
        ScrollPane list = new ScrollPane(listVbox);
        list.setLayoutX(580);
        list.setPrefWidth(900);
        list.setPrefHeight(580);
        ROOT_PANE.getChildren().addAll(scrollPane,list,start,showNetworkCards());
        start.setOnMouseClicked(event -> {
            if (Jpcap.getCard() == null) showMessageDialog("未选择网卡","错误");
            else{
                Jpcap.start();
            }
        });
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


    public void addBufferBlock(Map<String,Object> map){
        Pane pane;
        synchronized (bufferMaps){
            pane = bufferMaps.get(((IPPacket)map.get("pack")).ident);
        }
        addBufferBlock(map,pane);
    }

    /**
     * 新增一行分组
     * */
    private void addBufferBlock(Map<String,Object> map,Pane pane){
        if (pane == null){
            pane = new Pane();
            pane.setPrefHeight(PaneUtil.BLOCKHEIGHT);
//            pane.setStyle("-fx-border-color: #54cfcf");
            packList.put(((IPPacket)map.get("pack")).ident,new ArrayList<>());
            synchronized (bufferMaps){
                bufferMaps.put(((IPPacket)map.get("pack")).ident,pane);
            }
            synchronized (listVbox){
                listVbox.getChildren().add(pane);
            }
        }
        VBox vBox = (VBox) map.get("vbox");
        IPPacket packet = (IPPacket) map.get("pack");
        vBox.setLayoutX(packet.offset*8/10);
        vBox.setLayoutY(1);
        packList.get(((IPPacket)map.get("pack")).ident).add((IPPacket) map.get("pack"));
        List<IPPacket> list = packList.get(((IPPacket)map.get("pack")).ident);
        IPPacket[] packets = new IPPacket[list.size()];
        for (int i = 0;i < list.size();i ++)
            packets[i] = list.get(i);
        Sort.sort(packets);
        if (isCompleteIpPacket(packets)){
            pane.getChildren().add(setOk(packets[packets.length - 1]));
        }
        pane.getChildren().add(vBox);
    }

    private Text setOk(IPPacket ipPacket){
        Text ok = new Text("重组完成");
        ok.setLayoutY(20);
        ok.setLayoutX((ipPacket.offset * 8 + (ipPacket.length - 20))/10 + 5);
        ok.setFont(Font.font(20));
        ok.setFill(Color.GREEN);
        return ok;
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
    public static void main(String[] args) {
        launch(args);
    }
}
