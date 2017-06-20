package network.util;

import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import jpcap.packet.IPPacket;


/**
 * Created by 11723 on 2017/6/20.
 */
public class PaneUtil {

    public static final double BLOCKHEIGHT = 40;

    public static VBox getOneBlock(IPPacket packet){
        double width = (packet.length - 20)/10;
        Text offest = new Text(packet.offset == 0 ? " ident=" + packet.ident + "\n " + packet.offset*8+"" :" " +  packet.offset*8);
        offest.setWrappingWidth(width);
        offest.setTextAlignment(packet.offset == 0 ? TextAlignment.CENTER : TextAlignment.LEFT);
        VBox vBox = new VBox(offest);
        vBox.setPrefWidth(width);
        vBox.setMaxWidth(width);
        vBox.setPrefHeight(PaneUtil.BLOCKHEIGHT);
        vBox.setMaxHeight(PaneUtil.BLOCKHEIGHT);
        vBox.setSpacing(5);
        vBox.setStyle("-fx-border-color: #cf525c;-fx-background-color: #54cfcf");
        return vBox;
    }
}
