package network.util;

import jpcap.packet.IPPacket;
import network.windows.Main;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by 11723 on 2017/6/12.
 */
public class Util extends Thread{
    public static final ConcurrentLinkedQueue<IPPacket> ipPack = new ConcurrentLinkedQueue<>();
    public static int allPacksCount = 0;
    public static int shardPacksCount = 0;
    public static int waitMargePacksCount = 0;
    public synchronized static void addWaitMargePacksCount(){
        Util.waitMargePacksCount++;
    }
    public synchronized static void minusWaitMargePacksCount(){
        Util.waitMargePacksCount--;
    }

    private final  Main main;
    public Util(Main _main){
        main = _main;
    }
    @Override
    public void run() {
        while (true){
            try {
                boolean bool;
                IPPacket packet;
                synchronized (ipPack){
                    bool = !Util.ipPack.isEmpty();
                    packet = ipPack.poll();
                }
                if (bool && (main.isFull() || main.isHave(packet.ident))){
                    new Thread(() -> main.addPack(packet)).start();
                    minusWaitMargePacksCount();
                }else if (packet != null){
                    synchronized (ipPack){
                        ipPack.add(packet);
                    }
                }
                Thread.sleep(100);
            } catch (Exception e) {
                System.out.println("休眠失败");
                e.printStackTrace();
            }
        }
    }
} 
