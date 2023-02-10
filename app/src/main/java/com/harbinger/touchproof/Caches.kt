package com.harbinger.touchproof

import com.tencent.mmkv.MMKV

/**
 * Created by acorn on 2023/1/1.
 */
object Caches {
    private val mmkv = MMKV.mmkvWithID("CoverCache", MMKV.MULTI_PROCESS_MODE)

    var width by mmkv.int()
    var height by mmkv.int()
    var x by mmkv.int()
    var y by mmkv.int()
}