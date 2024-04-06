package dev.imprex.zirconium.util;

import java.util.concurrent.ExecutionException;

import dev.imprex.zirconium.resources.ResourcePackBuilder.ResourcePack;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.DefaultThreadFactory;

@Sharable
public class ResourcePackServer extends SimpleChannelInboundHandler<FullHttpRequest> {

	private final ResourcePack resourcePack;

	private Channel channel;

	public ResourcePackServer(ResourcePack resourcePack, int port) {
		this.resourcePack = resourcePack;	

		this.channel = new ServerBootstrap()
				.group(new NioEventLoopGroup(1, new DefaultThreadFactory("resource-pack-server", true)))
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<Channel>() {

					@Override
					protected void initChannel(Channel channel) throws Exception {
						channel.pipeline()
							.addLast(new HttpServerCodec())
							.addLast(new HttpObjectAggregator(65536))
							.addLast(ResourcePackServer.this);
					}
				}).localAddress(port).bind().syncUninterruptibly().channel();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		if (!request.decoderResult().isSuccess() || !request.method().equals(HttpMethod.GET)) {
			FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
					HttpResponseStatus.BAD_REQUEST);
			response.headers()
					.set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")
					.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
			ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
			return;
		}
		
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.headers().set(HttpHeaderNames.CONTENT_LENGTH, this.resourcePack.data().length)
			.set(HttpHeaderNames.CONTENT_TYPE, "application/zip")
			.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

		ctx.write(response);
		ctx.write(Unpooled.wrappedBuffer(this.resourcePack.data()));
		ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
			.addListener(ChannelFutureListener.CLOSE);
	}

	public void close() {
		try {
			this.channel.close().syncUninterruptibly().get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
}
