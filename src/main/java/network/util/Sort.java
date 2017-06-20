package network.util;


import jpcap.packet.IPPacket;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

public class Sort {

    public static IPPacket[] sort(IPPacket[] packets){
        for(int i = 1;i < packets.length; i ++){
            IPPacket k = packets[i];
            int j = i;
            for (;j > 0 && packets[j - 1].offset > k.offset;j--)
                packets[j] = packets[j - 1];
            packets[j] = k;
        }
        return packets;
    }

    public static void main(String[] args) {
        byte by = 0x19;
        System.out.println(Integer.toHexString((int)by));

    }
}


