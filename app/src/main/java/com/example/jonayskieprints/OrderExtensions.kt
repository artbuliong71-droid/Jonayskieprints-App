package com.example.jonayskieprints

import com.example.jonayskieprints.model.UserOrder

fun Order.toUserOrder(): UserOrder = UserOrder().apply {
    orderId      = this@toUserOrder.id
    service      = this@toUserOrder.serviceType
    serviceType  = this@toUserOrder.serviceType
    status       = this@toUserOrder.status
    date         = this@toUserOrder.date
    price        = this@toUserOrder.price
    fileName     = this@toUserOrder.fileName
}
