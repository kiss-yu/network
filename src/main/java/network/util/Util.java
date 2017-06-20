package network.util;

import javafx.application.Platform;
import javafx.scene.layout.VBox;
import jpcap.packet.IPPacket;
import network.windows.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by 11723 on 2017/6/12.
 */
public class Util extends Thread{
    public static final ConcurrentLinkedQueue<Map<String,Object>> ipPack = new ConcurrentLinkedQueue<>();
    public static int allPacksCount = 0;
    public static int shardPacksCount = 0;
    public static int waitMargePacksCount = 0;
    public synchronized static void addWaitMargePacksCount(){
        Util.waitMargePacksCount++;
    }
    public synchronized static void minusWaitMargePacksCount(){
        Util.waitMargePacksCount--;
    }

    private static   Main main;
    public Util(Main _main){
        main = _main;
    }
    @Override
    public void run() {
        while (true){
            try {
                boolean bool;
                Map<String,Object> packet;
                synchronized (ipPack){
                    bool = !Util.ipPack.isEmpty();
                    packet = ipPack.poll();
                }
                double k = Math.random()*10;
                if (bool && k < 7){
                    Platform.runLater(() -> main.addBufferBlock(packet));
                    minusWaitMargePacksCount();
                }else if (packet != null){
                    ipPack.add(packet);
                }
                Thread.sleep(500);
            } catch (Exception e) {
                System.out.println("休眠失败");
                e.printStackTrace();
            }
        }
    }

} 
