package com.vrooml.mypainting

class PaintingStyle {
    lateinit var name:String
    lateinit var imageUrl:String

    constructor(name: String, imageUrl: String) {
        this.name = name
        this.imageUrl = imageUrl
    }
}