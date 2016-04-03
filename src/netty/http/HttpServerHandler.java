package netty.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class HttpServerHandler  extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    private String html = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\""
            + "\"http://www.w3.org/TR/html4/loose.dtd\"><html><head><meta http-equiv"
            + "=\"Content-Type\" content=\"text/html; charset=utf-8\"><title>netty</title>"
            + "</head><body>Hello World!</body></html>";

    @Override
    protected void messageReceived(ChannelHandlerContext ctx,
            FullHttpRequest request) throws Exception {
        
        System.out.println("receive message : " + request.content());
        ByteBuf resp = Unpooled.copiedBuffer(html.getBytes());
        DefaultFullHttpResponse response=new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.ACCEPTED, resp); 
        response.headers().set("CONTENT_TYPE","text/html;chatset=UTF-8");
        ChannelFuture sendFileFutrue = ctx.writeAndFlush(response);     
    }

}
