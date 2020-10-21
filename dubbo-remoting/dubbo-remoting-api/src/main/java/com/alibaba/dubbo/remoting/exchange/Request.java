/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting.exchange;

import com.alibaba.dubbo.common.utils.StringUtils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 用于表示一个请求对象
 */
public class Request {

    /** 表示一个消息体：心跳检测时，发起的请求会将请求设置为事件的类型，并将mData设置为HEARTBEAT_EVENT */
    public static final String HEARTBEAT_EVENT = null;

    /** 表示一个消息体：当通道关闭时，会向通道发送一个READONLY_EVENT消息，即触发只读事件 */
    public static final String READONLY_EVENT = "R";

    /** 用于生成Request的ID */
    private static final AtomicLong INVOKE_ID = new AtomicLong(0);

    /** request的ID */
    private final long mId;

    /** 消息体的版本 */
    private String mVersion;

    /** 表示是否具备双向的Request/Response语义，一般在dubbo的信息交换层都是true，在信息传输层，这里的false */
    private boolean mTwoWay = true;

    /** 表示该请求是是否事件类型的请求，正常的接口调用不是事件请求，而像心跳检测请求和通道关闭时粗发的通道只读请求，属于事件类型的请求 */
    private boolean mEvent = false;

    private boolean mBroken = false;

    /** 表示发送消息体 */
    private Object mData;



    public Request() {
        mId = newId();
    }
    public Request(long id) {
        mId = id;
    }

    private static long newId() {
        // getAndIncrement() When it grows to MAX_VALUE, it will grow to MIN_VALUE, and the negative can be used as ID
        return INVOKE_ID.getAndIncrement();
    }

    private static String safeToString(Object data) {
        if (data == null) return null;
        String dataStr;
        try {
            dataStr = data.toString();
        } catch (Throwable e) {
            dataStr = "<Fail toString of " + data.getClass() + ", cause: " +
                    StringUtils.toString(e) + ">";
        }
        return dataStr;
    }

    public long getId() {
        return mId;
    }

    public String getVersion() {
        return mVersion;
    }
    public void setVersion(String version) {
        mVersion = version;
    }

    public boolean isTwoWay() {
        return mTwoWay;
    }
    public void setTwoWay(boolean twoWay) {
        mTwoWay = twoWay;
    }

    public boolean isEvent() {
        return mEvent;
    }
    public void setEvent(String event) {
        mEvent = true;
        mData = event;
    }

    public boolean isBroken() {
        return mBroken;
    }
    public void setBroken(boolean mBroken) {
        this.mBroken = mBroken;
    }

    public Object getData() {
        return mData;
    }
    public void setData(Object msg) {
        mData = msg;
    }

    public boolean isHeartbeat() {
        return mEvent && HEARTBEAT_EVENT == mData;
    }

    public void setHeartbeat(boolean isHeartbeat) {
        if (isHeartbeat) {
            setEvent(HEARTBEAT_EVENT);
        }
    }

    @Override
    public String toString() {
        return "Request [id=" + mId + ", version=" + mVersion + ", twoway=" + mTwoWay + ", event=" + mEvent
                + ", broken=" + mBroken + ", data=" + (mData == this ? "this" : safeToString(mData)) + "]";
    }
}
