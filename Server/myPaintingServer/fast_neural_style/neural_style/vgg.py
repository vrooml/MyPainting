from collections import namedtuple

import torch
from torchvision import models


class Vgg19(torch.nn.Module):
    def __init__(self, requires_grad=False):
        super(Vgg19, self).__init__()
        vgg_pretrained_features = models.vgg19(pretrained=True).features
        self.slice1 = torch.nn.Sequential()
        self.slice2 = torch.nn.Sequential()
        self.slice3 = torch.nn.Sequential()
        self.slice4 = torch.nn.Sequential()
        self.slice5 = torch.nn.Sequential()
        for x in range(4):
            self.slice1.add_module(str(x), vgg_pretrained_features[x])
        for x in range(4, 9):
            self.slice2.add_module(str(x), vgg_pretrained_features[x])
        for x in range(9, 14):
            self.slice3.add_module(str(x), vgg_pretrained_features[x])
        for x in range(14, 23):
            self.slice4.add_module(str(x), vgg_pretrained_features[x])
        for x in range(23, 32):
            self.slice5.add_module(str(x), vgg_pretrained_features[x])
        if not requires_grad:
            for param in self.parameters():
                param.requires_grad = False

    def forward(self, X):
        h = self.slice1(X)
        h_relu1_2 = h
        h = self.slice2(h)
        h_relu2_2 = h
        h = self.slice3(h)
        h_relu3_4 = h
        h = self.slice4(h)
        h_relu4_4 = h
        h = self.slice5(h)
        h_relu5_4 = h
        vgg_outputs = namedtuple("VggOutputs", ['relu1_2', 'relu2_2', 'relu3_4', 'relu4_4', 'relu5_4'])
        out = vgg_outputs(h_relu1_2, h_relu2_2, h_relu3_4, h_relu4_4, h_relu5_4)
        return out