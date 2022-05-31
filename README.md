# MyPainting
一个图像风格迁移的安卓端APPdemo

技术栈：

安卓端采用CameraX实现拍照功能，使用Kotlin语言编写。

服务器端使用Flask框架实现了简单的后端逻辑，算法部分含有已训练的十余种模型。

算法采用[Pytorch/example](https://github.com/pytorch/examples/tree/main/fast_neural_style)中实现的基于离线模型的图像风格迁移算法进行修改，采用VGG19替换原有的VGG16网络，并相应对损失函数进行修改。

对应的论文arxiv地址:[Perceptual Losses for Real-Time Style Transfer and Super-Resolution](https://arxiv.org/abs/1603.08155)
