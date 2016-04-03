package netty.nettyserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.io.UnsupportedEncodingException;
import java.util.Date;

public class TimeServerHandler extends ChannelHandlerAdapter {
    
    private String html = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\""
            + "\"http://www.w3.org/TR/html4/loose.dtd\"><html><head><meta http-equiv"
            + "=\"Content-Type\" content=\"text/html; charset=utf-8\"><title>netty</title>"
            + "</head><body>Hello World!</body></html>";
    
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws UnsupportedEncodingException{
        ByteBuf buf = (ByteBuf)msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String body = new String(req, "UTF-8");
        System.out.println("Time server reeive order : " + body);
        String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? 
                new Date(System.currentTimeMillis()).toString() : html;
        ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
        ctx.write(resp);
    }
    
    public void channelReadComplete(ChannelHandlerContext ctx){
        ctx.flush();
        ctx.close();
    }
    
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable casue){
        ctx.close();
    }

}
