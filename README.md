# SpinalMNIST

本项目使用Spinal HDL实现了卷积神经网络，目前使用的数据集为MNIST手写数字。



卷积的思路就是使用两行LineBuffer得到3x3的卷积窗口，将权重全展开后直接读取使用。



run:

```
git clone https://github.com/androny1012/SpinalMNIST.git
sbt run
```

本项目有三个运行选项：

```
ConvSim - Hardware simulation 硬件RTL仿真，可生成vcd/fst波形
GoldenLenet - Pure Software  纯软件网络仿真，和python软件结果对齐
MnistTop - Generate Verilog  生成Verilog代码
```



修改ConvTest.scala中的test_num以改变仿真图片数目，调试时1到2张图片，并开启波形即可。注意仿真全测试集（10000张图片）时，千万不要开启波形.withWave，血的教训。

Verilator仿真速度还可以，10000张图片全部仿真不到两分钟，仿真准确率88.12%

<img src=".\readme.assets\image-20230624102325391.png" alt="image-20230624102325391" style="zoom: 50%;" />



本项目生成的Verilog已经使用Xilinx ZYNQ器件进行上板部署验证，因为用的是Spinal中的FLOW接口，可以改一改以兼容AXI-Stream，使用AXI-DMA在ZYNQ上进行部署。

<img src=".\readme.assets\image-20230624101125388.png" alt="image-20230624102325391" style="zoom: 50%;" />

时序通过，资源方面由于没有使用DSP对乘法进行计算，导致资源消耗偏多，还有待优化。

<img src=".\readme.assets\image-20230624101147842.png" alt="image-20230624102325391" style="zoom: 50%;" />


准确率88.56%（？比仿真还高，可能是操作没完全对齐），平均每次推理耗时约0.7ms。

<img src=".\readme.assets\8d05aafe82ad590ae10b97a5fbf2a71.png" alt="8d05aafe82ad590ae10b97a5fbf2a71" style="zoom:33%;" />



本项目更多作为Spinal HDL入门的练手之作，很多特性没有用好，也没有专门针对FPGA进行优化和修改。

ref:

1. https://github.com/SpinalHDL/SpinalTemplateSbt 项目模板

2. https://github.com/yportne13/SpinalResNet 参考其testbench和代码框架

3. https://github.com/adamgallas/fpga_accelerator_yolov3tiny 参考其卷积架构

