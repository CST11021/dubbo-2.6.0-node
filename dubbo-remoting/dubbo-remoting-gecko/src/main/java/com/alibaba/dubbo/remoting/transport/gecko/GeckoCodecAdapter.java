package com.alibaba.dubbo.remoting.transport.gecko;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Codec2;
import com.taobao.gecko.core.core.CodecFactory;

/**
 * 这里需要将
 *
 * @Author: wanghz
 * @Date: 2021/1/5 11:07 上午
 */
public class GeckoCodecAdapter implements CodecFactory {

    /** 构造器中进行实例化：dubbo的上游是通过SPI机制进行实例化的 */
    private final Codec2 codec;

    private final URL url;

    private final ChannelHandler handler;

    private final int bufferSize;

    public GeckoCodecAdapter(Codec2 codec, URL url, ChannelHandler handler) {
        this.codec = codec;
        this.url = url;
        this.handler = handler;
        int b = url.getPositiveParameter(Constants.BUFFER_KEY, Constants.DEFAULT_BUFFER_SIZE);
        this.bufferSize = b >= Constants.MIN_BUFFER_SIZE && b <= Constants.MAX_BUFFER_SIZE ? b : Constants.DEFAULT_BUFFER_SIZE;
    }

    @Override
    public Encoder getEncoder() {
        return null;
    }

    @Override
    public Decoder getDecoder() {
        return null;
    }


}
