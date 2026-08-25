/**
 * 跨服务契约层（api）。
 *
 * <p>本包承载 OpenFeign 客户端接口与共享 DTO（TD §3.2 / §8.2），
 * 是服务间调用的唯一契约。各业务服务只能通过本包的 Feign 接口互相调用，
 * 禁止直接引用对方 module 的内部类。</p>
 *
 * <p>子包按业务域划分（ums / workspace / model / kb / agent / tool /
 * conv / billing / obs / notify），每子包内含两类内容：</p>
 * <ul>
 *   <li>{@code *.api}：OpenFeign 客户端接口（{@code @FeignClient}）</li>
 *   <li>{@code *.dto}：共享请求/响应 DTO</li>
 * </ul>
 *
 * <p>各业务域的契约在对应开发阶段（阶段 3 UMS 起）逐步补充。</p>
 */
package com.insightengine.api;
