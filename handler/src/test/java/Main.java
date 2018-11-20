import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.OpenSslEngine;
import io.netty.handler.ssl.OpenSslSessionContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.ReferenceCountUtil;

import io.netty.internal.tcnative.SSL;

import java.security.SecureRandom;

public class Main {

    public static void main(String[] args) throws Exception {
        SelfSignedCertificate cert = new SelfSignedCertificate();
        final SslContext sslContext = SslContextBuilder.forServer(cert.certificate(), cert.privateKey())
                .sslProvider(SslProvider.OPENSSL)
                .protocols("TLSv1.3")
                .maxEarlyData(16*1024)
                .build();
        
        byte[] name = new byte[16];
        byte[] hmacKey = new byte[16];
        byte[] aesKey = new byte[16];
        
        SecureRandom random = new SecureRandom();
        random.nextBytes(name);
        random.nextBytes(hmacKey);
        random.nextBytes(aesKey);
        
        OpenSslSessionContext sessionContext = (OpenSslSessionContext)sslContext.sessionContext();
        //sessionContext.setTicketKeys(new OpenSslSessionTicketKey(name, hmacKey, aesKey));

        ChannelInitializer<Channel> handler = new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                
                final OpenSslEngine engine = (OpenSslEngine)sslContext.newEngine(ch.alloc());
                pipeline.addLast(new SslHandler(engine));
                
                pipeline.addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        System.out.println(msg);
                        ReferenceCountUtil.release(msg);
                    }

                    @Override
                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                        int status = engine.getEarlyDataStatus();
    
                        if (status == SSL.SSL_EARLY_DATA_ACCEPTED) {
                            System.out.println("SSL_EARLY_DATA_ACCEPTED: " + status);
                        } else if (status == SSL.SSL_EARLY_DATA_REJECTED) {
                            System.out.println("SSL_EARLY_DATA_REJECTED: " + status);
                            
                        } else if (status == SSL.SSL_EARLY_DATA_NOT_SENT) {
                            System.out.println("SSL_EARLY_DATA_NOT_SENT: " + status);
                            
                        } else {
                            System.out.println("UNKNOWN: " + status);
                        }
                        ctx.fireUserEventTriggered(evt);
                    }
                });
            }
        };

        ServerBootstrap bootstrap = new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .group(new NioEventLoopGroup())
                .option(ChannelOption.SO_REUSEADDR, true)
                .childHandler(handler);

        bootstrap.bind(11000).sync();
    }
}
