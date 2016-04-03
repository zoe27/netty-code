package netty.http;

import io.netty.bootstrap.ServerBootstrap; 
import io.netty.channel.ChannelInitializer; 
import io.netty.channel.ChannelPipeline; 
import io.netty.channel.EventLoopGroup; 
import io.netty.channel.nio.NioEventLoopGroup; 
import io.netty.channel.socket.SocketChannel; 
import io.netty.channel.socket.nio.NioServerSocketChannel; 
import io.netty.handler.codec.http.HttpObjectAggregator; 
import io.netty.handler.codec.http.HttpRequestDecoder; 
import io.netty.handler.codec.http.HttpResponseEncoder; 
import io.netty.handler.codec.http.HttpServerCodec; 
   
public class HttpProtobufServer { 
    public static final String IP = "127.0.0.1"; 
    public static final int PORT = 8080; 
    //private static MessageLite lite=AddressBookProtos.AddressBook.getDefaultInstance(); 
    /**���ڷ��䴦��ҵ���̵߳��߳������ */ 
    protected static final int BIZGROUPSIZE = Runtime.getRuntime().availableProcessors()*2; //Ĭ�� 
    /** ҵ������̴߳�С*/ 
    protected static final int BIZTHREADSIZE = 4; 
    private static final EventLoopGroup bossGroup = new NioEventLoopGroup(BIZGROUPSIZE); 
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup(BIZTHREADSIZE); 
    protected static void run() throws Exception { 
        ServerBootstrap b = new ServerBootstrap(); 
        b.group(bossGroup, workerGroup); 
        b.channel(NioServerSocketChannel.class); 
        b.childHandler(new ChannelInitializer<SocketChannel>() { 
            @Override 
            public void initChannel(SocketChannel ch) throws Exception { 
                ChannelPipeline pipeline = ch.pipeline();                
                pipeline.addLast("decoder", new HttpRequestDecoder()); 
                /**usually we receive http message infragment,if we want full http message,
                 * we should bundle HttpObjectAggregator and we can get FullHttpRequest��
                 * ����ͨ�����յ�����һ��httpƬ�Σ����Ҫ����������һ��������������ݣ�������Ҫ��HttpObjectAggregator��Ȼ������
                 * �Ϳ����յ�һ��FullHttpRequest-��һ��������������Ϣ��
                **/ 
                pipeline.addLast("servercodec",new HttpServerCodec()); 
                pipeline.addLast("aggegator",new HttpObjectAggregator(1024*1024*64));//���建�������� 
                pipeline.addLast(new HttpProtobufServerHandler()); 
                pipeline.addLast("responseencoder",new HttpResponseEncoder()); 
            } 
        }); 
        b.bind(IP, PORT).sync(); 
        System.out.println("TCP������������"); 
    } 
   
    protected static void shutdown() { 
        workerGroup.shutdownGracefully(); 
        bossGroup.shutdownGracefully(); 
    } 
   
    public static void main(String[] args) throws Exception { 
        System.out.println("��ʼ����http������..."); 
        HttpProtobufServer.run(); 
//      TcpServer.shutdown(); 
    } 
}
