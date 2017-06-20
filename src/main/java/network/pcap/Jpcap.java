package network.pcap;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.packet.*;
import network.util.Util;
import network.windows.Main;

import java.io.IOException;
import java.net.InetAddress;

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
                if ((((IPPacket) tmp).length >= 1500 || ((IPPacket) tmp).offset > 0) && !networkInterface.addresses[1].address.equals(((IPPacket) tmp).src_ip)) {
                    Util.ipPack.add((IPPacket) tmp);
                    Util.shardPacksCount++;
                    Util.addWaitMargePacksCount();
                }
            }
        }
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
