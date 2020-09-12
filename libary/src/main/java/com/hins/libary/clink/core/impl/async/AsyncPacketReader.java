package com.hins.libary.clink.core.impl.async;

import com.hins.libary.clink.core.Frame;
import com.hins.libary.clink.core.IoArgs;
import com.hins.libary.clink.core.SendPacket;
import com.hins.libary.clink.core.ds.BytePriorityNode;
import com.hins.libary.clink.frames.AbstractSendPacketFrame;
import com.hins.libary.clink.frames.CancelSendFrame;
import com.hins.libary.clink.frames.SendEntityFrame;
import com.hins.libary.clink.frames.SendHeaderFrame;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author: hins
 * @created: 2020-09-09 21:13
 * @desc:
 **/
public class AsyncPacketReader implements Closeable {
    private final PacketProvier packetProvier;
    private volatile IoArgs ioArgs = new IoArgs();

    private volatile BytePriorityNode<Frame> node;
    private volatile int nodeSize = 0;

    // 1,2,3....255
    private short lastIdentifier = 0;


    AsyncPacketReader(PacketProvier packetProvier) {
        this.packetProvier = packetProvier;
    }

    /**
     * 取消Packet对应的发送，如果当前Packet已发送部分数据(就算只是头数据)
     * 也应该在当前帧队列中发送一份取消发送的标志{@link "CancelSendFrame"}
     *
     * @param sendPacket 待取消的packet
     */
    synchronized void cancel(SendPacket sendPacket) {

            if(nodeSize == 0){
                return ;
            }


            for(BytePriorityNode<Frame> x = node, before = null; x != null; before = x, x = x.next){

                Frame frame = x.item;
                if(frame instanceof AbstractSendPacketFrame){
                    AbstractSendPacketFrame packetFrame = (AbstractSendPacketFrame) frame;
                    if(packetFrame.getPacket() == packetProvier){
                        // 是当前包才触发终止操作
                        boolean removeable = packetFrame.abort();
                        if(removeable){
                            //A B C
                            removeFrame(x, before);
                            if(packetFrame instanceof SendHeaderFrame){
                                // 头帧，并且未被发送任何数据,直接取消后不需要添加取消发送帧
                                break;
                            }
                        }

                        // 添加终止帧, 通知接收方
                        CancelSendFrame cancelSendFrame = new CancelSendFrame(packetFrame.getBodyIdentifier());
                        appendNewFrame(cancelSendFrame);

                        // 意外终止,返回失败
                        packetProvier.completedPacket(sendPacket, false);

                        break;
                    }

                }

            }
    }

    /**
     * 请求从 {@link #packetProvier} 队列中拿一份Packet进行发送
     * @return 如果当前Reader中有可用于网络发送的数据,则返回 True
     */
    boolean requestTakePacket() {

        synchronized (this) {
            // 当帧队列的size大于1时
            // 表示肯定有数据可以发送
            if(nodeSize >= 1){
                return true;
            }
        }

        SendPacket packet = packetProvier.takePacket();
        if(packet != null){
            short identifier = generateIdentifier();
            SendHeaderFrame frame = new SendHeaderFrame(identifier, packet);
            appendNewFrame(frame);
        }

        synchronized (this) {
            return nodeSize != 0;
        }
    }

    /**
     * 填充数据到IoArgs
     * @return
     */
    IoArgs fillData() {

        Frame currentFrame = getCurrentFrame();
        if(currentFrame == null){
            return null;
        }

        try {
            if(currentFrame.handle(ioArgs)){
                // 消费完本帧
                // 尝试基于本帧构建后续帧
                Frame nextFrame = currentFrame.nextFrame();
                if(nextFrame != null){
                    appendNewFrame(nextFrame);
                }else if(currentFrame instanceof SendEntityFrame){
                    // 末尾实体帧
                    // 通知完成
                    packetProvier.completedPacket(((SendEntityFrame) currentFrame).getPacket(),
                            true);
                }
                // 从链头弹出
                popCurrentFrame();
            }
            return ioArgs;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public synchronized void close() {

        while (node != null){
            Frame frame = node.item;
            if( frame instanceof AbstractSendPacketFrame){
                SendPacket packet = ((AbstractSendPacketFrame) frame).getPacket();
                packetProvier.completedPacket(packet, false);
            }
        }

        nodeSize = 0;
        node = null;
    }


    private synchronized void appendNewFrame(Frame frame) {

        BytePriorityNode<Frame> newNode = new BytePriorityNode<>(frame);
        if(newNode != null){
            // 使用优先级别添加到链表
            node.appendWithPriority(newNode);
        } else {
            node = newNode;
        }

        nodeSize++;
    }

    private synchronized Frame getCurrentFrame() {
        if(node == null){
            return null;
        }

        return node.item;
    }

    private synchronized void popCurrentFrame() {

        node = node.next;
        nodeSize--;
        if(node == null){
            requestTakePacket();
        }
    }

    private void removeFrame(BytePriorityNode<Frame> removeNode, BytePriorityNode<Frame> before) {
        if(before == null){
            // A B C
            // B C
            node = removeNode.next;
        } else {
            // A B C
            // A C
            before.next = removeNode.next;
        }
        nodeSize--;
        if(node == null){
            requestTakePacket();
        }
    }


    private short generateIdentifier(){
        short identifier = ++lastIdentifier;
        if(identifier == 255){
            this.lastIdentifier = 0;
        }
        return identifier;
    }


    interface PacketProvier{

        SendPacket takePacket();

        void completedPacket(SendPacket sendPacket, boolean isSucceed);
    }
}
