package org.nyanya.simplemusixbox

import java.io.File
import java.util.*

class MusicIterator(base: String) {
    var dir = File(base)
    var history: Stack<Pair<File, Int>> = Stack()
    var index = 0

    init {

    }

    // GO DEPTH FIRST DIR ALWAYS FIRST

    fun goForward(item: String) {
        var idir = File(dir, item)
        history.push(Pair(dir, index))
        index = 0
        dir = idir
    }

    fun nextTrack() {
        var l = dir.list()
        l.sort()
        if (index >= l.size) {

        }
    }
    fun nextRecord() {

    }
}