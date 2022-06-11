这是一个自主实现大疆安卓SDK V4版航点任务功能的DEMO，主体部分来自于大疆的官方[航点任务示例](https://github.com/DJI-Mobile-SDK-Tutorials/Android-GSDemo-Gaode-Map)，然后自己实现了航点任务的部分功能。


## 为什么

因为大疆的移动端SDK的航点任务不支持部分机型，在WaypointMission文档中有说：

> It is not supported by Mavic Pro when using WiFi connection. It is not supported by Spark, Mavic Mini, DJI Mini 2, DJI Mini SE, Mavic Air 2, DJI Air 2S and Matrice 300 RTK.

本来我以为他的WaypointV2Mission会支持的，结果他的WaypointV2Mission文档中说：

> It is only supported by MATRICE_300_RTK.

所以其他的机器就是孤儿呗，只好自己实现一个了。

## 实现方式

其实实现的思路倒是蛮简单的，就是用虚拟摇杆控制着朝着目标点飞就完事了，到了一个点就飞下一个点。除了实现的简单粗暴怎么方便怎么来，没有按照官方使用WaypointMissionOperator来控制任务的方式来，不优雅，最大的缺点就是这个任务不能飞太远，得保持无人机和遥控器一直连接着，遥控器得一直给飞机发飞行指令。