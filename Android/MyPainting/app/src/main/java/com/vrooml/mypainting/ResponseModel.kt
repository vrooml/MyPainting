package com.vrooml.mypainting

/**
 * 通用的请求返回
 * @param <T>
</T> */
class ResponseModel<T> {
    var code = 0
    var data: T? = null
    var msg: String? = null
}