package com.tdp.dsp.gateway.model.common;

/**
 * 跨境方向。合规关口、桥接层、审计都按此分流。
 *
 * <ul>
 *   <li>{@link #INBOUND} — 境外 DSP 进入国内可信数据空间</li>
 *   <li>{@link #OUTBOUND} — 国内报文离开本空间，发往 IDS</li>
 * </ul>
 */
public enum Direction {
    INBOUND,
    OUTBOUND
}
