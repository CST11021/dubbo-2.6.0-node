package com.alibaba.dubbo.remoting.transport.gecko;

import com.taobao.gecko.core.command.CommandFactory;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.service.config.WireFormatType;

/**
 * @Author: wanghz
 * @Date: 2020/12/9 9:40 上午
 */
public class GeckoWireFormatType extends WireFormatType {

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public CodecFactory newCodecFactory() {
        return null;
    }

    @Override
    public CommandFactory newCommandFactory() {
        return null;
    }

    @Override
    public String name() {
        return null;
    }

}
