package network.pcap;

import javafx.application.Platform;
import javafx.scene.layout.VBox;
import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.packet.*;
import network.util.PaneUtil;
import network.util.Util;
import network.windows.Main;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class Jpcap{
    public final static ThreadLocal<NetworkInterface> card = new ThreadLocal<NetworkInterface>();


    /**
     * 循环抓包
     * */
    private static void getPacket(NetworkInterface networkInterface) throws IOException {
        JpcapCaptor captor = JpcapCaptor.openDevice(networkInterface,65535,false,20);
        while(true){
            Packet tmp=captor.getPacket();//获取数据包
            if(tmp instanceof IPPacket){
                Util.allPacksCount++;
                setMsg();
                if ((!((IPPacket) tmp).more_frag || ((IPPacket) tmp).offset > 0) && !networkInterface.addresses[1].address.equals(((IPPacket) tmp).src_ip)) {
                    add((IPPacket) tmp);
                    Util.shardPacksCount++;
                    Util.addWaitMargePacksCount();
                }
            }
        }
    }




    public static void add(IPPacket packet){
        Platform.runLater(() -> {
            Map<String,Object> map = new HashMap<>();
            VBox vBox = PaneUtil.getOneBlock(packet);
            map.put("pack",packet);
            map.put("vbox",vBox);
            Util.ipPack.add(map);
            Main.addBufferQueue(vBox);
        });
    }


    /**
     *设置网卡
     * */
    public static void setNetworkCard(NetworkInterface networkCard){
        card.set(networkCard);
    }

    /**
     * 获取网卡列表
     * */
    public static NetworkInterface[] getNetworkCards(){
        return JpcapCaptor.getDeviceList();
    }

    public static NetworkInterface getCard(){
        return card.get();
    }

    private static void setMsg(){
        new Thread(() -> Main.setPackMsg()).start();
    }
    public static void start(){
        NetworkInterface networkInterface = card.get();
        new Thread(()->{
            try {
                getPacket(networkInterface);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
