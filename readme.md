本项目使用Spinal HDL实现了卷积神经网络，目前使用的数据集为MNIST手写数字

git clone https://github.com/SpinalHDL/SpinalTemplateSbt.git

run:

    sbt run

本项目有三个运行选项：

    ConvSim - Hardware simulation 硬件RTL仿真，可生成vcd/fst波形
    GoldenLenet - Pure Software   纯软件网络仿真，和python软件结果对齐
    MnistTop - Generate Verilog   生成Verilog代码

ref:

- https://github.com/SpinalHDL/SpinalTemplateSbt 项目模板
- https://github.com/yportne13/SpinalResNet 参考其testbench和代码框架
- https://github.com/adamgallas/fpga_accelerator_yolov3tiny 参考其卷积架构

