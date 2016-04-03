package netty.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

public class HttpFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    private final String url;
    
    public HttpFileServerHandler(String url) {
        this.url = url;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx,
            FullHttpRequest request) throws Exception {
        System.out.println(request);
        if (!request.decoderResult().isSuccess()) {
            //sendE
            return;
        }
        
        if (request.method() != HttpMethod.GET) {
            return;
        }
        
        final String uri = request.uri();
        final String path = sanitizeUri(uri);
        
        if (path == null) {
            System.out.println("path is null");
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }
        
        File file = new File(path);
        if (file.isHidden() || !file.exists()) {
            System.out.println("file not found");
            return;
        }
        
        if (file.isDirectory()) {
            if (uri.endsWith("/")) {
                sendListing(ctx, file);
            }else {
                sendRedirect(ctx, uri + '/');
            }
            return;
        }
        
        if (!file.isFile()) {
            sendError(ctx, HttpResponseStatus.FORBIDDEN);
            return;
        }
        
        RandomAccessFile randomAccessFile = null;
        
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
        } catch (Exception e) {
            // TODO: handle exception
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }
        
        long fileLength = randomAccessFile.length();
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().setInt("Content-Length", (int) fileLength);
        setContentTypeHeader(response, file);
        ctx.write(response);
        
        ChannelFuture sendFileFutrue;
        sendFileFutrue = ctx.write(new ChunkedFile(randomAccessFile, 0, fileLength, 8192), ctx.newProgressivePromise());
        sendFileFutrue.addListener(new ChannelProgressiveFutureListener() {
            
            @Override
            public void operationComplete(ChannelProgressiveFuture arg0)
                    throws Exception {
                System.out.println("transfer complete......");
            }
            
            @Override
            public void operationProgressed(ChannelProgressiveFuture arg0, long progress,
                    long total) throws Exception {
                if (total < 0) {
                    System.out.println("transfer progress : " + progress);
                }else {
                    System.out.println("transfer progress :" + progress + "/"+total);
                }
                
            }
        });
        
        ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
    }
    
    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
    
    private String sanitizeUri(String uri){
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (Exception e) {
            try {
                uri = URLDecoder.decode(uri, "ISO-8859-1");
            } catch (Exception e2) {
                // TODO: handle exception
            }
        }
        
        if (!uri.startsWith(url)) {
            return null;
        }
        if (!uri.startsWith("/")) {
            return null;
        }
        
        uri = uri.replace('/', File.separatorChar);
        
        if (uri.contains(File.separator + '.') || uri.contains("." + File.separator)
                || uri.startsWith(".") || uri.endsWith(".") || INSECURE_URI.matcher(uri).matches()) {
            return null;
        }
        
        return System.getProperty("user.dir") + File.separator + uri;
    }
    
    
    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");
    
    private static void sendListing(ChannelHandlerContext ctx, File dir){
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set("CONTENT_TYPE","text/html;chatset=UTF-8");
        StringBuffer buf = new StringBuffer();
        
        String dirPath = dir.getPath();
        
        buf.append("<!DOCTYPE html>\r\n");
        buf.append("<html><head><title>");
        buf.append(dirPath);
        buf.append("目录:");
        buf.append("</title></head><body>\r\n");
        buf.append("<h3>");
        buf.append(dirPath).append(" 目录: ");
        buf.append("</h3>\r\n");
        buf.append("<ul>");
        buf.append("<li>链接: <a href=\"../\">..</a><.li>\r\n");
        
        for(File f : dir.listFiles()){
            if (f.isHidden() || !f.canRead()) {
                continue;
            }
            String name = f.getName();
            if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
                continue;
            }
            
            buf.append("<li>链接： <a href=\"");
            buf.append(name);
            buf.append("\">");
            buf.append(name);
            buf.append("</a></li>\r\n");
        }
        buf.append("</ul></body></html>\r\n");
        
        ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
       // ctx.close();
    }
    
    private static void sendRedirect(ChannelHandlerContext ctx, String newUri){
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        response.headers().set(newUri);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status){
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer("Failure: "+status.toString() + "\r\n", CharsetUtil.UTF_8));
        response.headers().set("CONTENT_TYPE","text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    private static void setContentTypeHeader(HttpResponse response, File file){
        MimetypesFileTypeMap mimeMap = new MimetypesFileTypeMap();
        response.headers().set(mimeMap.getContentType(file.getPath()));
    }


}
