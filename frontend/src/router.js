
import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router);


import VideoManager from "./components/videoManager"

import RentalManager from "./components/rentalManager"

import PayManager from "./components/payManager"
import RefundManager from "./components/refundManager"


import  from "./components/"
export default new Router({
    // mode: 'history',
    base: process.env.BASE_URL,
    routes: [
            {
                path: '/video',
                name: 'videoManager',
                component: videoManager
            },

            {
                path: '/rental',
                name: 'rentalManager',
                component: rentalManager
            },

            {
                path: '/pay',
                name: 'payManager',
                component: payManager
            },
            {
                path: '/refund',
                name: 'refundManager',
                component: refundManager
            },


            {
                path: '/',
                name: '',
                component: 
            },


    ]
})
